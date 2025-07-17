package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IConstraintsValidator;
import org.activitymgr.ui.web.logic.IConstraintsValidator.ErrorStatus;
import org.activitymgr.ui.web.logic.IConstraintsValidator.IStatus;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;

public abstract class AbstractTaskChooserLogicImpl<VIEW extends ITaskChooserLogic.View<?>>
		extends AbstractLogicImpl<VIEW> implements ITaskChooserLogic<VIEW> {
	
	private static final ErrorStatus EMPTY_SELECTION_STATUS = new ErrorStatus(
			"Please select a task");

	private TaskTreeCellProvider treeContentProvider;
	
	private Long selectedTaskId;

	public AbstractTaskChooserLogicImpl(AbstractLogicImpl<?> parent,
			Long selectedTaskId) {
		super(parent);

		// Set filter
		onTaskFilterChanged("");

		if (selectedTaskId != null) {
			getView().selectTask(selectedTaskId);
		}
	}
	
	
	/**
	 * Opens a window with associated view.
	 */
	public void showView() {
		// Open the window
		getRoot().getView().openWindow(getView());

		// Update state
		updateUI();
	}
	
	@Override
	public void onTaskFilterChanged(String filter) {
		if (treeContentProvider != null) {
			treeContentProvider.dispose();
		}
		
		// Register the tree content provider
		treeContentProvider = createFilteredContentProvider(filter);
		
		@SuppressWarnings("unchecked")
		ITreeContentProviderCallback<Long> cpCallback = 
			wrapLogicForView(treeContentProvider, ITreeContentProviderCallback.class);
		getView().setTasksTreeProviderCallback(cpCallback);
		
		Task filteredTask = treeContentProvider.getFilterMatching();
		if (filteredTask != null) {
			getView().expandToTask(filteredTask.getId());
		}
	}
	
	protected TaskTreeCellProvider createFilteredContentProvider(String filter) {
		return new TaskTreeCellProvider(this, filter, true);
	}

	@Override
	public void onSelectionChanged(Long taskId) {
		this.selectedTaskId = taskId;
		updateUI();
	}

	@Override
	public void dispose() {
		this.treeContentProvider.dispose();
		super.dispose();
	}

	protected Long getSelectedTaskId() {
		return selectedTaskId;
	}

	protected final void updateUI() {
		invoke(() -> {
			IStatus status = checkDialogRules();
			getView().setOkButtonEnabled(!status.isError());
			getView().setStatus(status.isError()
					? status.getErrorReason() 
					: "");
		});
	}

	protected IStatus checkDialogRules() throws ModelException {
		return selectedTaskId == null 
				? EMPTY_SELECTION_STATUS
				: IConstraintsValidator.OK_STATUS;
	}

}