package ca.uvic.chisel.atlantis.benchmarkRevisedSampling;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class BenchMarkRunner {

	
	public static final String DEFAULT_PASSWORD = "root";
	public static final String DEFAULT_USERNAME = "root";
	public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/";
	public static final String DEFAULT_DATABASE = "benchmark_0_1_7";
	protected static final String FILE_DATA_STATUS_TABLE = "fileDataTableStatus";
	protected static final String FILE_META_DATA_TABLE = "fileMetaData";
	private static Connection connection;
	
	private static final int branchOutFactor = 1000;
	private static IdGenerator idGenerator;
	private static PreparedStatement findLineMemRefStatement;
	
	private static Random random = new Random();
	
	// args contain a list of files, the last arg is a double representing the sample percent
	public static void main(String[] args) throws Exception {
		idGenerator = new IdGenerator(branchOutFactor, 4);
		run(args);
	}


	private static void run(String[] inputArgs) throws Exception {
		
		Class.forName("com.mysql.jdbc.Driver");
		connection = DriverManager.getConnection (DEFAULT_URL + DEFAULT_DATABASE, DEFAULT_USERNAME, DEFAULT_PASSWORD);
		
//		String[] args = inputArgs;
		// 10 percentiles, 30 repeats of each
		String[] args = {"10", "30",
				"50M.trace",
//				"100M.trace",
//				"500M.trace",
				"1G.trace",
//				"5G.trace",
		};
		
		// Number of different line quantiles to get in each file is 1st argument
		int fraction = Integer.parseInt(args[0]);
		// Number of times to sample (with some variation in exact line number) is 2nd argument
		int repeats = Integer.parseInt(args[1]);
		
		List<String> fileNames = new ArrayList<>();
		
		// DB does indeed allow dots in the table names now
		for(int i=2; i <= args.length - 1; i++) {
			fileNames.add(args[i]); //.replace(".", "_"));
		}
		
		Map<String, List<String>> fileNamesToOutput = new HashMap<>();
		
		for(String fileName : fileNames) {
			
			fileNamesToOutput.put(fileName, new ArrayList<String>());
			getFileStats(fileName, fraction, repeats, fileNamesToOutput.get(fileName));
		}
		
		outputResults(fraction, fileNamesToOutput);
	}


	private static void outputResults(double fraction,
			Map<String, List<String>> fileNamesToOutput) {
		
		System.out.print("Results for files: \t");
		for(Entry<String, List<String>> entry : fileNamesToOutput.entrySet()) {
			System.out.print(entry.getKey() + "\t");
		}
		System.out.println();
		System.out.println();
		
//		int numItems = 4;
		for(Entry<String, List<String>> fileEntry : fileNamesToOutput.entrySet()) {
			for(String sampleEntry: fileEntry.getValue()){
				System.out.print(fileEntry.getKey()+"\t");
				System.out.print(sampleEntry + "\t");
				System.out.println();
			}
//			for(int i=0; i<numItems * (int)(fraction +1); i++) {
//				if(i % numItems == 0){
//					System.out.print(entry.getKey()+"\t");
//				}
//				System.out.print(entry.getValue().get(i) + "\t");
//				if(i % numItems == numItems - 1){
//					System.out.println();
//				}
//			}
		}
	}
	
	
	public static void getFileStats(String fileName, int denom, int sampleLimit, List<String> strings) throws Exception {
		long numLines = getNumLinesInFile(fileName) + 1;
		
		String tableName = "`"+"mrd_"+fileName+"`";
		
		double numLinesProportion = (double)numLines / denom;
		
		double spot = 0;
		long line = 0;
		int quantileCounter = 0;
		while(line <= numLines) {
			
			int sampleCounter = 1;

			ArrayList<Long> sampleTimes = new ArrayList<>();
			ArrayList<Long> sampleLines = new ArrayList<>();
			ArrayList<Long> sampleMemoryDeltaCount = new ArrayList<>();
			ArrayList<Long> sampleMemoryDeltaSizes = new ArrayList<>();
			
			while(sampleCounter <= sampleLimit){
				long searchLine = line;
				if(line ==0){
					line = 1000;
				}
				if(line != 0) {
					double spread = (random.nextDouble() - 0.5) * 1000;
					searchLine += Math.round(spread);
				}
				
				findLineMemRefStatement = connection.prepareStatement("SELECT * FROM " + tableName +  " WHERE id >= ? AND id < ?");
				
				long startTime = System.currentTimeMillis();
//				System.out.println("searchLine:"+searchLine);
				List<String> deltas = getNonRootMemRefsAsync((int)searchLine);
				
				long finalTime = System.currentTimeMillis() - startTime;
				
				if((finalTime/1000)>200) {
					// Just a debug point?
					int x = 5;
				}
				
				// Get the number of nodes at each level in the results
				for(String delta: deltas){
					// Debug and see what we have here...
					int x = 5;
					break;
				}
				
				long count = deltas.size();
				long bytes = 0;
				for(String delta: deltas){
					bytes += delta.getBytes("UTF-8").length;
				}
				
				// TODO Grab the levels from the results.
				// TODO Is it ok to take times from Java rather than the DB?
				strings.add("Sample:\t" + sampleCounter
				+"\t"+"Time:\t" + finalTime
				+"\t"+"Line:\t" + searchLine
				+"\t"+"# Deltas:\t" + count
				+"\t"+"Byte Size of Deltas:\t" + bytes);
				
				sampleTimes.add(finalTime);
				sampleLines.add(searchLine);
				sampleMemoryDeltaCount.add(count);
				sampleMemoryDeltaSizes.add(bytes);
						
				sampleCounter++;
			}
			
			
			
			double sampleTimeMean = computeMean(sampleTimes);
			double sampleTimeStandardDeviation = computeStandardDeviation(sampleTimes, sampleTimeMean);
			
			double sampleLineMean = computeMean(sampleLines);
			double sampleLineStandardDeviation = computeStandardDeviation(sampleLines, sampleLineMean);
			
			
			double sampleMemoryDeltaCountMean = computeMean(sampleMemoryDeltaCount);
			double sampleMemoryDeltaCountMin = computeMin(sampleMemoryDeltaCount);
			double sampleMemoryDeltaCountMax = computeMax(sampleMemoryDeltaCount);
			double sampleMemoryDeltaCountStandardDeviation = computeStandardDeviation(sampleMemoryDeltaCount, sampleMemoryDeltaCountMean);
			
			double sampleMemoryDeltaBytesMean = computeMean(sampleMemoryDeltaSizes);
			double sampleMemoryDeltaBytesMin = computeMin(sampleMemoryDeltaSizes);
			double sampleMemoryDeltaBytesMax = computeMax(sampleMemoryDeltaSizes);
			double sampleMemoryDeltaBytesStandardDeviation = computeStandardDeviation(sampleMemoryDeltaSizes, sampleMemoryDeltaBytesMean);
			
			// Line averages
			strings.add("Total over # Samples:\t" + sampleTimes.size()
					+"\t"+"For Quantile\t"+quantileCounter+"/"+denom
					+"\t"+"For Line\t"+line
					
					+"\t"+"Avg Time:\t" + sampleTimeMean
					+"\t"+"Std Dev Time:\t" + sampleTimeStandardDeviation
					
					+"\t"+"Avg Line:\t" + sampleLineMean
					+"\t"+"Std Dev Line:\t" + sampleLineStandardDeviation
					
					+"\t"+"Avg Delta Count:\t" + sampleMemoryDeltaCountMean
					+"\t"+"Std Dev Delta Count:\t" + sampleMemoryDeltaCountStandardDeviation
					+"\t"+"Min Delta Count:\t" + sampleMemoryDeltaCountMin
					+"\t"+"Max Delta Count:\t" + sampleMemoryDeltaCountMax
					
					+"\t"+"Avg Delta Size in Bytes:\t" + sampleMemoryDeltaBytesMean
					+"\t"+"Std Dev Delta Size in Bytes:\t" + sampleMemoryDeltaBytesStandardDeviation
					+"\t"+"Min Delta Size in Bytes:\t" + sampleMemoryDeltaBytesMin
					+"\t"+"Max Delta Size in Bytes:\t" + sampleMemoryDeltaBytesMax

					);
			
			spot += numLinesProportion;
			line = Math.round(spot);
			quantileCounter++;
		}
	}
	
	static double computeMin(ArrayList<Long> numbers){
		double min = Double.MAX_VALUE;
		for(Long time: numbers){
			if(min > time)
			min = time;
		}
		return min;
	}
	
	static double computeMax(ArrayList<Long> numbers){
		double max = Double.MIN_VALUE;
		for(Long time: numbers){
			if(max < time)
				max += time;
		}
		return max;
	}
	
	static double computeMean(ArrayList<Long> numbers){
		double sampleSum = 0;
		for(Long time: numbers){
			sampleSum += time;
		}
		return sampleSum/numbers.size();
	}
	
	static double computeStandardDeviation(ArrayList<Long> numbers, double mean){
		double sampleStdDevSumOfSquares = 0;
		for(Long time: numbers){
			sampleStdDevSumOfSquares += Math.pow((time - mean), 2);
		}
		return Math.sqrt(sampleStdDevSumOfSquares/numbers.size());
	}
	

	protected static List<String> getNonRootMemRefsAsync(int lineNumber) throws SQLException {
		List<String> resList = new ArrayList<String>();
		
		List<Integer> positionDigits = getPositionDigits(lineNumber);
		
		int lastLevelPos = 0;
		
		for(int level = positionDigits.size() - 1; level >= 0; level--) {
			
			int levelDigit = positionDigits.get(level);
			
			int startPos = lastLevelPos * branchOutFactor;
			int startId = idGenerator.getId(level, startPos);
			int endId = startId + (levelDigit);

			// This is because endId is not inclusive, the line number passed in is not included in the results.
			if(startId == endId) {
				continue;
			}
			
			// TODO do a range query and compile some results.
			resList.addAll(getMemoryReferenceForIds(startId, endId));
			
			lastLevelPos = startPos + levelDigit;
		}
		
//		resList = this.sortAndCombineDatabaseResults(resList);
		
		return resList;
	}

	private static List<String> getMemoryReferenceForIds(int startId, int endId) throws SQLException {
		findLineMemRefStatement.setInt(1, startId);
		findLineMemRefStatement.setInt(2, endId);
		
		boolean success = findLineMemRefStatement.execute();
		List<String> rawDatabaseResults = new ArrayList<>();
		
		if(success){
			try(
			ResultSet results = findLineMemRefStatement.getResultSet();
			){
				while(results.next()){
					String string = results.getString("deltaData");
					rawDatabaseResults.add(string);
				}
			}
		}
		
		return rawDatabaseResults;
	}

	protected static List<Integer> getPositionDigits(int lineNumber) {
		List<Integer> positionDigits = new ArrayList<>();
		
		if(lineNumber == 0) {
			positionDigits.add(0);
			return positionDigits;
		}
		
		
		int maxLevel = (int) (Math.log(lineNumber) / Math.log(branchOutFactor));
		
		int curVal = lineNumber;
		
		for(int i=0; i <= maxLevel; i++) {
			int levelDigit = curVal % branchOutFactor;
			positionDigits.add(levelDigit);
			curVal /= branchOutFactor;
		}
		return positionDigits;
	}
	
	private static long getNumLinesInFile(String fileName) throws Exception {
		String tableName = "`"+"mrd_"+fileName+"`";
		PreparedStatement statement = connection.prepareStatement("SELECT max(endLine) FROM " + tableName);
		statement.execute();
		
		try(
		ResultSet results = statement.getResultSet();
		){
			long numLines = -1;
			if(results.next()) {
				numLines = results.getLong(1);
			}
			
			return numLines;
		}
	}
	
}
