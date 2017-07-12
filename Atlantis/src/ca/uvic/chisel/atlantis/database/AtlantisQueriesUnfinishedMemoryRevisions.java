package ca.uvic.chisel.atlantis.database;

public class AtlantisQueriesUnfinishedMemoryRevisions {
	/*
	 * While trying to reduce peak memory usage in issue #713, I developed
	 * new line based queries, to facilitate a tree structure that was responsive
	 * to the size of memory being used, rather than being a fixed structure with
	 * set branching counts. The queries would be needed because the current queries
	 * make use of the extremely predictable shape of the tree.
	 * If we need to reduce Gibraltar peak memory usage again, we might need to
	 * follow that path again. This is a set of changes to the schema needed for
	 * thsoe queries. The queries themselves are further below. There are two
	 * versions, one that requires looping over the query in Java, and the other
	 * that uses UNION to combine the components of the query so Java calls only
	 * one such query. They slowed query time from 0.008s to 1.9252s for some test
	 * case (T32_7ze line 2942959), which is too costly. They were not necessarily
	 * completely finished, but given the alternative solution to Gibraltar peak
	 * memory usage (changing some implementation details to commit as soon as
	 * possible, something I thought was already occurring), we will defer development
	 * of those queries for the future.
	 */
//	"CREATE TABLE "+MemoryDbConnection.MEMORY_TABLE_NAME+" ("+
//	" endLine INT NOT NULL,"+
//	" id INT NOT NULL,"+
//	" parentId INT,"+
//	" startLine INT NOT NULL,"+
//	" deltaData LONGBLOB,"+
//	" PRIMARY KEY (endLine, id) "+
//	" ) WITHOUT ROWID";
	
