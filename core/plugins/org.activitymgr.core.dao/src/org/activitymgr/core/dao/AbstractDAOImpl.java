/*
 * Copyright (c) 2004-2017, Jean-Francois Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
	
	protected <T> T wrapFailure(String finalMessage, Exception fail) throws DAOException {
		log.info("SQL failure", fail); //$NON-NLS-1$
		new DAOException(finalMessage, fail); //$NON-NLS-1$
		return null;
	}
	
	protected <T> T critical(Exception fail) throws DAOException {
		return wrapFailure(fail.getMessage(), fail);
	}

	protected <T> T critical(Exception fail, String messageId) throws DAOException {
		return wrapFailure(Strings.getString("DbMgr.errors." + messageId), fail);
	}

	protected <T> T critical(Exception fail, String messageId, Object... params) throws DAOException {
		return wrapFailure(Strings.getString("DbMgr.errors." + messageId, params), fail);
	}

}
