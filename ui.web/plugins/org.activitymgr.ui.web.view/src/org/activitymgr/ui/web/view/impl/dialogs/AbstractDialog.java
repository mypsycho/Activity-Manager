package org.activitymgr.ui.web.view.impl.dialogs;

import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class AbstractDialog extends Window {

	@Inject
	private IResourceCache resourceCache;

	public AbstractDialog(String title) {
		super(title);
		// Register escape key binding
		addCloseShortcut(ShortcutListener.KeyCode.ESCAPE);
		// Not resizeable
		//setResizable(false);
		// Trigger a focus as soon as it is possible (the dialog mus be attached
		// first)
		addAttachListener(event -> focus());
	}

	@Override
	public void setClosable(boolean closable) {
		// Unregister escape key binding
		removeCloseShortcut(ShortcutListener.KeyCode.ESCAPE);
		super.setClosable(closable);
	}

	protected IResourceCache getResourceCache() {
		return resourceCache;
	}


}