	/*

class SelectMemRefByLineRevisedBase extends TypedQuery {
	IntegerResult id = new IntegerResult("id");
	IntegerResult parentId = new IntegerResult("parentId");
	IntegerResult startLine = new IntegerResult("startLine");
	IntegerResult endLine = new IntegerResult("endLine");
	
	// The deltaData is actually a LONGBLOB in the database, but we put in and take out as a string.
	 
	StringResult deltaData = new StringResult("deltaData");
	
	IntegerParameter endLineOfSpan = new IntegerParameter("endLineOfSpan");
	
	IntegerParameter parentLineLimit = new IntegerParameter("parentLineLimit");
	
}


//  Stopped development on this. See comment above modified schema for memory
//  table for details.
 
@Deprecated
class SelectMemRefByLineRevisedL0 extends SelectMemRefByLineRevisedBase {
	{
		this.skipParameterCheck = true;
		// Revising...need to get from a span, not for a targeted line
		
		//  Fetch all of the top level nodes that are relevant; inner nodes only if they are bigger than the smallest
		//  top level node required; and leaf nodes only if they are bigger than the smallest of their required parents.
		//  Note that the leaf nodes fetched need to go to the smallest parent above, which may be from before the span.
		 
		this.q = 
			" SELECT id, startLine, endLine, deltaData "+
			" FROM memory_snapshot_delta_tree AS L1 "+
			" WHERE "+
			// L0, leaf nodes
			"( endLine <= :"+endLineOfSpan+" AND id >= 0 AND id < 1000000000 "+
			" AND endLine > :"+parentLineLimit+" "+
//			" AND endLine > ( "+
//			"   SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1000000000 AND id < 1001000000 "+
//			"   AND endLine < :"+startLineOfSpan+" "+
//			"    ) "+
			" ) "+
			"";
	}
}
@Deprecated
class SelectMemRefByLineRevisedL1 extends SelectMemRefByLineRevisedBase {
	{
		this.skipParameterCheck = true;
		// Revising...need to get from a span, not for a targeted line
		
		// Fetch all of the top level nodes that are relevant; inner nodes only if they are bigger than the smallest
		// top level node required; and leaf nodes only if they are bigger than the smallest of their required parents.
		// Note that the leaf nodes fetched need to go to the smallest parent above, which may be from before the span.
		
		this.q = 
			" SELECT id, startLine, endLine, deltaData "+
			" FROM memory_snapshot_delta_tree AS L1 "+
			" WHERE "+
			// L1
			" ( endLine <= :"+endLineOfSpan+" AND id >= 1000000000 AND id < 1001000000"+
			" AND endLine > :"+parentLineLimit+" "+
//			" AND endLine > ( "+
//			"    SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1001000000 AND id < 1001001000 "+
//			"    AND endLine < :"+startLineOfSpan+" "+
//			"    ) "+
			" ) "+
			"";
	}
}
@Deprecated
class SelectMemRefByLineRevisedL2 extends SelectMemRefByLineRevisedBase {
	{
		this.skipParameterCheck = true;
		// Revising...need to get from a span, not for a targeted line
		/**
		// Fetch all of the top level nodes that are relevant; inner nodes only if they are bigger than the smallest
		// top level node required; and leaf nodes only if they are bigger than the smallest of their required parents.
		// Note that the leaf nodes fetched need to go to the smallest parent above, which may be from before the span.
		 
		this.q = 
			" SELECT id, startLine, endLine, deltaData "+
			" FROM memory_snapshot_delta_tree AS L1 "+
			" WHERE "+
			// L2
			" ( endLine <= :"+endLineOfSpan+" AND id >= 1001000000 AND id < 1001001000"+
			" AND endLine > :"+parentLineLimit+" "+
//			" AND endLine > ( "+
//			"    SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1001001000 "+
//			"    AND endLine < :"+startLineOfSpan+" "+
//			"    ) "+
			" ) "+
			"";
	}
}
@Deprecated
class SelectMemRefByLineRevisedL3 extends SelectMemRefByLineRevisedBase {
	{
		this.skipParameterCheck = true;
		// Revising...need to get from a span, not for a targeted line
		
		// Fetch all of the top level nodes that are relevant; inner nodes only if they are bigger than the smallest
		// top level node required; and leaf nodes only if they are bigger than the smallest of their required parents.
		// Note that the leaf nodes fetched need to go to the smallest parent above, which may be from before the span.
		
		this.q = 
			" SELECT id, startLine, endLine, deltaData "+
			" FROM memory_snapshot_delta_tree AS L1 "+
			" WHERE "+
			// L3
			" (endLine <= :"+endLineOfSpan+"  AND id >= 1001001000 "+
			" AND :"+parentLineLimit+" "+ // placeholder, useless, easier use though
//			"  AND :"+startLineOfSpan+" "+
			" ) "+
			"";
	}
}

@Deprecated
class SelectMemRefByLineRevised extends TypedQuery {
	IntegerResult id = new IntegerResult("id");
	IntegerResult parentId = new IntegerResult("parentId");
	IntegerResult startLine = new IntegerResult("startLine");
	IntegerResult endLine = new IntegerResult("endLine");
	
	//  The deltaData is actually a LONGBLOB in the database, but we put in and take out as a string.
	 
	StringResult deltaData = new StringResult("deltaData");
	
//	IntegerParameter targetLine = new IntegerParameter("targetLine");
	IntegerParameter startLineOfSpan = new IntegerParameter("startLineOfSpan");
	IntegerParameter endLineOfSpan = new IntegerParameter("endLineOfSpan");
	
	// These hard coded id cutoffs are derived from the structure of the delta tree, and the branching and level parameters of 1000 and 4.
	{
//		this.q = 
//		"SELECT id, startLine, endLine "+
//		" FROM memory_snapshot_delta_tree AS L1 "+
//		" WHERE endLine <= "+targetLine+" AND id >= 0 AND endLine > ( "+
//		" SELECT IFNULL(MAX(endLine),0) FROM memory_snapshot_delta_tree WHERE id > 1000000000 AND id < 1001000000 AND endLine < "+targetLine+" "+
//		" ) "+
//	
//		" UNION ALL "+
//	
//		" SELECT id, startLine, endLine "+
//		" FROM memory_snapshot_delta_tree AS L2 "+
//		" WHERE endLine <= "+targetLine+" AND id >= 1001000000 AND endLine > ( "+
//		" SELECT IFNULL(MAX(endLine),0) FROM memory_snapshot_delta_tree WHERE id > 1001000000 AND id < 1001001000 AND endLine < "+targetLine+" "+
//		" ) "+
//	
//		" UNION ALL "+
//	
//		" SELECT id, startLine, endLine "+
//		" FROM memory_snapshot_delta_tree AS L3 "+
//		" WHERE endLine <= "+targetLine+" AND id >= 1001001000 ";
		
		this.skipParameterCheck = true;
		// Revising...need to get from a span, not for a targeted line
		// Fetch all of the top level nodes that are relevant; inner nodes only if they are bigger than the smallest
		// top level node required; and leaf nodes only if they are bigger than the smallest of their required parents.
		// Note that the leaf nodes fetched need to go to the smallest parent above, which may be from before the span.
		 
		this.q = 
				
				" SELECT id, startLine, endLine, deltaData "+
					" FROM memory_snapshot_delta_tree AS L1 "+
					" WHERE "+
					// L0, leaf nodes
					"( endLine <= :"+endLineOfSpan+" AND id >= 0 AND id < 1000000000 "+
					" AND endLine > ( "+
					"   SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1000000000 AND id < 1001000000 "+
					"   AND endLine < :"+startLineOfSpan+" "+
					"    ) "+
					" ) "+
					" OR "+
					// L1
					" ( endLine <= :"+endLineOfSpan+" AND id >= 1000000000 AND id < 1001000000"+
					" AND endLine > ( "+
					"    SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1001000000 AND id < 1001001000 "+
					"    AND endLine < :"+startLineOfSpan+" "+
					"    ) "+
					" ) "+
					" OR "+
					// L2
					" ( endLine <= :"+endLineOfSpan+" AND id >= 1001000000 AND id < 1001001000"+
					" AND endLine > ( "+
					"    SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1001001000 "+
					"    AND endLine < :"+startLineOfSpan+" "+
					"    ) "+
					" ) "+
					" OR "+
					// L3
					" (endLine <= :"+endLineOfSpan+"  AND id >= 1001001000 "+
					" ) "+
					"";
				
//				" SELECT id, startLine, endLine, deltaData "+
//				" FROM memory_snapshot_delta_tree AS L1 "+
//				" WHERE endLine <= :"+endLineOfSpan+" AND id >= 0 AND id < 1000000000 "+
//				" AND endLine > ( "+
//				"   SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1000000000 AND id < 1001000000 "+
//				"   AND endLine < :"+startLineOfSpan+" "+
//				" ) "+
//			
//				" UNION ALL "+
//				
//				" SELECT id, startLine, endLine, deltaData "+
//				" FROM memory_snapshot_delta_tree AS L2 "+
//				" WHERE endLine <= :"+endLineOfSpan+" AND id >= 1000000000 AND id < 1001000000"+
//				" AND endLine > ( "+
//				"    SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1001000000 AND id < 1001001000 "+
//				"    AND endLine < :"+startLineOfSpan+" "+
//				" ) "+
//				
//				" UNION ALL "+
//			
//				" SELECT id, startLine, endLine, deltaData "+
//				" FROM memory_snapshot_delta_tree AS L3 "+
//				" WHERE endLine <= :"+endLineOfSpan+" AND id >= 1001000000 AND id < 1001001000"+
//				" AND endLine > ( "+
//				"    SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1001001000 "+
//				"    AND endLine < :"+startLineOfSpan+" "+
//				" ) "+
//			
//				" UNION ALL "+
//				
//				" SELECT id, startLine, endLine, deltaData "+
//				" FROM memory_snapshot_delta_tree AS L4 "+
//				" WHERE endLine <= :"+endLineOfSpan+"  AND id >= 1001001000 "+
//				""
//				;
			
		
//SELECT id, startLine, endLine, deltaData 
//FROM memory_snapshot_delta_tree AS L1 
//WHERE endLine <= 2943359 AND id >= 0 
//AND endLine > (
//SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1000000000 AND id < 1001000000 
//AND endLine < 2942559
//) 
//
//UNION ALL 
//			
//SELECT id, startLine, endLine, deltaData 
//FROM memory_snapshot_delta_tree AS L2 
//WHERE endLine <= 2943359 AND id >= 1001000000 
//AND endLine > ( 
//SELECT IFNULL(MAX(endLine),-1) FROM memory_snapshot_delta_tree WHERE id > 1001000000 AND id < 1001001000 
//AND endLine < 2942559
//) 
//
//UNION ALL 
//			
//SELECT id, startLine, endLine, deltaData 
//FROM memory_snapshot_delta_tree AS L3 
//WHERE endLine <= 2943359  AND id >= 1001001000 
		 
	}


}

	 */
	
