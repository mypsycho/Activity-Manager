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

import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IStandardButtonLogic;
import org.activitymgr.ui.web.logic.impl.internal.KeyBinding;

public abstract class AbstractSafeStandardButtonLogicImpl extends AbstractLogicImpl<IStandardButtonLogic.View> implements IStandardButtonLogic {

	public AbstractSafeStandardButtonLogicImpl(ILogic<?> parent, String label,
			String iconId, String shortcutKey) {
		super(parent);
		if (iconId != null) {
			getView().setIcon(iconId);
		} else {
			getView().setLabel(label);
		}
		String scHint = "";
		if (shortcutKey != null) {
			KeyBinding kb = new KeyBinding(shortcutKey);
			getView().setShortcut(kb.getKey(), kb.isCtrl(), kb.isShift(), kb.isAlt());
			scHint = " <em>" + shortcutKey + "</em>" ;
		}
		if (label != null) {
			getView().setDescription(label + scHint);
		}
	}

	@Override
	public final void onClick() {
		invoke(() -> unsafeOnClick());
	}

	protected abstract void unsafeOnClick() throws Exception;
	
}
