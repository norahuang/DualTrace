package ca.uvic.chisel.atlantis.compare;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import ca.uvic.chisel.atlantis.compare.DiffRegion;
import ca.uvic.chisel.atlantis.database.TraceFileLineDbConnection;
import ca.uvic.chisel.bfv.datacache.FileLine;

public class SimpleSQLDiff {
	
	ArrayList<DiffRegion> matchingRegions = new ArrayList<DiffRegion>();
	ArrayList<DiffRegion> differentRegions = new ArrayList<DiffRegion>();
	ArrayList<DiffRegion> allRegions = new ArrayList<DiffRegion>();
	
	ArrayList<ArrayList<DiffRegion>> results = new ArrayList<ArrayList<DiffRegion>>();
  	DiffRegion lastMatchedRegion = new DiffRegion(-1, -1, -1 ,-1);
  	DiffRegion lastDifferentRegion = new DiffRegion(-1, -1, -1 ,-1);
  	
  	boolean lastLineMatched = false;
  	boolean currentLineMatched = false;
  	boolean anyRegions = false; 	
  	
  	int numConsumers;  	
  	int currentConsumer = 0;
  	int referenceRegionSize;
  	int challengerRegionSize;
  	int headPosition;
  	int referenceHeadPosition = 0;
  	
  	long lastReferenceOffset = 0;
  	long lastChallengerOffset = 0;
  	int lastReferenceLength = 0;
  	int lastChallengerLength = 0;
  	long challengerOffsetHead = 0;
  	int challengerLengthHead = 0;
  	int referenceLinePosition = 0;
  	long referenceOffsetMarker = 0;
  	int referenceLengthMarker = 0;
  	int referenceLineMarker = 0;
  	
  	Pattern pattern = Pattern.compile("[a-zA-Z0-9]* ");
  	Matcher matcher;

  	
	List<FileLine> referenceFileLineList;
	List<FileLine> challengerFileLineList;
	
  	String referenceTable;
  	String challengerTable;
  	TraceFileLineDbConnection referenceDB;
  	TraceFileLineDbConnection challengerDB;
  	
	public SimpleSQLDiff(String referenceTable, IPath referenceTablePath, String challengerTable, IPath challengerTablePath, int numConsumers) throws Exception{
		this.referenceTable = referenceTable;
		this.challengerTable = challengerTable;
		this.numConsumers = numConsumers;
		// TODO I needed to get the actual file reference for use with individualized database file per trace.
		// I had to comment this out. The diffs are not production ready at this time,
		// and should be modified to make use of the function call data now available anyway.
		System.out.println("Disabled SqimpleSQLDiff");
//		referenceDB = new TraceFileLineDbConnection(referenceTable, referenceTablePath);
//		challengerDB = new TraceFileLineDbConnection(challengerTable, challengerTablePath);
	}
	
