package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.ui.web.logic.ICopyButtonLogic;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.event.TaskSelectedEvent;

import com.google.inject.Inject;

public class CopyTaskPathButtonLogic extends AbstractLogicImpl<ICopyButtonLogic.View> implements ICopyButtonLogic {

	private IEventListener<TaskSelectedEvent> listener;
	
	private Long selectedTaskId;

	@Inject
	private IModelMgr modelMgr;
	
	@Inject
	public CopyTaskPathButtonLogic(ITasksTabLogic parent) { // See DownloadProjectReportButtonLogic
		super(parent);
		
		getView().setDisplay("Copy Task path", "duplicate");
		KeyBinding kb = new KeyBinding("CTRL+C");
		getView().setShortcut(kb.getKey(), kb.isCtrl(), kb.isShift(), kb.isAlt());
		getView().setDescription("Copy Task path <em>CTRL+C</em>");
		
		getView().setEnabled(false);
		listener = event -> {
			selectedTaskId = event.getSelectedTaskId();
			getView().setEnabled(selectedTaskId != null);
		};
		
		getEventBus().register(TaskSelectedEvent.class, listener);
	}
	
	@Override
	public String onClick() {
		return invoke(() -> {
			Task task = getModelMgr().getTask(selectedTaskId);
			if (task != null) {
				return modelMgr.getTaskCodePath(task);
			}
			
			return null;
		});
	}
	
	@Override
	public void dispose() {
		getEventBus().unregister(listener);
	}

}
