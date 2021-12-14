package org.activitymgr.ui.rcp.util;

import java.util.concurrent.Callable;

import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.util.Strings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public abstract class AbstractTableMgrUI extends AbstractTableMgr {


	/** Model manager */
	protected IModelMgr modelMgr;

	/** Composant parent */
	protected final Composite container;

	
	
	public AbstractTableMgrUI(Composite container, IModelMgr modelMgr) {
		this.modelMgr = modelMgr;
		this.container = container;
	}
	
	public SelectionListener onSelectionSafeRun(final SafeRunner.Exec run) {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				safeExec(run);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			
		};
	}
	
	protected <T> T safeExec(T defaultValue, Callable<T> runner) {
		return SafeRunner.exec(container.getShell(), defaultValue, runner);
	}
	
	protected void safeExec(SafeRunner.Exec runner) {
		SafeRunner.exec(container.getShell(), runner);
	}
	
	protected MenuItem createItem(Menu parent, String id, SafeRunner.Exec run) {
		MenuItem result = new MenuItem(parent, SWT.CASCADE);
		String textId = getClass().getSimpleName() + ".menuitems." + id; //$NON-NLS-1$
		result.setText(Strings.getString(textId));
		result.addSelectionListener(onSelectionSafeRun(run));
		return result;
	}

}
