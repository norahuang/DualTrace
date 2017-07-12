package ca.uvic.chisel.atlantis.database;

import java.sql.SQLException;

import com.axiomalaska.jdbc.NamedParameterPreparedStatement;

/**
 * Typed parameters for SQL queries. For use with {@link NamedParameterPreparedStatement},
 * though it could be adjusted to work without.
 * 
 * You might think to combine this with the {@link TypedResult} class, but we shall not.
 * 
 * @param <T>
 */
abstract public class TypedParam<T> {

	public final String parameterName;
	
	/**
	 * 
	 * @param parameterName	must match the SQL assignee variable name
	 */
	public TypedParam (String parameterName){
		this.parameterName = parameterName;
	}
	
	abstract public void setParam(T value, TypedQuery typedQuery) throws SQLException;
	
	@Override
	public String toString(){
		return this.parameterName;
	}
}
