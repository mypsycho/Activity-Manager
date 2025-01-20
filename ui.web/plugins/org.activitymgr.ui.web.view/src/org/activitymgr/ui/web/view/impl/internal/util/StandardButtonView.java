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
package org.activitymgr.ui.web.view.impl.internal.util;

import org.activitymgr.ui.web.logic.IStandardButtonLogic;
import org.activitymgr.ui.web.logic.IStandardButtonLogic.View;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.AbstractTabPanel.ButtonBasedShortcutListener;
import org.activitymgr.ui.web.view.IResourceCache;

import com.google.inject.Inject;
import com.vaadin.ui.Button;
import com.vaadin.ui.HasComponents;

@SuppressWarnings("serial")
public class StandardButtonView extends Button implements View {

	@SuppressWarnings("unused")
	private IStandardButtonLogic logic;
	
	@Inject
	private IResourceCache resourceCache;
	
	private ButtonBasedShortcutListener shortcut;
	
	@Override
	public void setIcon(String iconId) {
		setIcon(resourceCache.getResource(iconId + ".gif"));
	}

	@Override
	public void setLabel(String label) {
		setCaption(label);
	}

	@Override
	public void setShortcut(final char key, final boolean ctrl, final boolean shift, final boolean alt) {
		shortcut = new ButtonBasedShortcutListener(StandardButtonView.this, key, ctrl, shift, alt);
	}

	public ButtonBasedShortcutListener getShortcut() {
		return shortcut;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (enabled != isEnabled()) {
			if (shortcut != null) {
				// Go up in component hierarchy until the tab
				HasComponents cursor = getParent();
				// Parent may be null if component has not yet been attached
				// In this case, non need to enable/disable the shortcut as
				// it will be correctly initialized when it will be attached
				if (cursor != null) {
					while (!(cursor instanceof AbstractTabPanel)) {
						cursor = cursor.getParent();
					}
					if (enabled) {
						((AbstractTabPanel<?>)cursor)
						.enableShortcut(shortcut);
					} else {
						((AbstractTabPanel<?>)cursor)
						.disableShortcut(shortcut);
					}
				}
			}
			super.setEnabled(enabled);
		}
	}

	@Override
	public void registerLogic(final IStandardButtonLogic logic) {
		this.logic = logic;
		setImmediate(true);
		addClickListener(event -> logic.onClick());
	}

}
