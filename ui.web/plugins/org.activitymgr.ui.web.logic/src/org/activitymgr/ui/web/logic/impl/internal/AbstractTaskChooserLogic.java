package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;

/**
 * Logic to select any kind of task (for report).
 * 
 * @author nperansin
 */
public abstract class AbstractTaskChooserLogic extends
		AbstractTaskChooserLogicImpl<ITaskChooserLogic.View<?>> {

	/**
	 * Default constructor
	 * 
	 * @param parent context
	 * @param selectedTask to start with
	 */
	public AbstractTaskChooserLogic(AbstractLogicImpl<?> parent, Long selectedTask) {
		super(parent, selectedTask);
	}
	
	
	/**
	 * Opens a window with associated view.
	 */
	public void showDialog() {
		// Open the window
		getRoot().getView().openWindow(getView());

		// Update state
		updateUI();
	}
	
}
