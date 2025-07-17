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
package org.activitymgr.core.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.ReportCfg;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.IntervalContributions;
import org.activitymgr.core.dto.misc.TaskSearchFilter;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.dto.report.Report;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.apache.poi.ss.usermodel.Workbook;
import org.xml.sax.SAXException;

/**
 * Model manager.
 */
public interface IModelMgr {

	char PATH_SEP = '/';
	
	String ETC_ATTRIBUTE = "etc";
	String INITIALLY_CONSUMED_ATTRIBUTE = "initiallyConsumed";
	String BUDGET_ATTRIBUTE = "budget";
	String PATH_ATTRIBUTE = "path";
	String CODE_ATTRIBUTE = "code";

	/**
	 * Change la tache d'une liste de contributions.
	 * 
	 * @param contributions
	 *            la liste de contributions.
	 * @param newContributionTask
	 *            la tache à affecter.
	 * @return la liste de contributions mise à jour.
	 * @throws ModelException
	 *             levé dans le cas où la tache cible ne peut être acdepter de
	 *             contribution.
	 * 
	 */
	Contribution[] changeContributionTask(Contribution[] contributions,
			Task newContributionTask) throws ModelException;

	/**
	 * Vérifie si la tache spécifiée peut accueillir des sous-taches.
	 * 
	 * @param task
	 *            la tache à controler.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	void checkAcceptsSubtasks(Task task) throws ModelException;

	/**
	 * Crée un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à créer.
	 * @return le collaborateur après création.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	Collaborator createCollaborator(Collaborator collaborator)
			throws ModelException;

	/**
	 * Crée une contribution.
	 * 
	 * @param contribution
	 *            la contribution à créer.
	 * @param updateEstimatedTimeToComlete
	 *            booléen indiquant si le reste à faire doit être décrémenté.
	 * @return la contribution après création.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de contribution.
	 */
	Contribution createContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws ModelException;

	/**
	 * Crée une durée.
	 * 
	 * @param duration
	 *            la durée à créer.
	 * @return la durée créée.
	 * @throws ModelException
	 *             levé dans la cas ou la durée existe déjà.
	 */
	Duration createDuration(Duration duration) throws ModelException;

	/**
	 * Crée un nouveau collaborateur en générant automatiquement ses attributs.
	 * 
	 * @return le nouveau collaborateur.
	 */
	Collaborator createNewCollaborator();

	/**
	 * Crée une nouvelle tache en générant un nom et un code.
	 * 
	 * <p>
	 * Avant création, les caractéristiques de la tache de destination sont
	 * controllées pour voir si elle peut accueillir des sous-taches.
	 * </p>
	 * 
	 * <p>
	 * Cette méthode est synchronisé en raison de la génération du numéro de la
	 * tache qui est déplacée à un autre chemin.
	 * </p>
	 * 
	 * @param parentTask
	 *            la tache parent de destination.
	 * @return la tache créée.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 * @see IModelMgr#checkAcceptsSubtasks(Task)
	 */
	Task createNewTask(Task parentTask) throws ModelException;

	/**
	 * Crée les tables du modèle de données.
	 * 
	 */
	void createTables();

	/**
	 * Crée une nouvelle tache.
	 * 
	 * <p>
	 * Avant création, les caractéristiques de la tache de destination sont
	 * controllées pour voir si elle peut accueillir des sous-taches.
	 * </p>
	 * 
	 * @param parentTask
	 *            la tache parent de destination.
	 * @param task
	 *            la tache à créer.
	 * @return la tache créée.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 * @see ImodelMgr#checkAcceptsSubtasks(Task)
	 */
	Task createTask(Task parentTask, Task task) throws ModelException;

	/**
	 * Vérifie si la durée existe en base.
	 * 
	 * @param duration
	 *            la durée à vérifier.
	 * @return un booléen indiquant si la durée existe.
	 */
	boolean durationExists(Duration duration);

