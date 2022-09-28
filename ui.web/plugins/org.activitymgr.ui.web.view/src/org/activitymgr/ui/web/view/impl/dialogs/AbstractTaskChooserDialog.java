package org.activitymgr.ui.web.view.impl.dialogs;

import java.util.Stack;

import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.logic.spi.ITasksCellLogicFactory;
import org.activitymgr.ui.web.view.impl.internal.util.TreeTableDatasource;

import com.vaadin.data.Item;
import com.vaadin.event.ShortcutListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class AbstractTaskChooserDialog<LOGIC extends ITaskChooserLogic<?>>
		extends AbstractDialog 
		implements Button.ClickListener, ITaskChooserLogic.View<LOGIC> {

	private Button ok = new Button("Ok", this);
	private Button cancel = new Button("Cancel", this);
	private LOGIC logic;
	private Label statusLabel;
	private TextField filterField;
	private Tree taskTree;

	public AbstractTaskChooserDialog() {
        super("Select a task");
        setModal(true);

        setWidth(700, Unit.PIXELS);
        setHeight(550, Unit.PIXELS);

        // Global layout
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setMargin(new MarginInfo(true, true, true, true));
        setContent(content);
        
        // Main part 
		Component body = createBody();
		content.addComponent(body);
        
        // Footer containing status & OK / Cancel buttons
		Component footer = createFooter();
		content.addComponent(footer);
        
        // Set expand ratios
		content.setExpandRatio(body, 95);
		content.setExpandRatio(footer, 5);
        
        // Register listeners
        filterField.addTextChangeListener(event -> logic.onTaskFilterChanged(event.getText()));
        taskTree.addValueChangeListener(evt -> logic.onSelectionChanged((Long) taskTree.getValue()));

        // Key listener
        addShortcutListener(new ShortcutListener("OK", ShortcutListener.KeyCode.ENTER, new int[] {}) {
			@Override
			public void handleAction(Object sender, Object target) {
				if (ok.isEnabled()) {
			        if (getParent() != null) {
			            close();
			        }
		        	logic.onOkButtonClicked((Long) taskTree.getValue());
				} else if (target == taskTree && taskTree.getValue() != null) {
					taskTree.expandItem(taskTree.getValue());
				}
			}
		});
    }

	protected Component createBody() {
        // Task tree
        VerticalLayout vl = new VerticalLayout();
        vl.setSizeFull();
        filterField = new TextField();
        filterField.setWidth("100%");
        filterField.setImmediate(true);
        vl.addComponent(filterField);
        // Grey text when empty
        filterField.setInputPrompt("Type a text to filter...");
       
        Panel treeContainer = new Panel();
        treeContainer.setSizeFull();
        vl.addComponent(treeContainer);
        vl.setSizeFull();
        taskTree = new Tree();
        taskTree.setNullSelectionAllowed(false);
        treeContainer.setContent(taskTree);
        taskTree.setImmediate(true);
        taskTree.setHtmlContentAllowed(true);

        // Set expand ratios for left container
        vl.setExpandRatio(filterField, 7);
        vl.setExpandRatio(treeContainer, 93);

		return vl;
	}

	protected Component createFooter() {
		HorizontalLayout footerLayout = new HorizontalLayout();
		footerLayout.setSizeFull();

        statusLabel = new Label();
        footerLayout.addComponent(statusLabel);
        footerLayout.addComponent(ok);
        footerLayout.addComponent(cancel);
        footerLayout.setComponentAlignment(cancel, Alignment.TOP_RIGHT);

        // Set expand ratios
        footerLayout.setExpandRatio(statusLabel, 80);
        footerLayout.setExpandRatio(ok, 10);
        footerLayout.setExpandRatio(cancel, 10);
		return footerLayout;
	}

    public void focus() {
    	taskTree.focus();
    }

    @Override
	public void setTasksTreeProviderCallback(
			ITreeContentProviderCallback<Long> callback) {
		TreeTableDatasource<Long> dataSource = 
				new TreeTableDatasource<Long>(getResourceCache(), callback);
		taskTree.setContainerDataSource(dataSource);
		taskTree.setItemCaptionPropertyId(ITasksCellLogicFactory.NAME_PROPERTY_ID);
//		taskTree.setItemStyleGenerator((Tree source, Object itemId) -> {
//				Item item = source.getItem(itemId);
//				// item.getItemProperty(itemId).getValue();
//				return "";
//			});
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
        if (getParent() != null) {
            close();
        }
        if (event.getSource() == ok) {
        	logic.onOkButtonClicked((Long)taskTree.getValue());
        }
	}

	@Override
	public void registerLogic(LOGIC logic) {
		this.logic = logic;
	}

	@Override
	public void setOkButtonEnabled(boolean enabled) {
		ok.setEnabled(enabled);
	}

	@Override
	public void setStatus(String status) {
		statusLabel.setValue(status);
	}
	
	@Override
	public void selectTask(long taskId) {
		taskTree.setValue(taskId);
		expandToTask(taskId);
	}

	@Override
	public void expandToTask(long taskId) {
		Object parent = taskId;
		Stack<Object> parents = new Stack<Object>();
		while ((parent = taskTree.getParent(parent)) != null) {
			parents.push(parent);
		}
		while (!parents.isEmpty()) {
			Object element = parents.pop();
			taskTree.expandItem(element);
		}
	}

	protected LOGIC getLogic() {
		return logic;
	}

}