package org.activitymgr.core.dao;

import java.util.HashMap;
import java.util.Map;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.util.StringHelper;

import com.google.inject.Inject;

public class TaskDAOCache {
	
	private ITaskDAO taskDAO;
	
	private Map<Long, Task> taskByIdCache = new HashMap<>();
	private Map<Long, String> taskCodePathByIdCache = new HashMap<>();
	private Map<String, Long> taskIdByCodePathCache = new HashMap<>();
	private Map<String, Task> taskByPathCache = new HashMap<>();
	
	@Inject
	public TaskDAOCache(ITaskDAO dao) {
		this.taskDAO = dao;
	}
	
	public Task getById(long taskId) {
		if (!taskByIdCache.containsKey(taskId)) {
			Task task = taskDAO.selectByPK(taskId);
			addToCache(task);
		}
		return taskByIdCache.get(taskId);
	}
	
	public Task getByFullPath(String fullpath) {
		if (fullpath == null || fullpath.length()==0) {
			return null;
		}
		if (!taskByPathCache.containsKey(fullpath)) {
			int pathLength = fullpath.length() - 2;
			String path = fullpath.substring(0, pathLength);
			short number = StringHelper.hexToNumber(fullpath.substring(pathLength));

			Task task = selectTaskBySegment(path, "number", number);

			if (task != null) {
				addToCache(task);
			} else { 
				// Remember that this task doesn't exist
				taskByPathCache.put(fullpath, null);
			}
		}
		return taskByPathCache.get(fullpath);
	}
	
	public Task getParent(Task task) {
		return getByFullPath(task.getPath());
	}

	public String getCodePath(long taskId) {
		if (!taskCodePathByIdCache.containsKey(taskId)) {
			Task task = getById(taskId);
			Task parent = getParent(task);
			String taskCodePath = (parent != null ? getCodePath(parent.getId()) : "") + "/" + task.getCode();
			linkTaskAndPathInCache(taskCodePath, taskId);
		}
		return taskCodePathByIdCache.get(taskId);
	}

	public Task getByCodePath(String codePath) {
		if (!taskIdByCodePathCache.containsKey(codePath)) {
			int idx = codePath.lastIndexOf('/');
			String taskCode = codePath.substring(idx + 1);
			Task parentTask = null;
			if (idx > 1) {
				String parentTaskCodePath = codePath.substring(0, idx);
				parentTask = getByCodePath(parentTaskCodePath);
			}
			Task task = selectTaskBySegment(
					parentTask != null ? parentTask.getFullPath() : "",
					"code", taskCode);

			if (task != null) {
				if (!taskByIdCache.containsKey(task.getId())) {
					addToCache(task);
				} else {
					task = taskByIdCache.get(task.getId());
				}
				linkTaskAndPathInCache(codePath, task.getId());
			} else {
				// Remember that this task doesn't exist
				taskIdByCodePathCache.put(codePath, null);
			}
		}

		Long taskId = taskIdByCodePathCache.get(codePath);
		return taskId != null ? getById(taskId) : null;
	}
	
	private Task selectTaskBySegment(String path, String field, Object value) {
		Task[] tasks = taskDAO.select(
				new String[] { "path", field }, 
				new Object[] { path, value }, 
				null, -1);
		if (tasks.length > 1) {
			throw new IllegalStateException("More than one task returned");
		}
		return tasks.length == 1
				? tasks[0]
				: null;
	}

	private void addToCache(Task task) {
		taskByPathCache.put(task.getFullPath(), task);
		taskByIdCache.put(task.getId(), task);
	}

	private void linkTaskAndPathInCache(String taskCodePath, long taskId) {
		taskCodePathByIdCache.put(taskId, taskCodePath);
		taskIdByCodePathCache.put(taskCodePath, taskId);
	}
}
