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
package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.Align;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IUILogicContext;
import org.activitymgr.ui.web.logic.spi.ICellLogicFactory;
import org.activitymgr.ui.web.logic.spi.ICollaboratorsCellLogicFactory;

import com.google.inject.Inject;

public class CollaboratorsCellLogicFatory implements ICollaboratorsCellLogicFactory {
	

	@Override
	public ILogic<?> createCellLogic(final AbstractLogicImpl<?> parentLogic, final IUILogicContext context, final Collaborator collaborator, String propertyId, boolean readonly) {
		switch(propertyId) {
		case IS_ACTIVE_PROPERTY_NAME_ID: 
			return createBoolCell(parentLogic, !readonly, collaborator, 
				Collaborator::getIsActive, Collaborator::setIsActive);
		case LOGIN_PROPERTY_ID:
			return createTextCell(parentLogic, !readonly, collaborator, 
					Collaborator::getLogin, Collaborator::setLogin);
		case FIRST_PROPERTY_NAME_ID:
			return createTextCell(parentLogic, !readonly, collaborator, 
					Collaborator::getFirstName, Collaborator::setFirstName);
		case LAST_PROPERTY_NAME_ID:
			return createTextCell(parentLogic, !readonly, collaborator, 
					Collaborator::getLastName, Collaborator::setLastName);
		}

		throw new IllegalArgumentException(propertyId);
	}
	
	private <C extends Collaborator, T> Modifier<C, T> updater(BiConsumer<C, T> modifier) {
		return (logic, element, value) -> {
			modifier.accept(element, value);
			logic.getModelMgr().updateCollaborator(element);
		};
	}
	
	protected <T extends Collaborator> ILogic<?> createBoolCell(AbstractLogicImpl<?> parent,
			boolean editable, T collaborator, 
			Predicate<T> getter,  BiConsumer<T, Boolean> setter
			) {
		return ICellLogicFactory.createBoolCell(parent, editable, collaborator, getter, updater(setter));
	}
	
	protected <T extends Collaborator> ILogic<?> createTextCell(AbstractLogicImpl<?> parent,
			boolean editable, T collaborator, 
			Function<T, String> getter,  BiConsumer<T, String> setter
			) {
		return ICellLogicFactory.createTextCell(parent, editable, collaborator, getter, updater(setter));
	}
	

	
	@Override
	public Collection<String> getPropertyIds() {
		return PROPERTY_IDS;
	}

	@Override
	public Integer getColumnWidth(String propertyId) {
		switch (propertyId) {
		case IS_ACTIVE_PROPERTY_NAME_ID:
			return 30;
		case LOGIN_PROPERTY_ID:
			return 150;
		case FIRST_PROPERTY_NAME_ID:
		case LAST_PROPERTY_NAME_ID:
			return 200;
		default:
			return 70;
		}

	}


	@Override
	public Align getColumnAlign(String propertyId) {
		return Align.LEFT;
	}

}
