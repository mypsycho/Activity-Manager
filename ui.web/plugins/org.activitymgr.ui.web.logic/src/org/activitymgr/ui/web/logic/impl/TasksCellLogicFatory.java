package org.activitymgr.ui.web.logic.impl;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.Align;
import org.activitymgr.ui.web.logic.IConstraintsValidator;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IUILogicContext;
import org.activitymgr.ui.web.logic.impl.event.TaskUpdatedEvent;
import org.activitymgr.ui.web.logic.spi.ITasksCellLogicFactory;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;

import com.google.inject.Inject;

public class TasksCellLogicFatory implements ITasksCellLogicFactory {

	static final Map<String, String> ATTR_NAMES = Map.of(
			NAME_PROPERTY_ID, "name",
			CODE_PROPERTY_ID, "code",
			BUDGET_PROPERTY_ID, "budget",
			INITIAL_PROPERTY_ID, "initiallyConsumed",
			ETC_PROPERTY_ID, "todo");

	
	@Inject
	private Set<IConstraintsValidator> tasksValidators;

	@Override
	public ILogic<?> createCellLogic(final AbstractLogicImpl<?> parentLogic, final IUILogicContext context, 
			final String filter, final TaskSums taskSums, final String propertyId, boolean readOnly) {
		ILogic<?> logic = null;
		final Task task = taskSums.getTask();
		boolean editable = !readOnly 
				&& tasksValidators.stream().allMatch(it -> it.canEditTask(propertyId, taskSums.getTask()));
				
		
		if (NAME_PROPERTY_ID.equals(propertyId)) {
			if (!editable) {
				logic = new LabelLogicImpl(parentLogic, highlightFilter(filter, task.getName()), true);
			} else {
				logic = new TextFieldLogic(parentLogic, task.getName(), propertyId, task);
			}
		} else if (CODE_PROPERTY_ID.equals(propertyId)) {
			if (!editable) {
				logic = new LabelLogicImpl(parentLogic, highlightFilter(filter, task.getCode()), true);
			} else {
				logic = new TextFieldLogic(parentLogic, task.getCode(), propertyId, task);
			}
		} else if (BUDGET_PROPERTY_ID.equals(propertyId)) {
			if (!editable || !taskSums.isLeaf()) {
				logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getBudgetSum()));
			} else {
				logic = new DayFieldLogic(parentLogic, taskSums.getBudgetSum(), propertyId, task);
			}
		} else if (INITIAL_PROPERTY_ID.equals(propertyId)) {
			if (!editable || !taskSums.isLeaf()) {
				logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getInitiallyConsumedSum()));
			} else {
				logic = new DayFieldLogic(parentLogic, taskSums.getInitiallyConsumedSum(), propertyId, task);
			}
		} else if (CONSUMMED_PROPERTY_ID.equals(propertyId)) {
			logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getContributionsSums().getConsumedSum()));
		} else if (ETC_PROPERTY_ID.equals(propertyId)) {
			if (!editable || !taskSums.isLeaf()) {
				logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getTodoSum()));
			} else {
				logic = new DayFieldLogic(parentLogic, taskSums.getTodoSum(), propertyId, task);
			}
		} else if (CLOSED_PROPERTY_ID.equals(propertyId)) {
			if (!editable) {
				logic = new LabelLogicImpl(parentLogic, taskSums.getTask().isClosed() ? "X" : "");
			} else {
				logic = new AbstractSafeCheckBoxLogicImpl(parentLogic, taskSums.getTask().isClosed()) {
					@Override
					protected void unsafeOnValueChanged(Boolean newValue) throws ModelException {
						if (newValue != taskSums.getTask().isClosed()) {
							taskSums.getTask().setClosed(newValue);
							getModelMgr().updateTask(taskSums.getTask());
						}
					}
				};
			}
		} else if (DELTA_PROPERTY_ID.equals(propertyId)) {
			logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getBudgetSum()-taskSums.getInitiallyConsumedSum()-taskSums.getContributionsSums().getConsumedSum()-taskSums.getTodoSum())); 
		} else if (COMMENT_PROPERTY_ID.equals(propertyId)) {
			logic = new LabelLogicImpl(parentLogic, task.getComment());
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
		case NAME_PROPERTY_ID:
			return 200;
		case CODE_PROPERTY_ID:
		case BUDGET_PROPERTY_ID:
		case INITIAL_PROPERTY_ID:
		case CONSUMMED_PROPERTY_ID:
		case ETC_PROPERTY_ID:
		case DELTA_PROPERTY_ID:
			return 60;
		case CLOSED_PROPERTY_ID:
			return 45;
		case COMMENT_PROPERTY_ID:
			return 300;
		default:
			return 60;
		}
		
	}

	@Override
	public Align getColumnAlign(String propertyId) {
		switch (propertyId) {
		// Number on right (Excel like)
		case BUDGET_PROPERTY_ID:
		case INITIAL_PROPERTY_ID:
		case CONSUMMED_PROPERTY_ID:
		case ETC_PROPERTY_ID:
		case DELTA_PROPERTY_ID:
			return Align.RIGHT;
		case CLOSED_PROPERTY_ID:
			return Align.CENTER;
			default:
			return Align.LEFT;
		}
	}

	private String highlightFilter(final String filter, String text) {
		text = text.replaceAll("<", "&lt;");
		if (filter == null || filter.length() == 0) {
			// Simple text
			return text;
		}

		String filterLC = filter.toLowerCase();
		String textToLC = text.toLowerCase();
		StringWriter sw = new StringWriter();
		
		int lastIndexOf = 0;
		int indexOf = 0;
		while ((indexOf = textToLC.indexOf(filterLC, lastIndexOf)) >= 0) {
			sw.append(text.substring(lastIndexOf, indexOf));
			sw.append("<b><i>");
			sw.append(text.substring(indexOf, indexOf + filter.length()));
			sw.append("</i></b>");
			lastIndexOf = indexOf + filter.length();
		}
		int textLength = text.length();
		if (lastIndexOf < textLength) {
			sw.append(text.substring(lastIndexOf, textLength));
		}
		
		return sw.toString();
	}
	

	/** Field to handle days count: stored value is 100edth of day. */
	static class DayFieldLogic extends TextFieldLogic {
		
		public DayFieldLogic(AbstractLogicImpl<?> parent, long value, String property, Task task) {
			super(parent, StringHelper.hundredthToEntry(value), property, task);
			getView().setNumericFieldStyle();
		}

		@Override
		protected void unsafeOnValueChanged(String newValue)
				throws ModelException, IllegalAccessException, InvocationTargetException, StringFormatException, NumberFormatException, NoSuchMethodException {
			BeanUtilsBean beanUtils = BeanUtilsBean2.getInstance();
			long oldValue = Long.parseLong(beanUtils.getProperty(getTask(), TasksCellLogicFatory.ATTR_NAMES.get(getProperty())));
			long newValueAsLong = StringHelper.entryToHundredth(newValue);
			super.unsafeOnValueChanged(String.valueOf(newValueAsLong));
			getView().setValue(StringHelper.hundredthToEntry(newValueAsLong));
			getEventBus().fire(new TaskUpdatedEvent(this, getTask(), getProperty(), oldValue, newValueAsLong));
		}

	};

	static class TextFieldLogic extends AbstractSafeTextFieldLogicImpl {
		
		private String property;
		private Task task;

		public TextFieldLogic(AbstractLogicImpl<?> parent, String value, String property, Task task) {
			super(parent, value, true);
			this.property = property;
			this.task = task;
		}

		@Override
		protected void unsafeOnValueChanged(String newValue)
				throws ModelException, IllegalAccessException, InvocationTargetException, StringFormatException, NumberFormatException, NoSuchMethodException {
			BeanUtilsBean beanUtils = BeanUtilsBean2.getInstance();
			beanUtils.setProperty(task, TasksCellLogicFatory.ATTR_NAMES.get(property), newValue);
			getModelMgr().updateTask(task);
		}
		
		public Task getTask() {
			return task;
		}
		
		public String getProperty() {
			return property;
		}
		
	}
}


