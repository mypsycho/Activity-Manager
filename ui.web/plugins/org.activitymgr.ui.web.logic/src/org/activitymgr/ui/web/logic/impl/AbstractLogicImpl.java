/*
 * Copyright (c) 2004-2017, Jean-Francois Brazeau. All rights reserved.
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
package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.ui.web.logic.IAOPWrappersBuilder;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.IUILogicContext;

import com.google.inject.Inject;
import com.google.inject.Injector;


@SuppressWarnings("rawtypes")
public abstract class AbstractLogicImpl<VIEW extends ILogic.IView>
 		implements ILogic<VIEW>, CriticalService {

	@Inject
	private Injector injector;
	
	@Inject
	private IEventBus eventBus;
	
	@Inject
	private IAOPWrappersBuilder aopWrappersBuilder;

	@Inject
	private IModelMgr modelMgr;
	
	@Inject
	private IUILogicContext context;
	
	private ILogic<?> parent;

	private VIEW view;

	@SuppressWarnings("unchecked")
	protected AbstractLogicImpl(ILogic<?> parent) {
		this.parent = parent;
		
		// Perform injection
		parent.injectMembers(this);
		
		// Create transactional wrapper
		Class<ILogic<VIEW>> iLogicInterface = getILogicInterfaces(getClass());
		// Create the view and bind the logic to it
		Class<VIEW> viewInterface = null;
		for (Class<?> aClass : iLogicInterface.getDeclaredClasses()) {
			if ("View".equals(aClass.getSimpleName()) && aClass.isInterface()) {
				viewInterface = (Class<VIEW>) aClass;
				break;
			}
		}
		if (viewInterface == null) {
			throw new IllegalStateException(iLogicInterface.getSimpleName() + " does not seem to have a nested View interface");
		}
		// Create the view
		final VIEW realView = injector.getInstance(viewInterface);

		// Wrap it in order to block UI notifications when updates come from the
		// logic
		view = aopWrappersBuilder.buildViewWrapperForLogic(realView,
				viewInterface);

		// Wrap the logic in an AOP container that adds transaction management
		final ILogic<VIEW> transactionalWrapper = aopWrappersBuilder
				.buildLogicWrapperForView(this, iLogicInterface);

		// Register the wrapped logic
		view.registerLogic(transactionalWrapper);
	}

	public VIEW getView() {
		return view;
	}

	public ILogic<?> getParent() {
		return parent;
	}

	protected IRootLogic getRoot() {
		ILogic<?> cursor = this;
		while (cursor != null && !(cursor instanceof IRootLogic))  {
			cursor = cursor.getParent();
		}
		return (IRootLogic) cursor;
	}

	@SuppressWarnings("unchecked")
	private Class<ILogic<VIEW>> getILogicInterfaces(Class<?> c) {
		Class<? extends ILogic<?>> result = null;
		if (c != Object.class) {
			result = getILogicInterfaces(c.getSuperclass());
			for (Class<?> anInterface : c.getInterfaces()) {
				if (ILogic.class.isAssignableFrom(anInterface)
						&& (result == null || result
								.isAssignableFrom(anInterface))) {
					result = (Class<? extends ILogic<?>>) anInterface;
				}
			};
		}
		return (Class<ILogic<VIEW>>) result;
	}

	protected <LOGIC> LOGIC wrapLogicForView(
			final LOGIC wrapped, final Class<LOGIC> interfaceToWrapp) {
		return aopWrappersBuilder.buildLogicWrapperForView(wrapped,
				interfaceToWrapp);
	}

	public <T> T injectMembers(T instance) {
		injector.injectMembers(instance);
		return instance;
	}

	public IEventBus getEventBus() {
		return eventBus;
	}
	
	protected IModelMgr getModelMgr() {
		return modelMgr;
	}

	protected IUILogicContext getContext() {
		return context;
	}

	@Override
	public void dispose() {
	}

	

}
