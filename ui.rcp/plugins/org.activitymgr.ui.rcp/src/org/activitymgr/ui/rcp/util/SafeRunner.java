/*
 * Copyright (c) 2004-2017, Jean-Francois Brazeau. All rights reserved.
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
package org.activitymgr.ui.rcp.util;


import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.dialogs.ErrorDialog;
import org.apache.log4j.Logger;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;

/**
 * Offre un contexte d'exécution sécurisé.
 *
 * <p>
 * Si une exception est levée dans le traitement, elle est attrapée et un popup
 * d'erreur est affiché.
 * </p>
 *
 * <p>
 * Exemple d'utilisation :<br>
 *
 * <pre>
 * // Initialisation du contexte d'exécution sécurisé
 * SafeRunner safeRunner = new SafeRunner() {
 * 	public Object runUnsafe() throws Exception {
 * 		// Declare unsafe code...
 * 		return result;
 * 	}
 * };
 * // Exécution du traitement
 * Object result = safeRunner.run(parent.getShell(), &quot;&quot;);
 * </pre>
 */
public abstract class SafeRunner implements Callable<Object> {

	/** Logger */
	private static final Logger LOG = Logger.getLogger(SafeRunner.class);

	@Deprecated // prefer lambda call
	protected SafeRunner() {}

	/**
	 * Lance le traitement dans le contexte sécurisé.
	 *
	 * @param parentShell
	 *            shell parent (peut être nul).
	 * @return le résultat du traitement.
	 */
	public Object run(Shell parentShell) {
		return run(parentShell, null);
	}

	/**
	 * Lance le traitement dans le contexte sécurisé.
	 *
	 * @param parentShell
	 *            shell parent (peut être nul).
	 * @param defaultValue
	 *            la valeur à retourner par défaut.
	 * @return le résultat du traitement.
	 */
	public Object run(final Shell parentShell, Object defaultValue) {
		return exec(parentShell, defaultValue, this);
	}

	/**
	 * Traitement potentiellement à risque.
	 *
	 * <p>
	 * Cette méthode doit être implémentée.
	 * </p>
	 *
	 * @return le résultat du traitement.
	 * @throws Exception
	 *             le traitement peut potentiellement lever n'importe quelle
	 *             exception.
	 */
	protected abstract Object runUnsafe() throws Exception;

	@Override
	public Object call() throws Exception {
		return runUnsafe();
	}

	/**
	 * Interface supporting Exception.
	 */
	public interface Exec {
	    void run() throws Exception;
	}

	public static <T> T exec(Shell parentShell, T defaultValue, final Callable<T> runner) {
		final AtomicReference<T> result = new AtomicReference<>(defaultValue);
		// Exécution du traitement
		BusyIndicator.showWhile(parentShell.getDisplay(), () -> {
			String message = null;
			Throwable detail = null;
			try {
				result.set(runner.call());
				return;
			} catch (ModelException e) {
				LOG.info("DB Exception", e); //$NON-NLS-1$
				message = Strings.getString("SafeRunner.errors.UNABLE_TO_COMPLETE_OPERATION", //$NON-NLS-1$
						e.getMessage());
				detail = e;
			} catch (Throwable t) {
				LOG.error("Unexpected error", t); //$NON-NLS-1$
				message = Strings.getString("SafeRunner.errors.UNEXPECTED_ERROR"); //$NON-NLS-1$
				detail = t;
			}
			new ErrorDialog(parentShell, message, detail).open();
		});
		return result.get();
	}


	public static void exec(Shell parentShell, final Exec runner) {
		exec(parentShell, null, ()-> {
			runner.run();
			return null;
		});
	}

}
