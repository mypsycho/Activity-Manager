package org.activitymgr.ui.web.logic.impl;

import java.util.Collection;

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
		ILogic<?> logic = null;
		if (IS_ACTIVE_PROPERTY_NAME_ID.equals(propertyId)) {
			if (readonly) {
				logic = new LabelLogicImpl(parentLogic, collaborator.getIsActive() ? "X" : "");
			} else {
				logic = new AbstractSafeCheckBoxLogicImpl(parentLogic, collaborator.getIsActive()) {
					@Override
					protected void unsafeOnValueChanged(Boolean newValue)
							throws ModelException {
						if (newValue != collaborator.getIsActive()) {
							collaborator.setIsActive(newValue);
							modelMgr.updateCollaborator(collaborator);
						}
					}
				};
			}
		} else if (LOGIN_PROPERTY_ID.equals(propertyId)) {
			if (readonly) {
				logic = new LabelLogicImpl(parentLogic, collaborator.getLogin());
			} else {
				logic = new AbstractSafeTextFieldLogicImpl(parentLogic, collaborator.getLogin(), true) {
					@Override
					protected void unsafeOnValueChanged(String newValue)
							throws ModelException {
						collaborator.setLogin(newValue);
						modelMgr.updateCollaborator(collaborator);
					}
				};
			}
		} else if (FIRST_PROPERTY_NAME_ID.equals(propertyId)) {
			if (readonly) {
				logic = new LabelLogicImpl(parentLogic, collaborator.getFirstName());
			} else {
				logic = new AbstractSafeTextFieldLogicImpl(parentLogic, collaborator.getFirstName(), true) {
					@Override
					protected void unsafeOnValueChanged(String newValue)
							throws ModelException {
						collaborator.setFirstName(newValue);
						modelMgr.updateCollaborator(collaborator);
					}
				};
			}
		} else if (LAST_PROPERTY_NAME_ID.equals(propertyId)) {
			if (readonly) {
				logic = new LabelLogicImpl(parentLogic, collaborator.getLastName());
			} else {
				logic = new AbstractSafeTextFieldLogicImpl(parentLogic, collaborator.getLastName(), true) {
					@Override
					protected void unsafeOnValueChanged(String newValue)
							throws ModelException {
						collaborator.setLastName(newValue);
						modelMgr.updateCollaborator(collaborator);
					}
				};
			}
		} else {
			throw new IllegalArgumentException(propertyId);
		}
		return logic;
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
