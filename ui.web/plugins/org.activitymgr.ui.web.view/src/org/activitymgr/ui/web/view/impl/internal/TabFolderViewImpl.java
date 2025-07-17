package org.activitymgr.ui.web.view.impl.internal;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.themes.Runo;

@SuppressWarnings("serial")
public class TabFolderViewImpl extends TabSheet implements ITabFolderLogic.View {

	private ITabFolderLogic logic;
	
	private boolean moreThanOneTab = false;
	
	private Map<Component, String> tabIdsByComponentsMap = new IdentityHashMap<Component, String>();

	private Map<String, Component> componentsByTabIdsMap = new HashMap<String, Component>();

	
	@Inject
	private IResourceCache resourceCache;
	
	public TabFolderViewImpl() {
		setStyleName("main-activitymgr-tab " + Runo.TABSHEET_SMALL);
		 
		setTabsVisible(false);
		setSizeFull();
		addSelectedTabChangeListener(event -> {
			if (VaadinService.getCurrentRequest() != null) {
				String tabId = tabIdsByComponentsMap.get(getSelectedTab());
				if (tabId != null) {
					logic.onSelectedTabChanged(tabId);
				}
			}
		});
	}

	@Override
	public void registerLogic(ITabFolderLogic logic) {
		this.logic = logic;
	}

	@Override
	public void addTab(String id, String label, IView<?> view, String icon) {
		Component component = (Component) view;

		Resource iconRes = null;
		if (icon != null) {
			iconRes = resourceCache.getResource(icon + ".gif");
		}
		addTab(component, label, iconRes);
		
		tabIdsByComponentsMap.put(component, id);
		
		componentsByTabIdsMap.put(id, component);
		if (moreThanOneTab) {
			setTabsVisible(true);
		}
		moreThanOneTab = true;
	}

	@Override
	public void setSelectedTab(String id) {
		Component c = componentsByTabIdsMap.get(id);
		if (c != null) {
			setSelectedTab(c);
		}
	}
	
}
