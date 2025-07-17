package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Set;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.event.TaskSelectedEvent;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;

import com.google.inject.Inject;

public class TasksTabLogicImpl extends AbstractTabLogicImpl<ITasksTabLogic.View> implements ITasksTabLogic {

	@Inject(optional = true)
	private Set<ITabButtonFactory<ITasksTabLogic>> buttonFactories;
	private TaskTreeCellProvider treeContentCallback;
	
	public TasksTabLogicImpl(ITabFolderLogic parent) {
		super(parent);
		treeContentCallback = new TaskTreeCellProvider(this, null, false);
		@SuppressWarnings("unchecked") // no generic with *.class
		ITreeContentProviderCallback<Long> cpCallback = 
				wrapLogicForView(treeContentCallback, ITreeContentProviderCallback.class);
		getView().setTreeContentProviderCallback(cpCallback);

		// Add buttons
		registerButtons(buttonFactories);
	}

	@Override
	public String getLabel() {
		return "Tasks";
	}

	@Override
	public void onTaskSelected(Object value) {
		getEventBus().fire(new TaskSelectedEvent(this, (Long) value));
	}
	
	@Override
	public String getTaskCodePath(Long target) {
		Task task = getModelMgr().getTask(target);
		if (task != null) {
			try {
				return getModelMgr().getTaskCodePath(task);
			} catch (ModelException e) { // no task, no path
			}
		}
		return null;
	}

	@Override
	public void dispose() {
		treeContentCallback.dispose();
		super.dispose();
	}
}
