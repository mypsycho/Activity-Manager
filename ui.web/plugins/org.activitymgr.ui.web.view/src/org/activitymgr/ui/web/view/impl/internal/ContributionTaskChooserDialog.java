package org.activitymgr.ui.web.view.impl.internal;

import java.util.Map;

import org.activitymgr.ui.web.logic.IContributionTaskChooserLogic;
import org.activitymgr.ui.web.view.impl.dialogs.AbstractTaskChooserDialog;
import org.activitymgr.ui.web.view.impl.internal.util.MapBasedDatasource;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ContributionTaskChooserDialog 
		extends AbstractTaskChooserDialog<IContributionTaskChooserLogic> 
		implements IContributionTaskChooserLogic.View {

	private ListSelect recentTasksSelect;
	private CheckBox newSubTaskCheckbox;
	private Label newTaskFieldsGroup;
	private TextField newSubTaskCodeField;
	private TextField newTaskName;
	private ComboBox newTaskPattern;

	public ContributionTaskChooserDialog() {
    }

	@Override
	protected Component createBody() {
		return new HorizontalLayout() {{
			setSizeFull();
			// Task tree
			addComponent(ContributionTaskChooserDialog.super.createBody());
			
			addComponent(new VerticalLayout() {{
				setSizeFull();
				setMargin(new MarginInfo(false, false, false, true));
				
		        recentTasksSelect = new ListSelect("Recent :") {{
			        setSizeFull();
			        setImmediate(true);
			        setNullSelectionAllowed(false);
			        addValueChangeListener(evt -> 
						getLogic().onRecentTaskClicked((Long) getValue()));
		        }};

		        
		        addComponent(recentTasksSelect);
		        addComponent(new Label("&nbsp;", ContentMode.HTML)); // empty line
		        
		        AbstractLayout panel = createNewTaskPanel();
		        addComponent(panel);
				
		        setExpandRatio(recentTasksSelect, 60);
		        setExpandRatio(panel, 40);
			}});
	        setExpandRatio(components.getFirst(), 40);
	        setExpandRatio(components.getLast(), 60);
			
		}};
	}

	protected AbstractLayout createNewTaskPanel() {
		VerticalLayout result = new VerticalLayout();

        newSubTaskCheckbox = new CheckBox("Create a new task");
        newSubTaskCheckbox.setImmediate(true);
        result.addComponent(newSubTaskCheckbox);
        newSubTaskCheckbox.addValueChangeListener(evt -> 
			getLogic().onNewTaskCheckboxClicked());
        
        newTaskFieldsGroup = new Label("Task attributes");
        result.addComponent(newTaskFieldsGroup);
        
        // Line to identify new task
        HorizontalLayout idTaskLine = new HorizontalLayout();
        result.addComponent(idTaskLine);
        
        newSubTaskCodeField = new TextField();
        newSubTaskCodeField.setWidth("60px");
        newSubTaskCodeField.setImmediate(true);
        newSubTaskCodeField.setInputPrompt("Code");
        idTaskLine.addComponent(newSubTaskCodeField);
        newSubTaskCodeField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        newSubTaskCodeField.addTextChangeListener(evt -> 
			getLogic().onNewTaskCodeChanged(evt.getText()));
        
        newTaskName = new TextField();
        newTaskName.setWidth("150px");
        newTaskName.setImmediate(true);
        newTaskName.setInputPrompt("Name (required)");
        idTaskLine.addComponent(newTaskName);
        newTaskName.setTextChangeEventMode(TextChangeEventMode.EAGER);
        newTaskName.addTextChangeListener(evt -> 
			getLogic().onNewTaskNameChanged(evt.getText()));
        
        // Pattern
		newTaskPattern = new ComboBox("Creation pattern");
        newTaskPattern.setNullSelectionAllowed(false);
        newTaskPattern.setImmediate(true);
        newTaskPattern.setTextInputAllowed(false);
        newTaskPattern.setVisible(false); // Hidden by default
        result.addComponent(newTaskPattern);
        
        setNewTaskFieldsEnabled(newSubTaskCheckbox.getValue());
        
        return result;
	}
	
    @Override
    public void setCreationPatterns(Map<String, String> patterns) {
    	newTaskPattern.setVisible(true);
    	MapBasedDatasource<String> datasource = new MapBasedDatasource<String>(patterns);
    	newTaskPattern.setContainerDataSource(datasource);
    	newTaskPattern.setItemCaptionPropertyId(MapBasedDatasource.LABEL_PROPERTY_ID);
    	newTaskPattern.setValue(patterns.keySet().iterator().next());
    }

    @Override
    public void setRecentTasks(Map<Long, String> recentTasks) {
    	MapBasedDatasource<Long> datasource = new MapBasedDatasource<Long>(recentTasks);
    	recentTasksSelect.setContainerDataSource(datasource);
    	recentTasksSelect.setItemCaptionPropertyId(MapBasedDatasource.LABEL_PROPERTY_ID);
    }
    
	@Override
	public void setNewTaskFieldsEnabled(boolean enabled) {
		newTaskFieldsGroup.setEnabled(enabled);
		newSubTaskCodeField.setEnabled(enabled);
		newTaskName.setEnabled(enabled);
		newTaskPattern.setEnabled(enabled);
	}

	@Override
	public String getSelectedTaskCreationPatternId() {
		return (String) newTaskPattern.getValue();
	}

}