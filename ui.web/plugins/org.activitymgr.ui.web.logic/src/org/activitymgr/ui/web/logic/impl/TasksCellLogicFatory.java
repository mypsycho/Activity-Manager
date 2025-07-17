package org.activitymgr.ui.web.logic.impl;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.Align;
import org.activitymgr.ui.web.logic.IConstraintsValidator;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.IUILogicContext;
import org.activitymgr.ui.web.logic.impl.event.TaskUpdatedEvent;
import org.activitymgr.ui.web.logic.spi.ICellLogicFactory;
import org.activitymgr.ui.web.logic.spi.ITasksCellLogicFactory;

import com.google.inject.Inject;

public class TasksCellLogicFatory implements ITasksCellLogicFactory {

	@Inject
	private Set<IConstraintsValidator> tasksValidators;

	@Override
	public ILogic<?> createCellLogic(final AbstractLogicImpl<?> parentLogic, final IUILogicContext context, 
			final String filter, final TaskSums taskSums, final String propertyId, boolean readOnly) {

		final Task task = taskSums.getTask();
		boolean editable = !readOnly 
				&& tasksValidators.stream().allMatch(it -> it.canEditTask(propertyId, task));
			
		switch(propertyId) {
		case NAME_PROPERTY_ID: 
			return createTextCell(parentLogic, editable, task, Task::getName, Task::setName);
		case CODE_PROPERTY_ID:
			return createTextCell(parentLogic, editable, task, Task::getCode, Task::setCode);
		case BUDGET_PROPERTY_ID:
			return createDurationCell(parentLogic, editable, taskSums, TaskSums::getBudgetSum, Task::setBudget, propertyId);
		case INITIAL_PROPERTY_ID:
			return createDurationCell(parentLogic, editable, taskSums, TaskSums::getInitiallyConsumedSum, Task::setInitiallyConsumed, propertyId);
		case CONSUMMED_PROPERTY_ID:
			return ICellLogicFactory.createDurationCell(parentLogic, false, taskSums, it -> it.getContributionsSums().getConsumedSum(), null);
		case ETC_PROPERTY_ID:
			return createDurationCell(parentLogic, editable, taskSums, TaskSums::getTodoSum, Task::setTodo, propertyId);
		case CLOSED_PROPERTY_ID:
			return ICellLogicFactory.createBoolCell(parentLogic, editable, task, Task::isClosed, updater(Task::setClosed));
		case DELTA_PROPERTY_ID:
			return new LabelLogicImpl(parentLogic, getDelta(taskSums));
		case COMMENT_PROPERTY_ID:
			return new LabelLogicImpl(parentLogic, task.getComment());
		default:
			throw new IllegalArgumentException(propertyId);
		}
	}
	
	private static String getDelta(TaskSums it) {
		long projection = it.getInitiallyConsumedSum() + it.getContributionsSums().getConsumedSum() + it.getTodoSum();
		return StringHelper.hundredthToEntry(it.getBudgetSum() - projection);
	}
	
	protected ILogic<?> createTextCell(AbstractLogicImpl<?> parent,
			boolean editable, Task collaborator, 
			Function<Task, String> getter,  BiConsumer<Task, String> setter
			) {
		return ICellLogicFactory.createTextCell(parent, editable, collaborator, getter, updater(setter));
	}
	
	/** Field to handle days count: stored value is 100edth of day. */
	protected ILogic<?> createDurationCell(AbstractLogicImpl<?> parent,
			boolean editable, TaskSums element, 
			Function<TaskSums, Long> getter,  BiConsumer<Task, Long> setter, 
			String busProperty) {

		return ICellLogicFactory.createDurationCell(parent, editable && element.isLeaf(), 
				element, getter, (logic, context, newValue) -> {
			long oldValue = getter.apply(element);
			
			Task task = element.getTask();
			setter.accept(element.getTask(), newValue);
			logic.getModelMgr().updateTask(task);
			
			((ITextFieldLogic) this).getView().setValue(StringHelper.hundredthToEntry(newValue));
			logic.getEventBus().fire(new TaskUpdatedEvent(logic, task, busProperty, oldValue, newValue));
		});
	}
	
