/*
 * Copyright (c) 2004-2025, Jean-Francois Brazeau and Obeo.
 *
 * All rights reserved.
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
package org.activitymgr.ui.web.view.impl.internal;

import java.util.Date;

import org.activitymgr.ui.web.logic.IButtonLogic;
import org.activitymgr.ui.web.logic.IReportsLogic;
import org.activitymgr.ui.web.logic.ITwinSelectFieldLogic.View;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.dialogs.PopupDateFieldWithParser;

import com.google.inject.Inject;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class ReportsPanel extends GridLayout implements IReportsLogic.View {

	private OptionGroup intervalUnitGroup;
	private OptionGroup intervalBoundsModeGroup;
	private PopupDateFieldWithParser startDateField;
	private PopupDateFieldWithParser endDateField;
	private TextField rootTaskTextField;
	private Button browseTaskButton;
	private TextField taskDepthTextField;
	private Component selectedCollaboratorsComponent;
	private Component selectedColumnsComponent;
	private Label statusLabel;
	private Image warningIcon;
	private HorizontalLayout reportButtonsLayout;
	private CheckBox onlyKeepTasksWithContribsCheckbox;
	private Button decreaseTaskDepthButton;
	private Button increaseTaskDepthButton;
	private Label taskDepthLayoutDocLabel;
	private OptionGroup collaboratorsModeUnitGroup;
	private Label intervalCountLabel;
	private TextField intervalCountTextField;
	private IReportsLogic logic;
	private IResourceCache resourceCache;

	@Inject
	public ReportsPanel(IResourceCache resourceCache) {
		super(2, 16);
		setId("ReportsPanel");
		setSpacing(true);
		setWidth("650px");
		setHeight("100%");
		setColumnExpandRatio(0, 100);
//		setColumnExpandRatio(0, 30);
//		setColumnExpandRatio(1, 70);
		this.resourceCache = resourceCache;
	}

	@Override
	public void registerLogic(IReportsLogic logic) {
		this.logic = logic;
	}

	@Override
	public void initialize(boolean advancedMode) {
		// TODO-better: Swap with Vertical layout
		
		createIntervalConfigurationPanel();
		createScopeConfigurationPanel(advancedMode);
		createHeaderColumnsContentConfigurationPanel(advancedMode);
		createRowsContentConfigurationPanel(advancedMode);
		
		
		if (!advancedMode) {
			// Line separator should be on 13.
			while (getCursorY() < 13) {
				addComponent(new Label());
			}
		}
		
		// On line space
		Label bottomSeparator = new Label("&nbsp;", ContentMode.HTML);
		addComponent(bottomSeparator);
		setRowExpandRatio(getCursorY(), 1);
		addComponent(new Label("", ContentMode.HTML));

		// Report buttons
		addComponent(new Label("")); // Empty cell
		
		reportButtonsLayout = new HorizontalLayout();
		reportButtonsLayout.setDefaultComponentAlignment(Alignment.BOTTOM_LEFT);
		addComponent(reportButtonsLayout);
		reportButtonsLayout.setWidth(100, Unit.PERCENTAGE);
				
		Label leftAlign = new Label("");
		reportButtonsLayout.addComponent(leftAlign);
		reportButtonsLayout.setExpandRatio(leftAlign, 100);

		createStatusPanel();
	}

	private void createIntervalConfigurationPanel() {
		addTitle("Interval configuration");

		addComponent(new Label("Interval unit :"));
		intervalUnitGroup = new OptionGroup();
		intervalUnitGroup.setImmediate(true);
		intervalUnitGroup.setStyleName("horizontal");
		addComponent(intervalUnitGroup);
		
		addComponent(new Label("Interval bounds mode :"));
		intervalBoundsModeGroup = new OptionGroup();
		intervalBoundsModeGroup.setImmediate(true);
		intervalBoundsModeGroup.setStyleName("horizontal");
		addComponent(intervalBoundsModeGroup);

		addComponent(new Label("Interval bounds :"));
		HorizontalLayout intervalBoundsPanel = new HorizontalLayout();
		addComponent(intervalBoundsPanel);
		startDateField = newDateField();
		intervalBoundsPanel.addComponent(startDateField);
		endDateField = newDateField();
		intervalBoundsPanel.addComponent(endDateField);
		intervalBoundsPanel
				.addComponent(new Label("(&nbsp;", ContentMode.HTML));
		intervalCountTextField = new TextField();
		intervalCountTextField.setImmediate(true);
		intervalCountTextField.setWidth("40px");
		intervalCountTextField.addStyleName("center");
		intervalBoundsPanel.addComponent(intervalCountTextField);
		intervalCountLabel = new Label();
		intervalBoundsPanel.addComponent(new Label("&nbsp;", ContentMode.HTML));
		intervalBoundsPanel.addComponent(intervalCountLabel);
		intervalBoundsPanel.addComponent(new Label(")"));

		// Register listeners
		intervalUnitGroup.addValueChangeListener(event -> 
			logic.onIntervalTypeChanged(event.getProperty().getValue()));
		
		intervalBoundsModeGroup.addValueChangeListener(event -> 
			logic.onIntervalBoundsModeChanged(event.getProperty().getValue()));
		
		ValueChangeListener dateBoundsChangeListener = event -> 
			logic.onIntervalBoundsChanged(
					startDateField.getValue(),
					endDateField.getValue());
		startDateField.addValueChangeListener(dateBoundsChangeListener);
		endDateField.addValueChangeListener(dateBoundsChangeListener);
		intervalCountTextField.addValueChangeListener(event -> {
			int count;
			try {
				count = Integer.parseInt(intervalCountTextField.getValue());
			} catch (NumberFormatException e) {
				count = 1;
			}
			logic.onIntervalCountChanged(count);
		});
	}

	private void createScopeConfigurationPanel(boolean advancedMode) {
		addTitle("Scope configuration");

		addComponent(new Label("Root task :"));
		HorizontalLayout rootTaskPanel = new HorizontalLayout();
		addComponent(rootTaskPanel);
		rootTaskTextField = new TextField();
		rootTaskTextField.setImmediate(true);
		rootTaskTextField.setWidth("300px");
		rootTaskPanel.addComponent(rootTaskTextField);
		browseTaskButton = new Button("...");
		browseTaskButton.setImmediate(true);
		rootTaskPanel.addComponent(browseTaskButton);

		collaboratorsModeUnitGroup = new OptionGroup();
		collaboratorsModeUnitGroup.setImmediate(true);
		collaboratorsModeUnitGroup.setStyleName("horizontal");
		if (advancedMode) {
			addComponent(new Label("Collaborators :"));
			addComponent(collaboratorsModeUnitGroup);
		}

		selectedCollaboratorsComponent = new Label("");
		if (advancedMode) {
			addComponent(new Label(""));
			addComponent(selectedCollaboratorsComponent);
		}

		// Register listeners
		browseTaskButton.addClickListener(event -> logic.onBrowseTaskButtonCLicked());
		rootTaskTextField.addValueChangeListener(event -> 
			logic.onTaskScopePathChanged((String) event.getProperty().getValue()));
		collaboratorsModeUnitGroup.addValueChangeListener(event -> 
			logic.onCollaboratorsSelectionModeChanged(event.getProperty().getValue()));
	}

	private void createHeaderColumnsContentConfigurationPanel(
			boolean advancedMode) {
		if (advancedMode) {
			addTitle("Header columns content configuration");
		}
		selectedColumnsComponent = new Label("");
		if (advancedMode) {
			addComponent(new Label("Fields :"));
			addComponent(selectedColumnsComponent);
		}
	}

	private void createRowsContentConfigurationPanel(
			boolean advancedMode) {
		addTitle("Rows content configuration");

		addComponent(new Label("Task tree depth :"));
		HorizontalLayout taskDepthLayout = new HorizontalLayout();
		addComponent(taskDepthLayout);
		decreaseTaskDepthButton = new Button("-");
		decreaseTaskDepthButton.setImmediate(true);
		taskDepthLayout.addComponent(decreaseTaskDepthButton);
		taskDepthTextField = new TextField();
		taskDepthTextField.setImmediate(true);
		taskDepthTextField.setWidth("40px");
		taskDepthTextField.addStyleName("center");
		taskDepthLayout.addComponent(taskDepthTextField);
		increaseTaskDepthButton = new Button("+");
		increaseTaskDepthButton.setImmediate(true);
		taskDepthLayout.addComponent(increaseTaskDepthButton);
		taskDepthLayoutDocLabel = new Label(
				" (from root ; deeper contributions will be aggregated)");
		taskDepthLayout.addComponent(taskDepthLayoutDocLabel);
		taskDepthLayout.setComponentAlignment(taskDepthLayoutDocLabel, Alignment.MIDDLE_LEFT);

		onlyKeepTasksWithContribsCheckbox = new CheckBox(
				"Don't show rows for task that have no contribution");
		if (advancedMode) {
			addComponent(new Label("Filter empty tasks rows :"));
			addComponent(onlyKeepTasksWithContribsCheckbox);
		}

		decreaseTaskDepthButton.addClickListener(event -> increaseOrDecreaseTaskTreeDepth(-1));
		taskDepthTextField.addValueChangeListener(event -> {
			try {
				logic.onTaskTreeDepthChanged(
						Integer.parseInt(taskDepthTextField
								.getValue()));
			} catch (NumberFormatException e) {
				logic.onTaskTreeDepthChanged(0);
			}
		});
		increaseTaskDepthButton.addClickListener(event -> increaseOrDecreaseTaskTreeDepth(1));
		onlyKeepTasksWithContribsCheckbox.addValueChangeListener(event -> 
			logic.onOnlyKeepTaskWithContributionsCheckboxChanged(
					onlyKeepTasksWithContribsCheckbox.getValue()));
	}

	private void createStatusPanel() {
		addComponent(new Label("")); // Empty cell
		HorizontalLayout statusLayout = new HorizontalLayout();
		addComponent(statusLayout);
		warningIcon = new Image(null, resourceCache.getResource(
				"warning.gif"));
		statusLayout.addComponent(warningIcon);
		warningIcon.setVisible(false);
		statusLayout.setComponentAlignment(warningIcon, Alignment.MIDDLE_RIGHT);
		statusLabel = new Label("");
		statusLayout.addComponent(statusLabel);
		statusLayout.setComponentAlignment(statusLabel, Alignment.MIDDLE_RIGHT);
	}

	private void increaseOrDecreaseTaskTreeDepth(int amount) {
		try {
			int actual = Integer.parseInt(taskDepthTextField.getValue());
			taskDepthTextField.setValue(String.valueOf(actual + amount));
		} catch (NumberFormatException e) {
			taskDepthTextField.setValue("");
		}
	}

	@Override
	public void setColumnSelectionView(View view) {
		Component newComponent = (Component) view;
		substituteBodyComponent(selectedColumnsComponent, newComponent);
		selectedColumnsComponent = newComponent;
	}

	@Override
	public void setCollaboratorsSelectionView(View view) {
		Component newComponent = (Component) view;
		substituteBodyComponent(selectedCollaboratorsComponent,
				newComponent);
		selectedCollaboratorsComponent = newComponent;
	}

	@Override
	public void addReportButton(IButtonLogic.View<?> view) {
		reportButtonsLayout.addComponent((Button) view);
	}

	private void substituteBodyComponent(Component componentToSubstitute,
			Component newComponent) {
		Area area = getComponentArea(componentToSubstitute);
		removeComponent(componentToSubstitute);
		addComponent(newComponent,
				area.getColumn1(), area.getRow1(), area.getColumn2(),
				area.getRow2());
	}

	private void addTitle(String caption) {
		Label label = newTitleLabel(caption);
		addComponentWithHorizontalSpan(label);
	}

	private Label newTitleLabel(String caption) {
		Label label = new Label("<b>" + caption + "</b><hr>", ContentMode.HTML);
		label.setWidth("100%");
		return label;
	}

	private void addComponentWithHorizontalSpan(Component component) {
		addComponent(component, 0, getCursorY(), 1, getCursorY());
//		Area area = getComponentArea(component);
//		removeComponent(component);
//		addComponent(component, area.getColumn1(), area.getRow1(),
//				area.getColumn2() + 1, area.getRow2());
	}

	private PopupDateFieldWithParser newDateField() {
		PopupDateFieldWithParser startDateField = new PopupDateFieldWithParser();
		startDateField.setImmediate(true);
		startDateField.setDateFormat("E dd/MM/yyyy");
		startDateField.setShowISOWeekNumbers(true);
		startDateField.setStyleName("monday-date-field");
		startDateField.setValue(new Date());
		return startDateField;
	}

	@Override
	public void addIntervalTypeRadioButton(Object id, String label) {
		addCheckboxToGroup(intervalUnitGroup, id, label);
	}
	
	@Override
	public void selectIntervalTypeRadioButton(Object id) {
		intervalUnitGroup.setValue(id);
		updateIntervalCountLabel();
	}

	private void updateIntervalCountLabel() {
		boolean plural = false;
		try {
			plural = Integer.parseInt(intervalCountTextField.getValue()) > 1;
		} catch (NumberFormatException ignored) {
		}
		intervalCountLabel.setValue(" "
				+ intervalUnitGroup.getValue().toString().toLowerCase()
				+ (plural ? "s" : ""));
	}

	@Override
	public void addIntervalBoundsModeRadioButton(Object id, String label) {
		addCheckboxToGroup(intervalBoundsModeGroup, id, label);
	}

	@Override
	public void selectIntervalBoundsModeButton(Object id) {
		intervalBoundsModeGroup.setValue(id);
	}

	@Override
	public void addCollaboratorsSelectionModeRadioButton(Object id, String label) {
		addCheckboxToGroup(collaboratorsModeUnitGroup, id, label);
	}

	private static void addCheckboxToGroup(OptionGroup group, Object id, String label) {
		group.addItem(id);
		group.setItemCaption(id, label);
	}

	@Override
	public void setIntervalBoundsModeEnablement(boolean startDateEnablement, boolean endDateEnablement) {
		startDateField.setEnabled(startDateEnablement);
		endDateField.setEnabled(endDateEnablement);
		intervalCountTextField.setEnabled(endDateEnablement);
	}
 
	@Override
	public void setIntervalBounds(Date startDate, Date endDate) {
		startDateField.setValue(startDate);
		endDateField.setValue(endDate);
	}

	@Override
	public void setIntervalCount(int intervalCount) {
		intervalCountTextField.setValue(
				String.valueOf(intervalCount));
		updateIntervalCountLabel();
	}

	@Override
	public void setTaskScopePath(String path) {
		rootTaskTextField.setValue(path);
	}

	@Override
	public void setTaskTreeDepth(int i) {
		taskDepthTextField.setValue(String.valueOf(i));
	}

	@Override
	public void setOnlyKeepTaskWithContributions(
			boolean onlyKeepTaskWithContributions) {
		onlyKeepTasksWithContribsCheckbox
				.setValue(
				onlyKeepTaskWithContributions);
	}

	@Override
	public void setErrorMessage(String message) {
		if (message != null && !"".equals(message.trim())) {
			warningIcon.setVisible(true);
			statusLabel.setValue(message);
		} else {
			warningIcon.setVisible(false);
			statusLabel.setValue("");
		}
	}

	@Override
	public void setRowContentConfigurationEnabled(boolean enabled) {
		decreaseTaskDepthButton.setEnabled(enabled);
		taskDepthTextField.setEnabled(enabled);
		increaseTaskDepthButton.setEnabled(enabled);
		taskDepthLayoutDocLabel.setEnabled(enabled);
		onlyKeepTasksWithContribsCheckbox.setEnabled(enabled);
	}

	@Override
	public void setCollaboratorsSelectionUIEnabled(boolean enabled) {
		selectedCollaboratorsComponent.setEnabled(enabled);
	}

	@Override
	public void selectCollaboratorsSelectionModeRadioButton(Object newValue) {
		collaboratorsModeUnitGroup.setValue(newValue);
	}


}
