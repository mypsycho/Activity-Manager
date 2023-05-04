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
package org.activitymgr.ui.rcp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskContributionsSums;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.DatabaseUI.IDbStatusListener;
import org.activitymgr.ui.rcp.dialogs.ContributionsViewerDialog;
import org.activitymgr.ui.rcp.dialogs.DialogException;
import org.activitymgr.ui.rcp.dialogs.TaskChooserTreeWithHistoryDialog;
import org.activitymgr.ui.rcp.util.AbstractTableMgrUI;
import org.activitymgr.ui.rcp.util.SWTHelper;
import org.activitymgr.ui.rcp.util.TableOrTreeColumnsMgr;
import org.activitymgr.ui.rcp.util.TaskFinderPanel;
import org.activitymgr.ui.rcp.util.UITechException;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * IHM de gestion des tâches.
 */
public class TasksUI extends AbstractTableMgrUI
		implements IDbStatusListener, ICellModifier, ITreeContentProvider,
		ITableColorProvider, ContributionsUI.IContributionListener {

	/** Logger */
	private static Logger log = Logger.getLogger(TasksUI.class);

	/** Constantes associées aux colonnes */
	public static final int NAME_COLUMN_IDX = 0;
	public static final int CODE_COLUMN_IDX = 1;
	public static final int INITIAL_FUND_COLUMN_IDX = 2;
	public static final int INITIALLY_CONSUMED_COLUMN_IDX = 3;
	public static final int CONSUMED_COLUMN_IDX = 4;
	public static final int TODO_COLUMN_IDX = 5;
	public static final int DELTA_COLUMN_IDX = 6;
	public static final int COMMENT_COLUMN_IDX = 7;
	public static final int CLOSED_COLUMN_IDX = 8;

	private static final BiConsumer<MenuItem, TreeItem[]> FOR_SINGLE =
			enableMItem((emptySelection, singleSelection) -> singleSelection);

	private TableOrTreeColumnsMgr treeColsMgr;

	/** Listeners */
	private List<ITaskListener> listeners = new ArrayList<>();

	/** Viewer */
	private TreeViewer treeViewer;

	private List<Consumer<TreeItem[]>> menuRefreshs = new ArrayList<>();

	/** Composant parent */
	private Composite parent;

	/** Popup permettant de choisir une tache */
	private TaskChooserTreeWithHistoryDialog taskChooserDialog;

	/** Popup permettant de lister les contributions d'une tache */
	private ContributionsViewerDialog contribsViewerDialog;

	/** Panneau de recherche de tache */
	private TaskFinderPanel taskFinderPanel;

	/** Couleur de police de caractère utilisée pour les zones non modifiables */
	private Color disabledFGColor;

	/**
	 * Booléen permettant de savoir si un refresh doit être exécuté lors du
	 * prochain paint
	 */
	private boolean needRefresh = false;


	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 *
	 * @param tabItem
	 *            item parent.
	 * @param modelMgr
	 *            the model manager instance.
	 */
	public TasksUI(TabItem tabItem, IModelMgr modelMgr) {
		this(tabItem.getParent(), modelMgr);
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 *
	 * @param parentComposite
	 *            composant parent.
	 */
	public TasksUI(Composite parentComposite, final IModelMgr modelMgr) {
		super(parentComposite, modelMgr);

		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		// Panneau permettant de recherche une tache
		taskFinderPanel = new TaskFinderPanel(parent, modelMgr);
		GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		taskFinderPanel.setLayoutData(gridData);
		taskFinderPanel.addTaskListener(selectedTask -> {
			TaskSums selectedElement = safeExec(null, () -> {
				TaskSums result = null;
				for (Object element : treeViewer.getExpandedElements()) {
					TaskSums sums = (TaskSums) element;
					if (sums.getTask().equals(selectedTask)) {
						result = sums;
						break;
					}
				}
				if (result == null) {
					result = modelMgr.getTaskSums(selectedTask.getId(), null, null);
				}
				return result;
			});

			if (selectedElement != null) {
				treeViewer.setSelection(new StructuredSelection(selectedElement));
				treeViewer.getTree().setFocus();
			}
		});

		initTree();
		initTreeDnd();
		initColumns();
		initMenu();

		// Initialisation des popups
		taskChooserDialog = new TaskChooserTreeWithHistoryDialog(parent.getShell(), modelMgr);
		contribsViewerDialog = new ContributionsViewerDialog(parent.getShell(), modelMgr);

		bindKeys(parentComposite);

		log.debug("UI initialization done"); //$NON-NLS-1$

		// Ajout d'un listener permettant de détecter lorsque le
		// composant est affiché (passage d'un onglet à l'autre)
		parent.addPaintListener(paintevent -> {
			if (needRefresh) {
				needRefresh = false;
				treeViewer.refresh();
			}
		});

	}

	private void bindKeys(Composite parentComposite) {
		// Ajout de KeyListeners pour faciliter le déplacement vers le bas et
		// vers le haut des taches
		// (Rq: les accélérateurs sont ignorés dans les menus contextuels)
		KeyListener keyListener = KeyListener.keyReleasedAdapter(evt -> {
			if (evt.stateMask != SWT.CTRL) {
				return;
			}
			TreeSelectionAction action;

			switch (evt.keyCode) {
			case SWT.ARROW_UP:
				action = selection -> moveTask(selection, true);
				break;
			case SWT.ARROW_DOWN:
				action = selection -> moveTask(selection, false);
				break;
			case 'c':
				action = this::copyTask;
				break;
			default:
				return;
			}

			TreeItem[] selection = treeViewer.getTree().getSelection();
			safeExec(() -> action.accept(selection));
		});
		parentComposite.addKeyListener(keyListener);
		treeViewer.getTree().addKeyListener(keyListener);
	}

	private void moveTask(TreeItem[] selection, boolean up) throws Exception {
		TaskSums selected = (TaskSums) selection[0].getData();
		Task selectedTask = selected.getTask();
		String oldTaskFullpath = selectedTask.getFullPath();
		if (up) {
			modelMgr.moveUpTask(selectedTask);
		} else {
			modelMgr.moveDownTask(selectedTask);
		}
		// Mise à jour de l'IHM
		treeViewer.refresh(selection[0].getParent().getData(), false);
		// Notification des listeners
		notifyTaskMoved(oldTaskFullpath, selectedTask);
	}

	private void copyTask(TreeItem[] selection) throws Exception {
		// Implémentation en multi sélection => pour l'instant on ne
		// veut gérer qu'une seule tache à la fois
		// TreeItem[] selectedItems = treeViewer.getTree().getSelection();
		// Clipboard clipboard = new Clipboard(parent.getDisplay());
		// String[] taskCodePaths = new String[selectedItems.length];
		// Transfer[] transfers = new Transfer[selectedItems.length];
		// for (int i=0; i<selectedItems.length; i++) {
		// Task task = (Task) selectedItems[i].getData();
		// taskCodePaths[i] = modelMgr.getTaskCodePath(task);
		// transfers[i] = TextTransfer.getInstance();
		// }
		// clipboard.setContents(taskCodePaths, transfers);
		// clipboard.dispose();

		// Implémentation en sélection simple
		if (selection != null && selection.length > 0) {
			TreeItem selectedItem = selection[0];
			Clipboard clipboard = new Clipboard(parent.getDisplay());
			TaskSums selected = (TaskSums) selectedItem.getData();
			String taskCodePath = modelMgr.getTaskCodePath(selected.getTask());
			clipboard.setContents(new String[] { taskCodePath },
					new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
		}
	}

	private void initMenu() {

		// Configuration du menu popup
		final Menu menu = new Menu(treeViewer.getTree());
		menu.addMenuListener(MenuListener.menuShownAdapter(evt -> updateMenu()));

		createNewMenu(menu);
		createMoveMenu(menu);

		createTreeMenuItem(menu, "COPY",  //$NON-NLS-1$
				enableMItem((emptySelection, singleSelection) -> !emptySelection),
				this::copyTask);

		createTreeMenuItem(menu, "REMOVE",  //$NON-NLS-1$
				FOR_SINGLE,
				selection -> {
					TreeItem selectedItem = selection[0];
					TreeItem parentItem = selectedItem.getParentItem();
					TaskSums selected = (TaskSums) selectedItem.getData();
					Task selectedTask = selected.getTask();
					Task parentTask = parentItem != null ? ((TaskSums) parentItem
							.getData()).getTask() : null;
					// Suppression
					modelMgr.removeTask(selectedTask);
					// Suppression dans l'arbre
					treeViewer.remove(selected);
					updateParentSums(parentItem);
					// Mise à jour des taches soeurs
					if (parentTask != null)
						treeViewer.refresh(parentItem.getData());
					else
						treeViewer.refresh();
					// Notification des listeners
					notifyTaskRemoved(selectedTask);
				});

		createTreeMenuItem(menu, "EXPAND_ALL",  //$NON-NLS-1$
				FOR_SINGLE,
				selection -> {
					TreeItem selectedItem = treeViewer.getTree().getSelection()[0];
					treeViewer.expandToLevel(selectedItem.getData(),
							AbstractTreeViewer.ALL_LEVELS);
				});
		createTreeMenuItem(menu, "COLLAPSE_ALL",  //$NON-NLS-1$
				FOR_SINGLE,
				selection -> {
					TreeItem selectedItem = treeViewer.getTree().getSelection()[0];
					treeViewer.collapseToLevel(selectedItem.getData(),
							AbstractTreeViewer.ALL_LEVELS);
				});

		createTreeMenuItem(menu, "LIST_CONTRIBUTIONS",  //$NON-NLS-1$
				FOR_SINGLE,
				selection -> {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task selectedTask = selected.getTask();
					contribsViewerDialog.setFilter(selectedTask, null);
					// Ouverture du dialogue
					contribsViewerDialog.open();
				});

		createTreeMenuItem(menu, "REFRESH",  //$NON-NLS-1$
				FOR_SINGLE,
				selection -> {
					// Récupération du noeud parent
					TreeItem selectedItem = selection[0];
					TreeItem parentItem = selectedItem.getParentItem();
					TaskSums parentTaskSums = parentItem != null
							? (TaskSums) parentItem.getData()
							: null;
					// Mise à jour
					if (parentTaskSums != null) {
						treeViewer.refresh(parentTaskSums);
					} else {
						treeViewer.refresh();
					}
				});

		createImportExportMenu(menu);
		treeViewer.getTree().setMenu(menu);
	}

	private Menu createSubMenu(Menu menu, String labelId) {
		// Sous-menu 'Nouveau'
		MenuItem subItem = new MenuItem(menu, SWT.CASCADE);
		subItem.setText(Strings.getString("TasksUI.menuitems." + labelId)); //$NON-NLS-1$
		Menu result = new Menu(subItem);
		subItem.setMenu(result);
		return result;
	}

	private static BiConsumer<MenuItem, TreeItem[]> enableMItem(TreeActionEnable enabler) {
		return (item, selection) -> {
			item.setEnabled(enabler.enable(
					selection.length == 0,
					selection.length == 1));
		};
	}

	private MenuItem createTreeMenuItem(Menu menu, String code,
			BiConsumer<MenuItem, TreeItem[]> onUpdate, TreeSelectionAction action) {
		MenuItem result = new MenuItem(menu, SWT.CASCADE);
		result.setText(Strings.getString("TasksUI.menuitems." + code)); //$NON-NLS-1$
		result.addSelectionListener(onTreeSelection(action));
		if (onUpdate != null) {
			menuRefreshs.add(selection -> onUpdate.accept(result, selection));
		}
		return result;
	}


	private void createNewMenu(Menu menu) {
		// Sous-menu 'Nouveau'
		Menu newMenu = createSubMenu(menu, "NEW"); //$NON-NLS-1$

		createTreeMenuItem(newMenu, "NEW_TASK",
				enableMItem((emptySelection, singleSelection) -> emptySelection || singleSelection),
				selection -> {
					// Récupération du noeud parent
					TreeItem parentItem = selection.length > 0
							? selection[0].getParentItem()
							: null;
					TaskSums parentSums = parentItem != null
							? (TaskSums) parentItem.getData()
							: null;
					// Création de la tache
					Task newTask = newTask(parentSums);
					// Notification des listeners
					notifyTaskAdded(newTask);
				});

		createTreeMenuItem(newMenu, "NEW_SUBTASK",
				FOR_SINGLE,
				selection -> {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task newTask = newTask(selected);
					// Then remember that the parent task is not a leaf task
					selected.setLeaf(false);
					// Notification des listeners
					notifyTaskAdded(newTask);
				});
	}


	private void createMoveMenu(Menu menu) {

		// Sous-menu 'Déplacer'
		Menu moveToMenu = createSubMenu(menu, "MOVE"); //$NON-NLS-1$

		createTreeMenuItem(moveToMenu, "MOVE_UP", //$NON-NLS-1$
				FOR_SINGLE,
				selection -> moveTask(selection, true));

		createTreeMenuItem(moveToMenu, "MOVE_DOWN", //$NON-NLS-1$
				FOR_SINGLE,
				selection -> moveTask(selection, false));

		createTreeMenuItem(moveToMenu, "MOVE_BEFORE_ANOTHER_TASK", //$NON-NLS-1$
				FOR_SINGLE,
				selection -> moveTaskItem(selection, true));

		createTreeMenuItem(moveToMenu, "MOVE_AFTER_ANOTHER_TASK", //$NON-NLS-1$
				FOR_SINGLE,
				selection -> moveTaskItem(selection, false));

		createTreeMenuItem(moveToMenu, "MOVE_UNDER_ANOTHER_TASK", //$NON-NLS-1$
				FOR_SINGLE,
				selection -> {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task taskToMove = selected.getTask();
					// Récupération du noeud parent
					TreeItem parentItem = selection[0].getParentItem();
					final Task srcParentTask = parentItem != null
							? ((TaskSums) parentItem.getData()).getTask()
									: null;
					// Création du valideur
					taskChooserDialog.setValidator(selectedTask -> {
						if (srcParentTask != null && srcParentTask.equals(selectedTask))
							throw new DialogException(
									Strings.getString("TasksUI.errors.MOVE_TO_SAME_PARENT"), null); //$NON-NLS-1$
						try {
							modelMgr.checkAcceptsSubtasks(selectedTask);
						} catch (ModelException e1) {
							throw new DialogException(e1.getMessage(), null);
						}
					});
					// Affichage du popup
					if (taskChooserDialog.open() == Window.OK) {
						Task newParentTask = (Task) taskChooserDialog.getValue();
						doMoveToAnotherTask(taskToMove, newParentTask);
					}
				});

		createTreeMenuItem(moveToMenu, "MOVE_UNDER_ROOT", //$NON-NLS-1$
				(item, selection) -> {
					boolean enable = selection.length == 1;
					if (enable) {
						TaskSums selected = (TaskSums) selection[0].getData();
						// only in sub-element
						enable = !selected.getTask().getPath().isEmpty();
					}

					item.setEnabled(enable);
				},
				selection -> {
					TaskSums selected = (TaskSums) selection[0].getData();
					Task taskToMove = selected.getTask();
					String oldTaskFullpath = taskToMove.getFullPath();
					// Déplacement
					modelMgr.moveTask(taskToMove, null);
					treeViewer.refresh();
					// Notification des listeners
					notifyTaskMoved(oldTaskFullpath, taskToMove);
				});

	}

	private void createImportExportMenu(Menu menu) {

		Menu exportMenu = createSubMenu(menu, "EXPORT_IMPORT"); //$NON-NLS-1$

		createTreeMenuItem(exportMenu, "XLS_EXPORT", //$NON-NLS-1$
				null,
				selection ->  {
					Long parentTaskId = null;
					if (selection.length > 0) {
						TaskSums selected = (TaskSums) selection[0].getData();
						parentTaskId = selected.getTask().getId();
					}
					FileDialog fd = new FileDialog(parent.getShell(), SWT.APPLICATION_MODAL | SWT.SAVE);
					fd.setFilterExtensions(new String[] { "*.xls", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
					fd.setOverwrite(true);
					String fileName = fd.open();
					// Si le nom est spécifié
					if (fileName != null) {
						try {
							// Correction du nom du fichier si besoin
							if (!fileName.endsWith(".xls")) //$NON-NLS-1$
								fileName += ".xls"; //$NON-NLS-1$
							// Sauvegarde du document
							byte[] excel = modelMgr.exportToExcel(parentTaskId);
							FileOutputStream out = new FileOutputStream(fileName);
							out.write(excel);
							out.close();
						} catch (IOException e) {
							log.error("I/O exception", e); //$NON-NLS-1$
							throw new UITechException(
									Strings.getString("SWTHelper.errors.IO_EXCEPTION_WHILE_EXPORTING"), e); //$NON-NLS-1$
						}
					}
				});

		createTreeMenuItem(exportMenu, "XLS_IMPORT", //$NON-NLS-1$
				null,
				selection ->  {
					Long parentTaskId = null;
					TaskSums selected = null;
					if (selection.length > 0) {
						selected = (TaskSums) selection[0].getData();
						parentTaskId = selected.getTask().getId();
						// Expand the tree
						treeViewer.expandToLevel(selected, 1);
					}
					FileDialog fd = new FileDialog(parent.getShell(), SWT.APPLICATION_MODAL | SWT.OPEN);
					fd.setFilterExtensions(new String[] { "*.xls", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
					String fileName = fd.open();
					// Si le nom est spécifié
					if (fileName != null) {
						try {
							FileInputStream in = new FileInputStream(fileName);
							modelMgr.importFromExcel(parentTaskId, in);
							in.close();
							// Refresh the tree
							treeViewer.refresh();
						} catch (IOException e) {
							log.error("I/O exception", e); //$NON-NLS-1$
							throw new UITechException(
									Strings.getString("SWTHelper.errors.IO_EXCEPTION_WHILE_EXPORTING"), e); //$NON-NLS-1$
						}
					}
				});

		createTreeMenuItem(exportMenu, "XLS_SNAPSHOT_EXPORT", //$NON-NLS-1$
				null,
				selection -> SWTHelper.exportToWorkBook(treeViewer.getTree()));

	}

	private void initColumns() {
		Tree tree = treeViewer.getTree();

		// Configuration des colonnes
		treeColsMgr = new TableOrTreeColumnsMgr();
		initColumn("NAME", 200, SWT.LEFT); //$NON-NLS-1$
		initColumn("CODE", 70, SWT.LEFT); //$NON-NLS-1$
		initColumn("BUDGET", 70, SWT.RIGHT); //$NON-NLS-1$
		initColumn("INITIAL", 70, SWT.RIGHT); //$NON-NLS-1$
		initColumn("CONSUMED", 70, SWT.RIGHT); //$NON-NLS-1$
		initColumn("TODO", 70, SWT.RIGHT); //$NON-NLS-1$
		initColumn("DELTA", 70, SWT.RIGHT); //$NON-NLS-1$
		initColumn("COMMENT", 200, SWT.LEFT); //$NON-NLS-1$
		initColumn("CLOSED", 70, SWT.CENTER); //$NON-NLS-1$

		treeColsMgr.configureTree(treeViewer);

		// Configuration des éditeurs de cellules
		treeViewer.setCellEditors(new CellEditor[] {
				new TextCellEditor(tree), // name
				new TextCellEditor(tree), // code
				new TextCellEditor(tree), // budget
				new TextCellEditor(tree), // initial
				null, // consumed
				new TextCellEditor(tree), // Todo
				null, // Delta
				new TextCellEditor(tree), // comment
				new CheckboxCellEditor(tree) // comment

		});
	}

	private void initColumn(String columnCode, int columnWidth, int columnAlignment) {
		treeColsMgr.addColumn(columnCode,
				Strings.getString("TasksUI.columns.TASK_" + columnCode),  //$NON-NLS-1$
				columnWidth, columnAlignment);
	}


	private void initTreeDnd() {
		Tree tree = treeViewer.getTree();

	    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
	    int operations = DND.DROP_MOVE;
	    final DragSource source = new DragSource(tree, operations);
	    source.setTransfer(types);
	    source.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				event.doit = selection.size() == 1;
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				TaskSums firstElement = (TaskSums) ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
				event.data = String.valueOf(firstElement.getTask().getId());
			}
		});

		DropTarget target = new DropTarget(tree, operations);
		target.setTransfer(types);
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(final DropTargetEvent event) {
				safeExec(()->{
					Task taskToMove = modelMgr.getTask(Long.parseLong((String)event.data));
					TreeItem item = (TreeItem)event.item;
					if (item == null) {
						// If we move the task to outside of the tree (at the bottom), simply
						// move the task under root
						doMoveToAnotherTask(taskToMove, null);
					} else {
						Task destTask = ((TaskSums) item.getData()).getTask();
						Point pt = tree.getDisplay().map(null, tree, event.x, event.y);
						Rectangle bounds = item.getBounds();
						if (pt.y < bounds.y + bounds.height/3) {
							doMoveBeforeOrAfter(taskToMove, destTask, true);
						} else if (pt.y > bounds.y + 2*bounds.height/3) {
							doMoveBeforeOrAfter(taskToMove, destTask, false);
						} else {
							treeViewer.expandToLevel(event.item.getData(), 1);
							doMoveToAnotherTask(taskToMove, destTask);
						}
					}
				});
			}
			@Override
			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
				if (event.item != null) {
					TreeItem item = (TreeItem)event.item;
					Point pt = tree.getDisplay().map(null, tree, event.x, event.y);
					Rectangle bounds = item.getBounds();
					if (pt.y < bounds.y + bounds.height/3) {
						event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
					} else if (pt.y > bounds.y + 2*bounds.height/3) {
						event.feedback |= DND.FEEDBACK_INSERT_AFTER;
					} else {
						event.feedback |= DND.FEEDBACK_SELECT;
					}
				}
			}
		});

	}

	private Tree initTree() {
		// Arbre tableau
		final Tree tree = new Tree(parent,
				SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 75;
		tree.setLayoutData(gridData);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.setEnabled(true);

		// Création du viewer
		treeViewer = new TreeViewer(tree);
		treeViewer.setCellModifier(this);
		treeViewer.setContentProvider(this);
		treeViewer.setLabelProvider(this);

		// Création des polices de caractère
		disabledFGColor = tree.getDisplay().getSystemColor(
				SWT.COLOR_TITLE_INACTIVE_FOREGROUND);

		return tree;
	}


	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(null);
	}

	@Override
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element //$NON-NLS-1$
				+ ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final TaskSums taskSums = (TaskSums) element;
		final int propertyIdx = treeColsMgr.getColumnIndex(property);
		return safeExec(false, () -> {

			switch (propertyIdx) {
			case NAME_COLUMN_IDX:
			case CODE_COLUMN_IDX:
				return true;
			case INITIAL_FUND_COLUMN_IDX:
			case INITIALLY_CONSUMED_COLUMN_IDX:
			case TODO_COLUMN_IDX:
				return taskSums.isLeaf();
			case CONSUMED_COLUMN_IDX:
			case DELTA_COLUMN_IDX:
				return false;
			case COMMENT_COLUMN_IDX:
			case CLOSED_COLUMN_IDX:
				return true;

			default:
				throw new UITechException(
						Strings.getString("TasksUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}

		});
	}

	@Override
	public Object getValue(Object element, String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final TaskSums taskSums = (TaskSums) element;
		final int propertyIdx = treeColsMgr.getColumnIndex(property);
		return safeExec(null, () -> {

			switch (propertyIdx) {
			case NAME_COLUMN_IDX:
				return taskSums.getTask().getName();
			case CODE_COLUMN_IDX:
				return taskSums.getTask().getCode();

			case INITIAL_FUND_COLUMN_IDX:
				return StringHelper.hundredthToEntry(taskSums.getBudgetSum());

			case INITIALLY_CONSUMED_COLUMN_IDX:
				return StringHelper.hundredthToEntry(taskSums.getInitiallyConsumedSum());

			case CONSUMED_COLUMN_IDX:
				return StringHelper.hundredthToEntry(taskSums
						.getContributionsSums().getConsumedSum()
						+ taskSums.getInitiallyConsumedSum());

			case TODO_COLUMN_IDX:
				return StringHelper.hundredthToEntry(taskSums.getTodoSum());

			case DELTA_COLUMN_IDX:
				long delta = taskSums.getBudgetSum()
						- taskSums.getInitiallyConsumedSum()
						- taskSums.getContributionsSums().getConsumedSum()
						- taskSums.getTodoSum();
				return StringHelper.hundredthToEntry(delta);

			case COMMENT_COLUMN_IDX:
				String comment = taskSums.getTask().getComment();
				return comment != null ? comment : ""; //$NON-NLS-1$

			case CLOSED_COLUMN_IDX:
				return taskSums.getTask().isClosed();
			default:
				throw new UITechException(
						Strings.getString("TasksUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}
		});
	}

	@Override
	public void modify(Object element, String property, final Object value) {
		log.debug("ICellModifier.modify(" + element //$NON-NLS-1$
				+ ", " + property  //$NON-NLS-1$
				+ ", " + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final TreeItem item = (TreeItem) element;
		final TaskSums taskSums = (TaskSums) item.getData();
		final Task task = taskSums.getTask();
		final int columnIdx = treeColsMgr.getColumnIndex(property);
		safeExec(()->{
			boolean parentsMustBeRefreshed = false;
			switch (columnIdx) {
			case NAME_COLUMN_IDX:
				task.setName((String) value);
				break;
			case CODE_COLUMN_IDX:
				task.setCode(((String) value).trim());
				break;
			case INITIAL_FUND_COLUMN_IDX:
				long newInitialFund = StringHelper
						.entryToHundredth((String) value);
				task.setBudget(newInitialFund);
				taskSums.setBudgetSum(newInitialFund);
				parentsMustBeRefreshed = true;
				break;
			case INITIALLY_CONSUMED_COLUMN_IDX:
				long newInitiallyConsumed = StringHelper
						.entryToHundredth((String) value);
				task.setInitiallyConsumed(newInitiallyConsumed);
				taskSums.setInitiallyConsumedSum(newInitiallyConsumed);
				parentsMustBeRefreshed = true;
				break;
			case TODO_COLUMN_IDX:
				long newTodo = StringHelper
						.entryToHundredth((String) value);
				task.setTodo(newTodo);
				taskSums.setTodoSum(newTodo);
				parentsMustBeRefreshed = true;
				break;
			case COMMENT_COLUMN_IDX:
				String comment = (String) value;
				if (comment != null) {
					comment = comment.trim();
				}
				// Si le commentaire est vide, il devient nul
				if ("".equals(comment)) {//$NON-NLS-1$
					comment = null;
				}
				task.setComment((String) value);
				break;

			case CLOSED_COLUMN_IDX:
				task.setClosed((Boolean) value);
				break;

			case CONSUMED_COLUMN_IDX:
			case DELTA_COLUMN_IDX:
				throw new UITechException(
						Strings.getString("TasksUI.errros.READ_ONLY_COLUMN")); //$NON-NLS-1$
			default:
				throw new UITechException(
						Strings.getString("TasksUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}

			// Mise à jour en base
			modelMgr.updateTask(task);
			// Mise à jour des labels
			if (parentsMustBeRefreshed) {
				// Mise à jour des sommes des taches parentes
				updateParentSums(item);
			} else {
				// Notification de la mise à jour uniquement pour la tache
				notifyLabelProviderListener(taskSums);
			}
			// Notification de la mise à jour de la tache pour les listeners
			notifyTaskUpdated(task);

		});

	}

	protected void notifyLabelProviderListener(TaskSums... sums) {
		super.notifyLabelProviderListener(new LabelProviderChangedEvent(this, sums));
	}

	/**
	 * Met à jour les sommes associés aux taches de la branche associée à l'item
	 * spécifié.
	 *
	 * @param item
	 *            l'item de tableau.
	 * @throws ModelException
	 *             levée en cas d'invalidité associée au modèle.
	 */
	private void updateParentSums(TreeItem item) throws ModelException {
		List<TaskSums> list = new ArrayList<>();
		// Nettoyage du cache
		TreeItem cursor = item;
		while (cursor != null) {
			TaskSums taskCursor = (TaskSums) cursor.getData();
			TaskSums newTaskSums = modelMgr.getTaskSums(taskCursor.getTask().getId(), null, null);
			cursor.setData(newTaskSums);
			log.debug("Update task " + newTaskSums.getTask().getName()); //$NON-NLS-1$
			list.add(0, newTaskSums);
			cursor = cursor.getParentItem();
		}
		// Notification de la mise à jour (ce qui recharge automatiquement
		// le cache des sommes de taches)
		notifyLabelProviderListener(list.toArray(new TaskSums[list.size()]));
	}


	@Override
	public boolean hasChildren(Object element) {
		log.debug("ITreeContentProvider.getChildren(" + element + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		TaskSums sums = (TaskSums) element;
		return !sums.isLeaf();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		log.debug("ITreeContentProvider.getChildren(" + parentElement + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final TaskSums parentTaskSums = (TaskSums) parentElement;
		return safeExec(new Object[] {}, () -> {
			Task parentTask = parentTaskSums != null ? parentTaskSums.getTask() : null;
			return modelMgr.getSubTasksSums(parentTask, null, null).toArray();
		});
	}

	@Override
	public Object getParent(Object element) {
		log.debug("ITreeContentProvider.getParent(" + element + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		final TaskSums sums = (TaskSums) element;
		return safeExec(new Object[] {}, () -> {
			Task parentTask = modelMgr.getParentTask(sums.getTask());
			return parentTask == null ? treeViewer.getInput() : modelMgr.getTaskSums(parentTask.getId(), null, null);
		});
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		log.debug("ITableLabelProvider.getColumnText(" + element + ", " + columnIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Object value =
				getValue(element,
				treeColsMgr.getColumnCode(columnIndex));
		log.debug("  =>" + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		return value != null ? String.valueOf(value) : "";
	}


	@Override
	public Color getBackground(Object element, int columnIndex) {
		return null;
	}

	@Override
	public Color getForeground(Object element, int columnIndex) {
		return canModify(element, treeColsMgr.getColumnCode(columnIndex)) ? null : disabledFGColor;
	}

	interface TreeSelectionAction {
		void accept(TreeItem[] selecteds) throws Exception;
	}

	private SelectionListener onTreeSelection(TreeSelectionAction action) {
		return new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(final SelectionEvent evt) {
				TreeItem[] selection = treeViewer.getTree().getSelection();
				safeExec(() -> action.accept(selection));
			}
		};

	}

	private void moveTaskItem(TreeItem[] selection, boolean before) throws ModelException {
		TaskSums selected = (TaskSums) selection[0].getData();
		Task selectedTask = selected.getTask();
		final Task finalSelectedTask = selectedTask;
		// Création du valideur
		taskChooserDialog.setValidator(choosenTask -> {
			if (finalSelectedTask.equals(choosenTask))
				throw new DialogException("Please select another task", null); //$NON-NLS-1$
			});
		taskChooserDialog.setValue(selectedTask);
		if (taskChooserDialog.open() == Window.OK) {
			Task chosenTask = (Task) taskChooserDialog.getValue();
			doMoveBeforeOrAfter(selectedTask, chosenTask, before);
		}
	}


	/**
	 * Move to another task.
	 *
	 * @param taskToMove the task to move.
	 * @param newParentTask the new parent task.
	 * @throws ModelException thrown if a model error is detected.
	 */
	private void doMoveToAnotherTask(Task taskToMove, Task newParentTask)
			throws ModelException {
		log.debug("Selected parent task=" + newParentTask); //$NON-NLS-1$
		modelMgr.moveTask(taskToMove, newParentTask);
		// Rafraichir l'ancien et le nouveau parent ne suffit
		// pas
		// dans le cas ou le parent destination change de numéro
		// (ex : déplacement d'une tache A vers une tache B avec
		// A et B initialement soeurs, A étant placé avant B)
		// treeViewer.refresh(newParentTask);
		// treeViewer.refresh(srcParentTask);
		treeViewer.refresh();
		// Notification des listeners
		String oldTaskFullpath = taskToMove.getFullPath();
		notifyTaskMoved(oldTaskFullpath, taskToMove);
	}

	/**
	 * Moves a task before or after another task.
	 * @param taskToMove the task to move.
	 * @param chosenTask the chosen task.
	 * @param before <code>true</code> if the task must be moved before the chosen tasks, <code>false</code> if it must be moved after.
	 * @throws ModelException if a model error occurs.
	 */
	private void doMoveBeforeOrAfter(Task taskToMove, Task chosenTask,
			boolean before) throws ModelException {
		boolean needRefresh = false;
		// Traitement du changement éventuel de parent
		if (!chosenTask.getPath()
				.equals(taskToMove.getPath())) {
			Task destParentTask = modelMgr.getParentTask(chosenTask);
			// Déplacement
			modelMgr.moveTask(taskToMove, destParentTask);
			// Rafraichissement de la tache
			taskToMove = modelMgr.getTask(taskToMove
					.getId());
			needRefresh = true;
		}
		// Déplacement de la tache
		int targetNumber = chosenTask.getNumber();
		if (before && targetNumber > taskToMove.getNumber()) {
			targetNumber--;
		} else if (!before && targetNumber < taskToMove.getNumber()) {
			targetNumber++;
		}
		if (targetNumber != taskToMove.getNumber()) {
			modelMgr.moveTaskUpOrDown(taskToMove, targetNumber);
			needRefresh = true;
		}
		if (needRefresh) {
			// Notification des listeners
			String oldTaskFullpath = taskToMove.getFullPath();
			notifyTaskMoved(oldTaskFullpath, taskToMove);
			// Mise à jour de l'IHM
			treeViewer.refresh();
		}
	}


	/**
	 * Ajoute une tache.
	 *
	 * @param parentSums
	 *            les totaux de la tache parente ou null pour une tache racine.
	 * @throws ModelException
	 *             levé en cas de violation du modèle de données.
	 * @return la tache créée.
	 */
	private Task newTask(TaskSums parentSums) throws ModelException {
		Task parentTask = parentSums != null ? parentSums.getTask() : null;
		log.debug("newTask(" + parentTask + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// Création de la nouvelle tache
		Task newTask = modelMgr.createNewTask(parentTask);
		// Ajout dans l'arbre et création en base
		TaskSums newSums = new TaskSums();
		newSums.setLeaf(true);
		newSums.setTask(newTask);
		newSums.setContributionsSums(new TaskContributionsSums());
		treeViewer.add(parentSums == null ? treeViewer.getInput() : parentSums,
				newSums);
		treeViewer.setSelection(new StructuredSelection(newSums), true);
		return newTask;
	}

	private interface TreeActionEnable {
		boolean enable(boolean emptySelection, boolean singleSelection);
	}

	private void updateMenu() {
		TreeItem[] selection = treeViewer.getTree().getSelection();
		menuRefreshs.forEach(it -> it.accept(selection));
	}


	@Override
	public void databaseOpened() {
		// Création d'une racine fictive
		treeViewer.setInput(ROOT_NODE);
	}


	@Override
	public void databaseClosed() {
		Tree tree = treeViewer.getTree();
		TreeItem[] items = tree.getItems();
		for (TreeItem item : items) {
			item.dispose();
		}
		taskChooserDialog.databaseClosed();
	}

	/**
	 * Ajoute un listener.
	 *
	 * @param listener
	 *            le nouveau listener.
	 */
	public void addTaskListener(ITaskListener listener) {
		listeners.add(listener);
	}

	/**
	 * Ajoute un listener.
	 *
	 * @param listener
	 *            le nouveau listener.
	 */
	public void removeTaskListener(ITaskListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifie les listeners qu'une tache a été ajoutée.
	 *
	 * @param newTask
	 *            la tache ajouté.
	 */
	private void notifyTaskAdded(Task newTask) {
		Iterator<ITaskListener> it = listeners.iterator();
		while (it.hasNext()) {
			ITaskListener listener = it.next();
			listener.taskAdded(newTask);
		}
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskAdded(newTask);
	}

	/**
	 * Notifie les listeners qu'une tache a été supprimée.
	 *
	 * @param task
	 *            la tache supprimée.
	 */
	private void notifyTaskRemoved(Task task) {
		Iterator<ITaskListener> it = listeners.iterator();
		while (it.hasNext()) {
			ITaskListener listener = it.next();
			listener.taskRemoved(task);
		}
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskRemoved(task);
	}

	/**
	 * Notifie les listeners qu'une tache a été modifiée.
	 *
	 * @param task
	 *            la tache modifiée.
	 */
	private void notifyTaskUpdated(Task task) {
		Iterator<ITaskListener> it = listeners.iterator();
		while (it.hasNext()) {
			ITaskListener listener = it.next();
			listener.taskUpdated(task);
		}
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskUpdated(task);
	}

	/**
	 * Notifie les listeners qu'une tache a été déplacée.
	 *
	 * @param task
	 *            la tache modifiée.
	 */
	private void notifyTaskMoved(String oldTaskPath, Task task) {
		Iterator<ITaskListener> it = listeners.iterator();
		while (it.hasNext()) {
			ITaskListener listener = it.next();
			listener.taskMoved(oldTaskPath, task);
		}
		// Transfert de la notification au popup de choix de tache
		taskChooserDialog.taskMoved(oldTaskPath, task);
	}

	/**
	 * Indique qu'une contribution a été ajoutée au référentiel.
	 *
	 * @param contribution
	 *            la contribution ajoutée.
	 */
	@Override
	public void contributionAdded(Contribution contribution) {
		needRefresh = true;
	}

	/**
	 * Indique que des contributions ont été supprimées du référentiel.
	 *
	 * @param contributions
	 *            les contributions supprimées.
	 */
	@Override
	public void contributionsRemoved(Contribution[] contributions) {
		needRefresh = true;
	}

	/**
	 * Indique que des contributions ont été modifiée dans le référentiel.
	 *
	 * @param contributions
	 *            les contributions modifiées.
	 */
	@Override
	public void contributionsUpdated(Contribution[] contributions) {
		needRefresh = true;
	}

}