	public ArrayList<ArrayList<DiffRegion>> doTheDiff(IProgressMonitor monitor){
			  
			  System.out.println("Starting the diff");
			  long startTime = System.currentTimeMillis();
			  String referenceLine = "";
			  String challengerLine = "";
			  getRegionSize(numConsumers);
			  FileLine referenceFileLine = null;
			  FileLine challengerFileLine = null;  
			  while(currentConsumer < numConsumers){
				  if(currentConsumer%100==1){
					  System.out.println("Doing consumer number " + currentConsumer);
				  }	 			 
				  anyRegions = false;
				  lastLineMatched = false;
				  currentLineMatched = false;
				  loadChunks(currentConsumer);
				  ListIterator<FileLine> referenceIterator;
				  if(referenceLinePosition != 0){
				//	  int counter = 0;
				//	  int position = referenceLinePosition - challengerRegionSize/2;
					  int position = 0;
					  boolean found = false;
					  for(FileLine line :  referenceFileLineList){
						  if(line.equals(referenceFileLine)){
							  found = true;
							  break;
						  }
						  position++;
					  }
					  if(found){
						  referenceIterator = referenceFileLineList.listIterator(position);
					  }else{
						  referenceIterator = referenceFileLineList.listIterator();
					  }

				  }else{
					  referenceIterator = referenceFileLineList.listIterator();
				  }

				  ListIterator<FileLine> challengerIterator = challengerFileLineList.listIterator();		
				  referenceLinePosition = 0;
				  while(referenceIterator.hasNext())
				  {
					  if(referenceFileLine != null){
						  lastReferenceOffset = referenceFileLine.getLineOffset();
						  lastReferenceLength = referenceFileLine.getLineLength();
					  }
				  
					  referenceFileLine = referenceIterator.next();
					  referenceLine = referenceFileLine.getLineContent();
		//			  if(referenceLinePosition == challengerRegionSize){
		//			  if(headPosition == challengerRegionSize){
		//				  referenceOffsetMarker = referenceFileLine.getLineOffset();
		//				  referenceLengthMarker = referenceFileLine.getLineLength();
		//				  referenceLineMarker = referenceFileLine.getLineNumber();
		//				  break;
		//			  }	
					  while(challengerIterator.hasNext()){
						  if(challengerFileLine != null){
							  lastChallengerOffset = challengerFileLine.getLineOffset();
							  lastChallengerLength = challengerFileLine.getLineLength();
						  }
						  challengerFileLine = challengerIterator.next(); 
						  challengerLine = challengerFileLine.getLineContent();
						  currentLineMatched = false;
						  if(challengerLine.equals(referenceLine)){
				//		  if(maskCompare(referenceLine, challengerLine)){
							  currentLineMatched = true;
							  headPosition = challengerFileLineList.indexOf(challengerFileLine) + 1;
							  challengerOffsetHead = challengerFileLine.getLineOffset();
							  challengerLengthHead = challengerFileLine.getLineLength();
							  if(isMatchingRegionStart()){
								  startNewMatchingRegion(referenceFileLine, challengerFileLine);
							  }
					//		  if(lastDifferentRegion.checkStartValidity()){
					//			  endDifferenceRegion(referenceFileLine, challengerFileLine);
					//		  }
							  if(isDifferenceRegionEnd()){
								  endDifferenceRegion(referenceFileLine, challengerFileLine);
							  }
							  lastLineMatched = true;
							  break;
						  }
						  else{
							  if(isMatchingRegionEnd()){
								  endMatchingRegion(referenceFileLine, challengerFileLine);
							  }
							  if(isDifferenceRegionStart()){
								  startNewDifferenceRegion(referenceFileLine, challengerFileLine);
							  }
							  lastLineMatched = false;
						  }						  
					  }
					  if((headPosition == challengerRegionSize + 1) && (currentConsumer != numConsumers -1)){
						  referenceOffsetMarker = referenceFileLine.getLineOffset();
						  referenceLengthMarker = referenceFileLine.getLineLength();
						  referenceLineMarker = referenceFileLine.getLineNumber();
					//	  referenceHeadPosition = referenceFileLineList.indexOf(referenceFileLine);
						  break;
					  }	
					  challengerIterator = challengerFileLineList.listIterator(headPosition);	
					//  challengerIterator = challengerFileLineList.listIterator();
					  referenceLinePosition++;
			  }
				    currentConsumer++;
				    checkForTail(referenceFileLine, challengerFileLine);
				  	monitor.worked(1);			  
			  }
		  long endTime = System.currentTimeMillis();
		  System.out.println("Total elapsed time in milliseconds: " + (endTime-startTime));
		  System.out.println("Total elapsed time in seconds: " + (endTime-startTime)/1000.0);
		  System.out.println("Total elapsed time in minutes: " + (endTime-startTime)/1000.0/60.0);
		  System.out.println("Total elapsed time in hours: " + (endTime-startTime)/1000.0/60.0/60.0);
		  		  
		  results.add(matchingRegions);
		  results.add(differentRegions);
		  createAllRegionsList();
		  results.add(allRegions);
		  return results;

	}
	
	public boolean maskCompare(String referenceLine, String challengerLine){
		matcher = pattern.matcher(referenceLine);
		String newReferenceLine = matcher.replaceFirst("");
		matcher = pattern.matcher(challengerLine);
		String newChallengerLine = matcher.replaceFirst("");
		if(newReferenceLine.equals(newChallengerLine)){
			return true;
		}		
		return false;
	}
	
/*	public void createAllRegionsList(){
		ListIterator<DiffRegion> matchesIterator = matchingRegions.listIterator();
		ListIterator<DiffRegion> differenceIterator = differentRegions.listIterator();
	//	DiffRegion matchesRegion;
	//	DiffRegion differenceRegion;
		if(matchingRegions.size() > 0 && differentRegions.size() > 0){

	//		matchesRegion =  matchesIterator.next();
	//		differenceRegion =  differenceIterator.next();
			while(matchesIterator.hasNext()){
				allRegions.add(matchesIterator.next());
			}
			while(differenceIterator.hasNext()){
				allRegions.add(differenceIterator.next());
			}
		}
	}*/
	
