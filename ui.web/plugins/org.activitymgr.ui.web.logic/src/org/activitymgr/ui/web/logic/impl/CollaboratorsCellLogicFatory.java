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
import org.activitymgr.ui.web.logic.spi.ICollaboratorsCellLogicFactory;

import com.google.inject.Inject;

public class CollaboratorsCellLogicFatory implements ICollaboratorsCellLogicFactory {

	@Inject
	private IModelMgr modelMgr;

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.ICollaboratorsCellLogicFactory#createCellLogic(org.activitymgr.core.dto.Collaborator, java.lang.String, boolean)
	 */
	@Override
	public ILogic<?> createCellLogic(final AbstractLogicImpl<?> parentLogic, final IUILogicContext context, final Collaborator collaborator, String propertyId, boolean readonly) {
		ILogic<?> result = null;
		if (IS_ACTIVE_PROPERTY_NAME_ID.equals(propertyId)) {
			result = createBoolCell(parentLogic, !readonly, collaborator, 
					Collaborator::getIsActive, Collaborator::setIsActive);
		} else if (LOGIN_PROPERTY_ID.equals(propertyId)) {
			result = createTextCell(parentLogic, !readonly, collaborator, 
					Collaborator::getLogin, Collaborator::setLogin);
		} else if (FIRST_PROPERTY_NAME_ID.equals(propertyId)) {
			result = createTextCell(parentLogic, !readonly, collaborator, 
					Collaborator::getFirstName, Collaborator::setFirstName);
		} else if (LAST_PROPERTY_NAME_ID.equals(propertyId)) {
			result = createTextCell(parentLogic, !readonly, collaborator, 
					Collaborator::getLastName, Collaborator::setLastName);
		} else {
			throw new IllegalArgumentException(propertyId);
		}
		return result;
	}
	
	protected <T extends Collaborator> ILogic<?> createBoolCell(AbstractLogicImpl<?> parent,
			boolean editable, T collaborator, 
			Predicate<T> getter,  BiConsumer<T, Boolean> setter
			) {
		boolean value = getter.test(collaborator);
		
		if (!editable) {
			return new LabelLogicImpl(parent, value ? "X" : "");
		}
		
		return new AbstractSafeCheckBoxLogicImpl(parent, value) {
			@Override
			protected void unsafeOnValueChanged(Boolean newValue)
					throws ModelException {
				setter.accept(collaborator, newValue);
				modelMgr.updateCollaborator(collaborator);
			}
		};
	}
	
	protected <T extends Collaborator> ILogic<?> createTextCell(AbstractLogicImpl<?> parent,
			boolean editable, T collaborator, 
			Function<T, String> getter,  BiConsumer<T, String> setter
			) {
		String value = getter.apply(collaborator);
		
		if (!editable) {
			return new LabelLogicImpl(parent, value);
		}
		
		return new AbstractSafeTextFieldLogicImpl(parent, value, true) {
			@Override
			protected void unsafeOnValueChanged(String newValue)
					throws ModelException {
					setter.accept(collaborator, newValue);
					getModelMgr().updateCollaborator(collaborator);
			}
		};
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
