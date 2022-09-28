/*
 * Copyright (c) 2020, Obeo. All rights reserved.
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
package org.activitymgr.core.model.util;

import java.util.Map;

import org.activitymgr.core.dto.Task;

/**
 * Utilities class for tasks.
 * <p>
 * Methods only manipulated provided data. There is no DB access.
 * </p> 
 *  
 * @author nperansin
 *
 */
public class Tasks {
	
	
	/**
	 * Indicates a task is modifiable.
	 * <p>
	 * A task is modifiable only if it is not closed and no ancestor is closed.
	 * </p>
	 * 
	 * @param task to evaluate
	 * @param cache containing a task and its ancestors by it full path
	 * @return true if modifiable
	 */
	public static boolean isModifiable(Task task, Map<String, Task> cache) {
		if (task.isClosed()) {
			return false;
		}
		for (Task parent = cache.get(task.getPath()); parent != null; 
				parent = cache.get(parent.getPath())) {
			if (parent.isClosed()) {
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Create path of code from a task.
	 * 
	 * @param task to evaluate
	 * @param cache containing a task and its ancestors by it full path
	 * @return path
	 */
	public static String buildTaskCodePath(Task task, Map<String, Task> cache) {
		String path = "";
		
		for (Task segment = task; segment != null; 
				segment = cache.get(segment.getPath())) {
			path = "/" + segment.getCode() + path;
		}
		return path;
	}
}
