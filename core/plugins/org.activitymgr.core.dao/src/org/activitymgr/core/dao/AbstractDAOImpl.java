package org.activitymgr.core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class AbstractDAOImpl {
	
	
	/** Logger */
	protected static Logger log = Logger.getLogger(AbstractDAOImpl.class);
	
	/** Transaction provider */
	@Inject
	private Provider<Connection> tx;
	
	/**
	 * @return the active connection.
	 */
	protected Connection tx() {
		return tx.get();
	}

	/**
	 * Tries to close in a last attempt the {@link Statement}.
	 * 
	 * @param stmt
	 *            the {@link Statement} to close.
	 */
	protected void lastAttemptToClose(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (Throwable ignored) {
			}
		}
	}

	protected ResultSet executeRequired(PreparedStatement stmt) throws DAOException, SQLException {
		return executeRequired(stmt, "SQL_EMPTY_QUERY_RESULT");
	}
	protected ResultSet executeRequired(PreparedStatement stmt, String messageId) throws DAOException, SQLException {
		ResultSet result = stmt.executeQuery();
		if (!result.next()) {
			throw new DAOException(
					Strings.getString("DbMgr.errors." + messageId), null); //$NON-NLS-1$
		}
		return result;
	}
	
	protected void wrapFailure(String finalMessage, Exception fail) throws DAOException {
		log.info("SQL failure", fail); //$NON-NLS-1$
		new DAOException(finalMessage, fail); //$NON-NLS-1$
	}
	
	protected <T> T critical(Exception fail) throws DAOException {
		wrapFailure(fail.getMessage(), fail);
		return null;
	}


	protected <T> T critical(Exception fail, String messageId) throws DAOException {
		wrapFailure(Strings.getString("DbMgr.errors." + messageId), fail);
		return null;
	}

	protected <T> T critical(Exception fail, String messageId, Object... params) throws DAOException {
		wrapFailure(Strings.getString("DbMgr.errors." + messageId, params), fail);
		return null;
	}

}
