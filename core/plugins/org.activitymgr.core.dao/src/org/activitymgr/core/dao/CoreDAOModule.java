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
package org.activitymgr.core.dao;

import java.util.function.Function;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.ReportCfg;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.impl.dao.CollaboratorDAOImpl;
import org.activitymgr.core.impl.dao.ContributionDAOImpl;
import org.activitymgr.core.impl.dao.CoreDAOImpl;
import org.activitymgr.core.impl.dao.DTOFactoryImpl;
import org.activitymgr.core.impl.dao.DurationDAOImpl;
import org.activitymgr.core.impl.dao.ReportCfgDAOImpl;
import org.activitymgr.core.impl.dao.ReportDAOImpl;
import org.activitymgr.core.impl.dao.TaskDAOImpl;
import org.activitymgr.core.orm.DAOFactory;
import org.activitymgr.core.orm.IDAO;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class CoreDAOModule implements Module {
	
	<T> Provider<IDAO<T>> createInjectedProvider(
				DAOFactory factory, 
				Class<T> defaultType, 
				Function<IDTOClassProvider, Class<? extends T>> adapter
				) {
		
			return new Provider<IDAO<T>>() {
			
			@Inject(optional = true)
			IDTOClassProvider dtoClassProvider;
			
			@SuppressWarnings("unchecked")
			public IDAO<T> get() {
				Class<? extends T> impClass = dtoClassProvider == null 
						? defaultType
						: adapter.apply(dtoClassProvider);
				@SuppressWarnings("rawtypes") // Dirty trick
				// as injection uses strict generic
				IDAO dao = factory.getDAO(impClass);
				return (IDAO<T>) dao;
			}
		};
		
	}
	
	
	@Override
	public void configure(Binder binder) {
		// Bind DAOs
		final DAOFactory daoFactory = new DAOFactory();
		
		binder.bind(new TypeLiteral<IDAO<Collaborator>>() {})
			.toProvider(createInjectedProvider(daoFactory, Collaborator.class, IDTOClassProvider::getCollaboratorClass))
			.in(Singleton.class);
		
		binder.bind(new TypeLiteral<IDAO<Task>>() {})
			.toProvider(createInjectedProvider(daoFactory, Task.class, IDTOClassProvider::getTaskClass))
			.in(Singleton.class);
		
		binder.bind(new TypeLiteral<IDAO<Duration>>() {})
			.toProvider(createInjectedProvider(daoFactory, Duration.class, IDTOClassProvider::getDurationClass))
			.in(Singleton.class);
		
		binder.bind(new TypeLiteral<IDAO<Contribution>>() {})
			.toProvider(createInjectedProvider(daoFactory, Contribution.class, IDTOClassProvider::getContributionClass))
			.in(Singleton.class);
		
		binder.bind(new TypeLiteral<IDAO<ReportCfg>>() {})
			.toProvider(() -> daoFactory.getDAO(ReportCfg.class))
			.in(Singleton.class);
		
		// Bind DAO wrappers
		binder.bind(ICollaboratorDAO.class)
			.to(CollaboratorDAOImpl.class)
			.in(Singleton.class);
		binder.bind(ITaskDAO.class)
			.to(TaskDAOImpl.class)
			.in(Singleton.class);
		binder.bind(IDurationDAO.class)
			.to(DurationDAOImpl.class)
			.in(Singleton.class);
		binder.bind(IContributionDAO.class)
			.to(ContributionDAOImpl.class)
			.in(Singleton.class);
		binder.bind(IReportCfgDAO.class)
			.to(ReportCfgDAOImpl.class)
			.in(Singleton.class);

		// Bind core DAO & ModelManager
	    binder.bind(IDTOFactory.class)
	    	.to(DTOFactoryImpl.class)
    		.in(Singleton.class);
	    binder.bind(ICoreDAO.class)
	    	.to(CoreDAOImpl.class)
	    	.in(Singleton.class);
	    
	    // Other DAOs
	    binder.bind(IReportDAO.class)
	    	.to(ReportDAOImpl.class)
	    	.in(Singleton.class);
	}
	
	
	
}
