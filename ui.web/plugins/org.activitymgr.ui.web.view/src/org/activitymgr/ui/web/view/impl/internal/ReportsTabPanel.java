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
		editionContainer.setId("ReportsTabPanel");
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
