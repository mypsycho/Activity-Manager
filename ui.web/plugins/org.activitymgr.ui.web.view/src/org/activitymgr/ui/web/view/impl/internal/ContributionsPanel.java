package org.activitymgr.ui.web.view.impl.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.function.Consumer;

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

	private PopupDateField dateField;

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
		
		Button selectMeButton = new Button("Select myself");
		selectMeButton.setWidth(100, Unit.PERCENTAGE);
		selectMeButton.addClickListener(evt -> getLogic().onSelectMe());
		left.addComponent(selectMeButton);
		left.setWidth(100, Unit.PERCENTAGE);
		
		addSpaceComponent(result, 5); // separator
		
		GridLayout dateLayout = new GridLayout(12, 1);
		result.addComponent(dateLayout);
		result.setExpandRatio(dateLayout, CENTER_RATIO);
		result.setComponentAlignment(dateLayout, Alignment.MIDDLE_CENTER);
		
		
		createTimeButton(dateLayout, true, Time.year, IContributionsTabLogic::onPreviousYear);
		createTimeButton(dateLayout, true, Time.month, IContributionsTabLogic::onPreviousMonth);
		createTimeButton(dateLayout, true, Time.week, IContributionsTabLogic::onPreviousWeek);

		addSpaceComponent(dateLayout, 20);
		
		Button todayButton = new Button("Today");
		todayButton.addClickListener(evt -> getLogic().onDateChange(new GregorianCalendar()));
		dateLayout.addComponent(todayButton);
		
		dateField = new PopupDateFieldWithParser();
		dateField.setImmediate(true);
		dateField.setDateFormat("E dd/MM/yyyy");
		dateField.setShowISOWeekNumbers(true);
		dateField.setStyleName("monday-date-field");
		dateField.addValueChangeListener(evt -> {
				Calendar cal = new GregorianCalendar();
				cal.setTime(dateField.getValue() != null 
						? dateField.getValue() 
						: new Date());
				getLogic().onDateChange(cal);
		});
		dateLayout.addComponent(dateField);
		
		addSpaceComponent(dateLayout, 20);
		
		createTimeButton(dateLayout, false, Time.week, IContributionsTabLogic::onNextWeek);
		createTimeButton(dateLayout, false, Time.month, IContributionsTabLogic::onNextMonth);
		createTimeButton(dateLayout, false, Time.year, IContributionsTabLogic::onNextYear);
		
		addSpaceComponent(result, 5 + 50); // separator + Actions Column
		
		return result;
	}
	
	enum Time {
		week("Week", "< ", " >", "Ctrl+", ModifierKey.CTRL),
		month("Month", "<< ", " >>", "Ctrl+Shift+", ModifierKey.CTRL, ModifierKey.SHIFT),
		year("Year", "<<< ", " >>>", "Ctrl+Shift+Alt+", ModifierKey.CTRL, ModifierKey.SHIFT, ModifierKey.ALT);		

		String label;
		int[] modifierKeys;
		String modifierNames;
		String leftHint;
		String rightHint;

		Time(String label, String leftHint, String rightHint, String modifierNames, int... modifierKeys) {
			this.label = label;
			this.leftHint = leftHint;
			this.rightHint = rightHint;
			this.modifierNames = modifierNames;
			this.modifierKeys = modifierKeys;
		}
	}
	
	private Button createTimeButton(ComponentContainer parent, boolean before, Time step, Consumer<IContributionsTabLogic> task) {
		
		String leftHint = "";
		String rightHint = "";
		int keyCode = KeyCode.ARROW_RIGHT;
		String keyName = "Right";
		
		String qual = "Next ";
		if (before) {
			keyCode = KeyCode.ARROW_LEFT;
			keyName = "Left";
			qual = "Previous ";
			leftHint = step.leftHint;
		} else {
			rightHint = step.rightHint;
		}
		
		Button result = new Button(leftHint + step.label + rightHint);
		result.setDescription(step.modifierNames + keyName);
		
		result.addClickListener(evt -> task.accept(getLogic()));
		
		addShortcutListener(new ShortcutListener(qual + step.label, keyCode, step.modifierKeys) {
			@Override
			public void handleAction(Object sender, Object target) {
				result.click();
			}
		});
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
		collaboratorsTable = new Table();
		collaboratorsTable.setSelectable(true);
		collaboratorsTable.setImmediate(true);
		collaboratorsTable.setNullSelectionAllowed(false);
		collaboratorsTable.setSizeFull();
		
		collaboratorsTable.addValueChangeListener(evt -> 
			getLogic().onSelectedCollaboratorChanged((Long) collaboratorsTable.getValue()));

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
		
		contributionsTable.setStyleName("activitymgr-contributions");
		
		return contributionsTable;
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
		cellProvider.getRootElements(); // load cache
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
