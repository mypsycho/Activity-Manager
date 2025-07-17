package org.activitymgr.ui.web.logic.spi;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.Align;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeCheckBoxLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTextFieldLogicImpl;
import org.activitymgr.ui.web.logic.impl.LabelLogicImpl;

public interface ICellLogicFactory {

	Collection<String> getPropertyIds();
	
	Integer getColumnWidth(String propertyId);

	Align getColumnAlign(String propertyId);

	/** */
	interface Modifier<T, U> {

	    /**
	     * Performs this operation on the given arguments.
	     *
	     * @param t the first input argument
	     * @param u the second input argument
	     */
	    void update(AbstractLogicImpl<?> logic, T t, U u) throws Exception;
	    
	}


	static <T> ILogic<?> createBoolCell(AbstractLogicImpl<?> parent,
			boolean editable, T element, 
			Predicate<T> getter,  Modifier<T, Boolean> setter
			) {
		boolean value = getter.test(element);
		
		if (!editable) {
			return new LabelLogicImpl(parent, value ? "X" : "");
		}
		
		return new AbstractSafeCheckBoxLogicImpl(parent, value) {
			@Override
			protected void unsafeOnValueChanged(Boolean newValue)
					throws Exception {
				setter.update(this, element, newValue);
			}
		};
	}
	
	static <T> ILogic<?> createTextCell(AbstractLogicImpl<?> parent,
			boolean editable, T element, 
			Function<T, String> getter, Modifier<T, String> updater
			) {
		String value = getter.apply(element);
//		if (value != null && !value.isBlank()) {
//			value = value.replaceAll("<", "&lt;");
//		}
		
		if (!editable) {
			return new LabelLogicImpl(parent, value);
		}
		
		return new AbstractSafeTextFieldLogicImpl(parent, value, true) {
			@Override
			protected void unsafeOnValueChanged(String newValue)
					throws Exception {
				updater.update(this, element, newValue);
			}
		};
	}
	
	static <T> ILogic<?> createDurationCell(AbstractLogicImpl<?> parent,
			boolean editable, T element, 
			Function<T, Long> getter, Modifier<T, Long> updater
			) {
		String value = StringHelper.hundredthToEntry(getter.apply(element));
		
		if (!editable) {
			return new LabelLogicImpl(parent, value);
		}
		ITextFieldLogic result = new AbstractSafeTextFieldLogicImpl(parent, value, true) {
			@Override
			protected void unsafeOnValueChanged(String newValue)
					throws Exception {
				updater.update(this, element, StringHelper.entryToHundredth(newValue));
			}
		};
		result.getView().setNumericFieldStyle();
		
		return result;
	}

}
