package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.activitymgr.ui.web.logic.IAOPWrappersBuilder;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.ITabLogic;
import org.activitymgr.ui.web.logic.IUILogicContext;
import org.activitymgr.ui.web.logic.impl.event.ConnectedCollaboratorEvent;
import org.activitymgr.ui.web.logic.impl.event.EventBusImpl;
import org.activitymgr.ui.web.logic.impl.event.LogoutEvent;
import org.activitymgr.ui.web.logic.spi.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.spi.ITabFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class RootLogicImpl implements IRootLogic {

	private static final Comparator<ITabFactory> TAB_SORTER = Comparator.comparing(ITabFactory::getTabOrderPriority);

	private Injector userInjector;
	
	private IRootLogic.View view;
	
	private IEventBus eventBus;
	
	private IEventListener<LogoutEvent> logoutListener;
	
	private IEventListener<ConnectedCollaboratorEvent> collaboratorListener;
	
	public RootLogicImpl(IRootLogic.View rootView, Injector mainInjector) {
		userInjector = mainInjector.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(IEventBus.class).to(EventBusImpl.class).in(Singleton.class);
				bind(IUILogicContext.class).to(UIContextImpl.class).in(Singleton.class);
				// and Transactional wrapper builder
				bind(IAOPWrappersBuilder.class)
						.to(AOPWrappersBuilderImpl.class);
				bind(IRootLogic.class).toInstance(RootLogicImpl.this);
			}
		});

		IAOPWrappersBuilder aop = userInjector
				.getInstance(IAOPWrappersBuilder.class);
		// View registration
		this.view = aop.buildViewWrapperForLogic(rootView, IRootLogic.View.class);
		view.registerLogic(aop.buildLogicWrapperForView(this, IRootLogic.class));

		// Event listeners registration
		eventBus = userInjector.getInstance(IEventBus.class);
		collaboratorListener = event -> {
			// Create the tab container
			TabFolderLogicImpl tabFolderLogic = new TabFolderLogicImpl(RootLogicImpl.this);
			getView().setContentView(tabFolderLogic.getView());
			
			// Add tabs
			IFeatureAccessManager accessMgr = userInjector.getInstance(IFeatureAccessManager.class);
			Set<ITabFactory> tabFactories = userInjector.getInstance(Key.get(new TypeLiteral<Set<ITabFactory>>() {}));
			List<ITabFactory> sortedTabFactories = new ArrayList<>(tabFactories);
			Collections.sort(sortedTabFactories, TAB_SORTER);
			for (ITabFactory tabFactory : sortedTabFactories) {
				if (accessMgr.hasAccessToTab(event.getConnectedCollaborator(), tabFactory.getTabId())) {
					ITabLogic<?> tabLogic = tabFactory.create(tabFolderLogic);
					tabFolderLogic.addTab(tabFactory.getTabId(), tabLogic.getLabel(), tabLogic);
				}
			}
			String selectedTab = getView().getCookie(TabFolderLogicImpl.SELECTED_TAB_COOKIE);
			if (selectedTab != null) {
				tabFolderLogic.setSelectedTab(selectedTab);
			}
		};
		eventBus.register(ConnectedCollaboratorEvent.class, collaboratorListener);

		// Create authentication logic
		showAuthenticationUI(false);
		
		// Register logout listener
		logoutListener = event -> showAuthenticationUI(true);
		eventBus.register(LogoutEvent.class, logoutListener);
	}

	private void showAuthenticationUI(boolean afterLogout) {
		getView().setContentView(
				new AuthenticationLogicImpl(this, afterLogout).getView());
	}
	
	
	@Override
	public ILogic<?> getParent() {
		return null;
	}

	@Override
	public View getView() {
		return view;
	}
	
	@Override
	public <T> T injectMembers(T instance) {
		userInjector.injectMembers(instance);
		return instance;
	}

	@Override
	public IUILogicContext getContext() {
		return userInjector.getInstance(IUILogicContext.class);
	}

	@Override
	public void dispose() {
		eventBus.unregister(logoutListener);
		eventBus.unregister(collaboratorListener);
	}
}