	private <T> Modifier<Task, T> updater(BiConsumer<Task, T> modifier) {
		return (logic, element, value) -> {
			modifier.accept(element, value);
			logic.getModelMgr().updateTask(element);
		};
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

//	private String highlightFilter(final String filter, String text) {
//		text = text.replaceAll("<", "&lt;");
//		if (filter == null || filter.length() == 0 
//				// Disable wild formatting
//				|| true) {
//			// Simple text
//			return text;
//		}
//		// For AbstractTaskChooserDialog,
//		// this behavior lead to an event issue.
//		// Part with format does not respond on click
//		
//		String filterLC = filter.toLowerCase();
//		String textToLC = text.toLowerCase();
//		StringWriter sw = new StringWriter();
//		
//		int lastIndexOf = 0;
//		int indexOf = 0;
//		while ((indexOf = textToLC.indexOf(filterLC, lastIndexOf)) >= 0) {
//			sw.append(text.substring(lastIndexOf, indexOf));
//			sw.append("<b><i>");
//			sw.append(text.substring(indexOf, indexOf + filter.length()));
//			sw.append("</i></b>");
//			lastIndexOf = indexOf + filter.length();
//		}
//		int textLength = text.length();
//		if (lastIndexOf < textLength) {
//			sw.append(text.substring(lastIndexOf, textLength));
//		}
//		
//		return sw.toString();
//	}
//	
//
//	/** Field to handle days count: stored value is 100edth of day. */
//	static class DayFieldLogic extends TextFieldLogic {
//		
//		public DayFieldLogic(AbstractLogicImpl<?> parent, long value, String property, Task task) {
//			super(parent, StringHelper.hundredthToEntry(value), property, task);
//			getView().setNumericFieldStyle();
//		}
//
//		@Override
//		protected void unsafeOnValueChanged(String newValue)
//				throws ModelException, IllegalAccessException, InvocationTargetException, StringFormatException, NumberFormatException, NoSuchMethodException {
//			BeanUtilsBean beanUtils = BeanUtilsBean2.getInstance();
//			long oldValue = Long.parseLong(beanUtils.getProperty(getTask(), TasksCellLogicFatory.ATTR_NAMES.get(getProperty())));
//			long newValueAsLong = StringHelper.entryToHundredth(newValue);
//			
//			super.unsafeOnValueChanged(String.valueOf(newValueAsLong));
//			
//			
//			getView().setValue(StringHelper.hundredthToEntry(newValueAsLong));
//			getEventBus().fire(new TaskUpdatedEvent(this, getTask(), getProperty(), oldValue, newValueAsLong));
//		}
//
//	};
//	
//
//	@Deprecated
//	static class TextFieldLogic extends AbstractSafeTextFieldLogicImpl {
//		
//		private String property;
//		private Task task;
//
//		public TextFieldLogic(AbstractLogicImpl<?> parent, String value, String property, Task task) {
//			super(parent, value, true);
//			this.property = property;
//			this.task = task;
//		}
//
//		@Override
//		protected void unsafeOnValueChanged(String newValue)
//				throws ModelException, IllegalAccessException, InvocationTargetException, StringFormatException, NumberFormatException, NoSuchMethodException {
//			BeanUtilsBean beanUtils = BeanUtilsBean2.getInstance();
//			beanUtils.setProperty(task, TasksCellLogicFatory.ATTR_NAMES.get(property), newValue);
//			getModelMgr().updateTask(task);
//		}
//		
//		public Task getTask() {
//			return task;
//		}
//		
//		public String getProperty() {
//			return property;
//		}
//		
//	}
}


