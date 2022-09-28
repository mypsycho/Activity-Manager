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
package org.activitymgr.ui.web.logic.impl;

import java.util.concurrent.Callable;

/**
 * Common interface to handle critical call.
 * 
 * @author nperansin
 *
 */
public interface CriticalService {

	/**
	 * Interface supporting Exception.
	 */
	public interface Exec {
	    /**
	     * Task to perform.
	     * 
	     * @throws Exception to handle
	     */
	    void run() throws Exception;
	}
	
	/**
	 * Interface supporting Exception.
	 */
	public interface ContextExec<T> {
	    /**
	     * Task to perform.
	     * 
	     * @param param context
	     * @throws Exception to handle
	     */
	    void accept(T param) throws Exception;
	}
	

	/**
	 * Executes a task and wrap exception.
	 * 
	 * @param task to run
	 */
	default void invoke(Exec task) {
		try {
			task.run();
		} catch (Throwable t) {
			doThrow(t);
		}
	}
	
	/**
	 * Executes a task and wrap exception or returns the result.
	 * 
	 * @param <T> type of result
	 * @param task to run
	 * @return result
	 */
	default <T> T invoke(Callable<T> task) {
		try {
			return task.call();
		} catch (Throwable t) {
			doThrow(t);
			return null;
		}
	}
		
	
	/**
	 * Wraps any error as critical.
	 * 
	 * @param t to wrap
	 */
	default void doThrow(Throwable t) {
		if (t instanceof Error) {
			throw (Error) t;
		} else if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		} else {
			throw new IllegalStateException(t);
		}
	}
}
