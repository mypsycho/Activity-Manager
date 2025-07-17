package org.activitymgr.ui.web.view.impl.internal.vaadin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;

import org.activitymgr.ui.web.view.impl.internal.ViewModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

@SuppressWarnings("rawtypes")
public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	public static final String BUNDLE_ID = "org.activitymgr.ui.web.view";

	private static final String UI_PROVIDER_PARAM = "UIProvider";

	private static final String PRODUCTION_MODE_PARAM = "productionMode";

	private static final List<Class<?>> SERVICE_CLASSES = Arrays
			.asList(IExtensionRegistry.class,
					HttpService.class 
					/* , HttpContextExtensionService.class */);
	
	private static String WEB_RES_PATH = "VAADIN";
	
	private static String BUNDLE_PRIORITY = "Vaadin-priority";
	
	private static Comparator<Bundle> BUNDLE_COMPARATOR = Comparator.comparing((Bundle it) -> {
		String priority = it.getHeaders().get(BUNDLE_PRIORITY);
		if (priority != null && !priority.isBlank()) {
			try {
				return Integer.parseInt(priority);
			} catch (NumberFormatException e) {
			}
		}
		return 0;
	}).reversed();

	private BundleContext context;

	private Map<Class<?>, ServiceTracker<?, ?>> serviceTrackers = new HashMap<Class<?>, ServiceTracker<?, ?>>();

	private IExtensionRegistry extensionRegistryService;

	private boolean initialized = false;

	private HttpService httpService;

	private static Activator singleton = null;
	
	private Injector injector;

	public Activator() {
		// Save singleton
		singleton = this;
	}
	
	public static Activator getDefault() {
		return singleton;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		for (Class<?> cl : SERVICE_CLASSES) {
			@SuppressWarnings({ "unchecked" })
			ServiceTracker st = new ServiceTracker(context, cl.getName(), this);
			serviceTrackers.put(cl, st);
			st.open();
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		this.context = null;
		for (ServiceTracker<?, ?> st : serviceTrackers.values()) {
			st.close();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object addingService(ServiceReference reference) {
		Object service = context.getService(reference);
		if (service instanceof IExtensionRegistry
				&& extensionRegistryService == null) {
			extensionRegistryService = (IExtensionRegistry) service;
		}
		if (service instanceof HttpService && httpService == null) {
			httpService = (HttpService) service;
		}
		if (!initialized && extensionRegistryService != null
				&& httpService != null) {
			// Prevents from initializing several times
			initialized = true;
			init();
		}
		return null;
	}

	@Override
	public void modifiedService(ServiceReference reference,
			Object service) {
	}

	@Override
	public void removedService(ServiceReference reference,
			Object service) {
		context.ungetService(reference);
	}

	public IExtensionRegistry getExtensionRegistryService() {
		return extensionRegistryService;
	}


	private void init() {
		List<AbstractModule> modules = new ArrayList<AbstractModule>();
		IConfigurationElement[] cfgs = getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.additionalModules");
		for (IConfigurationElement cfg : cfgs) {
			try {
				modules.add((AbstractModule) cfg.createExecutableExtension("class"));
			} catch (CoreException e) {
				throw new IllegalStateException(e);
			}
		}
		// Activity Manager module can be overriden
		Module module = Modules.override(new ViewModule()).with(modules);
		// Injector creation
		injector = Guice.createInjector(module);

		Properties props = new Properties();
		props.put(PRODUCTION_MODE_PARAM, "true");
		props.put(UI_PROVIDER_PARAM, OSGiUIProvider.class.getName());

		// Retrieve bundles that may contain resources
		List<Bundle> resourceProviderBundles = new ArrayList<>();
		for (Bundle bundle : context.getBundles()) {
			if (bundle.getResource(WEB_RES_PATH) != null) {
				System.out.println("Register vaadin contributions from " + bundle);
				resourceProviderBundles.add(bundle);
			}
		}
		
		java.util.Collections.sort(resourceProviderBundles, BUNDLE_COMPARATOR);
		
		// Register application bundle
		try {
			HttpContext appContext = new OSGiUIHttpContext(httpService.createDefaultHttpContext(), 
					resourceProviderBundles);
			
			httpService.registerServlet("/", new ActivityMgrServlet(), props, appContext);
		} catch (ServletException | NamespaceException ex) {
			logError("Error while registering Vaadin UI", ex);
		}

	}

	public void logError(String message, Throwable exception) {
		ILog log = Platform.getLog(context.getBundle());
		log.log(new Status(IStatus.ERROR, BUNDLE_ID, message, exception));

	}

	public void logError(String message) {
		ILog log = Platform.getLog(context.getBundle());
		log.log(new Status(IStatus.ERROR, BUNDLE_ID, message));

	}

	public void logWarn(String message) {
		ILog log = Platform.getLog(context.getBundle());
		log.log(new Status(IStatus.WARNING, BUNDLE_ID, message));
	}

	public Injector getInjector() {
		return injector;
	}

}
