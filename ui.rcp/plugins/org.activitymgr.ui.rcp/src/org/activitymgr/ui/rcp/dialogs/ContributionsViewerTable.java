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

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.util.AbstractTableMgr;
import org.activitymgr.ui.rcp.util.SWTHelper;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.activitymgr.ui.rcp.util.TableOrTreeColumnsMgr;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;

public class ContributionsViewerTable extends AbstractTableMgr implements
		SelectionListener, MenuListener {

	/** Logger */
	private static Logger log = Logger
			.getLogger(ContributionsViewerTable.class);

	/** Filtre de recherche */
	private Task task;
	private Collaborator contributor;

	/** Constantes associées aux colonnes */
	public static final int DATE_COLUMN_IDX = 0;
	public static final int COLLABORATOR_COLUMN_IDX = 1;
	public static final int TASK_CODE_PATH_COLUMN_IDX = 2;
	public static final int TASK_NAME_COLUMN_IDX = 3;
	public static final int DURATION_COLUMN_IDX = 4;
	private static final TableOrTreeColumnsMgr TABLE_MGR = new TableOrTreeColumnsMgr();
	static {
		BiConsumer<String, Integer> addColumn = (code, size) -> TABLE_MGR.addColumn(
				code, Strings.getString("ContributionsViewerTable.columns." + code), //$NON-NLS-1$
				size, SWT.LEFT);
		addColumn.accept("DATE", 70); //$NON-NLS-1$
		addColumn.accept("COLLABORATOR", 100); //$NON-NLS-1$
		addColumn.accept("TASK_PATH", 170); //$NON-NLS-1$
		addColumn.accept("TASK_NAME", 170); //$NON-NLS-1$
		addColumn.accept("DURATION", 50); //$NON-NLS-1$
	}

	/** Formatteur de date */
	private SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd/MM/yyyy"); //$NON-NLS-1$

	/** Viewer */
	private TableViewer tableViewer;

	/** Composant parent */
	private Composite parent;

	/** Items de menu */
	private MenuItem exportItem;

	/** Cache de taches */
	private Map<Long, Task> tasksCache = new HashMap<>();

	/** Cache de chemins de tache */
	private Map<Long, String> taskCodePathsCache = new HashMap<>();

	/** Cache de collaborateurs */
	private Map<Long, Collaborator> collaboratorsCache = new HashMap<>();

	/** Model manager */
	private IModelMgr modelMgr;

	/**
	 * Constructeur par défaut.
	 *
	 * @param parentComposite
	 *            composant parent.
	 * @param layoutData
	 *            données associées au layout.
	 * @param modelMgr
	 *            the model manager.
	 */
	public ContributionsViewerTable(Composite parentComposite, Object layoutData, IModelMgr modelMgr) {
		log.debug("new ContributionsViewerTable()"); //$NON-NLS-1$
		this.modelMgr = modelMgr;
		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayoutData(layoutData);
		parent.setLayout(new GridLayout(1, false));

		// Arbre tableau
		final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER
				| SWT.HIDE_SELECTION | SWT.MULTI);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 300;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setEnabled(true);

		// Création du viewer
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(this);
		tableViewer.setLabelProvider(this);

		// Configuration des colonnes
		TABLE_MGR.configureTable(tableViewer);

		// Configuration du menu popup
		final Menu menu = new Menu(table);
		menu.addMenuListener(this);
		exportItem = new MenuItem(menu, SWT.CASCADE);
		exportItem.setText(Strings
				.getString("ContributionsViewerTable.menuitems.EXPORT")); //$NON-NLS-1$
		exportItem.addSelectionListener(this);
		table.setMenu(menu);
	}

	/**
	 * Initialise le filtre de recherche des contributions.
	 *
	 * @param task
	 *            la tache.
	 * @param contributor
	 *            le collaborateur.
	 */
	public void setFilter(Task task, Collaborator contributor) {
		// Initialisation du filtre de recherche
		this.task = task;
		this.contributor = contributor;
		// Création d'une racine fictive
		tableViewer.setInput(ROOT_NODE);
	}

	private static final Contribution[] NO_CONTRIBUTIONS = {};
	@Override
	public Object[] getElements(Object inputElement) {
		return SafeRunner.exec(parent.getShell(), NO_CONTRIBUTIONS,
			() -> modelMgr.getContributions(contributor, task, null, null));
	}

	@Override
	public String getColumnText(final Object element, final int columnIndex) {
		return SafeRunner.exec(parent.getShell(), "", () -> { //$NON-NLS-1$
			Contribution c = (Contribution) element;
			String text = null;
			switch (columnIndex) {
			case DATE_COLUMN_IDX:
				text = DAY_FORMAT.format(c.getDate().getTime());
				break;
			case COLLABORATOR_COLUMN_IDX:
				Collaborator collaborator = getCachedCollaborator(c
						.getContributorId());
				text = collaborator.getFirstName()
						+ " " + collaborator.getLastName(); //$NON-NLS-1$
				break;
			case TASK_CODE_PATH_COLUMN_IDX:
				text = getCachedTaskCodePath(c.getTaskId());
				break;
			case TASK_NAME_COLUMN_IDX:
				text = getCachedTask(c.getTaskId()).getName();
				break;
			case DURATION_COLUMN_IDX:
				text = StringHelper.hundredthToEntry(c.getDurationId());
				break;
			default:
				throw new Error(
						Strings.getString("ContributionsViewerTable.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}
			return text;
		});
	}

	/**
	 * Retourne le chemin de la tache associée à l'identifiant spécifié.
	 *
	 * @param taskId
	 *            l'identifiant de la tache.
	 * @return le chemin.
	 * @throws ModelException
	 *             levé en cas de viloation du modèle.
	 */
	private String getCachedTaskCodePath(long taskId) throws ModelException {
		String taskCodePath = taskCodePathsCache.get(taskId);
		if (taskCodePath == null) {
			log.debug("Registering in cache task code path for taskId=" + taskId); //$NON-NLS-1$
			Task task = getCachedTask(taskId);
			taskCodePath = modelMgr.getTaskCodePath(task);
			taskCodePathsCache.put(taskId, taskCodePath);
		}
		return taskCodePath;
	}

	/**
	 * Retourne la tache associée à l'identifiant spécifié.
	 *
	 * @param taskId
	 *            l'identifiant de la tache.
	 * @return la tache.
	 */
	private Task getCachedTask(long taskId) {
		Task task = tasksCache.get(taskId);
		if (task == null) {
			log.debug("Registering in cache task for taskId=" + taskId); //$NON-NLS-1$
			task = modelMgr.getTask(taskId);
			tasksCache.put(taskId, task);
		}
		return task;
	}

	/**
	 * Retourne le collaborateur associée à l'identifiant spécifié.
	 *
	 * @param collaboratorId
	 *            l'identifiant du collaborateur.
	 * @return le collaborateur.
	 */
	private Collaborator getCachedCollaborator(long collaboratorId) {
		Collaborator collaborator = collaboratorsCache
				.get(collaboratorId);
		if (collaborator == null) {
			log.debug("Registering in cache collaborator for collaboratorId=" + collaboratorId); //$NON-NLS-1$
			collaborator = modelMgr.getCollaborator(collaboratorId);
			collaboratorsCache.put(collaboratorId, collaborator);
		}
		return collaborator;
	}


	@Override
	public void menuShown(MenuEvent e) {
		log.debug("menuShown(" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// TableItem[] selection = tableViewer.getTable().getSelection();
		// boolean emptySelection = selection.length==0;
		// boolean singleSelection = selection.length==1;
		exportItem.setEnabled(true);
	}


	@Override
	public void menuHidden(MenuEvent e) {
		// Do nothing...
	}


	@Override
	public void widgetSelected(final SelectionEvent evt) {
		log.debug("SelectionListener.widgetSelected(" + evt + ")"); //$NON-NLS-1$ //$NON-NLS-2$

		SafeRunner.exec(parent.getShell(), () -> {
			// Cas d'une demande d'export du tableau
			if (exportItem.equals(evt.getSource())) {
				// Export du tableau
				SWTHelper.exportToWorkBook(tableViewer.getTable());
			}
		});
	}


	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

}