	public void createAllRegionsList(){
		ListIterator<DiffRegion> matchesIterator = matchingRegions.listIterator();
		ListIterator<DiffRegion> differenceIterator = differentRegions.listIterator();
		DiffRegion matchesRegion;
		DiffRegion differenceRegion;
		if(matchingRegions.size() > 0 && differentRegions.size() > 0){

			matchesRegion =  matchesIterator.next();
			differenceRegion =  differenceIterator.next();
			while(matchesIterator.hasNext() || differenceIterator.hasNext()){
				if(matchesRegion != null && differenceRegion != null){
					if((matchesRegion.getLeftStartLine() < differenceRegion.getLeftStartLine()) &&
							matchesRegion.getRightStartLine() < differenceRegion.getRightStartLine()){
						allRegions.add(matchesRegion);
						matchesRegion = matchesIterator.next();
					}
					else if((differenceRegion.getLeftStartLine() < matchesRegion.getLeftStartLine()) &&
							differenceRegion.getRightStartLine() < matchesRegion.getRightStartLine()){
						allRegions.add(differenceRegion);
						differenceRegion = differenceIterator.next();
					}
					else if(differenceRegion.getLeftStartLine() == matchesRegion.getLeftStartLine()){
						if(matchesRegion.getRightStartLine() < differenceRegion.getRightStartLine()){
							allRegions.add(matchesRegion);
							matchesRegion = matchesIterator.next();
						}
						else{
							allRegions.add(differenceRegion);
							differenceRegion = differenceIterator.next();
						}
					}
					else if(differenceRegion.getRightStartLine() == matchesRegion.getRightStartLine()){
						if(matchesRegion.getLeftStartLine() < differenceRegion.getLeftStartLine()){
							allRegions.add(matchesRegion);
							matchesRegion = matchesIterator.next();
						}
						else{
							allRegions.add(differenceRegion);
							differenceRegion = differenceIterator.next();
						}
					}
				}
				else if(matchesIterator.hasNext() && !differenceIterator.hasNext()){
					allRegions.add(matchesRegion);
					matchesRegion = matchesIterator.next();
					differenceRegion = null;
				}
				else if(!matchesIterator.hasNext() && differenceIterator.hasNext()){
					allRegions.add(differenceRegion);
					differenceRegion = differenceIterator.next();
					matchesRegion = null;
				}
			}
			if(matchesRegion != null && differenceRegion != null){
				if((matchesRegion.getLeftStartLine() < differenceRegion.getLeftStartLine()) &&
						matchesRegion.getRightStartLine() < differenceRegion.getRightStartLine()){
					allRegions.add(matchesRegion);
					allRegions.add(differenceRegion);
				}
				else if((differenceRegion.getLeftStartLine() < matchesRegion.getLeftStartLine()) &&
						differenceRegion.getRightStartLine() < matchesRegion.getRightStartLine()){
					allRegions.add(differenceRegion);
					allRegions.add(matchesRegion);
				}
				else if(differenceRegion.getLeftStartLine() == matchesRegion.getLeftStartLine()){
					if(matchesRegion.getRightStartLine() < differenceRegion.getRightStartLine()){
						allRegions.add(matchesRegion);
						matchesRegion = matchesIterator.next();
					}
					else{
						allRegions.add(differenceRegion);
						allRegions.add(matchesRegion);
					}
				}
				else if(differenceRegion.getRightStartLine() == matchesRegion.getRightStartLine()){
					if(matchesRegion.getLeftStartLine() < differenceRegion.getLeftStartLine()){
						allRegions.add(matchesRegion);
						matchesRegion = matchesIterator.next();
					}
					else{
						allRegions.add(differenceRegion);
						allRegions.add(matchesRegion);
					}
				}
			}
		}		
	}
	
	public void startNewMatchingRegion(FileLine referenceFileLine, FileLine challengerFileLine){
		  anyRegions = true;
		  lastMatchedRegion.setLeftStartLine(referenceFileLine.getLineNumber());
		  lastMatchedRegion.setRightStartLine(challengerFileLine.getLineNumber());
		  lastMatchedRegion.setLeftOffset(referenceFileLine.getLineOffset());
		  lastMatchedRegion.setRightOffset(challengerFileLine.getLineOffset());
	}
	
