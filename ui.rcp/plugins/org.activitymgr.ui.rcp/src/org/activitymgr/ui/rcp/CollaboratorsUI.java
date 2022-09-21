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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.DatabaseUI.IDbStatusListener;
import org.activitymgr.ui.rcp.dialogs.ContributionsViewerDialog;
import org.activitymgr.ui.rcp.images.ImagesDatas;
import org.activitymgr.ui.rcp.util.AbstractTableMgr;
import org.activitymgr.ui.rcp.util.SWTHelper;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.activitymgr.ui.rcp.util.TableOrTreeColumnsMgr;
import org.activitymgr.ui.rcp.util.UITechException;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * IHM de gestion des collaborateurs.
 */
public class CollaboratorsUI extends AbstractTableMgr implements
		IDbStatusListener, ICellModifier, MenuListener {

	/** Logger */
	private static Logger log = Logger.getLogger(CollaboratorsUI.class);

	/** Constantes associées aux colonnes */
	public static final int IS_ACTIVE_COLUMN_IDX = 0;
	public static final int IDENTIFIER_COLUMN_IDX = 1;
	public static final int FIRST_NAME_COLUMN_IDX = 2;
	public static final int LAST_NAME_COLUMN_IDX = 3;
	private static final TableOrTreeColumnsMgr tableColsMgr;
	static {
		tableColsMgr = new TableOrTreeColumnsMgr("CollaboratorsUI.columns."); //$NON-NLS-1$
		tableColsMgr.addColumn("IS_ACTIVE", "!", 20, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr.addColumn("IDENTIFIER", 100, SWT.LEFT); //$NON-NLS-1$
		tableColsMgr.addColumn("FIRST_NAME", 100, SWT.LEFT); //$NON-NLS-1$
		tableColsMgr.addColumn("LAST_NAME", 100, SWT.LEFT); //$NON-NLS-1$
	}

	/**
	 * Interface utilisée pour permettre l'écoute de la suppression ou de
	 * l'ajout de collaborateurs.
	 */
	public interface ICollaboratorListener {

		/**
		 * Indique qu'un collaborateur a été ajouté au référentiel.
		 *
		 * @param collaborator
		 *            le collaborateur ajouté.
		 */
		void collaboratorAdded(Collaborator collaborator);

		/**
		 * Indique qu'un collaborateur a été supprimé du référentiel.
		 *
		 * @param collaborator
		 *            le collaborateur supprimé.
		 */
		void collaboratorRemoved(Collaborator collaborator);

		/**
		 * Indique qu'un collaborateur a été modifié du référentiel.
		 *
		 * @param collaborator
		 *            le collaborateur modifié.
		 */
		void collaboratorUpdated(Collaborator collaborator);

		/**
		 * Indique que l'état d'activation d'un collaborateur a été désactivé
		 * dans le référentiel.
		 *
		 * @param collaborator
		 *            le collaborateur modifié.
		 */
		void collaboratorActivationStatusChanged(
				Collaborator collaborator);

	}

	/** Model manager */
	private IModelMgr modelMgr;

	/** Listeners */
	private List<ICollaboratorListener> listeners = new ArrayList<>();

	/** Viewer */
	private TableViewer tableViewer;

	/** Items de menu */
	private MenuItem newItem;
	private MenuItem removeItem;
	private MenuItem listTaskContributionsItem;
	private MenuItem exportItem;

	/** Composant parent */
	private Composite parent;

	/** Popup permettant de lister les contributions d'une tache */
	private ContributionsViewerDialog contribsViewerDialog;

	/** Index de la colonne utilisé pour trier les collaborateurs */
	private int sortColumnIndex = LAST_NAME_COLUMN_IDX;

	/** Icone utilisé pour marquer le collaborateur actifs */
	private Image checkedIcon;

	/** Icone utilisé pour les collaborateurs non actifs */
	private Image uncheckedIcon;

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 *
	 * @param tabItem
	 *            item parent.
	 * @param modelMgr
	 *            the model manager instance.
	 * @param modelMgr
	 *            the model manager.
	 */
	public CollaboratorsUI(TabItem tabItem, IModelMgr modelMgr) {
		this(tabItem.getParent(), modelMgr);
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 *
	 * @param parentComposite
	 *            composant parent.
	 * @param modelMgr
	 *            the model manager.
	 */
	public CollaboratorsUI(Composite parentComposite, IModelMgr modelMgr) {
		this.modelMgr = modelMgr;

		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		// Table
		final Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.HIDE_SELECTION);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 75;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setEnabled(true);
		table.setToolTipText(Strings
				.getString("CollaboratorsUI.table.TOOL_TIP")); //$NON-NLS-1$

		// Création du viewer
		tableViewer = new TableViewer(table);
		tableViewer.setCellModifier(this);
		tableViewer.setContentProvider(this);
		tableViewer.setLabelProvider(this);

		// Configuration des colonnes

		tableColsMgr.configureTable(tableViewer);

		// Ajout du listener de gestion du tri des colonnes
		// Add sort indicator and sort data when column selected
		Listener sortListener = e -> {
			log.debug("handleEvent(" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			TableColumn previousSortColumn = table.getSortColumn();
			TableColumn newSortColumn = (TableColumn) e.widget;
			int dir = table.getSortDirection();
			if (previousSortColumn == newSortColumn) {
				dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
			} else {
				table.setSortColumn(newSortColumn);
				dir = SWT.UP;
			}
			table.setSortDirection(dir);
			sortColumnIndex = Arrays.asList(table.getColumns()).indexOf(
					newSortColumn);
			// Rafraichissement des données
			tableViewer.refresh();
		};
		for (TableColumn column : table.getColumns()) {
			column.addListener(SWT.Selection, sortListener);
		}
		table.setSortColumn(table.getColumns()[sortColumnIndex]);
		table.setSortDirection(SWT.UP);

		// Configuration des éditeurs de cellules
		CellEditor[] editors = new CellEditor[table.getColumns().length];
		editors[IS_ACTIVE_COLUMN_IDX] = new CheckboxCellEditor(table);
		editors[IDENTIFIER_COLUMN_IDX] = new TextCellEditor(table);
		editors[FIRST_NAME_COLUMN_IDX] = new TextCellEditor(table);
		editors[LAST_NAME_COLUMN_IDX] = new TextCellEditor(table);
		tableViewer.setCellEditors(editors);

		// Initialisation des popups
		contribsViewerDialog = new ContributionsViewerDialog(parent.getShell(), modelMgr);

		// Configuration du menu popup
		final Menu menu = new Menu(table);
		menu.addMenuListener(this);
		newItem = createItem(menu, "NEW", //$NON-NLS-1$
			() -> {
			Collaborator newCollaborator = modelMgr.createNewCollaborator();
			newLine(newCollaborator);
			// Notification des listeners
			notifyCollaboratorAdded(newCollaborator);
		});
		removeItem = createItem(menu, "REMOVE", //$NON-NLS-1$
			()-> {
				TableItem[] items = tableViewer.getTable().getSelection();
				for (TableItem item : items) {
					Collaborator collaborator = (Collaborator) item
							.getData();
					modelMgr.removeCollaborator(collaborator);
					item.dispose();
					// Notification des listeners
					notifyCollaboratorRemoved(collaborator);
				}
		});
		listTaskContributionsItem = createItem(menu, "LIST_CONTRIBUTIONS", //$NON-NLS-1$
			()-> {
				TableItem[] items = tableViewer.getTable().getSelection();
				Collaborator selectedCollaborator = (Collaborator) items[0]
						.getData();
				contribsViewerDialog.setFilter(null, selectedCollaborator);
				// Ouverture du dialogue
				contribsViewerDialog.open();

		});
		exportItem = createItem(menu, "EXPORT", //$NON-NLS-1$
			()-> {
				SWTHelper.exportToWorkBook(tableViewer.getTable());
			});
		table.setMenu(menu);

		// Chargement des icones
		checkedIcon = new Image(parentComposite.getDisplay(), ImagesDatas.CHECKED_ICON);
		uncheckedIcon = new Image(parentComposite.getDisplay(), ImagesDatas.UNCHECKED_ICON);
	}


	@Override
	public Object[] getElements(Object inputElement) {
		// Chargement des données
		return safeExec(new Collaborator[0], () -> {
				// Recherche des collaborateurs
				int orderByFieldIndex = -1;
				switch (sortColumnIndex) {
				case IS_ACTIVE_COLUMN_IDX:
					orderByFieldIndex = Collaborator.IS_ACTIVE_FIELD_IDX;
					break;
				case IDENTIFIER_COLUMN_IDX:
					orderByFieldIndex = Collaborator.LOGIN_FIELD_IDX;
					break;
				case FIRST_NAME_COLUMN_IDX:
					orderByFieldIndex = Collaborator.FIRST_NAME_FIELD_IDX;
					break;
				case LAST_NAME_COLUMN_IDX:
				default:
					orderByFieldIndex = Collaborator.LAST_NAME_FIELD_IDX;
					break;
				}
				// Récupération
				return modelMgr.getCollaborators(orderByFieldIndex, tableViewer
						.getTable().getSortDirection() == SWT.UP);
			}
		);
	}


	@Override
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return true;
	}


	@Override
	public Object getValue(final Object element, final String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return safeExec(new Collaborator[0], () -> {
				Collaborator collaborator = (Collaborator) element;
				Object value = null;
				int columnIndex = tableColsMgr.getColumnIndex(property);
				switch (columnIndex) {
				case IS_ACTIVE_COLUMN_IDX:
					value = collaborator.getIsActive() ? Boolean.TRUE
							: Boolean.FALSE;
					break;
				case IDENTIFIER_COLUMN_IDX:
					value = collaborator.getLogin();
					break;
				case FIRST_NAME_COLUMN_IDX:
					value = collaborator.getFirstName();
					break;
				case LAST_NAME_COLUMN_IDX:
					value = collaborator.getLastName();
					break;
				default:
					throw new Error(
							Strings.getString("CollaboratorsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
				return value;
			});
	}

	@Override
	public void modify(final Object element, final String property,
			final Object value) {
		log.debug("ICellModifier.modify(" + element + ", " + property + ", " + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		TableItem item = (TableItem) element;
		final Collaborator collaborator = (Collaborator) item.getData();
		final IBaseLabelProvider labelProvider = this;
		final int columnIndex = tableColsMgr.getColumnIndex(property);
		safeExec(() -> {
			boolean mustNotifyUpdateEvent = false;
			boolean mustNotifyActivationStatusChangeEvent = false;
			switch (columnIndex) {
				case IS_ACTIVE_COLUMN_IDX:
					Boolean isActive = (Boolean) value;
					collaborator.setIsActive(isActive);
					mustNotifyActivationStatusChangeEvent = true;
					break;
				case IDENTIFIER_COLUMN_IDX:
					String newIdentifier = (String) value;
					if (!collaborator.getLogin().equals(newIdentifier)) {
						collaborator.setLogin(newIdentifier);
					}
					mustNotifyUpdateEvent = true;
					break;
				case FIRST_NAME_COLUMN_IDX:
					collaborator.setFirstName((String) value);
					mustNotifyUpdateEvent = true;
					break;
				case LAST_NAME_COLUMN_IDX:
					collaborator.setLastName((String) value);
					mustNotifyUpdateEvent = true;
					break;
				default:
					throw new UITechException(
							Strings.getString("CollaboratorsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}
			// Mise à jour en base
			modelMgr.updateCollaborator(collaborator);
			// Notification des listeners
			notifyLabelProviderListener(new LabelProviderChangedEvent(
					labelProvider, collaborator));
			if (mustNotifyUpdateEvent) {
				notifyCollaboratorUpdated(collaborator);
			}
			if (mustNotifyActivationStatusChangeEvent) {
				notifyCollaboratorActivationStatusChanged(collaborator);
			}

		});
	}


	@Override
	public String getColumnText(final Object element, final int columnIndex) {
		log.debug("ITableLabelProvider.getColumnText(" + element + ", " + columnIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return safeExec("", () -> {
			Collaborator collaborator = (Collaborator) element;
			String text = null;
			switch (columnIndex) {
			case IS_ACTIVE_COLUMN_IDX:
				text = ""; // La colonne est renseignée par une icone //$NON-NLS-1$
				break;
			case IDENTIFIER_COLUMN_IDX:
				text = collaborator.getLogin();
				break;
			case FIRST_NAME_COLUMN_IDX:
				text = collaborator.getFirstName();
				break;
			case LAST_NAME_COLUMN_IDX:
				text = collaborator.getLastName();
				break;
			default:
				throw new Error(
						Strings.getString("CollaboratorsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}
			return text;
		});
	}

	@Override
	public Image getColumnImage(final Object element, final int columnIndex) {
		log.debug("ITableLabelProvider.getColumnImage(" + element + ", " + columnIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return safeExec(null, () -> {
			Collaborator collaborator = (Collaborator) element;
			Image image = null;
			switch (columnIndex) {
			case IS_ACTIVE_COLUMN_IDX:
				image = collaborator.getIsActive() ? checkedIcon
						: uncheckedIcon;
				break;
			case IDENTIFIER_COLUMN_IDX:
			case FIRST_NAME_COLUMN_IDX:
			case LAST_NAME_COLUMN_IDX:
				image = null;
				break;
			default:
				throw new Error(
						Strings.getString("CollaboratorsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}
			return image;
		});
	}

	/**
	 * Ajoute une ligne dans le tableau.
	 *
	 * @param collaborator
	 *            le collaborateur associé à la nouvelle ligne.
	 */
	private void newLine(Collaborator collaborator) {
		// Ajout dans l'arbre
		tableViewer.add(collaborator);
		tableViewer.setSelection(new StructuredSelection(collaborator), true);
	}

	protected MenuItem createItem(Menu parent, String id, SafeRunner.Exec task) {
		MenuItem result = new MenuItem(parent, SWT.CASCADE);
		String textId = "CollaboratorsUI.menuitems." + id; //$NON-NLS-1$
		result.setText(Strings.getString(textId));
		result.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				safeExec(task);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

		});
		return result;
	}


	@Override
	public void menuShown(MenuEvent e) {
		log.debug("menuShown(" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		TableItem[] selection = tableViewer.getTable().getSelection();
		boolean emptySelection = selection.length == 0;
		boolean singleSelection = selection.length == 1;
		newItem.setEnabled(emptySelection || singleSelection);
		removeItem.setEnabled(!emptySelection);
		listTaskContributionsItem.setEnabled(singleSelection);
		exportItem.setEnabled(true);
	}


	@Override
	public void menuHidden(MenuEvent e) {
		// Do nothing...
	}

	/**
	 * Ajoute un listener.
	 *
	 * @param listener
	 *            le nouveau listener.
	 */
	public void addCollaboratorListener(ICollaboratorListener listener) {
		listeners.add(listener);
	}

	/**
	 * Ajoute un listener.
	 *
	 * @param listener
	 *            le nouveau listener.
	 */
	public void removeCollaboratorListener(ICollaboratorListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifie les listeners qu'un collaborateur a été ajouté.
	 *
	 * @param newCollaborator
	 *            le collaborateur ajouté.
	 */
	private void notifyCollaboratorAdded(Collaborator newCollaborator) {
		Iterator<ICollaboratorListener> it = listeners.iterator();
		while (it.hasNext()) {
			ICollaboratorListener listener = it.next();
			listener.collaboratorAdded(newCollaborator);
		}
	}

	/**
	 * Notifie les listeners qu'un collaborateur a été supprimé.
	 *
	 * @param collaborator
	 *            le collaborateur supprimé.
	 */
	private void notifyCollaboratorRemoved(Collaborator collaborator) {
		Iterator<ICollaboratorListener> it = listeners.iterator();
		while (it.hasNext()) {
			ICollaboratorListener listener = it.next();
			listener.collaboratorRemoved(collaborator);
		}
	}

	/**
	 * Notifie les listeners qu'un collaborateur a été modifié.
	 *
	 * @param collaborator
	 *            le collaborateur modifié.
	 */
	private void notifyCollaboratorUpdated(Collaborator collaborator) {
		Iterator<ICollaboratorListener> it = listeners.iterator();
		while (it.hasNext()) {
			ICollaboratorListener listener = it.next();
			listener.collaboratorUpdated(collaborator);
		}
	}

	/**
	 * Notifie les listeners que l'état d'activation d'un collaborateur a été
	 * modifié.
	 *
	 * @param collaborator
	 *            le collaborateur modifié.
	 */
	private void notifyCollaboratorActivationStatusChanged(
			Collaborator collaborator) {
		Iterator<ICollaboratorListener> it = listeners.iterator();
		while (it.hasNext()) {
			ICollaboratorListener listener = it.next();
			listener.collaboratorActivationStatusChanged(collaborator);
		}
	}

	@Override
	public void databaseOpened() {
		// Création d'une racine fictive
		tableViewer.setInput(ROOT_NODE);
	}

	@Override
	public void databaseClosed() {
		Table table = tableViewer.getTable();
		TableItem[] items = table.getItems();
		for (TableItem item : items) {
			item.dispose();
		}
	}



	protected <T> T safeExec(T defaultValue, Callable<T> runner) {
		return SafeRunner.exec(parent.getShell(), defaultValue, runner);
	}

	protected void safeExec(SafeRunner.Exec runner) {
		SafeRunner.exec(parent.getShell(), runner);
	}

}
