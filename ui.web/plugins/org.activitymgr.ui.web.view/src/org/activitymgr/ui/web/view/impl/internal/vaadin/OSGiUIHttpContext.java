package org.activitymgr.ui.web.view.impl.internal.vaadin;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class OSGiUIHttpContext implements HttpContext {

	private HttpContext defaultContext;
	private Collection<Bundle> resourceProvidersBundles;
	
	protected OSGiUIHttpContext(HttpContext defaultContext, Collection<Bundle> resourceProvidersBundles) {
		this.defaultContext = defaultContext;
		this.resourceProvidersBundles = resourceProvidersBundles;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		return defaultContext.handleSecurity(request, response);
	}
	
	@Override
	public URL getResource(String name) {
		for (Bundle bundle : resourceProvidersBundles) {
			URL url = bundle.getResource(name);
			if (url != null) {
				return url;
			}
		}
		return defaultContext.getResource(name);
	}
	
	@Override
	public String getMimeType(String name) {
		return defaultContext.getMimeType(name);
	}

}