	public void startNewDifferenceRegion(FileLine referenceFileLine, FileLine challengerFileLine){
		  anyRegions = true;
		  lastDifferentRegion.setLeftStartLine(referenceFileLine.getLineNumber());
		  lastDifferentRegion.setRightStartLine(challengerFileLine.getLineNumber());
		  lastDifferentRegion.setLeftOffset(referenceFileLine.getLineOffset());
		  lastDifferentRegion.setRightOffset(challengerFileLine.getLineOffset());
	}
	
	public void endMatchingRegion(FileLine referenceFileLine, FileLine challengerFileLine){
		  lastMatchedRegion.setLeftEndLine(referenceFileLine.getLineNumber() - 1);
		  lastMatchedRegion.setRightEndLine(challengerFileLine.getLineNumber() - 1);
		  lastMatchedRegion.setLeftLength((int)lastReferenceOffset + lastReferenceLength - (int)lastMatchedRegion.getLeftOffset());
		  lastMatchedRegion.setRightLength((int)lastChallengerOffset + lastChallengerLength - (int)lastMatchedRegion.getRightOffset());
//		  checkEmptyRegion(lastMatchedRegion);
		  if(matchingRegions.size() > 0){
			  checkAdjacency(lastMatchedRegion, matchingRegions);
		  }else{
			  lastMatchedRegion.setMatchState(true);
			  matchingRegions.add(lastMatchedRegion);
		  }
		  lastMatchedRegion = new DiffRegion(-1, -1, -1, -1);
	}
	
	public void endDifferenceRegion(FileLine referenceFileLine, FileLine challengerFileLine){
		  lastDifferentRegion.setLeftEndLine(referenceFileLine.getLineNumber() - 1);
		  lastDifferentRegion.setRightEndLine(challengerFileLine.getLineNumber() -1 );
		  lastDifferentRegion.setLeftLength((int)lastReferenceOffset + lastReferenceLength - (int)lastDifferentRegion.getLeftOffset());
		  lastDifferentRegion.setRightLength((int)lastChallengerOffset + lastChallengerLength - (int)lastDifferentRegion.getRightOffset());
//		  checkEmptyRegion(lastDifferentRegion);
		  if(differentRegions.size() > 0){
			checkAdjacency(lastDifferentRegion, differentRegions);
		  }
		  else{
			lastDifferentRegion.setMatchState(false);
			differentRegions.add(lastDifferentRegion);
		  }			 			
		  lastDifferentRegion = new DiffRegion(-1,-1,-1,-1);
	}
	
/*	public void startNewRegion(FileLine referenceFileLine, FileLine challengerFileLine){
		  anyRegions = true;
		  lastMatchedRegion.setLeftStartLine(referenceFileLine.getLineNumber());
		  lastMatchedRegion.setRightStartLine(challengerFileLine.getLineNumber());
		  lastMatchedRegion.setLeftOffset(referenceFileLine.getLineOffset());
		  lastMatchedRegion.setRightOffset(challengerFileLine.getLineOffset());
		  if(lastDifferentRegion.checkStartValidity()){
			  lastDifferentRegion.setLeftEndLine(referenceFileLine.getLineNumber() - 1);
			  lastDifferentRegion.setRightEndLine(challengerFileLine.getLineNumber() -1 );
			  lastDifferentRegion.setLeftLength((int)lastReferenceOffset + lastReferenceLength - (int)lastDifferentRegion.getLeftOffset());
			  lastDifferentRegion.setRightLength((int)lastChallengerOffset + lastChallengerLength - (int)lastDifferentRegion.getRightOffset());
			  if(differentRegions.size() > 0){
	 			checkAdjacency(lastDifferentRegion, differentRegions);
			  }
			  else{
				lastDifferentRegion.setMatchState(false);
	 			differentRegions.add(lastDifferentRegion);
			  }			 			
			  lastDifferentRegion = new DiffRegion(-1,-1,-1,-1);
		  }
	}
	
	public void endRegion(FileLine referenceFileLine, FileLine challengerFileLine){
		  lastMatchedRegion.setLeftEndLine(referenceFileLine.getLineNumber() - 1);
		  lastMatchedRegion.setRightEndLine(challengerFileLine.getLineNumber() - 1);
		  lastMatchedRegion.setLeftLength((int)lastReferenceOffset + lastReferenceLength - (int)lastMatchedRegion.getLeftOffset());
		  lastMatchedRegion.setRightLength((int)lastChallengerOffset + lastChallengerLength - (int)lastMatchedRegion.getRightOffset());
		  lastDifferentRegion.setLeftStartLine(referenceFileLine.getLineNumber());
		  lastDifferentRegion.setRightStartLine(challengerFileLine.getLineNumber());
		  lastDifferentRegion.setLeftOffset(referenceFileLine.getLineOffset());
		  lastDifferentRegion.setRightOffset(challengerFileLine.getLineOffset());
		  if(matchingRegions.size() > 0){
			  checkAdjacency(lastMatchedRegion, matchingRegions);
		  }else{
			  lastMatchedRegion.setMatchState(true);
			  matchingRegions.add(lastMatchedRegion);
		  }
		  lastMatchedRegion = new DiffRegion(-1, -1, -1, -1);
	}*/
	
