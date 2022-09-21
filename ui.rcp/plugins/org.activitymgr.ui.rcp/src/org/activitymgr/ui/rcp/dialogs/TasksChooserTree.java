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
package org.activitymgr.ui.rcp.dialogs;


import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.util.AbstractTableMgr;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.activitymgr.ui.rcp.util.TableOrTreeColumnsMgr;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

public class TasksChooserTree extends AbstractTableMgr
		implements ITreeContentProvider {

	/** Logger */
	private static Logger log = Logger.getLogger(TasksChooserTree.class);

	/** Constantes associées aux colonnes */
	public static final int NAME_COLUMN_IDX = 0;
	public static final int CODE_COLUMN_IDX = 1;
	private static final TableOrTreeColumnsMgr COLUMNS_MGR =
			new TableOrTreeColumnsMgr("TasksChooserTree.columns.TASK_"); //$NON-NLS-1$
	static {
		COLUMNS_MGR.addColumn("NAME", 200, SWT.LEFT); //$NON-NLS-1$
		COLUMNS_MGR.addColumn("CODE", 70, SWT.LEFT); //$NON-NLS-1$
	}

	/** Viewer */
	private TreeViewer treeViewer;

	/** Composant parent */
	private Composite parent;

	/** Model manager */
	private IModelMgr modelMgr;

	/**
	 * Constructeur par défaut.
	 *
	 * @param parentComposite
	 *            composant parent.
	 * @param modelMgr
	 *            the model manager.
	 */
	public TasksChooserTree(Composite parentComposite, IModelMgr modelMgr) {
		this.modelMgr = modelMgr;
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		// Arbre tableau
		final Tree tree = new Tree(parent, SWT.FULL_SELECTION | SWT.BORDER
				| SWT.HIDE_SELECTION);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 300;
		tree.setLayoutData(gridData);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.setEnabled(true);

		// Création du viewer
		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(this);
		treeViewer.setLabelProvider(this);

		// Configuration des colonnes
		COLUMNS_MGR.configureTree(treeViewer);

		// Création d'une racine fictive
		treeViewer.setInput(ROOT_NODE);
	}


	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(null);
	}

	public Object getValue(Object element, String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Task task = (Task) element;
		String value = null;
		switch (COLUMNS_MGR.getColumnIndex(property)) {
		case NAME_COLUMN_IDX:
			value = task.getName();
			break;
		case CODE_COLUMN_IDX:
			value = task.getCode();
			break;
		default:
			throw new Error(
					Strings.getString("TasksChooserTree.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
		}
		return value;
	}


	@Override
	public boolean hasChildren(Object element) {
		log.debug("ITreeContentProvider.getChildren(" + element + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		Task task = (Task) element;
		return modelMgr.getSubTasksCount(task.getId()) > 0;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		log.debug("ITreeContentProvider.getChildren(" + parentElement + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final Task parentTask = (Task) parentElement;
		return SafeRunner.exec(parent.getShell(), new Object[0],
				()-> modelMgr.getSubTasks(parentTask != null ? parentTask.getId() : null));
	}

	@Override
	public Object getParent(Object element) {
		log.debug("ITreeContentProvider.getParent(" + element + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final Task task = (Task) element;

		return SafeRunner.exec(parent.getShell(), null, () -> {
			Task parentTask = modelMgr.getParentTask(task);
			return parentTask == null ? treeViewer.getInput() : parentTask;
		});
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		log.debug("ITableLabelProvider.getColumnText(" + element + ", " + columnIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Task task = (Task) element;
		String text = null;
		switch (columnIndex) {
		case NAME_COLUMN_IDX:
			text = task.getName();
			break;
		case CODE_COLUMN_IDX:
			text = task.getCode();
			break;
		default:
			throw new Error(
					Strings.getString("TasksChooserTree.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
		}
		return text;
	}

	/**
	 * Retourne le viewer associé à l'arbre.
	 *
	 * @return le viewer associé à l'arbre.
	 */
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
}
