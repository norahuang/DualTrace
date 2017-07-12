package ca.uvic.chisel.atlantis.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.axiomalaska.jdbc.NamedParameterPreparedStatement;

/**
 * 
 * Consider this as a utility for safer {@link ResultSet} column name/index constants.
 * 
 * False justification: (this will become true if we see that the performance penalty is high enough)
 *  Each instance should be used with only one query, because of the index memoization; we want
 *  to avoid a (potential) penalty from result set column lookups by string, so we do them once,
 *  then use the stored column index for the actual value fetching. If we did not do this, they
 *  could be reused across queries. If we discover that column access by name has no performance
 *  penalty, refactor to reduce.
 * 
 * Facilitates transparent, explicit typed linkage between SQL query results and the programmer's
 * expectations. The programmer provides the column index and column name for the result, and uses
 * class instances of {@link TypedResult} to fetch values, rather than using int and String constants
 * or literals.
 * 
 * The {@link ResultSet} allows the user to request any type given the column number
 * or string column name. Bugs can arise where the type is silently converted, and data misused.
 * Using column names requires constants for safe and reliable use. The {@link TypedResult} links
 * the int and String constants together, and furthermore, provides efficient (one time) verification
 * of column name and index position, to catch development mistakes quickly.
 * 
 * Subclasses are explicit now, but this could be changed to use reflection. The fetching methods in
 * {@link ResultSet} could be selected for each instance on the basis of the generic type provided
 * in the constructor. it is not clear to me that this would be more convenient to use. For our purposes,
 * we only have about four types stored in the database, so having a handful of subclasses is simple
 * to use.
 *
 * Would use just column names for retrieval and get rid of the index, except there are anecdotal reports
 * of 20 second queries using 4 seconds of that just on column lookup:
 * http://stackoverflow.com/questions/186799/resultset-retrieving-column-values-by-index-versus-retrieving-by-label
 *
 * @param <T>
 */
abstract public class TypedResult<T> {
	
	/**
	 * Note that the toString() method does not return merely this columnName. It does
	 * additional work, creating SQL syntax for alias creation on a column, in order to
	 * force result sets to have table aliased column names.
	 * 
	 * SQL does not commute the column names as used, with table alias prefixes, in the
	 * result sets. Therefore, we need to transform the columnName when requesting the data.
	 */
	private final String columnName;
	
	/**
	 * As long as we use {@link NamedParameterPreparedStatement}, we can use
	 * just the name of the result column. It reduces maintenance costs to not
	 * have an explicit order index.
	 * 
	 * Instead, let's assign this index using resultset metadata, which we will have
	 * after testing the presence of each column name...see {@link TypedQuery} for
	 * that.
	 * 
	 * There can be cases where the result set has competing names (presumably with *).
	 * In those cases, the programmer will need to maintain these themselves.
	 */
//	public int resultPositionIndex = -1;
	
	public TypedResult (String columnName){
		this.columnName = columnName;
		// this.resultPositionIndex = resultPositionIndex;
	}
	
	/**
	 * Columns for select statements use this version of the column name.
	 * @return
	 */
	public String getInputColumnName(){
		return this.columnName;
	}
	
	/**
	 * Columns are aliased to this name in result sets.
	 * 
	 * @return
	 */
	public String getOutputColumnName(){
		return this.columnName.replace(".", "_");
	}

	/*
	 * This could be made to include the type of the TypedQuery. I did this at first,
	 * but there is sooooo much generics needed, all over the place, to do this. The programmer
	 * cannot easily make a mistake in which query they provide for this; there is a chance when
	 * the result set is not processed adjacent to the query execution, but we will just be
	 * intelligent in those cases, rather than  over-genericizing this system.
	 */
	abstract public T fetch(TypedQuery q, ResultSet rs) throws SQLException;
	
	/**
	 * Should we used based on meta data from the query, so that the programmer does
	 * not need to meticulously maintain this when queries evolve.
	 * 
	 * @param index
	 */
//	public void setColumnIndex(int index){
//		if(this.resultPositionIndex == -1){
//			this.resultPositionIndex = index;
//		} else {
//			System.err.println("Attempt to overwrite explicit TypedResult column index: '"+this.columnName+"' had index "
//					+this.resultPositionIndex+" and attempting to change to "+index+".");
//		}
//	}
	
	@Override
	public String toString(){
		return this.getInputColumnName()+" AS "+this.getOutputColumnName();
	}
}