	public void checkForTail(FileLine refernceFileLine, FileLine challengerFileLine){
     	if(anyRegions == true){
		 	if(!matchingRegions.contains(lastMatchedRegion)){
		 		if(lastMatchedRegion.checkStartValidity()){
		 			lastMatchedRegion.setLeftEndLine(refernceFileLine.getLineNumber());
				 	lastMatchedRegion.setRightEndLine(challengerFileLine.getLineNumber());
				 	lastMatchedRegion.setLeftLength((int)referenceOffsetMarker + referenceLengthMarker - (int)lastMatchedRegion.getLeftOffset());
				 	lastMatchedRegion.setRightLength((int)challengerFileLine.getLineOffset() + challengerFileLine.getLineLength() - (int)lastMatchedRegion.getRightOffset());
//				 	checkEmptyRegion(lastMatchedRegion);
					  if(matchingRegions.size() > 0){
						  checkAdjacency(lastMatchedRegion, matchingRegions);
					  }else{
						  lastMatchedRegion.setMatchState(true);
						  matchingRegions.add(lastMatchedRegion);
					  }
			 		lastMatchedRegion = new DiffRegion(-1, -1, -1, -1);
		 		}
		 		if(lastDifferentRegion.checkStartValidity()){
		 			lastDifferentRegion.setLeftEndLine(challengerFileLine.getLineNumber());
		 			lastDifferentRegion.setRightEndLine(challengerFileLine.getLineNumber());
		 			lastDifferentRegion.setLeftLength((int)referenceOffsetMarker + referenceLengthMarker - (int)lastDifferentRegion.getLeftOffset());
				  	lastDifferentRegion.setRightLength((int) lastChallengerOffset + lastChallengerLength - (int)lastDifferentRegion.getRightOffset());
//				  	checkEmptyRegion(lastDifferentRegion);
		 			if(differentRegions.size() > 0){
		 				checkAdjacency(lastDifferentRegion, differentRegions);
		 			}
		 			else{
		 				lastDifferentRegion.setMatchState(false);
		 				differentRegions.add(lastDifferentRegion);
		 			}			 			
		 			lastDifferentRegion = new DiffRegion(-1,-1,-1,-1);
		 		}
		 	}				
	 	}
	}
	
/*	public void checkEmptyRegion(DiffRegion region){
		if(region.getLeftEndLine() - region.getLeftStartLine() < 0){
			region.setLeftStartLine(0);
			region.setLeftEndLine(0);
		}
		if(region.getRightEndLine() - region.getRightStartLine() < 0){
			region.setRightStartLine(0);
			region.setRightEndLine(0);
		}
	}*/

	public void checkAdjacency(DiffRegion targetRegion, ArrayList<DiffRegion> targetRegionList){
		  DiffRegion tailRegion = targetRegionList.get(targetRegionList.size()-1);
		  if(tailRegion.getLeftEndLine() == targetRegion.getLeftStartLine() &&
				  tailRegion.getRightEndLine() == targetRegion.getRightStartLine()){
			  targetRegionList.remove(targetRegionList.size()-1);
			  DiffRegion mergedRegion = new DiffRegion(tailRegion.getRightStartLine(), targetRegion.getRightEndLine(), tailRegion.getLeftStartLine(), targetRegion.getLeftEndLine());
			  mergedRegion.setOffsets(tailRegion.getRightOffset(), (int)targetRegion.getRightOffset() + targetRegion.getRightLength() - (int)tailRegion.getRightOffset(), tailRegion.getLeftOffset(), (int)targetRegion.getLeftOffset() + targetRegion.getLeftLength() - (int)tailRegion.getLeftOffset());
			  mergedRegion.setMatchState(tailRegion.getMatchState());
			  targetRegionList.add(mergedRegion);
		  }
		  else{
			  targetRegion.setMatchState(tailRegion.getMatchState());
			  targetRegionList.add(targetRegion);
		  }
	}
	