	/**
	 * Exporte le contenu de la base dans un fichier XML.
	 * 
	 * @param out
	 *            le flux dans lequel est généré le flux XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 */
	void exportToXML(OutputStream out) throws IOException;

	/**
	 * Tells whether the given task is leaf or not.
	 * 
	 * @param taskId
	 *            the task identifier.
	 * @return <code>true</code> if the task is leaf.
	 * @throws thrown
	 *             if a DAO exception occurs.
	 */
	boolean isLeaf(long taskId);

	/**
	 * @param parentTaskId
	 *            the task identifier.
	 * @return the sub tasks count.
	 * @throws thrown
	 *             if a DAO exception occurs.
	 */
	int getSubTasksCount(long parentTaskId);

	/**
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @return la liste des collaborateurs actifs.
	 */
	Collaborator[] getActiveCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort);

	/**
	 * @return la liste des durées actives.
	 */
	Duration[] getActiveDurations();

	/**
	 * @param collaboratorId
	 *            l'identifiant du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant est spécifié.
	 */
	Collaborator getCollaborator(long collaboratorId);

	/**
	 * @param login
	 *            l'identifiant de connexion du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant de connexion est spécifié.
	 */
	Collaborator getCollaborator(String login);

	/**
	 * @return la liste des collaborateurs.
	 */
	Collaborator[] getCollaborators();

	/**
	 * @param orderByClauseFieldIndex
	 *            index de l'attribut utilisé pour le tri.
	 * @param ascendantSort
	 *            booléen indiquant si le tri doit être ascendant.
	 * @return la liste des collaborateurs.
	 */
	Collaborator[] getCollaborators(int orderByClauseFieldIndex,
			boolean ascendantSort);

	/**
	 * @param contributor
	 *            le contributeur.
	 * @param fromDate
	 *            date de début.
	 * @param toDate
	 *            date de fin.
	 * @return la liste de taches associées au collaborateur entre les 2 dates
	 *         spécifiées.
	 */
	Task[] getContributedTasks(Collaborator contributor, Calendar fromDate,
			Calendar toDate);

	/**
	 * @param contributor
	 *            le collaborateur associé aux contributions.
	 * @param task
	 *            la tache associée aux contributions.
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la liste des contributions associées aux paramétres spécifiés.
	 * @throws ModelException
	 */
	Contribution[] getContributions(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws ModelException;

	/**
	 * Calcule le nombre des contributions associée aux paramétres spécifiés.
	 * 
	 * @param task
	 *            la tâche associée aux contributions (facultative).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return le nombre de contributions.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données en entrée avec le
	 *             modèle.
	 * 
	 * @see CoreDAOImpl.tools.activitymgr.core.DbMgrImpl#getContributionsNb(DbTransaction,
	 *      Task, Collaborator, Integer, Integer, Integer)
	 */
	int getContributionsCount(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws ModelException;

	/**
	 * Calcule le total des contributions associée aux paramétres spécifiés.
	 * 
	 * @param task
	 *            the task of the contributions to select (or parent task).
	 * @param contributor
	 *            le collaborateur associé aux contributions (facultatif).
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la seomme des contributions.
	 * @throws ModelException
	 */
	long getContributionsSum(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws ModelException;

	/**
	 * @param task
	 *            la tache associée aux contributions.
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return the contributors list corresponding to the given date interval.
	 * @throws ModelException
	 *             levé si l'interval est incohérent.
	 */
	Collaborator[] getContributors(Task task, Calendar fromDate, Calendar toDate)
			throws ModelException;

	/**
	 * @param durationId
	 *            identifiant de la durée.
	 * @return la durée dont l'identifiant est spécifiée.
	 */
	Duration getDuration(long durationId);

	/**
	 * @return la liste des durées actives.
	 */
	Duration[] getDurations();

	/**
	 * Retourne la liste des contributions associées à une tache, un
	 * collaborateur et à un interval de temps donnés.
	 * 
	 * <p>
	 * Un tableau dont la taille est égale au nombre de jours séparant les deux
	 * dates spécifiées est retourné.
	 * 
	 * @param contributor
	 *            le collaborateur associé aux contributions.
	 * @param task
	 *            la tache associée aux contributions (optionel).
	 * @param fromDate
	 *            la date de départ.
	 * @param toDate
	 *            la date de fin.
	 * @return la liste des contributions.
	 * @throws ModelException
	 *             levé dans le cas ou la date de fin spécifiée est antérieure à
	 *             la date de début spécifiée.
	 */
	IntervalContributions getIntervalContributions(Collaborator contributor,
			Task task, Calendar fromDate, Calendar toDate)
			throws ModelException;

	/**
	 * @param task
	 *            la tache dont on veut connaitre la tache parent.
	 * @return la tache parent d'une tache spécifiée.
	 */
	Task getParentTask(Task task);

	/**
	 * @return the root tasks count.
	 * @throws thrown
	 *             if a database exception occurs.
	 */
	int getRootTasksCount();

	/**
	 * @param parentTaskId
	 *            l'identifiant de la tache dont on veut connaître les
	 *            sous-taches.
	 * @return la liste des taches associées à un chemin donné.
	 */
	Task[] getSubTasks(Long parentTaskId);

	/**
	 * Returns the sub tasks of a given task filtered by a given string.
	 * <p>
	 * Note : the filter is not only applied on the sub tasks, it is also
	 * applied on any nested task in the tree. This helps to implement dialogs
	 * in which you can type a text and filter the resulting tree.
	 * </p>
	 * 
	 * @param parentTaskId
	 *            l'identifiant de la tache dont on veut connaître les
	 *            sous-taches.
	 * @param filter
	 *            a string that filters sub tasks.
	 * @return the sub task list.
	 */
	Task[] getSubTasks(Long parentTaskId, String filter);

	/**
	 * Returns the first task matching the given filter.
	 * @param filter
	 *            a string that filters tasks.
	 * @return the matching task or null if no task matches.
	 */
	Task getFirstTaskMatching(String filter);

	/**
	 * Fetches 1 task using its ID.
	 * 
	 * @param taskId
	 *            l'identifiant de la tache recherchée.
	 * @return la tache dont l'identifiant est spécifié.
	 */
	Task getTask(long taskId);

	
	/**
	 * Fetches several tasks using its IDs.
	 * <p>
	 * If some tasks cannot be found, they are ignored.
	 * </p>
	 * 
	 * @param tasksIds
	 *            ids of tasks
	 * @return found tasks
	 */
	Task[] getTasks(long[] tasksIds);

		
	/**
	 * @param taskPath
	 *            le chemin de la tache recherchée.
	 * @param taskCode
	 *            le code de la tache recherchée.
	 * @return la tache dont le code et la tache parent sont spécifiés.
	 */
	Task getTask(String taskPath, String taskCode);

	/**
	 * Retourne la tache associée à un chemin construit à partir de codes de
	 * taches.
	 * 
	 * @param codePath
	 *            le chemin à base de code.
	 * @return la tache trouvée.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin de tache est inconnu.
	 */
	Task getTaskByCodePath(final String codePath) throws ModelException;

	/**
	 * Retourne les taches d'un chemin construit à partir de codes de
	 * taches.
	 * 
	 * @param codePath
	 *            le chemin à base de code.
	 * @return la tache trouvée.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin de tache est inconnu.
	 */
	Task[] getTasksInPath(final String codePath) throws ModelException;
	
	/**
	 * Construit le chemin de la tâche à partir des codes de tache.
	 * 
	 * @param task
	 *            la tache dont on veut connaître le chemin.
	 * @return le chemin.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 */
	String getTaskCodePath(Task task) throws ModelException;

	/**
	 * Retourn la liste des taches correspondant au filtre de recherche
	 * spécifié.
	 * 
	 * @param filter
	 *            le filtre de recherche.
	 * @return la liste des taches correspondant au filtre de recherche
	 *         spécifié.
	 */
	Task[] getTasks(TaskSearchFilter filter);

	/**
	 * @param taskId
	 *            l'identifiant de la tâche pour laquelle on souhaite connaître les totaux.
	 * @param fromDate
	 *            date de départ à prendre en compte pour le calcul.
	 * @param toDate
	 *            date de fin à prendre en compte pour le calcul.
	 * @return les totaux associés à une tache (consommé, etc.).
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache en
	 *             base ne sont pas ceux de la tache spécifiée.
	 */
	TaskSums getTaskSums(long taskId, Calendar fromDate, Calendar toDate)
			throws ModelException;

	/**
	 * @param parentTask
	 *            the parent task
	 * @param fromDate
	 *            start of the date interval to consider
	 * @param toDate
	 *            end of the date interval to consider
	 * @return the sub tasks sums (consummed, ...)
;	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache en
	 *             base ne sont pas ceux de la tache spécifiée.
	 */
	List<TaskSums> getSubTasksSums(Task parentTask, Calendar fromDate, Calendar toDate)
			throws ModelException;

	/**
	 * Importe le contenu d'un fichier XML.
	 * 
	 * @param in
	 *            le flux depuis lequel est lu le flux XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de la lecture sur le flux
	 *             d'entrée
	 * @throws ParserConfigurationException
	 *             levé en cas de mauvaise configuration du parser XML.
	 * @throws SAXException
	 *             levé en cas d'erreur de mauvais format du fichier XML.
	 * @throws ModelException
	 *             levé en cas d'incohérence des données lors de l'import
	 */
	void importFromXML(InputStream in) throws IOException,
			ParserConfigurationException, SAXException, ModelException;
	
	/**
	 * Imports several tasks under a given parent task.
	 * 
	 * <p>Only the first sheet of the workbook is processed.</p>
	 * <p>The sheet is expected to contain the following columns :</p>
	 * <ul>
	 * <li>path (2)</li>
	 * <li>code (1)</li>
	 * <li>name (1)</li>
	 * <li>budget (2)</li>
	 * <li>initiallyConsumed (2)</li>
	 * <li>todo (2)</li>
	 * <li>comment (2)</li>
	 * </ul>
	 * 
	 * <p>(1) : required, (2) : optionnal</p>
	 * 
	 * <p>If path is present, it will be used relatively to the given parent task.</p>
	 * 
	 * @param parentTaskId the parent task identifier.
	 * @param xls the stream containing the EXCEL file.
	 * @throws IOException if an I/O error occurs. 
	 * @throws ModelException if a model violation occurs.
	 */
	void importFromExcel(Long parentTaskId, InputStream xls) throws IOException, ModelException;

	/**
	 * Exports sub tasks to EXCEL.
	 * @param parentTaskId the parent task identifier.
	 * @return the excel workbook.
	 * @throws IOException if an I/O error occurs. 
	 * @throws ModelException if a model violation occurs.
	 */
	byte[] exportToExcel(Long parentTaskId) throws IOException, ModelException;

	/**
	 * Déplace la tache d'un cran vers le bas.
	 * <p>
	 * Le chemin de la tache et son numéro ne doivent pas avoir changés pour
	 * pouvoir invoquer cette méthode (la modification des attributs n'est
	 * autorisée que pour les champs autres que le chemin et le numéro de la
	 * tache.
	 * </p>
	 * 
	 * @param task
	 *            la tache à déplacer vers le bas.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 */
	void moveDownTask(Task task) throws ModelException;

	/**
	 * Déplace la tache vers un autre endroit dans la hiérarchie des taches.
	 * 
	 * <p>
	 * Le chemin de la tache et son numéro ne doivent pas avoir changés pour
	 * pouvoir invoquer cette méthode (la modification des attributs n'est
	 * autorisée que pour les champs autres que le chemin et le numéro de la
	 * tache.
	 * </p>
	 * 
	 * <p>
	 * Cette méthode est synchronisé en raison de la génération du numéro de la
	 * tache qui est déplacée à un autre chemin.
	 * </p>
	 * 
	 * @param task
	 *            la tache à déplacer.
	 * @param destParentTask
	 *            tache parent de destination.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 */
	void moveTask(Task task, Task destParentTask) throws ModelException;;

	/**
	 * Déplace une tache de plus d'un cran (au contraire des méthodes
	 * <code>moveUp</code> et <code>moveDown</code>.
	 * 
	 * @param task
	 *            la tache à déplacer.
	 * @param newTaskNumber
	 *            le nouveau numéro de la tâche.
	 * @throws ModelException
	 *             levé en cas de violation du modèle.
	 */
	void moveTaskUpOrDown(Task task, int newTaskNumber) throws ModelException;

	/**
	 * Déplace la tache d'un cran vers le haut.
	 * <p>
	 * Le chemin de la tache et son numéro ne doivent pas avoir changés pour
	 * pouvoir invoquer cette méthode (la modification des attributs n'est
	 * autorisée que pour les champs autres que le chemin et le numéro de la
	 * tache.
	 * </p>
	 * 
	 * @param task
	 *            la tache à déplacer vers le haut.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 */
	void moveUpTask(Task task) throws ModelException;

	/**
	 * Supprime un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à supprimer.
	 * @throws ModelException
	 *             levé dans le cas ou le collaborateur est associé à des
	 *             contributions en base.
	 */
	void removeCollaborator(Collaborator collaborator) throws ModelException;

	/**
	 * Supprime une contribution.
	 * 
	 * @param contribution
	 *            la contribution à supprimer.
	 * @param updateEstimatedTimeToComlete
	 *            booléen indiquant si le reste à faire doit être incrémenté.
	 * @throws ModelException
	 *             levé dans le cas ou la donnée a changé en base de données.
	 */
	void removeContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws ModelException;

	/**
	 * Supprime des contributions.
	 * 
	 * @param contributions
	 *            les contributions à supprimer.
	 */
	void removeContributions(Contribution[] contributions);

	/**
	 * Supprime une durée du référentiel de durées.
	 * 
	 * @param duration
	 *            la durée à supprimer.
	 * @throws ModelException
	 *             levé dans le cas ou la durée n'existe pas en base.
	 */
	void removeDuration(Duration duration) throws ModelException;

	/**
	 * Supprime une tache.
	 * 
	 * <p>
	 * Cette méthode est synchronisé en raison de la modification potentielle du
	 * numéro de certaines taches.
	 * </p>
	 * 
	 * @param task
	 *            la tache à supprimer.
	 * @throws ModelException
	 *             levé en cas de violation d'une contrainte d'intégrité du
	 *             modèle.
	 */
	void removeTask(Task task) throws ModelException;

	/**
	 * Vérifie si les tables existent dans le modèle.
	 * 
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 */
	boolean tablesExist();

	/**
	 * Modifie les attributs d'un collaborateur.
	 * 
	 * @param collaborator
	 *            le collaborateur à modifier.
	 * @return le collaborateur modifié.
	 * @throws ModelException
	 *             levé en cas de non unicité du login.
	 */
	Collaborator updateCollaborator(Collaborator collaborator)
			throws ModelException;

	/**
	 * Modifie les attributs d'une contribution.
	 * 
	 * @param contribution
	 *            la contribution à modifier.
	 * @param updateEstimatedTimeToComlete
	 *            booléen indiquant si le reste à faire doit être décrémenté.
	 * @return la contribution modifiée.
	 * @throws ModelException
	 *             levé dans le cas ou la donnée a changé en base de données.
	 */
	Contribution updateContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws ModelException;

	/**
	 * Met à jour une durée.
	 * 
	 * @param duration
	 *            la durée à mettre à jour.
	 * @return la durée mise à jour.
	 */
	Duration updateDuration(Duration duration);

	/**
	 * Modifie une durée.
	 * <p>
	 * Pour pouvoir être modifiée, la durée ne doit pas être utilisée.
	 * </p>
	 * 
	 * @param duration
	 *            la durée à modifier.
	 * @param newDuration
	 *            la nouvelle valeur de la durée.
	 * @return la durée modifiée.
	 * @throws ModelException
	 *             levé dans le cas ou la durée à changer est utilisée ou dans
	 *             le cas ou la nouvelle valeur pour la durée existe déja dans
	 *             le référentiel.
	 */
	Duration updateDuration(Duration duration, Duration newDuration)
			throws ModelException;

	/**
	 * Met à jour les attributs d'une tache en base.
	 * <p>
	 * Le chemin de la tache et son numéro ne doivent pas avoir changés pour
	 * pouvoir invoquer cette méthode (la modification des attributs n'est
	 * autorisée que pour les champs autres que le chemin et le numéro de la
	 * tache.
	 * </p>
	 * 
	 * @param task
	 *            la tache à mettre à jour.
	 * @return la tache mise à jour.
	 * @throws ModelException
	 *             levé dans le cas ou le chemin ou le numéro de la tache ont
	 *             changé.
	 */
	Task updateTask(Task task) throws ModelException;

	/**
	 * Return the contribution years list.
	 * 
	 * @return the contribution years list.
	 */
	Collection<Integer> getContributionYears();

	/**
	 * Builds a report.
	 * 
	 * @param start
	 *            the start date to consider [Optional]. If omitted, the first
	 *            contribution in the database will be considered.
	 * @param intervalType
	 *            the interval type (days, weeks, months, years) [Required].
	 * @param intervalCount
	 *            the interval count to cover [Optional]. If omitted, the last
	 *            contribution in the database will be considered.
	 * @param rootTaskId
	 *            the root task identifier [Optional]. If omitted, the whole
	 *            database will be considered.
	 * @param taskDepth
	 *            the task depth to consider [Required]. If this is equal to 0,
	 *            no task will appear in the report (all tasks contributions
	 *            will be cumulated).
	 * @param onlyKeepTasksWithContributions
	 *            <code>true</code> if the report must only keep tasks with
	 *            contributions. Has no effect if <code>taskDepth == 0</code> or
	 *            if <code>contributorCentricMode == true</code>.
	 * @param byContributor
	 *            <code>true</code> if the report must decline contributions by
	 *            contributors.
	 * @param contributorCentricMode
	 *            <code>true</code> if the report must be ordered by
	 *            contributors and then by tasks or the inverse. If
	 *            <code>(byContributor == false) || (taskDepth <= 0)</code>, it
	 *            has no effect.
	 * @return the report.
	 * @throws ModelException
	 *             if start date is not specified and no contribution exist in
	 *             the database.
	 */
	Report buildReport(Calendar start, ReportIntervalType intervalType,
			Integer intervalCount, Long rootTaskId, int taskDepth,
			boolean onlyKeepTasksWithContributions, boolean byContributor,
			boolean contributorCentricMode, long[] contributorIds)
			throws ModelException;
	
	/**
	 * Builds a report and converts it in Excel format.
	 * 
	 * <p>
	 * It is possible to select the columns that will be exported. A column
	 * identifier is built by concatenating the object name (<code>task</code>
	 * or <code>collaborator</code>) and the field name. Possible values are the
	 * following :
	 * <table border="1" cellspacing="0" cellpadding="0">
	 * <tr>
	 * <td><b>Identifier</b></td>
	 * <td><b>Description</b></td>
	 * <td><b>Only available in task oriented reports (*)</b></td>
	 * </tr>
	 * <tr>
	 * <td><code>task.path</code></code></td>
	 * <td>the code based task path.</td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * <tr>
	 * <td><code>task.code</code></td>
	 * <td>the task code</td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * <tr>
	 * <td><code>task.name</code></td>
	 * <td>the task name</td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * <tr>
	 * <td><code>task.comment</code></td>
	 * <td>the comment of the task</td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * <tr>
	 * <td><code>task.budget</code></td>
	 * <td>the task budget</td>
	 * <td>X</td>
	 * </tr>
	 * <tr>
	 * <td><code>task.initiallyConsumed</code></td>
	 * <td>the initially consumed sum of the task</td>
	 * <td>X</td>
	 * </tr>
	 * <tr>
	 * <td><code>task.etc</code></td>
	 * <td>the estimated time to complete of the task</td>
	 * <td>X</td>
	 * </tr>
	 * <tr>
	 * <td><code>collaborator.login</code></td>
	 * <td>the collaborator login</td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * <tr>
	 * <td><code>collaborator.firstName</code></td>
	 * <td>the collaborator first name</td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * <tr>
	 * <td><code>collaborator.lastName</code></td>
	 * <td>the collaborator last name</td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * <tr>
	 * <td><code>collaborator.isActive</code></td>
	 * <td>a boolean telling whether the collaborator is active or not</td>
	 * <td>&nbsp;</td>
	 * </tr>
	 * </table>
	 * </p>
	 * <p>
	 * (*) These fields cannnot be aggregated if <code>orderByContributor</code>
	 * is set to <cdoe>true</code>. A given task may be contributed by several
	 * contributors. That is the reason why them are allowed only if the report
	 * is ordered by task.
	 * </p>
	 * 
	 * @param start
	 *            the start date to consider [Optional]. If omitted, the first
	 *            contribution in the database will be considered.
	 * @param intervalType
	 *            the interval type (days, weeks, months, years) [Required].
	 * @param intervalCount
	 *            the interval count to cover [Optional]. If omitted, the last
	 *            contribution in the database will be considered.
	 * @param rootTaskId
	 *            the root task identifier [Optional]. If omitted, the whole
	 *            database will be considered.
	 * @param taskDepth
	 *            the task depth to consider [Required]. If this is equal to 0,
	 *            no task will appear in the report (all tasks contributions
	 *            will be cumulated).
	 * @param onlyKeepTasksWithContributions
	 *            <code>true</code> if the report must only keep tasks with
	 *            contributions. Has no effect if <code>taskDepth == 0</code> or
	 *            if <code>contributorCentricMode == true</code>.
	 * @param byContributor
	 *            <code>true</code> if the report must decline contributions by
	 *            contributors.
	 * @param contributorCentricMode
	 *            <code>true</code> if the report must be ordered by
	 *            contributors and then by tasks or the inverse. If
	 *            <code>(byContributor == false) || (taskDepth <= 0)</code>, it
	 *            has no effect.
	 * @param contributorIds
	 *            contributor identifiers (optional).
	 * @param columnIds
	 *            the column identifiers to user.
	 * @param orderContributorsBy
	 *            fields to use to order contributors (ignored if
	 *            <code>byContributor</code> is <code>false</code>).
	 * @param includeTotals
	 *            <code>true</code> to include a column with row sums and a row
	 *            with columns sums.
	 * @param dryRun
	 *            <code>true</code> to perform a dry run test (report is not
	 *            generated in such case).
	 * @return the report.
	 * @throws ModelException
	 *             if start date is not specified and no contribution exist in
	 *             the database.
	 */
	Workbook buildReport(Calendar start, ReportIntervalType intervalType,
			Integer intervalCount, Long rootTaskId, int taskDepth,
			boolean onlyKeepTasksWithContributions, boolean byContributor,
			boolean contributorCentricMode, long[] contributorIds,
			String[] columnIds, boolean includeTotals, boolean dryRun)
			throws ModelException;

	/**
	 * Builds a report and converts it in Excel format.
	 * 
	 * </p>
	 * 
	 * @param start
	 *            the start date to consider [Optional]. If omitted, the first
	 *            contribution in the database will be considered.
	 * @param intervalType
	 *            the interval type (days, weeks, months, years) [Required].
	 * @param intervalCount
	 *            the interval count to cover [Optional]. If omitted, the last
	 *            contribution in the database will be considered.
	 * @param rootTaskId
	 *            the root task identifier [Optional]. If omitted, the whole
	 *            database will be considered.
	 * @param taskDepth
	 *            the task depth to consider [Required]. If this is equal to 0,
	 *            no task will appear in the report (all tasks contributions
	 *            will be cumulated).
	 * @param onlyKeepTasksWithContributions
	 *            <code>true</code> if the report must only keep tasks with
	 *            contributions. Has no effect if <code>taskDepth == 0</code> or
	 *            if <code>contributorCentricMode == true</code>.
	 * @param byContributor
	 *            <code>true</code> if the report must decline contributions by
	 *            contributors.
	 * @param contributorCentricMode
	 *            <code>true</code> if the report must be ordered by
	 *            contributors and then by tasks or the inverse. If
	 *            <code>(byContributor == false) || (taskDepth <= 0)</code>, it
	 *            has no effect.
	 * @param contributorIds
	 *            contributor identifiers (optional).
	 * @param columns
	 *            the column computers to use.
	 * @param orderContributorsBy
	 *            fields to use to order contributors (ignored if
	 *            <code>byContributor</code> is <code>false</code>).
	 * @param includeTotals
	 *            <code>true</code> to include a column with row sums and a row
	 *            with columns sums.
	 * @param dryRun
	 *            <code>true</code> to perform a dry run test (report is not
	 *            generated in such case).
	 * 
	 * @return the report.
	 * @throws ModelException
	 *             if start date is not specified and no contribution exist in
	 *             the database.
	 */
	Workbook buildReport(Calendar start, ReportIntervalType intervalType,
			Integer intervalCount, Long rootTaskId, int taskDepth,
			boolean onlyKeepTasksWithContributions, boolean byContributor,
			boolean contributorCentricMode, long[] contributorIds,
			IReportColumnComputer[] columns, String[] orderContributorsBy,
			boolean includeTotals, boolean dryRun)
			throws ModelException;


	/**
	 * Returns the max task tree depth under a given task.
	 * 
	 * @param rootTaskId the root task to consider.
	 * @return the max task tree depth.
	 * @throws DAOException
	 *             thrown if a technical error occurs.
	 */
	int getMaxTaskDepthUnder(Long rootTaskId) throws DAOException;

	/**
	 * Returns the contributions interval.
	 * 
	 * @param rootTask
	 *            the optional root task identifier to filter the contributions.
	 * @return the contributions interval.
	 */
	Calendar[] getContributionsInterval(Long rootTaskId);

	/**
	 * Returns the report configurations in the given category for the given
	 * owner.
	 * <p>
	 * The owner identifier can be null. In that case, only common
	 * configurations are returned.
	 * </p>
	 * 
	 * @param category
	 *            the category.
	 * @param ownerId
	 *            the owner identifier.
	 * @return the configurations.
	 * @throws ModelException
	 *             if a model violation is detected.
	 */
	ReportCfg[] getReportCfgs(String category, Long ownerId)
			throws ModelException;

	/**
	 * Loads a report configuration.
	 * 
	 * @param id
	 *            the report configuration identifier.
	 * @return the report configuration.
	 */
	ReportCfg getReportCfg(long id);

	/**
	 * Creates a new report configuration.
	 * 
	 * @param reportCfg
	 *            the report configuration to create.
	 * @return the new report configuration.
	 * @throws ModelException
	 *             if a model violation is detected.
	 */
	ReportCfg createReportCfg(ReportCfg reportCfg) throws ModelException;

	/**
	 * Removes a report configuration.
	 * 
	 * @param id
	 *            the report configuration identifier to remove.
	 */
	void removeReportCfg(long id);

	/**
	 * Updates a report configuration.
	 * 
	 * @param reportCfg
	 *            the report configuration to update.
	 */
	void updateReportCfg(ReportCfg reportCfg);

}
