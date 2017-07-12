package ca.uvic.chisel.atlantis.benchmark;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import ca.uvic.chisel.atlantis.database.MemoryDbConnection;

public class BenchMarkRunner {

	
	public static final String DEFAULT_PASSWORD = "root";
	public static final String DEFAULT_USERNAME = "root";
	public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/";
	public static final String DEFAULT_DATABASE = "atlantis";
	protected static final String FILE_DATA_STATUS_TABLE = "fileDataTableStatus";
	protected static final String FILE_META_DATA_TABLE = "fileMetaData";
	private static Connection connection;
	
	private static final int branchOutFactor = 1000;
	private static IdGenerator idGenerator;
	private static PreparedStatement findLineMemRefStatement;
	
	private static Random random = new Random();
	
	// args contain a list of files, the last arg is a double representing the sampel percent
	public static void main(String[] args) throws Exception {
		idGenerator = new IdGenerator(branchOutFactor, 4);
		run(args);
	}


	private static void run(String[] args) throws Exception {
		
		Class.forName("com.mysql.jdbc.Driver");
		connection = DriverManager.getConnection ("jdbc:mysql://localhost:3306/" + "atlantis", "root", "root");
		
		List<String> fileNames = new ArrayList<>();
		
		for(int i=0; i<args.length -1; i++) {
			fileNames.add(args[i].replace(".", "_"));
		}
		
		double fraction = Double.parseDouble(args[args.length - 1]);
		
		Map<String, List<String>> fileNamesToOutput = new HashMap<>();
		
		for(String fileName : fileNames) {
			
			fileNamesToOutput.put(fileName, new ArrayList<String>());
			getFileStats(fileName, fraction, fileNamesToOutput.get(fileName));
		}
		
		outputResults(fraction, fileNamesToOutput);
	}


	private static void outputResults(double fraction,
			Map<String, List<String>> fileNamesToOutput) {
		
		for(Entry<String, List<String>> entry : fileNamesToOutput.entrySet()) {
			System.out.print(entry.getKey() + "\t");
		}
		System.out.println();
		
		for(int i=0; i<3 * (int)(fraction +1); i++) {
			for(Entry<String, List<String>> entry : fileNamesToOutput.entrySet()) {
				System.out.print(entry.getValue().get(i) + "\t");
			}
			System.out.println();
		}
	}
	
	
	public static void getFileStats(String fileName, double denom, List<String> strings) throws Exception {
		long numLines = getNumLinesInFile(fileName) + 1;
		
		double numLinesProportion = (double)numLines / denom;
		
		double spot = 0;
		long line = 0;
		while(line <= numLines) {
			
			long searchLine = line;
			
			if(line != 0) {
				double spread = (random.nextDouble() - 0.5) * 1000;
				searchLine += Math.round(spread);
			}
			
			findLineMemRefStatement = connection.prepareStatement("SELECT * FROM "+ MemoryDbConnection.MEMORY_TABLE_NAME +  " WHERE id >= ? AND id < ?");
			
			long startTime = System.currentTimeMillis();
			
			List<String> deltas = getNonRootMemRefsAsync((int)searchLine);
			
			long finalTime = System.currentTimeMillis() - startTime;
			
			if((finalTime/1000)>200) {
				int x = 5;
			}
			
			int count = deltas.size();
			
			strings.add("Time "+finalTime);
			strings.add("Line "+searchLine);
			strings.add("Count " + count);
			
			
			spot += numLinesProportion;
			line = Math.round(spot);
		}
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
		PreparedStatement statement = connection.prepareStatement("SELECT max(endLine) FROM "+MemoryDbConnection.MEMORY_TABLE_NAME);
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
