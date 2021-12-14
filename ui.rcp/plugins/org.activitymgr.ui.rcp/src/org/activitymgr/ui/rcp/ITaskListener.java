package org.activitymgr.ui.rcp;

import org.activitymgr.core.dto.Task;

/**
 * Interface utilisée pour permettre l'écoute de la suppression ou de
 * l'ajout de taches.
 */
public interface ITaskListener {

	/**
	 * Indique qu'une tache a été ajoutée au référentiel.
	 * 
	 * @param task
	 *            la tache ajoutée.
	 */
	public void taskAdded(Task task);

	/**
	 * Indique qu'une tache a été supprimée du référentiel.
	 * 
	 * @param task
	 *            la tache supprimée.
	 */
	public void taskRemoved(Task task);

	/**
	 * Indique qu'une tache a été modifiée duans le référentiel.
	 * 
	 * @param task
	 *            la tache modifiée.
	 */
	public void taskUpdated(Task task);

	/**
	 * Indique qu'une tache a été déplacée duans le référentiel.
	 * 
	 * @param oldTaskFullpath
	 *            ancien chemin de la tache.
	 * @param task
	 *            la tache déplacée.
	 */
	public void taskMoved(String oldTaskFullpath, Task task);
}