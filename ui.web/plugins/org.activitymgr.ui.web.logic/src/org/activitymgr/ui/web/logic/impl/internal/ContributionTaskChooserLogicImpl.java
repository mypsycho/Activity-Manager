package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IConstraintsValidator;
import org.activitymgr.ui.web.logic.IConstraintsValidator.ErrorStatus;
import org.activitymgr.ui.web.logic.IConstraintsValidator.IStatus;
import org.activitymgr.ui.web.logic.IContributionTaskChooserLogic;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.spi.ITaskCreationPatternHandler;

import com.google.inject.Inject;

public class ContributionTaskChooserLogicImpl
		extends AbstractTaskChooserLogicImpl<IContributionTaskChooserLogic.View>
		implements IContributionTaskChooserLogic {
	
	private Collection<Long> alreadySelectedTaskIds;
	
	@Inject
	private IDTOFactory dtoFactory;
	
	@Inject
	private Map<String, ITaskCreationPatternHandler> taskCreationPatternHandlers;
	
	@Inject
	private Set<IConstraintsValidator> constraintsValidators;

	private String newTaskName;
	private String newTaskCode;
	private boolean newTaskChecked;
	
	public ContributionTaskChooserLogicImpl(AbstractLogicImpl<?> parent,
			Long taskIdToExpand, Collection<Long> selectedTaskIds,
			Task[] recentTasks) {
		super(parent, taskIdToExpand);
		
		// Remember already selected task ids
		this.alreadySelectedTaskIds = selectedTaskIds;
		
		// Retrieve recent tasks labels
		Map<Long, Task> tasks = new HashMap<Long, Task>();
		
		// Retrieve recent tasks
		for (Task recentTask : recentTasks) {
			tasks.put(recentTask.getId(), recentTask);
		}
		// Add selected ID (if missing)
		for (Long id : alreadySelectedTaskIds) {
			if (!tasks.containsKey(id)) {
				tasks.put(id, getModelMgr().getTask(id));
			}
		}
		// Retrieve task code path
		Map<Long, String> labels = new LinkedHashMap<Long, String>();
		Map<Long, String> codes = new HashMap<Long, String>();
		
		try {
			List<Long> recentTasksIds = new ArrayList<Long>(tasks.keySet());
			for (Long taskId : recentTasksIds) {
				Task task = tasks.get(taskId);
				codes.put(taskId, getModelMgr().getTaskCodePath(task));
			}
			Collections.sort(recentTasksIds, createTaskSorter(tasks));
			for (Long taskId : recentTasksIds) {
				labels.put(taskId, 
						"[" + codes.get(taskId) + "] " + tasks.get(taskId).getName());
			}
		} catch (ModelException e) {
			// Is this critical ?
			throw new IllegalStateException("Unexpected error while retrieving recent tasks", e);
		}
		
		getView().setRecentTasks(labels);

		// Pattern handler list
		List<String> patternIds = new ArrayList<String>(taskCreationPatternHandlers.keySet());
		Collections.sort(patternIds);
		Map<String, String> patternLabels = new HashMap<String, String>();
		for (String id : patternIds) {
			patternLabels.put(id, taskCreationPatternHandlers.get(id).getLabel());
		}
		getView().setCreationPatterns(patternLabels);

		// Reset button state & status label
		onSelectionChanged(taskIdToExpand);
	}
	
	private Comparator<Long> createTaskSorter(Map<Long, Task> maps) {
		return (Long taskId1, Long taskId2) -> {
			Task task1 = maps.get(taskId1);
			Task task2 = maps.get(taskId2);
			String fullPath1 = task1.getFullPath();
			String fullPath2 = task2.getFullPath();
			return fullPath1.compareTo(fullPath2);
		};
	}
	
	@Override
	protected TaskTreeCellProvider createFilteredContentProvider(String filter) {
		// Contribution is only for open task.
		return new TaskTreeCellProvider(this, filter, true, true);
	}

	@Override
	public void onNewTaskCheckboxClicked() {
		newTaskChecked = !newTaskChecked;
		updateUI();
	}

	@Override
	public void onNewTaskNameChanged(String newTaskName) {
		this.newTaskName = newTaskName;
		updateUI();
	}

	@Override
	public void onNewTaskCodeChanged(String newTaskCode) {
		this.newTaskCode = newTaskCode;
		updateUI();
	}

	@Override
	protected IStatus checkDialogRules() throws ModelException {
		IStatus status = super.checkDialogRules();
		if (status.isError()) {
			return status;
		}
		Task selectedTask = getSelectedTaskId() != null 
				? getModelMgr().getTask(getSelectedTaskId()) 
				: null;
		String newStatus = null;
		boolean newTaskFieldsEnabled = false;
		if (selectedTask != null) {
			boolean isLeaf = getModelMgr().isLeaf(selectedTask.getId());
			if (newTaskChecked) {
				newTaskFieldsEnabled = true;
				newStatus = getNewTaskStatus(selectedTask);
				
			} else if (!isLeaf) {
				newStatus = "You cannot select a container task";
			} else if (alreadySelectedTaskIds != null
					&& alreadySelectedTaskIds.contains(selectedTask.getId())) {
				newStatus = "This task is already selected";
			}
		}
		getView().setNewTaskFieldsEnabled(newTaskFieldsEnabled);
		return newStatus != null 
				? new ErrorStatus(newStatus)
				: IConstraintsValidator.OK_STATUS;
	}
	
	protected String getNewTaskStatus(Task selectedTask) throws ModelException {
		// Check constraints
		for (IConstraintsValidator cv : constraintsValidators) {
			IStatus status = cv.canCreateSubTaskUnder(selectedTask);
			if (status.isError()) {
				return "It's not possible to create a sub task under the selected task :\n" 
						+ status.getErrorReason();
			}
		}
		// If constraints OK, check following rules
	
		if (newTaskCode != null 
				&& !"".equals(newTaskCode = newTaskCode.trim()) 
				&& getModelMgr().getTask(selectedTask.getFullPath(), newTaskCode) != null) {
			return "This code is already in use";
		} else if (newTaskName == null || "".equals(newTaskName.trim())) {
			return "Enter a task name";
		}
		return null;
	}

	@Override
	public void onOkButtonClicked(long taskId) {
		try {
			long[] selectedIds = !newTaskChecked
					? new long[] { taskId }
					: createNewTasks(taskId);
			((AbstractContributionTabLogicImpl) getParent()).addTasks(selectedIds);
		
		} catch (ModelException e) {
			doThrow(e);
		}
	}
	
	protected long[] createNewTasks(long taskId) throws ModelException {

		Task parent = getModelMgr().getTask(taskId);
		Task newTask = dtoFactory.newTask();
		newTask.setName(newTaskName.trim());
		String code = newTaskCode != null ? newTaskCode.trim() : "";
		if ("".equals(code)) {
			code = newTask.getName().trim().replaceAll(" ", "").toUpperCase();
			if (code.length() > 7) {
				code = code.substring(0, 7);
			}
			code = '$' + code;
		}
		newTask.setCode(code);
		getModelMgr().createTask(parent, newTask);
		
		// Init task to select list
		List<Long> selectedTaskIds = new ArrayList<Long>();

		// Task creation pattern management
		String patternId = getView().getSelectedTaskCreationPatternId();
		if (patternId != null) {
			ITaskCreationPatternHandler handler = taskCreationPatternHandlers.get(patternId);
			List<Task> createdTasks = handler.handle(getContext(), newTask);
			for (Task subTask : createdTasks) {
				long id = subTask.getId();
				if (!selectedTaskIds.contains(id)) {
					selectedTaskIds.add(id);
				}
			}
		}
		// If no task has been selected (which may occur if the creation pattern handler doesn't return
		// anything), auto select a task
		if (selectedTaskIds.isEmpty()) {
			Task[] subTasks = getModelMgr().getSubTasks(newTask.getId());
			selectedTaskIds.add(subTasks.length == 0 ? newTask.getId() : subTasks[0].getId());
		}
		// Turn the selection list into an array
		long[] selectedTaskIdsArray = new long[selectedTaskIds.size()];
		int i = 0;
		for (long id : selectedTaskIds) {
			selectedTaskIdsArray[i++] = id;
		}
		return selectedTaskIdsArray;
	}

	@Override
	public void onRecentTaskClicked(long taskId) {
		getView().selectTask(taskId);
	}

}