	public void getRegionSize(int numConsumers){
		challengerRegionSize = challengerDB.getNumberOfLines()/numConsumers;
		referenceRegionSize = challengerRegionSize*2;	
		//referenceRegionSize = referenceDB.getNumberOfLines()/numConsumers;
	}
	
/*	public void loadChunks(int consumerNumber){
		int referenceStartPoint = referenceRegionSize*consumerNumber;
		referenceFileLineList = new ArrayList<FileLine>();
		int challengerStartPoint = challengerRegionSize*consumerNumber;
		challengerFileLineList = new ArrayList<FileLine>();	
		if(consumerNumber == 0){		
			referenceFileLineList = referenceDB.getTraceFileLineRange(referenceStartPoint, referenceStartPoint + referenceRegionSize);
			challengerFileLineList = challengerDB.getTraceFileLineRange(challengerStartPoint, challengerStartPoint + challengerRegionSize);
		}
		else if(consumerNumber < numConsumers -1){
			referenceStartPoint = challengerStartPoint;
			referenceFileLineList = referenceDB.getTraceFileLineRange(referenceStartPoint, referenceStartPoint + referenceRegionSize);
			challengerFileLineList = challengerDB.getTraceFileLineRange(challengerStartPoint, challengerStartPoint + challengerRegionSize);
		}
		else{			
			int referenceEndOfFile = referenceDB.getNumberOfLines();
			referenceStartPoint = challengerStartPoint;
			referenceFileLineList = referenceDB.getTraceFileLineRange(referenceStartPoint, referenceEndOfFile);	
			int challengerEndOfFile = challengerDB.getNumberOfLines();
			challengerFileLineList = challengerDB.getTraceFileLineRange(challengerStartPoint, challengerEndOfFile);		
		}

	} */
	public void loadChunks(int consumerNumber){
		int referenceStartPoint = referenceRegionSize*consumerNumber;
		referenceFileLineList = new ArrayList<FileLine>();
		int challengerStartPoint = challengerRegionSize*consumerNumber;
		challengerFileLineList = new ArrayList<FileLine>();	
		if(consumerNumber == 0){		
			referenceFileLineList = referenceDB.getFileLineRange(referenceStartPoint, referenceStartPoint + referenceRegionSize);
			challengerFileLineList = challengerDB.getFileLineRange(challengerStartPoint, challengerStartPoint + challengerRegionSize);
		}
		else if(consumerNumber < numConsumers - 1){
			referenceStartPoint = challengerStartPoint - challengerRegionSize/2;
			referenceFileLineList = referenceDB.getFileLineRange(referenceStartPoint, referenceStartPoint + referenceRegionSize);
			challengerFileLineList = challengerDB.getFileLineRange(challengerStartPoint, challengerStartPoint + challengerRegionSize);
		}
		else{			
			int referenceEndOfFile = referenceDB.getNumberOfLines();
			referenceStartPoint = challengerStartPoint - challengerRegionSize;
			referenceFileLineList = referenceDB.getFileLineRange(referenceStartPoint, referenceEndOfFile);	
			int challengerEndOfFile = challengerDB.getNumberOfLines();
			challengerFileLineList = challengerDB.getFileLineRange(challengerStartPoint, challengerEndOfFile);		
		}

	}

	
	public boolean isMatchingRegionStart(){
		if (lastLineMatched == false && currentLineMatched == true){
			return true;
		}
		return false;
	}
	
	public boolean isMatchingRegionEnd(){
		if(lastLineMatched == true && currentLineMatched == false){
			return true;
		}
		return false;
	}
	
	public boolean isDifferenceRegionStart(){
		if(currentLineMatched == false && !lastDifferentRegion.checkStartValidity()){
			return true;
		}
		return false;
	}
	
	public boolean isDifferenceRegionEnd(){
		if(lastLineMatched == false && lastDifferentRegion.checkStartValidity()){
			return true;
		}
		return false;
	}
}
