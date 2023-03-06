package org.activitymgr.ui.web.view.impl.internal;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.dialogs.PopupDateFieldWithParser;
import org.activitymgr.ui.web.view.impl.internal.util.AlignHelper;
import org.activitymgr.ui.web.view.impl.internal.util.TableDatasource;

import com.google.inject.Inject;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PopupDateField;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

@SuppressWarnings("serial")
public class ContributionsPanel extends AbstractTabPanel<IContributionsTabLogic> 
		implements IContributionsTabLogic.View {

	private Button selectMeButton;

	private Button previousYearButton;
	private Button previousMonthButton;
	private Button previousWeekButton;

	private PopupDateField dateField;
	private Button todayButton;

	private Button nextWeekButton;
	private Button nextMonthButton;
	private Button nextYearButton;

	private Table contributionsTable;
	private Table collaboratorsTable;

	private ITableCellProviderCallback<Long> contributionsProvider;

	@Inject
	public ContributionsPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createHeaderComponent() {
		HorizontalLayout result = new HorizontalLayout();		
		// addComponent(result);
		
		HorizontalLayout left = new HorizontalLayout();
		result.addComponent(left);
		result.setExpandRatio(left, LEFT_SIDE_RATIO);
		
		selectMeButton = new Button("Select myself");
		selectMeButton.setWidth(100, Unit.PERCENTAGE);
		left.addComponent(selectMeButton);
		left.setWidth(100, Unit.PERCENTAGE);
		
		addSpaceComponent(result, 5); // separator
		
		GridLayout dateLayout = new GridLayout(12, 1);
		result.addComponent(dateLayout);
		result.setExpandRatio(dateLayout, CENTER_RATIO);
		result.setComponentAlignment(dateLayout, Alignment.MIDDLE_CENTER);
		
		previousYearButton = createBoundButton(dateLayout, "<<< Year", "Ctrl+Shift+Alt+Left");
		previousMonthButton = createBoundButton(dateLayout, "<< Month", "Ctrl+Shift+Left");
		previousWeekButton = createBoundButton(dateLayout, "< Week", "Ctrl+Left");

		addSpaceComponent(dateLayout, 20);
		
		todayButton = new Button("Today");
		dateLayout.addComponent(todayButton);
		dateField = new PopupDateFieldWithParser();
		dateField.setImmediate(true);
		dateField.setDateFormat("E dd/MM/yyyy");
		dateField.setShowISOWeekNumbers(true);
		dateField.setStyleName("monday-date-field");
		dateLayout.addComponent(dateField);
		
		addSpaceComponent(dateLayout, 20);

		nextWeekButton = createBoundButton(dateLayout, "Week >", "Ctrl+Right");
		nextMonthButton = createBoundButton(dateLayout, "Month >>", "Ctrl+Shift+Right");
		nextYearButton = createBoundButton(dateLayout, "Year >>>", "Ctrl+Shift+Alt+Right");

		
		addSpaceComponent(result, 5 + 50); // separator + Actions Column
		
		return result;
	}
	
	private Button createBoundButton(ComponentContainer parent, String label, String description) {
		Button result = new Button(label);
		result.setDescription(description);
		parent.addComponent(result);
		return result;
	}

	private void addSpaceComponent(ComponentContainer container, int width) {
		Label emptyLabel = new Label();
		emptyLabel.setWidth(width, Unit.PIXELS);
		container.addComponent(emptyLabel);
	}
	
	@Override
	protected Component createLeftComponent() {
		/*
		 * Collaborators table
		 */
		collaboratorsTable = new Table();
		collaboratorsTable.setSelectable(true);
		collaboratorsTable.setImmediate(true);
		collaboratorsTable.setNullSelectionAllowed(false);
		collaboratorsTable.setSizeFull();

		return collaboratorsTable;
	}

	@Override
	protected Component createBodyComponent() {
		/*
		 * Contributions table
		 */
		contributionsTable = new Table();
		contributionsTable.setFooterVisible(true);
		contributionsTable.setSizeFull();
		return contributionsTable;
	}

	@Override
	public void registerLogic(final IContributionsTabLogic logic) {
		super.registerLogic(logic);
		registerListeners();
	}

	
	private void registerBoundButton(Button button, String caption, Runnable task,
			int keyCode, int... modifierKeys) {
		if (button != null) {
			button.addClickListener(evt -> task.run());
		}
		addShortcutListener(new ShortcutListener(caption, keyCode, modifierKeys) {
			@Override
			public void handleAction(Object sender, Object target) {
				task.run();
			}
		});
	}
	
	private void registerListeners() {
		collaboratorsTable.addValueChangeListener(evt -> 
				getLogic().onSelectedCollaboratorChanged((Long) collaboratorsTable.getValue()));

		todayButton.addClickListener(evt -> getLogic().onDateChange(new GregorianCalendar()));
		selectMeButton.addClickListener(evt -> getLogic().onSelectMe());
		
		registerBoundButton(previousYearButton, "Previous year",
				() -> getLogic().onPreviousYear(),
				KeyCode.ARROW_LEFT,
				ModifierKey.CTRL, ModifierKey.SHIFT, ModifierKey.ALT);

		registerBoundButton(previousMonthButton, "Previous month",
				() -> getLogic().onPreviousMonth(),
				KeyCode.ARROW_LEFT,
				ModifierKey.CTRL, ModifierKey.SHIFT);

		registerBoundButton(previousWeekButton, "Previous week",
				() -> getLogic().onPreviousWeek(),
				KeyCode.ARROW_LEFT,
				ModifierKey.CTRL);

		registerBoundButton(nextYearButton, "next year",
				() -> getLogic().onNextYear(),
				KeyCode.ARROW_LEFT,
				ModifierKey.CTRL, ModifierKey.SHIFT, ModifierKey.ALT);

		registerBoundButton(nextMonthButton, "Next month",
				() -> getLogic().onNextMonth(),
				KeyCode.ARROW_LEFT,
				ModifierKey.CTRL, ModifierKey.SHIFT);

		registerBoundButton(nextWeekButton, "Next week",
				() -> getLogic().onNextWeek(),
				KeyCode.ARROW_LEFT,
				ModifierKey.CTRL);
	}

	@Override
	public void setContributionsProvider(final ITableCellProviderCallback<Long> provider) {
		this.contributionsProvider = provider;
		TableDatasource<Long> dataSource = new TableDatasource<>(getResourceCache(), provider);
		contributionsTable.setContainerDataSource(dataSource);
		
		ColumnGenerator colGen = (source, itemId, prop) -> provider.getCell((Long) itemId, (String) prop);
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			contributionsTable.addGeneratedColumn(propertyId, colGen);
			
			int columnWidth = provider.getColumnWidth(propertyId);
			if (columnWidth > 0) {
				contributionsTable.setColumnWidth(propertyId, columnWidth);
			} else {
				contributionsTable.setColumnExpandRatio(propertyId, -columnWidth/100.f);
			}
			
			contributionsTable.setColumnAlignment(propertyId, 
					AlignHelper.toVaadinAlign(provider.getColumnAlign(propertyId)));
		}
	}
	
	@Override
	public void reloadContributionTableItems() {
		contributionsTable.refreshRowCache();
		reloadContributionTableFooter();
	}

	@Override
	public void reloadContributionTableFooter() {
		for (String columnId : contributionsProvider.getPropertyIds()) {
			contributionsTable.setColumnFooter(columnId, contributionsProvider.getFooter(columnId));
		}
	}

	@Override
	public void setDate(Calendar date) {
		dateField.setValue(date.getTime());
	}



	@Override
	public void setCollaboratorsProvider(ITableCellProviderCallback<Long> cellProvider) {
		TableDatasource<Long> dataSource = new TableDatasource<>(getResourceCache(), cellProvider);
		collaboratorsTable.setContainerDataSource(dataSource);
		
		ColumnGenerator colGen = (source, itemId, prop) -> 
			cellProvider.getCell((Long) itemId, (String) prop);
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			collaboratorsTable.addGeneratedColumn(propertyId, colGen);
			int columnWidth = cellProvider.getColumnWidth(propertyId);
			collaboratorsTable.setColumnExpandRatio(propertyId, columnWidth);
			collaboratorsTable.setColumnAlignment(propertyId, 
					AlignHelper.toVaadinAlign(cellProvider.getColumnAlign(propertyId)));
		}
	}

	@Override
	public void selectCollaborator(final long collaboratorId) {
		collaboratorsTable.select(collaboratorId);
		collaboratorsTable.focus();
	}

	@Override
	public void focus() {
		super.focus();
	}

	@Override
	public void setColumnTitle(String propertyId, String title) {
		contributionsTable.setColumnHeader(propertyId, title);
	}

}
