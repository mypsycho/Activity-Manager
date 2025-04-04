package org.activitymgr.ui.web.view.impl.internal.vaadin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.activitymgr.ui.web.logic.IUILogicContext;
import org.activitymgr.ui.web.logic.spi.IRESTServiceLogic;
import org.activitymgr.ui.web.view.impl.internal.ActivityManagerUI;
import org.jsoup.nodes.Element;

import com.google.inject.Inject;
import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
final class ActivityMgrServlet extends VaadinServlet {


	@Inject
	private Set<IRESTServiceLogic> serviceLogics;

	private Map<String, IRESTServiceLogic> serviceLogicsMap = new HashMap<>();

	ActivityMgrServlet() {
		Activator.getDefault().getInjector().injectMembers(this);
		for (IRESTServiceLogic serviceLogic : serviceLogics) {
			String path = serviceLogic.getPath().trim();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			serviceLogicsMap.put(path, serviceLogic);
		}
	}

	@Override
	public void service(ServletRequest req,
			ServletResponse res) throws ServletException,
			IOException {
		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) res;
		String uri = request.getRequestURI();
		if (uri.startsWith("/service")) {
			String servicePath = uri.substring("/service".length());
			IRESTServiceLogic serviceLogic = serviceLogicsMap.get(servicePath);
			if (serviceLogic != null) {
				IUILogicContext context = null;
				String uiIdStr = request.getParameter("v-uiId");
				if (uiIdStr != null) {
					int uiId = Integer.parseInt(uiIdStr);
					HttpSession session = request.getSession(false);
					if (session != null) {
						VaadinSession vaadinSession = (VaadinSession) session
								.getAttribute("com.vaadin.server.VaadinSession." + ActivityMgrServlet.class.getName());
						if (vaadinSession != null) {
							Iterator<UI> iterator = vaadinSession.getUIs()
									.iterator();
							while (iterator.hasNext()) {
								UI ui = (UI) iterator.next();
								if (ui.getUIId() == uiId) {
									context = ((ActivityManagerUI) ui)
											.getRootLogic().getContext();
								}
							}
						}
					}
				}
				final IUILogicContext theContext = context;
				serviceLogic.service(new IRESTServiceLogic.Request() {
					@Override
					public Enumeration<String> getParameterNames() {
						return request.getParameterNames();
					}

					@Override
					public String getCookie(String name) {
						Cookie[] cookies = request.getCookies();
						if (name != null && cookies != null) {
							for (Cookie cookie : cookies) {
								if (name.equals(cookie.getName())) {
									return cookie.getValue();
								}
							}
						}
						return null;
					}

					@Override
					public String getHeader(String name) {
						return request.getHeader(name);
					}

					@Override
					public String getParameter(String name) {
						return request.getParameter(name);
					}

					@Override
					public String[] getListParameter(String name) {
						String str = request.getParameter(name);
						String[] result = null;
						if (str != null) {
							str = str.trim();
							if (!"".equals(str)) {
								result = str.split(",");
								for (int i = 0; i < result.length; i++) {
									result[i] = result[i].trim();
								}
							}
						}
						return result;
					}

					@Override
					public IUILogicContext getAttachedUILogicContext() {
						return theContext;
					}
				}, new IRESTServiceLogic.Response() {
					private String contentType;

					@Override
					public void setContentType(String contentType) {
						this.contentType = contentType;
						response.setHeader("Content-type", contentType);
					}

					@Override
					public void sendError(int sc, String msg)
							throws IOException {
						response.sendError(sc, msg);
					}

					@Override
					public OutputStream getOutputStream() throws IOException {
						return response.getOutputStream();
					}

					@Override
					public String getContentType() {
						return contentType;
					}

					@Override
					public void addHeader(String name, String value) {
						response.addHeader(name, value);
					}
				});
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}

		} else {
			super.service(req, res);
		}
	}

	@Override
	protected void servletInitialized() throws ServletException {
		super.servletInitialized();
		getService().addSessionInitListener(event -> event.getSession().addBootstrapListener(
			new BootstrapListener() {

				@Override
				public void modifyBootstrapFragment(BootstrapFragmentResponse response) {
				}

				@Override
				public void modifyBootstrapPage(BootstrapPageResponse response) {
					Element head = response.getDocument().head();
					head.prependElement("script")
							.attr("src",
									"https://apis.google.com/js/platform.js")
							.attr("async", "true")
							.attr("defer", "true");
				}
			})
		);
	}

}