	/*
	
	protected synchronized AsyncResult<MemoryQueryResults> getMemRefsAsyncTiered(int lineNumber, IProgressMonitor monitor) throws SQLException {
		String queryOrCache;
		long start = System.currentTimeMillis();
		boolean force = true;
		if(force || !(mostRecentLineQueriedStartSpan <= lineNumber && mostRecentLineQueriedEndSpan >= lineNumber)){
			boolean success = this.queryForMemoryReferencesTiered(lineNumber, monitor);
			queryOrCache = "queried from "+mostRecentLineQueriedStartSpan+" to "+mostRecentLineQueriedEndSpan+" for line "+lineNumber;
			if(monitor.isCanceled()) {
				mostRecentLineQueriedStartSpan = -1;
				mostRecentLineQueriedEndSpan = -1;
				mostRecentLineQueried = -1;
				return AsyncResult.cancelled();
			}
		} else {
			// Leave for a while, likely want to remove, but it will be good to watch this behavior and query time
			// even after this is committed.
			// System.out.println("Request in span, no requery:"+mostRecentLineQueriedStartSpan+" to "+mostRecentLineQueriedEndSpan+" for "+lineNumber);
			queryOrCache = "cache from "+mostRecentLineQueriedStartSpan+" to "+mostRecentLineQueriedEndSpan+" for line "+lineNumber;
		}
		
		// This resList may have previous queries that suffice for the current line (results span it),
		// or it might contain newly fetched data.
		
		long end = System.currentTimeMillis();
		System.out.println("Tiered Memory retrieval took "+(end - start)/1000.0+" seconds ("+queryOrCache+")");
		System.out.println();
		
		return new AsyncResult<MemoryQueryResults>(this.mostRecentMemoryQueryResults, Status.OK_STATUS);
	}
	
	protected synchronized AsyncResult<MemoryQueryResults> getMemRefsAsyncNew(int lineNumber, IProgressMonitor monitor) throws SQLException {
		String queryOrCache;
		long start = System.currentTimeMillis();
		boolean force = true;
		if(force || !(mostRecentLineQueriedStartSpan <= lineNumber && mostRecentLineQueriedEndSpan >= lineNumber)){
			boolean success = this.newQueryForMemoryReferences(lineNumber, monitor);
			queryOrCache = "queried from "+mostRecentLineQueriedStartSpan+" to "+mostRecentLineQueriedEndSpan+" for line "+lineNumber;
			if(monitor.isCanceled()) {
				mostRecentLineQueriedStartSpan = -1;
				mostRecentLineQueriedEndSpan = -1;
				mostRecentLineQueried = -1;
				return AsyncResult.cancelled();
			}
		} else {
			// Leave for a while, likely want to remove, but it will be good to watch this behavior and query time
			// even after this is committed.
			// System.out.println("Request in span, no requery:"+mostRecentLineQueriedStartSpan+" to "+mostRecentLineQueriedEndSpan+" for "+lineNumber);
			queryOrCache = "cache from "+mostRecentLineQueriedStartSpan+" to "+mostRecentLineQueriedEndSpan+" for line "+lineNumber;
		}
		
		// This resList may have previous queries that suffice for the current line (results span it),
		// or it might contain newly fetched data.
		
		long end = System.currentTimeMillis();
		System.out.println("New Memory retrieval took "+(end - start)/1000.0+" seconds ("+queryOrCache+")");
		System.out.println();
		
		return new AsyncResult<MemoryQueryResults>(this.mostRecentMemoryQueryResults, Status.OK_STATUS);
	}
	
	private synchronized boolean newQueryForMemoryReferences(int lineNumber, IProgressMonitor monitor) throws SQLException {
		mostRecentLineQueried = lineNumber;
		
		// Originally, queries were only up to and including the line
		// requested, but instead we will ask for lines forward and back from the clicked line.
		// Grabbing 400 forward and back.
		// We grab the memory data up to the earlier line (400 back), then grab all leaf
		// nodes from that point until the later line (400 forward). We only re-fetch when
		// the cached line data does not satisfy a request for memory data.
		// This means that we occasionally bypass a useful parent node, but it simplifies
		// querying dramatically, and reduces the number of database calls. In terms of
		// storage, it is not much worse than worst case (when the line just before a branching
		// factor is requested, requiring 999 leaf nodes).
		int startSpan = Math.max(0, lineNumber - cachedLineMemoryRefSpread);
		int endSpan = Math.min(maxLine, lineNumber + cachedLineMemoryRefSpread); // Could take Math.min() with max line number, but maybe don't need to
		
		mostRecentLineQueriedStartSpan = startSpan;
		mostRecentLineQueriedEndSpan = endSpan;
		
		// Re-initialize for getting fresh results.
		this.mostRecentMemoryQueryResults = new MemoryQueryResults(startSpan, lineNumber, endSpan);
		
		getMemoryReferenceForIdsRevised(lineNumber, startSpan, endSpan, monitor, this.mostRecentMemoryQueryResults);
		
		return true;
	}
	
	private synchronized boolean queryForMemoryReferencesTiered(int lineNumber, IProgressMonitor monitor) throws SQLException {
		mostRecentLineQueried = lineNumber;
		
		// Originally, queries were only up to and including the line
		// requested, but instead we will ask for lines forward and back from the clicked line.
		// Grabbing 400 forward and back.
		// We grab the memory data up to the earlier line (400 back), then grab all leaf
		// nodes from that point until the later line (400 forward). We only re-fetch when
		// the cached line data does not satisfy a request for memory data.
		// This means that we occasionally bypass a useful parent node, but it simplifies
		// querying dramatically, and reduces the number of database calls. In terms of
		// storage, it is not much worse than worst case (when the line just before a branching
		// factor is requested, requiring 999 leaf nodes).
		int startSpan = Math.max(0, lineNumber - cachedLineMemoryRefSpread);
		int endSpan = Math.min(maxLine, lineNumber + cachedLineMemoryRefSpread); // Could take Math.min() with max line number, but maybe don't need to
		
		mostRecentLineQueriedStartSpan = startSpan;
		mostRecentLineQueriedEndSpan = endSpan;
		
		// Re-initialize for getting fresh results.
		this.mostRecentMemoryQueryResults = new MemoryQueryResults(startSpan, lineNumber, endSpan);
		int parentEndLineCutoff = -1;
		for(int level = 3; level >= 0; level--) {
			
			System.out.println("Tiered query level: "+level);
			getMemoryReferenceForIdsRevisedTiered(startSpan, lineNumber, endSpan, level, parentEndLineCutoff, monitor, this.mostRecentMemoryQueryResults);
			parentEndLineCutoff = this.mostRecentMemoryQueryResults.highestLineSoFar;
			
			if(monitor.isCanceled()) {
				mostRecentLineQueriedStartSpan = -1;
				mostRecentLineQueriedEndSpan = -1;
				mostRecentLineQueried = -1;
				return false;
			}
		}
		
//		// We can grab the leaf nodes leading to the end span directly, and after the prior set. It
//		// keeps all leaf nodes towards the end of the resList.
//		// NB: startSpan to endSpan+1 because the lower loop will only go up-to-before startSpan,
//		// and only up to the second argument, thus +1 to cover that.
//		getMemoryReferenceForIdsRevisedTiered(startSpan, endSpan+1, monitor, 0, parentEndLineCutoff, this.mostRecentMemoryQueryResults);
		
		return true;
	}
	
	 // This method will get all memory references from nodes between startId and endId, where startId is inclusive and endId is exclusive.
	 // If the progress monitor (eg the query) was cancelled before we execute the query, this method will return an empty list.
	 // 
	 // We never ask for the memory data for *only* one line, we always get extra lines below and above. This is to reduce
	 // the number of calls when changing lines, and is a form of caching.
	 
	private void getMemoryReferenceForIdsRevisedTiered(int targetLine, int startSpan, int endSpan, int level, int parentEndLineCutoff, IProgressMonitor monitor, MemoryQueryResults memRefObj) throws SQLException {
		boolean nullReceived = true;
		int tries = 0;
		if(monitor.isCanceled()) {
			return;
		}
		
		// min, max and count are for debugging only, usage commented out below
		int min = Integer.MAX_VALUE;
		int max = -1;
		int count = 0;
		
		while(nullReceived && tries < 2){
			tries++;
			
			refreshConnection();
			
			
			SelectMemRefByLineRevisedBase q;
			if(level == 3){ //L3
				q = findLineMemRefStatementRevisedL3;
			} else if(level == 2){ //L2
				q = findLineMemRefStatementRevisedL2;
			} else if(level == 1){ //L1
				q = findLineMemRefStatementRevisedL1;
			} else { //L0
				q = findLineMemRefStatementRevisedL0;
			}
			
//			findLineMemRefStatementRevised.setParam(findLineMemRefStatementRevised.targetLine, targetLine);
//			q.setParam(q.startLineOfSpan, startSpan);
			q.setParam(q.endLineOfSpan, endSpan);
			q.setParam(q.parentLineLimit, parentEndLineCutoff);
			long start = System.currentTimeMillis();
			boolean success = q.execute();
			long end = System.currentTimeMillis();
			System.out.println("Tiered Memory pure query execute "+(end - start)/1000.0+" seconds");
			System.out.println();

			if(success){
				try(
					TypedResultSet results = q.getResultSet();
				){
					while(results.next()){
//						System.out.println("id: "+results.get(findLineMemRefStatement.id)+" start: "+results.get(findLineMemRefStatement.startLine)+" end: "+results.get(findLineMemRefStatement.endLine));

						if(monitor.isCanceled()) {
							return;
						}
						
						String string = results.get(q.deltaData);
						if(null == string){
							// System.out.println("Null result on #"+tries);
							nullReceived = true;
							break;
						} else {
							// if(tries > 1) System.out.println("No null result on #"+tries); // verify behavior
							nullReceived = false; // prevents a second try
						}
						
						DeltaConverter.convertDeltaQueryResults(string, memRefObj);
						
						// For debugging, verifying leaf node span
						 if(results.get(q.startLine) == results.get(q.endLine)){
							 min = Math.min(min, results.get(q.startLine));
							 max = Math.max(max, results.get(q.startLine));
						 }
						 count++;
						
					}
				} catch(SQLException e){
					// Likely canceled
					e.printStackTrace();
					System.out.println("Catching ok?");
					monitor.setCanceled(true);
					return;
				}
			}
		}
		 System.out.println("Tiered Way Fetched min and max: "+min+"-"+max);
		 System.out.println("Count: "+count);
		
		return;
	}
	
	
	 // This method will get all memory references from nodes between startId and endId, where startId is inclusive and endId is exclusive.
	 // If the progress monitor (eg the query) was cancelled before we execute the query, this method will return an empty list.
	 // 
	 // We never ask for the memory data for *only* one line, we always get extra lines below and above. This is to reduce
	 // the number of calls when changing lines, and is a form of caching.
	 
	private void getMemoryReferenceForIdsRevised(int targetLine, int startSpan, int endSpan, IProgressMonitor monitor, MemoryQueryResults memRefObj) throws SQLException {
		boolean nullReceived = true;
		int tries = 0;
		if(monitor.isCanceled()) {
			return;
		}
		
		// min, max and count are for debugging only, usage commented out below
		int min = Integer.MAX_VALUE;
		int max = -1;
		int count = 0;
		
		while(nullReceived && tries < 2){
			tries++;
			
			refreshConnection();
			
//			findLineMemRefStatementRevised.setParam(findLineMemRefStatementRevised.targetLine, targetLine);
			findLineMemRefStatementRevised.setParam(findLineMemRefStatementRevised.startLineOfSpan, startSpan);
			findLineMemRefStatementRevised.setParam(findLineMemRefStatementRevised.endLineOfSpan, endSpan);
			long start = System.currentTimeMillis();
			boolean success = findLineMemRefStatementRevised.execute();
			long end = System.currentTimeMillis();
			System.out.println("New Memory pure query execute "+(end - start)/1000.0+" seconds");
			System.out.println();

			if(success){
				try(
				TypedResultSet results = findLineMemRefStatementRevised.getResultSet();
				){
					while(results.next()){
//						System.out.println("id: "+results.get(findLineMemRefStatement.id)+" start: "+results.get(findLineMemRefStatement.startLine)+" end: "+results.get(findLineMemRefStatement.endLine));

						if(monitor.isCanceled()) {
							return;
						}
						
						String string = results.get(findLineMemRefStatementRevised.deltaData);
						if(null == string){
							// System.out.println("Null result on #"+tries);
							nullReceived = true;
							break;
						} else {
							// if(tries > 1) System.out.println("No null result on #"+tries); // verify behavior
							nullReceived = false; // prevents a second try
						}
						
						DeltaConverter.convertDeltaQueryResults(string, memRefObj);
						
						// For debugging, verifying leaf node span
						 if(results.get(findLineMemRefStatementRevised.startLine) == results.get(findLineMemRefStatementRevised.endLine)){
							 min = Math.min(min, results.get(findLineMemRefStatementRevised.startLine));
							 max = Math.max(max, results.get(findLineMemRefStatementRevised.startLine));
						 }
						 count++;
						
					}
				} catch(SQLException e){
					// Likely canceled
					e.printStackTrace();
					System.out.println("Catching ok?");
					monitor.setCanceled(true);
					return;
				}
			}
		}
		 System.out.println("New Way Fetched min and max: "+min+"-"+max);
		 System.out.println("Count: "+count);
		
		return;
	}
	
	*/
}
