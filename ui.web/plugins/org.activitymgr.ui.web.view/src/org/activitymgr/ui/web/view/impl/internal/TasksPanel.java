package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.internal.util.AlignHelper;
import org.activitymgr.ui.web.view.impl.internal.util.TreeTableDatasource;

import com.google.inject.Inject;
import com.vaadin.ui.Component;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.Table.ColumnGenerator;

@SuppressWarnings("serial")
public class TasksPanel extends AbstractTabPanel<ITasksTabLogic> implements ITasksTabLogic.View {

	private TreeTable taskTree;

	@Inject
	public TasksPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createBodyComponent() {
		taskTree = new TreeTable();
		addComponent(taskTree);
		taskTree.setImmediate(true);
		taskTree.setMultiSelect(false);
		taskTree.setSizeFull();
		taskTree.addValueChangeListener(event ->
				getLogic().onTaskSelected(event.getProperty().getValue()));
		return taskTree;
	}
	
    @Override
	public void setTreeContentProviderCallback(
			final ITreeContentProviderCallback<Long> tasksProvider) {
		TreeTableDatasource<Long> dataSource = new TreeTableDatasource<Long>(getResourceCache(), tasksProvider);
		taskTree.setContainerDataSource(dataSource);
		
		ColumnGenerator cellProvider = (source, itemId, prop) -> 
			tasksProvider.getCell((Long) itemId, (String) prop);
		
		for (String propertyId : dataSource.getContainerPropertyIds()) {
			taskTree.addGeneratedColumn(propertyId, cellProvider);
			taskTree.setColumnWidth(propertyId, tasksProvider.getColumnWidth(propertyId));
			taskTree.setColumnAlignment(propertyId, AlignHelper.toVaadinAlign(tasksProvider.getColumnAlign(propertyId)));
		}
	}
    
}
