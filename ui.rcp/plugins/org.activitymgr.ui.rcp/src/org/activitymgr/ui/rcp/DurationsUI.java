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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.DatabaseUI.IDbStatusListener;
import org.activitymgr.ui.rcp.images.ImagesDatas;
import org.activitymgr.ui.rcp.util.AbstractTableMgrUI;
import org.activitymgr.ui.rcp.util.SWTHelper;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.activitymgr.ui.rcp.util.TableOrTreeColumnsMgr;
import org.activitymgr.ui.rcp.util.UITechException;
import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * IHM de gestion des durées.
 */
public class DurationsUI extends AbstractTableMgrUI implements IDbStatusListener,
		ICellModifier {

	/** Logger */
	private static Logger log = Logger.getLogger(DurationsUI.class);

	/** Constantes associées aux colonnes */
	public static final int IS_ACTIVE_COLUMN_IDX = 0;
	public static final int DURATION_COLUMN_IDX = 1;
	private static TableOrTreeColumnsMgr tableColsMgr;

	/**
	 * Interface utilisée pour permettre l'écoute de la suppression ou de
	 * l'ajout de durées.
	 */
	public interface IDurationListener {

		/**
		 * Indique qu'une durée a été ajoutée au référentiel.
		 *
		 * @param duration
		 *            la durée ajoutée.
		 */
		void durationsChanged(Duration[] durations);

	}

	/** Viewer */
	private TableViewer tableViewer;


	/** Composant parent */
	private Composite parent;

	/** Listeners */
	private List<IDurationListener> listeners = Collections.emptyList();

	/** Icone utilisé pour marquer les durées actifs */
	private Image checkedIcon;

	/** Icone utilisé pour les durées non actifs */
	private Image uncheckedIcon;

	/** Bean factory */
	private IDTOFactory factory;

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 *
	 * @param tabItem
	 *            item parent.
	 * @param modelMgr
	 *            the model manager instance.
	 */
	public DurationsUI(TabItem tabItem, IModelMgr modelMgr, IDTOFactory factory) {
		this(tabItem.getParent(), modelMgr, factory);
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par défaut.
	 *
	 * @param parentComposite
	 *            composant parent.
	 * @param modelMgr
	 *            the model manager instance.
	 * @param factory
	 *            bean factory.
	 */
	public DurationsUI(Composite parentComposite, IModelMgr modelMgr, IDTOFactory factory) {
		super(parentComposite, modelMgr);
		this.factory = factory;

		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		// Table
		final Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION
				| SWT.BORDER | SWT.HIDE_SELECTION);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 75;
		table.setLayoutData(gridData);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setEnabled(true);
		table.setToolTipText(Strings.getString("DurationsUI.table.TOOL_TIP")); //$NON-NLS-1$

		// Création du viewer
		tableViewer = new TableViewer(table);
		tableViewer.setCellModifier(this);
		tableViewer.setContentProvider(this);
		tableViewer.setLabelProvider(this);

		// Configuration des colonnes
		tableColsMgr = new TableOrTreeColumnsMgr();
		tableColsMgr.addColumn("IS_ACTIVE", "!", 20, SWT.CENTER); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr
				.addColumn(
						"DURATION", Strings.getString("DurationsUI.columns.DURATION"), 100, SWT.LEFT); //$NON-NLS-1$ //$NON-NLS-2$
		tableColsMgr.configureTable(tableViewer);

		// Configuration des éditeurs de cellules
		tableViewer.setCellEditors(new CellEditor[] {
				new CheckboxCellEditor(table),
				new TextCellEditor(table)
		});

		// Configuration du menu popup
		final Menu menu = new Menu(table);
		MenuItem newItem = createItem(menu, "NEW", this::onNewElement); //$NON-NLS-1$
		MenuItem removeItem = createItem(menu, "REMOVE", this::onRemoveElement); //$NON-NLS-1$
		createItem(menu, "EXPORT", this::onExport); //$NON-NLS-1$
		createItem(menu, "REFRESH", this::databaseOpened); //$NON-NLS-1$

		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				log.debug("menuShown(" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				TableItem[] selection = tableViewer.getTable().getSelection();
				boolean emptySelection = selection.length == 0;
				boolean singleSelection = selection.length == 1;
				newItem.setEnabled(emptySelection || singleSelection);
				removeItem.setEnabled(!emptySelection);
			}
		});

		table.setMenu(menu);

		// Chargement des icones
		checkedIcon = new Image(parentComposite.getDisplay(),
				ImagesDatas.CHECKED_ICON);
		uncheckedIcon = new Image(parentComposite.getDisplay(),
				ImagesDatas.UNCHECKED_ICON);
	}

	@Override
	public Duration[] getElements(Object inputElement) {
		return safeExec(new Duration[0], ()-> modelMgr.getDurations());
	}

	@Override
	public boolean canModify(Object element, String property) {
		log.debug("ICellModifier.canModify(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return true;
	}

	@Override
	public Object getValue(final Object element, final String property) {
		log.debug("ICellModifier.getValue(" + element + ", " + property + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Exécution
		return SafeRunner.exec(parent.getShell(), "", ()->{ //$NON-NLS-1$
			Duration duration = (Duration) element;
			int columnIndex = tableColsMgr.getColumnIndex(property);
			switch (columnIndex) {
			case IS_ACTIVE_COLUMN_IDX:
				return duration.getIsActive()
						? Boolean.TRUE
						: Boolean.FALSE;
			case DURATION_COLUMN_IDX:
				return String.valueOf(new BigDecimal(duration.getId())
						.movePointLeft(2));
			default:
				throw new Error(
						Strings.getString("DurationsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}

		});

	}

	@Override
	public void modify(final Object element, String property, final Object value) {
		log.debug("ICellModifier.modify(" + element + ", " + property + ", " + value + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		TableItem item = (TableItem) element;
		final Duration duration = (Duration) item.getData();
		final IBaseLabelProvider labelProvider = this;
		final int columnIndex = tableColsMgr.getColumnIndex(property);

		safeExec(() -> {
			// Création d'un clone dans le cas ou il s'agît
			// d'une modification de la valeur de la durée
			Duration oldDuration = factory.newDuration();
			oldDuration.setId(duration.getId());
			oldDuration.setIsActive(duration.getIsActive());

			switch (columnIndex) {
			case IS_ACTIVE_COLUMN_IDX:
				Boolean isActive = (Boolean) value;
				duration.setIsActive(isActive);
				modelMgr.updateDuration(duration);
				break;
			case DURATION_COLUMN_IDX:
				// Mise à jour en base
				Duration newDuration = factory.newDuration();
				newDuration.setId(StringHelper
						.entryToHundredth((String) value));
				newDuration.setIsActive(duration.getIsActive());
				newDuration = modelMgr.updateDuration(oldDuration,
						newDuration);
				// Mise à jour dans le modèle
				duration.setId(newDuration.getId());
				// Tri des données
				databaseOpened();
				break;
			default:
				throw new UITechException(
						Strings.getString("DurationsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}
			// Notification des listeners
			notifyLabelProviderListener(new LabelProviderChangedEvent(labelProvider, duration));
			notifyDurationChanged(getElements(ROOT_NODE));
		});

	}

	@Override
	public String getColumnText(final Object element, final int columnIndex) {
		log.debug("ITableLabelProvider.getColumnText(" + element //$NON-NLS-1$
				+ ", " + columnIndex //$NON-NLS-1$
				+ ")"); //$NON-NLS-1$
		return SafeRunner.exec(parent.getShell(), "",  () -> {//$NON-NLS-1$

			Duration duration = (Duration) element;
			switch (columnIndex) {
			case IS_ACTIVE_COLUMN_IDX:
				return ""; // colonne indiquant si la durée est active ou non //$NON-NLS-1$
			case DURATION_COLUMN_IDX:
				return String.valueOf(new BigDecimal(duration.getId())
						.movePointLeft(2));
			default:
				throw new Error(
						Strings.getString("DurationsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
			}
		});
	}

	@Override
	public Image getColumnImage(final Object element, final int columnIndex) {
		log.debug("ITableLabelProvider.getColumnImage(" + element + ", " + columnIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return safeExec(null, ()-> {
				Duration duration = (Duration) element;
				switch (columnIndex) {
				case IS_ACTIVE_COLUMN_IDX:
					return duration.getIsActive()
							? checkedIcon
							: uncheckedIcon;
				case DURATION_COLUMN_IDX:
					return null;
				default:
					throw new Error(
							Strings.getString("DurationsUI.errors.UNKNOWN_COLUMN")); //$NON-NLS-1$
				}
			});
	}

	private void onNewElement() throws Exception {
		InputDialog dialog = new InputDialog(
				parent.getShell(),
				Strings.getString("DurationsUI.titles.INPUT_DIALOG"), //$NON-NLS-1$
				Strings.getString("DurationsUI.questions.NEW_DURATION"), //$NON-NLS-1$
				"0", //$NON-NLS-1$
				newText -> {
					String errorMsg = null;
					Duration duration = factory.newDuration();
					try {
						// Parsing de la saisie et contrôle du
						// format
						duration.setId(StringHelper
								.entryToHundredth(newText));
						// Vérification de la non existence de
						// la durée
						if (modelMgr.durationExists(duration))
							errorMsg = Strings
									.getString("DurationsUI.errors.DURATION_ALREADY_EXIST"); //$NON-NLS-1$
					} catch (StringFormatException e) {
						errorMsg = e.getMessage();
					}
					// Retour du résultat
					return errorMsg;
				});
		// Ouverture du dialogue
		if (dialog.open() != Window.OK) {
			return;
		}
		Duration newDuration = factory.newDuration();
		newDuration.setId(StringHelper.entryToHundredth(dialog
				.getValue()));
		modelMgr.createDuration(newDuration);
		newLine(newDuration);
		// Notification des listeners
		notifyDurationChanged(getElements(ROOT_NODE));
		// Tri des données
		databaseOpened();
	}

	private void onRemoveElement() throws Exception {
		TableItem[] items = tableViewer.getTable().getSelection();
		boolean notify = false;
		for (TableItem item : items) {
			Duration duration = (Duration) item.getData();
			modelMgr.removeDuration(duration);
			item.dispose();
			// Notification des listeners
			notify = true;
		}
		if (notify) {
			notifyDurationChanged(getElements(ROOT_NODE));
		}
	}

	private void onExport() throws Exception {
		SWTHelper.exportToWorkBook(tableViewer.getTable());
	}


	/**
	 * Ajoute une ligne dans le tableau.
	 *
	 * @param duration
	 *            la durée associée à la nouvelle ligne.
	 */
	private void newLine(Duration duration) {
		// Ajout dans l'arbre
		tableViewer.add(duration);
		tableViewer.setSelection(new StructuredSelection(duration), true);
	}


	/**
	 * Ajoute un listener.
	 *
	 * @param listener
	 *            le nouveau listener.
	 */
	public void addDurationListener(IDurationListener listener) {
		synchronized (this) {
			List<IDurationListener> copy = new ArrayList<>(listeners.size() + 1);
			copy.addAll(listeners);
			copy.add(listener);
			listeners = copy;
		}

	}

	/**
	 * Ajoute un listener.
	 *
	 * @param listener
	 *            le nouveau listener.
	 */
	public void removeDurationListener(IDurationListener listener) {
		synchronized (this) {
			List<IDurationListener> copy = new ArrayList<>(listeners);
			copy.remove(listener);
			listeners = copy;
		}
	}

	/**
	 * Notifie les listeners qu'une durée a été ajoutée.
	 *
	 * @param newDuration
	 *            la durée ajoutée.
	 */
	private void notifyDurationChanged(Duration[] durations) {
		for (IDurationListener listener : listeners) {
			listener.durationsChanged(durations);
		}
	}

	@Override
	public void databaseOpened() {
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


}
