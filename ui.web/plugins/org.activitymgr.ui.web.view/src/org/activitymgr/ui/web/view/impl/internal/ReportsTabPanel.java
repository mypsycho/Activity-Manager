package org.activitymgr.ui.web.view.impl.internal;

import java.util.Collection;

import org.activitymgr.ui.web.logic.IReportsLogic.View;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.IStandardButtonLogic;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ReportsTabPanel extends AbstractTabPanel<IReportsTabLogic>
		implements IReportsTabLogic.View {


	private HorizontalLayout editionContainer;

	private ListSelect confsSelector;

	private HorizontalLayout confsToolbar;

	private IndexedContainer confsModel;

	private View reportsView;

	@Inject
	public ReportsTabPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}


	@Override
	protected Component createLeftComponent() {
		VerticalLayout result = new VerticalLayout();
		// style= overflow : auto
		result.setSpacing(true);
		result.addStyleName("parent-selector");
		// result.setMargin(new MarginInfo(false, true, false, false)); //?

		
		confsToolbar = new HorizontalLayout();
		result.addComponent(confsToolbar);
		// reportCfgsButtonsPanel.setWidth(100, Unit.PERCENTAGE);

		confsSelector = new ListSelect();
		confsSelector.setImmediate(true);
		confsSelector.setMultiSelect(true);
		confsSelector.setNullSelectionAllowed(false);
		confsSelector.setSizeFull();
		confsSelector.addStyleName("main-selector");
		// reportsList.
		// reportsList.style = min-width: 250px
		
		confsModel = (IndexedContainer) confsSelector
				.getContainerDataSource();

		result.addComponent(confsSelector);

		result.setExpandRatio(confsSelector, 100);
		result.setSizeFull();

		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void registerLogic(IReportsTabLogic logic) {
		super.registerLogic(logic);
		confsSelector.addValueChangeListener(event ->
			getLogic().onSelectionChanged((Collection<Long>) confsSelector.getValue()));

	}
	
	
	@Override
	public void removeReportCfg(long id) {
		confsModel.removeItem(id);
	}

	@Override
	public void addReportCfg(long id, String name, int idx) {
		confsModel.addItemAt(idx, id);
		confsSelector.setItemCaption(id, name);
	}


	@Override
	protected Component createBodyComponent() {
		editionContainer = new HorizontalLayout();
		editionContainer.setSizeFull();
		editionContainer.setMargin(new MarginInfo(false, false, false, true));
		editionContainer.addStyleName("parent-selector");
		return editionContainer;
	}

	@Override
	public void setReportsView(View view) {
		this.reportsView = view;
		Component display = (Component) view;
		// display.setSizeFull();
		editionContainer.addComponent(display);
		editionContainer.setExpandRatio(display, 100);
	}

	@Override
	public void addReportConfigurationButton(IStandardButtonLogic.View view) {
		confsToolbar.addComponent((Component) view);
	}

	@Override
	public void setReportsPanelEnabled(boolean b) {
		((Component) reportsView).setEnabled(b);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectReportCfg(long id) {
		for (Long selected : (Collection<Long>) confsSelector.getValue()) {
			confsSelector.unselect(selected);
		}
		confsSelector.select(id);
	}

}
