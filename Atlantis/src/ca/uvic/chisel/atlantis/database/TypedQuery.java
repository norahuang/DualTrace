package ca.uvic.chisel.atlantis.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;

import com.axiomalaska.jdbc.NamedParameterPreparedStatement;

/**
 * When instantiating subclasses, the {@link Connection} required to initialize the {@link PreparedStatement}
 * cannot be passed to a constructor (it would require too much boilerplate in the liteweight subclasses).
 * Instead, we must either call {@link TypedQuery#preparedStatement} manually after instantiating,
 * or we can use anonymous class initialization like so:
 *        SubTypedQuery q = new SubTypedQuery(){{this.prepare(connection);}};
 * This gives us succinct initialization. Alternatives are calling "q.prepare(connection);" after,
 * or using a factory method that like:
 *        SubTypedQuery q = AtlantisQuery.mk(connection, new SubTypedQuery());
 * which is similar to what we used to do.
 *  
 *
 */
public abstract class TypedQuery implements AutoCloseable {
	
//	private boolean firstExecutionPerformChecks = true;
//	
//	private HashMap<String, TypedQuery> map;
//	// Something to store parameters. Not iterated over, I don't think.
	
	/**
	 * Collection of parameters that have been set for the not-yet-executed query.
	 * Once executed, the parameters are cleared.
	 */
	private HashMap<TypedParam<?>, Object> parametersSet = new HashMap<>();
	private HashMap<TypedParam<?>, Object> prevParametersSet = new HashMap<>();
	
	NamedParameterPreparedStatement preparedStatement;
	
	/**
	 * For queries that re-use the same parameters, for which a count of used parameters will 
	 * be wrong, we can bypass the check.
	 */
	public boolean skipParameterCheck = false;
	
	/**
	 * The query, in string format, with ? tokens as appropriate, for use in prepared statements.
	 */
	public String q;
	
	/**
	 * Consider making this optional for explicit calls, and check for a null preparedStatement in the setParam and execute
	 * methods.
	 * @param connection
	 * @throws SQLException
	 */
	public void prepare(Connection connection) throws SQLException{
		this.preparedStatement = AtlantisQueries.mk(connection,  this.q);
		
		
	}
	
	// How to add parameters to prepared query? Generics method, make contingent on type from constant for each parameter
	public <T> void setParam(TypedParam<T> param, T value) throws SQLException{
		param.setParam(value, this);
		parametersSet.put(param, value);
	}
	
	private String stringifyParametersSet(){
		String paramStr = "";
		for(TypedParam<?> p: parametersSet.keySet()){
			paramStr += p.parameterName+": "+parametersSet.get(p).toString()+", ";
		}
		return paramStr;
	}
	
	private void checkParametersAreSetAndClearForNextQuery() throws SQLException{
		if(this.skipParameterCheck){
			return;
		}
		int expected = this.preparedStatement.getParameterMetaData().getParameterCount();
		int counted = this.parametersSet.size();
		if(expected != counted){
			throw new SQLException("Not all parameters set prior to execution, expect "+expected+" parameters, have "+counted+": "+stringifyParametersSet());
		}
		clearParameterCheckContainers();
	}
	

	private void clearParameterCheckContainers(){
		// Clearing only our record of the parameters, not the prepared statement's set parameters.
		// Keep a copy for debugging purposes, just swap them back and forth as current and previous.
		HashMap<TypedParam<?>, Object> p = this.prevParametersSet;
		this.prevParametersSet = this.parametersSet;
		this.parametersSet = p;
		this.parametersSet.clear();
	}
	
	public TypedResultSet executeQuery() throws SQLException {
		checkParametersAreSetAndClearForNextQuery();
		this.preparedStatement.execute();
		return this.getResultSet();
	}
	
	public boolean execute() throws SQLException {
		checkParametersAreSetAndClearForNextQuery();
		return this.preparedStatement.execute();
	}
	
	public int executeUpdate() throws SQLException{
		checkParametersAreSetAndClearForNextQuery();
		return this.preparedStatement.executeUpdate();
	}
	
	public TypedResultSet getResultSet() throws SQLException {
		return new TypedResultSet(this, this.preparedStatement.getResultSet());
	}
	
	public void addBatch() throws SQLException {
		checkParametersAreSetAndClearForNextQuery();
		this.preparedStatement.addBatch();
	}
	
