package org.activitymgr.ui.web.logic.spi;

import java.util.function.Function;

import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITabLogic;

public interface ITabFactory {
	
	int getTabOrderPriority();
	
	String getTabId();

	ITabLogic<?> create(ITabFolderLogic parent);

	default String getIcon() { return null; }
	
	
	class Impl implements ITabFactory {
		
		final int priority;
		final String tabId;
		final Function<ITabFolderLogic, ITabLogic<?>> creator;
		final String icon;
		
		public Impl(int priority, String tabId, String icon, Function<ITabFolderLogic, ITabLogic<?>> creator) {
			this.priority = priority;
			this.tabId = tabId;
			this.icon = icon;
			this.creator = creator;
		}
		

		@Override
		public int getTabOrderPriority() {
			return priority;
		}


		@Override
		public String getTabId() {
			return tabId;
		}

		@Override
		public ITabLogic<?> create(ITabFolderLogic parent) {
			return creator.apply(parent);
		}
		
		@Override
		public String getIcon() {
			return icon;
		}
	}
}