	public void executeBatch() throws SQLException {
		this.preparedStatement.executeBatch();
	}
	
	public void cancel() throws SQLException {
		clearParameterCheckContainers();
		this.preparedStatement.cancel();
	}
	
	public void close() throws SQLException{
		this.preparedStatement.close();
	}
	
}

/*
 * Typed Results
 */

/**
 * Facilitates less verbose fetching of columns than accessing from either the {@link TypedQuery}
 * or the {@link TypedResult} classes.
 *
 */
class TypedResultSet implements AutoCloseable {
	private TypedQuery q;
	public final ResultSet resultSet;
	TypedResultSet(TypedQuery q, ResultSet rs){
		this.q = q;
		this.resultSet = rs;
	}
	
	public <T> T get(TypedResult<T> type) throws SQLException{
		return type.fetch(this.q, this.resultSet);
	}
	
	public boolean next() throws SQLException {
		return this.resultSet.next();
	}
	
	public void close() throws SQLException {
		this.resultSet.close();
	}
}

class IntegerResult extends TypedResult<Integer> {
	public IntegerResult(String columnName) {
		super(columnName);
	}

	@Override
	public Integer fetch(TypedQuery q, ResultSet rs) throws SQLException {
		return rs.getInt(this.getOutputColumnName());
	}
}

class StringResult extends TypedResult<String> {
	public StringResult(String columnName) {
		super(columnName);
	}

	@Override
	public String fetch(TypedQuery q, ResultSet rs) throws SQLException {
		return rs.getString(this.getOutputColumnName());
	}
}

class LongResult extends TypedResult<Long> {
	public LongResult(String columnName) {
		super(columnName);
	}

	@Override
	public Long fetch(TypedQuery q, ResultSet rs) throws SQLException {
		return rs.getLong(this.getOutputColumnName());
	}
}

class InstructionIdResult extends TypedResult<InstructionId> {
	public InstructionIdResult(String columnName) {
		super(columnName);
	}

	@Override
	public InstructionId fetch(TypedQuery q, ResultSet rs) throws SQLException {
		return new InstructionId(rs.getString(this.getOutputColumnName()));
	}
}

class BooleanResult extends TypedResult<Boolean> {
	public BooleanResult(String columnName) {
		super(columnName);
	}

	@Override
	public Boolean fetch(TypedQuery q, ResultSet rs) throws SQLException {
		return rs.getBoolean(this.getOutputColumnName());
	}
}

/*
 * Typed Parameters
 */

class IntegerParameter extends TypedParam<Integer> {
	public IntegerParameter(String paramAssigneeName) {
		super(paramAssigneeName);
	}

	@Override
	public void setParam(Integer value, TypedQuery q) throws SQLException {
		q.preparedStatement.setInt(this.parameterName, value);
	}
}

class NullParameter extends TypedParam<Integer> {
	public NullParameter(String paramAssigneeName) {
		super(paramAssigneeName);
	}

	/**
	 * sqlType is int, derived from {@link Types} (e.g. INTEGER type therein).
	 */
	@Override
	public void setParam(Integer sqlType, TypedQuery q) throws SQLException {
		q.preparedStatement.setNull(this.parameterName, sqlType);
	}
}

class LongParameter extends TypedParam<Long> {
	public LongParameter(String paramAssigneeName) {
		super(paramAssigneeName);
	}
	
	@Override
	public void setParam(Long value, TypedQuery q) throws SQLException {
		q.preparedStatement.setLong(this.parameterName, value);
	}
}

class StringParameter extends TypedParam<String> {
	public StringParameter(String paramAssigneeName) {
		super(paramAssigneeName);
	}

	@Override
	public void setParam(String value, TypedQuery q) throws SQLException {
		q.preparedStatement.setString(this.parameterName, value);
	}
}

class InstructionIdParameter extends TypedParam<InstructionId> {
	public InstructionIdParameter(String paramAssigneeName) {
		super(paramAssigneeName);
	}

	@Override
	public void setParam(InstructionId value, TypedQuery q) throws SQLException {
		q.preparedStatement.setString(this.parameterName, value.getString());
	}
}

class BooleanParameter extends TypedParam<Boolean> {
	public BooleanParameter(String paramAssigneeName) {
		super(paramAssigneeName);
	}

	@Override
	public void setParam(Boolean value, TypedQuery q) throws SQLException {
		q.preparedStatement.setBoolean(this.parameterName, value);
	}
}

