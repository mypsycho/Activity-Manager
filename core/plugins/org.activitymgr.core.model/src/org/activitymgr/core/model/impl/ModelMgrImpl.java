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
package org.activitymgr.core.model.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.ICollaboratorDAO;
import org.activitymgr.core.dao.IContributionDAO;
import org.activitymgr.core.dao.ICoreDAO;
import org.activitymgr.core.dao.IDurationDAO;
import org.activitymgr.core.dao.IReportCfgDAO;
import org.activitymgr.core.dao.IReportDAO;
import org.activitymgr.core.dao.ITaskDAO;
import org.activitymgr.core.dao.TaskDAOCache;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.ReportCfg;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.IntervalContributions;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.core.dto.misc.TaskContributionsSums;
import org.activitymgr.core.dto.misc.TaskSearchFilter;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.dto.report.Report;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.dto.report.ReportItem;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.IReportColumnComputer;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.model.XLSModelException;
import org.activitymgr.core.model.impl.XlsImportHelper.XLSCell;
import org.activitymgr.core.model.impl.XmlHelper.ModelMgrDelegate;
import org.activitymgr.core.model.impl.report.ReflectiveReportColumnComputer;
import org.activitymgr.core.model.impl.report.TaskPathReportColumnComputer;
import org.activitymgr.core.model.util.Tasks;
import org.activitymgr.core.orm.query.AscendantOrderByClause;
import org.activitymgr.core.orm.query.DescendantOrderByClause;
import org.activitymgr.core.orm.query.InStatement;
import org.activitymgr.core.orm.query.LikeStatement;
import org.activitymgr.core.util.DateHelper;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.core.util.Strings;
import org.activitymgr.core.util.WorkbookBuilder;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.google.inject.Inject;

/**
 * Gestionnaire du modèle.
 * 
 * <p>
 * Les services offerts par cette classe garantissent l'intégrité du modèle.
 * </p>
 */
public class ModelMgrImpl implements IModelMgr {

	// On trie les taches manuellement car le tri base de données
	// pose un problème dans la mesure ou la BDD considère le champ
	// tsk_path comme numérique pour le tri ce qui pose un pb
	// Ex :
	// ROOT (path : 01)
	// +- T1 (path : 0101)
	// | +- T11 (path : 010101)
	// | +- T12 (path : 010102)
	// +- T2 (path : 0102)
	// Si on ramène l'ensemble des sous taches de ROOT, on voudrait
	// avoir
	// dans l'ordre T1, T11, T12, T2
	// Avec un tri base de donnée, on obtiendrait T1, T2, T11, T12 ; T2
	// ne se
	// trouve pas ou on l'attend, ceci en raison du fait qu'en
	// comparaison
	// numérique 0102 est < à 010101 et à 010102. Par contre, en
	// comparaison
	// de chaînes (en java), on a bien 0102 > 010101 et 010102.


	private static final Comparator<Task> TASK_PATH_SORTER = Comparator.comparing(it -> it.getFullPath());
	
	/** Logger */
	private static Logger log = Logger.getLogger(ModelMgrImpl.class);

	/** DAO */
	@Inject
	private ICoreDAO dao;

	/** Collaborators DAO */
	@Inject
	private ICollaboratorDAO collaboratorDAO;

	/** Tasks DAO */
	@Inject
	private ITaskDAO taskDAO;

	/** Durations DAO */
	@Inject
	private IDurationDAO durationDAO;

	/** Contributions DAO */
	@Inject
	private IContributionDAO contributionDAO;

	/** Reports DAO */
	@Inject
	private IReportDAO reportDAO;

	/** Report configurations DAO */
	@Inject
	private IReportCfgDAO reportCfgDAO;

	/** Bean factory */
	@Inject
	private IDTOFactory factory;
	
	/** Report columns computers map */
	private Map<String, IReportColumnComputer> defaultReportColumnComputers = new HashMap<String, IReportColumnComputer>();
	
	/**
	 * Default constructor.
	 */
	public ModelMgrImpl() {
		defaultReportColumnComputers.put(ReflectiveReportColumnComputer.TASK_PREFIX + PATH_ATTRIBUTE, new TaskPathReportColumnComputer());
	}

	@Override
	public boolean tablesExist() {
		return dao.tablesExist();
	}

	@Override
	public void createTables() {
		dao.createTables();
	}

	/**
	 * Substitue une partie du chemin d'un groupe de tache et de leurs
	 * sous-taches par un nouvelle valeur.
	 * <p>
	 * Cette méthode est utilisée pour déplacer les sous-taches d'une tache qui
	 * vient d'être déplacée.
	 * </p>
	 * 
	 * @param tx
	 *            le contexte de transaction.
	 * @param tasks
	 *            les taches dont on veut changer le chemin.
	 * @param oldPathLength
	 *            la taille de la portion de chemin à changer.
	 * @param newPath
	 *            le nouveau chemin.
	 */
	private void changeTasksPaths(Task[] tasks, int oldPathLength,
			String newPath) {
		// Récupération de la liste des taches
		int newPathLength = newPath.length();
		StringBuffer buf = new StringBuffer(newPath);
		for (Task task : tasks) {
			log.debug("Updating path of task '" + task.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			// Mise à jour des taches filles
			Task[] subTasks = getSubTasks(task);
			if (subTasks.length > 0)
				changeTasksPaths(subTasks, oldPathLength, newPath);
			// Puis mise à jour de la tache elle-même
			buf.setLength(newPathLength);
			buf.append(task.getPath().substring(oldPathLength));
			log.debug(" - old path : '" + task.getPath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			task.setPath(buf.toString());
			log.debug(" - new path : '" + task.getPath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			// Mise à jour
			taskDAO.update(task);
		}
	}

	private void verify(String errorCode, boolean predicate) throws ModelException {
		if (!predicate) {
			String msgKey = "ModelMgr.errors." + errorCode; //$NON-NLS-1$
			throw new ModelException(Strings.getString(msgKey));
		}
	}
	
	private void verify(String errorCode, boolean predicate, Object... details) throws ModelException {
		if (!predicate) {
			String msgKey = "ModelMgr.errors." + errorCode; //$NON-NLS-1$
			throw new ModelException(Strings.getString(msgKey, details));
		}
	}
	
	@Override
	public void checkAcceptsSubtasks(Task task) throws ModelException {
		// If the task is null, it means it is root task, so it always
		// accepts sub tasks
		if (task == null) {
			return;
		}
		// Rafraichissement des attributs de la tache
		task = getTask(task.getId());
		// Une tâche qui admet déja des sous-taches peut en admettre d'autres.
		// La suite des verification n'est donc exécutée que si la tache
		// n'admet pas de sous-tâches
		if (getSubTasksCount(task.getId()) != 0) {
			return;
		}
		verify("NON_NULL_TASK_BUDGET", //$NON-NLS-1$
				task.getBudget() == 0, task.getName());
		verify("NON_NULL_TASK_INITIALLY_CONSUMMED", //$NON-NLS-1$
				task.getInitiallyConsumed() == 0, task.getName());
		verify("NON_NULL_TASK_ESTIMATED_TIME_TO_COMPLETE", //$NON-NLS-1$
				task.getTodo() == 0, task.getName());
		
		// Une tache ne peut admettre une sous-tache que si elle
		// n'est pas déja associée à un consommé (ie: à des
		// contributions)
		long contribsNb = contributionDAO.getContributionsCount(
				null /*everyone*/, task, null /*from*/, null /*until*/);
		
		verify("TASK_USED_BY_CONTRIBUTIONS", //$NON-NLS-1$
				contribsNb == 0, task.getName(), contribsNb);
	}

	/**
	 * Vérifie que le chemin et le numéro de la tache en base de données
	 * coincident avec la copie de la tache spécifiée.
	 * 
	 * @param task
	 *            la copie de la tache en mémoire.
	 * @throws ModelException
	 *             levé dans la cas ou la tache de destination ne peut recevoir
	 *             de sous-tache.
	 */
	private void verifyTaskPath(Task task) throws ModelException {
		boolean noErrorOccured = false;
		Task _task = null;
		try {
			_task = getTask(task.getId());
			verify("UNKNOWN_TASK", _task != null, task.getId()); //$NON-NLS-1$
			verify("TASK_PATH_UPDATE_DETECTED", _task.getPath().equals(task.getPath())); //$NON-NLS-1$
			verify("TASK_NUMBER_UPDATE_DETECTED", _task.getNumber() == task.getNumber()); //$NON-NLS-1$
			
			noErrorOccured = true;
		} finally {
			// log concurrent modification
			if (!noErrorOccured && _task != null && task != null) {
				log.error("Task id = " + task.getId()); //$NON-NLS-1$
				log.error("     name = " + task.getName()); //$NON-NLS-1$
				log.error("     fullpath = " + task.getPath() + "/" + task.getNumber()); //$NON-NLS-1$ //$NON-NLS-2$
				log.error("     db fullpath = " + _task.getPath() + "/" + _task.getNumber()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private Duration verifyExistingDuration(Contribution contribution) throws  ModelException {		
		// La durée existe-t-elle ?
		Duration result = getDuration(contribution.getDurationId());
		// Verify "active" ??
		verify("INVALID_DURATION", result != null && result.getIsActive()); //$NON-NLS-1$
		return result;
	}
	
	/**
	 * Vérifie l'unicité d'un login.
	 * 
	 * @param collaborator
	 *            le collaborateur dont on veut vérifier l'unicité de login.
	 * @throws ModelException
	 *             levé dans le cas ou le ogin n'est pas unique.
	 */
	private void verifyUniqueLogin(Collaborator collaborator)
			throws  ModelException {
		// Vérification de l'unicité
		Collaborator existing = getCollaborator(collaborator.getLogin());
		// Vérification du login
		verify("NON_UNIQUE_COLLABORATOR_LOGIN", //$NON-NLS-1$
				existing == null || existing.equals(collaborator),
				collaborator.getLogin());
	}

	@Override
	public Collaborator createCollaborator(Collaborator collaborator)
			throws ModelException {
		log.info("createCollaborator(" + collaborator + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// Control de l'unicité du login
		verifyUniqueLogin(collaborator);

		// Collaborator creation
		return collaboratorDAO.insert(collaborator);
	}

	@Override
	public Contribution createContribution(Contribution contribution,
			boolean updateEtc) throws ModelException {
		log.info("createContribution(" + contribution + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// La tache ne peut accepter une contribution que
		// si elle n'admet aucune sous-tache
		verify("TASK_WITH_AT_LEAST_ONE_SUBTASK_CANNOT_ACCEPT_CONTRIBUTIONS", //$NON-NLS-1$ 
				getSubTasksCount(contribution.getTaskId()) == 0);
		Task task = getTask(contribution.getTaskId());

		verifyExistingDuration(contribution);

		// Contribution creation
		contribution = contributionDAO.insert(contribution);

		// Faut-il mettre à jour automatiquement le RAF de la tache ?
		if (updateEtc) {
			// Mise à jour du RAF de la tache
			long newEtc = task.getTodo() - contribution.getDurationId();
			task.setTodo(newEtc > 0 ? newEtc : 0);
			taskDAO.update(task);
		}

		// Retour du résultat
		return contribution;
	}

	@Override
	public Duration createDuration(Duration duration) throws ModelException {
		log.info("createDuration(" + duration + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// Vérification de l'unicité
		verify("DUPLICATE_DURATION", !durationExists(duration)); //$NON-NLS-1$
		verify("NUL_DURATION_FORBIDDEN", duration.getId() != 0); //$NON-NLS-1$
	
		// Duration creation
		return durationDAO.insert(duration);
	}

	@Override
	public Collaborator createNewCollaborator() {
		// Le login doit être unique => il faut vérifier si
		// celui-ci n'a pas déja été attribué

		String newLogin = null;

		for (int idx = 0; newLogin == null; idx++) {
			String login = "<" + Strings.getString("ModelMgr.defaults.COLLABORATOR_LOGIN_PREFIX") //$NON-NLS-1$ //$NON-NLS-2$
				+ (idx == 0 ? "" : String.valueOf(idx)) + ">"; //$NON-NLS-1$ //$NON-NLS-2$
			if (getCollaborator(login) == null) {
				newLogin = login;
			}
		}
		// Création du nouveau collaborateur
		Collaborator collaborator = factory.newCollaborator();
		collaborator.setLogin(newLogin);
		collaborator
				.setFirstName("<" + Strings.getString("ModelMgr.defaults.COLLABORATOR_FIRST_NAME") + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		collaborator
				.setLastName("<" + Strings.getString("ModelMgr.defaults.COLLABORATOR_LAST_NAME") + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Collaborator creation
		return collaboratorDAO.insert(collaborator);
	}

	@Override
	public synchronized Task createNewTask(Task parentTask) 
			throws ModelException {
		// Le code doit être unique => il faut vérifier si
		// celui-ci n'a pas déja été attribué
		int idx = 0;
		boolean unique = false;
		String newCode = null;
		String taskPath = parentTask != null ? parentTask.getFullPath() : ""; //$NON-NLS-1$
		while (!unique) {
			newCode = "<N" + (idx == 0 ? "" : String.valueOf(idx)) + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			unique = getTask(taskPath, newCode) == null;
			idx++;
		}
		// Création du nouveau collaborateur
		Task task = factory.newTask();
		task.setName("<" + Strings.getString("ModelMgr.defaults.TASK_NAME") + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		task.setCode(newCode);

		// Création en base
		return createTask(parentTask, task);
	}

	private void assertUniquePath(Task parentTask, Task task) throws ModelException {
		// Check sur l'unicité du code pour le chemin considéré
		Task sameCodeTask = getTask(
				parentTask != null ? parentTask.getFullPath() : "", task.getCode()); //$NON-NLS-1$
		verify("TASK_CODE_ALREADY_IN_USE", //$NON-NLS-1$
				sameCodeTask == null || sameCodeTask.equals(task), task.getCode());
	}
	
	@Override
	public synchronized Task createTask(Task parentTask, Task task)
			throws ModelException {
		log.info("createTask(" + parentTask + ", " + task + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// Une tache ne peut admettre une sous-tache que si elle
		// n'est pas déja associée à un consommé
		if (parentTask != null) {
			checkAcceptsSubtasks(parentTask);
		}

		assertUniquePath(parentTask, task);

		// Mise à jour du chemin de la tâche
		String parentPath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
		task.setPath(parentPath);

		// Génération du numéro de la tâche
		byte taskNumber = taskDAO.newTaskNumber(parentPath);
		task.setNumber(taskNumber);

		// Création de la tache
		return taskDAO.insert(task);
	}

	@Override
	public boolean durationExists(Duration duration) {
		return getDuration(duration.getId()) != null;
	}

	private class XmlModelMgr implements ModelMgrDelegate {
		
		final ModelMgrImpl modelMgr = ModelMgrImpl.this;
		
		final Map<String, Task> taskCache = new HashMap<>();
		final Map<String, Collaborator> collaboratorsCache = new HashMap<>();

		public Duration createDuration(Duration duration)
				throws ModelException {
			return modelMgr.createDuration(duration);
		}

		public Collaborator createCollaborator(Collaborator collaborator)
				throws ModelException {
			collaborator = modelMgr.createCollaborator(collaborator);
			collaboratorsCache.put(collaborator.getLogin(), collaborator);
			return collaborator;
		}

		public Task createTask(Task parentTask, Task task)
				throws ModelException {
			task = modelMgr.createTask(parentTask, task);
			String taskPath = modelMgr.buildTaskCodePath(task);
			taskCache.put(taskPath, task);
			return task;
		}

		public Contribution createContribution(Contribution contribution)
				throws ModelException {
			return modelMgr.createContribution(contribution, false);
		}

		public Task getTaskByCodePath(String codePath)
				throws ModelException {
			Task task = (Task) taskCache.get(codePath);
			if (task == null) {
				task = modelMgr.getTaskByCodePath(codePath);
				taskCache.put(codePath, task);
			}
			return task;
		}

		public Collaborator getCollaborator(String login) {
			Collaborator collaborator = (Collaborator) collaboratorsCache
					.get(login);
			if (collaborator == null) {
				collaborator = modelMgr.getCollaborator(login);
				collaboratorsCache.put(login, collaborator);
			}
			return collaborator;
		}

	}
	
	@Override
	public void importFromXML(InputStream in) throws IOException,
			ParserConfigurationException, SAXException, ModelException {
		try (InputStream src = in) {
			// Création du gestionnaire de modèle de données
			ModelMgrDelegate modelMgrDelegate = new XmlModelMgr();

			// Import des données
			SAXParserFactory saxFactory = SAXParserFactory.newInstance();
			saxFactory.setValidating(true);
			saxFactory.setNamespaceAware(false);
			SAXParser parser = saxFactory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			XmlHelper xmlHelper = new XmlHelper(modelMgrDelegate, factory);
			// La DTD est chargée dans le CLASSPATH
			reader.setEntityResolver(xmlHelper);
			// Positionnement du gestionnaire d'erreur
			reader.setErrorHandler(xmlHelper);
			// Positionnement du gestionnaire de contenu XML
			reader.setContentHandler(xmlHelper);
			// Parsing du fichier
			InputSource is = new InputSource(src);
			 // Pour empâcher la levée d'erreur associée à l'URI de la DTD 
			is.setSystemId(""); //$NON-NLS-1$
			reader.parse(is);

		} catch (SAXParseException e) {
			if (e.getCause() instanceof ModelException) {
				throw (ModelException) e.getCause();
			} else if (e.getCause() instanceof DAOException) {
				throw (DAOException) e.getCause();
			} else {
				throw e;
			}
		}
	}

	private final static String X_PREFIX = "ModelMgr.xmlexport.comment."; //$NON-NLS-1$
	@Override
	public void exportToXML(OutputStream out) throws IOException {
		// Entête XML
		XmlHelper.println(out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		XmlHelper.println(out, "<!DOCTYPE model SYSTEM \"activitymgr.dtd\">"); //$NON-NLS-1$

		// Ajout des sommes de controle
		List<TaskSums> rootTasksSums = getSubTasksSums(null, "", null, null);
		
		if (rootTasksSums.size() > 0) {
			XmlHelper.println(out, "<!-- "); //$NON-NLS-1$
			XmlHelper.println(out,
							Strings.getString(X_PREFIX + "ROOT_TASKS_CHECK_SUMS")); //$NON-NLS-1$
			int i = 0;
			for (TaskSums sums : rootTasksSums) {
				i ++;
				Task rootTask = sums.getTask();
				for (String line : Arrays.asList(
						Strings.getString(X_PREFIX 
								+ "ROOT_TASK", i, rootTask.getCode(), rootTask.getName()), //$NON-NLS-1$
						Strings.getString(X_PREFIX 
								+ "BUDGET") + (sums.getBudgetSum() / 100d), //$NON-NLS-1$
						Strings.getString(X_PREFIX 
								+ "INITIALLY_CONSUMED") + (sums.getInitiallyConsumedSum() / 100d), //$NON-NLS-1$
						Strings.getString(X_PREFIX 
								+ "CONSUMED") + (sums.getContributionsSums().getConsumedSum() / 100d), //$NON-NLS-1$
						Strings.getString(X_PREFIX 
								+ "ESTIMATED_TIME_TO_COMPLETE") + (sums.getTodoSum() / 100d), //$NON-NLS-1$
						Strings.getString(X_PREFIX
								+ "CONTRIBUTIONS_NUMBER") + sums.getContributionsSums().getContributionsNb() //$NON-NLS-1$
						)) {
					XmlHelper.println(out, line);
				}
			}
			XmlHelper.println(out, "  -->"); //$NON-NLS-1$
		}

		// Ajout du noeud racine
		XmlHelper.startXmlNode(out, "", XmlHelper.MODEL_NODE); //$NON-NLS-1$
		final String INDENT = "      "; //$NON-NLS-1$

		// Exportation des durées
		Duration[] durations = durationDAO.selectAll();
		if (durations.length > 0) {
			XmlHelper.startXmlNode(out, "  ", XmlHelper.DURATIONS_NODE); //$NON-NLS-1$
			for (int i = 0; i < durations.length; i++) {
				Duration duration = durations[i];
				XmlHelper.startXmlNode(out, "    ", XmlHelper.DURATION_NODE); //$NON-NLS-1$
				XmlHelper.printTextNode(out, INDENT, XmlHelper.VALUE_NODE,
						String.valueOf(duration.getId()));
				XmlHelper.printTextNode(out, INDENT, XmlHelper.IS_ACTIVE_NODE,
						String.valueOf(duration.getIsActive()));
				XmlHelper.endXmlNode(out, "    ", XmlHelper.DURATION_NODE); //$NON-NLS-1$
			}
			XmlHelper.endXmlNode(out, "  ", XmlHelper.DURATIONS_NODE); //$NON-NLS-1$
		}
		// Exportation des collaborateurs
		Collaborator[] collaborators = getCollaborators();
		Map<Long, String> collaboratorsLoginsMap = new HashMap<Long, String>();
		if (collaborators.length > 0) {
			XmlHelper.startXmlNode(out, "  ", XmlHelper.COLLABORATORS_NODE); //$NON-NLS-1$
			for (int i = 0; i < collaborators.length; i++) {
				Collaborator collaborator = collaborators[i];
				// Enregitrement du login dans le dictionnaire de logins
				collaboratorsLoginsMap.put(collaborator.getId(),
						collaborator.getLogin());
				XmlHelper
						.startXmlNode(out, "    ", XmlHelper.COLLABORATOR_NODE); //$NON-NLS-1$
				XmlHelper.printTextNode(out, INDENT, XmlHelper.LOGIN_NODE,
						collaborator.getLogin());
				XmlHelper.printTextNode(out, INDENT, XmlHelper.FIRST_NAME_NODE,
						collaborator.getFirstName());
				XmlHelper.printTextNode(out, INDENT, XmlHelper.LAST_NAME_NODE,
						collaborator.getLastName());
				XmlHelper.printTextNode(out, INDENT, XmlHelper.IS_ACTIVE_NODE,
						String.valueOf(collaborator.getIsActive()));
				XmlHelper.endXmlNode(out, "    ", XmlHelper.COLLABORATOR_NODE); //$NON-NLS-1$
			}
			XmlHelper.endXmlNode(out, "  ", XmlHelper.COLLABORATORS_NODE); //$NON-NLS-1$
		}
		// Exportation des taches
		Map<Long, String> tasksCodePathMap = new HashMap<Long, String>();
		exportSubTasksToXML(out, INDENT, null, "", tasksCodePathMap); //$NON-NLS-1$
		// Exportation des contributions
		Contribution[] contributions = contributionDAO.getContributions(null, null, null,
				null);
		if (contributions.length > 0) {
			XmlHelper.startXmlNode(out, "  ", XmlHelper.CONTRIBUTIONS_NODE); //$NON-NLS-1$
			for (int i = 0; i < contributions.length; i++) {
				Contribution contribution = contributions[i];
				XmlHelper.print(out, "    <"); //$NON-NLS-1$
				XmlHelper.print(out, XmlHelper.CONTRIBUTION_NODE);
				XmlHelper.printTextAttribute(out, XmlHelper.YEAR_ATTRIBUTE,
						String.valueOf(contribution.getYear()));
				XmlHelper.printTextAttribute(out, XmlHelper.MONTH_ATTRIBUTE,
						String.valueOf(contribution.getMonth()));
				XmlHelper.printTextAttribute(out, XmlHelper.DAY_ATTRIBUTE,
						String.valueOf(contribution.getDay()));
				XmlHelper.printTextAttribute(out, XmlHelper.DURATION_ATTRIBUTE,
						String.valueOf(contribution.getDurationId()));
				XmlHelper.println(out, ">"); //$NON-NLS-1$
				XmlHelper.printTextNode(out, INDENT,
						XmlHelper.CONTRIBUTOR_REF_NODE,
						(String) collaboratorsLoginsMap.get(
								contribution.getContributorId()));
				XmlHelper.printTextNode(out, INDENT, XmlHelper.TASK_REF_NODE,
						(String) tasksCodePathMap.get(contribution
								.getTaskId()));
				XmlHelper.endXmlNode(out, "    ", XmlHelper.CONTRIBUTION_NODE); //$NON-NLS-1$
			}
			XmlHelper.endXmlNode(out, "  ", XmlHelper.CONTRIBUTIONS_NODE); //$NON-NLS-1$
		}
		XmlHelper.endXmlNode(out, "", "model"); //$NON-NLS-1$ //$NON-NLS-2$
		out.flush();
	}

	/**
	 * Ecrit les sous taches sous forme de XML dans le flux d'écriture.
	 * 
	 * @param out
	 *            le flux d'écriture.
	 * @param indent
	 *            l'indentation.
	 * @param parentTask
	 *            la tache parent.
	 * @param parentCodePath
	 *            le chemin de la tache parente.
	 * @param taskCodesPathMap
	 *            cache contenant les taches indexées par leur chemin.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 */
	private void exportSubTasksToXML(OutputStream out, String indent,
			Task parentTask, String parentCodePath,
			Map<Long, String> taskCodesPathMap) throws IOException {
		Task[] tasks = getSubTasks(parentTask);
		if (tasks.length > 0) {
			// Cas particulier pour la racine
			if (parentTask == null)
				XmlHelper.startXmlNode(out, "  ", XmlHelper.TASKS_NODE); //$NON-NLS-1$
			for (int i = 0; i < tasks.length; i++) {
				Task task = tasks[i];
				XmlHelper.startXmlNode(out, "    ", XmlHelper.TASK_NODE); //$NON-NLS-1$
				String taskCodePath = parentCodePath + "/" + task.getCode(); //$NON-NLS-1$
				// Enregistrement du chemin dans le dictionnaire de chemins
				taskCodesPathMap.put(task.getId(), taskCodePath);
				XmlHelper.printTextNode(out, indent, XmlHelper.PATH_NODE,
						taskCodePath);
				XmlHelper.printTextNode(out, indent, XmlHelper.NAME_NODE,
						task.getName());
				XmlHelper.printTextNode(out, indent, XmlHelper.BUDGET_NODE,
						String.valueOf(task.getBudget()));
				XmlHelper.printTextNode(out, indent,
						XmlHelper.INITIALLY_CONSUMED_NODE,
						String.valueOf(task.getInitiallyConsumed()));
				XmlHelper.printTextNode(out, indent, XmlHelper.TODO_NODE,
						String.valueOf(task.getTodo()));
				if (task.getComment() != null)
					XmlHelper.printTextNode(out, indent,
							XmlHelper.COMMENT_NODE, task.getComment());
				XmlHelper.endXmlNode(out, "    ", XmlHelper.TASK_NODE); //$NON-NLS-1$
				exportSubTasksToXML(out, indent, task, taskCodePath,
						taskCodesPathMap);
			}
			// Cas particulier pour la racine
			if (parentTask == null)
				XmlHelper.endXmlNode(out, "  ", XmlHelper.TASKS_NODE); //$NON-NLS-1$
		}
	}

	@Override
	public boolean isLeaf(long parentTaskId) {
		return getSubTasksCount(parentTaskId) == 0;
	}
	
	@Override
	public int getSubTasksCount(long parentTaskId) {
		return taskDAO.getSubTasksCount(parentTaskId);
	}

	@Override
	public Collaborator getCollaborator(long collaboratorId) {
		return collaboratorDAO.selectByPK(new Object[] { collaboratorId });
	}

	@Override
	public Collaborator getCollaborator(String login) {
		Collaborator[] collaborators = collaboratorDAO.select(new String[] { "login" }, new Object[] { login }, null, -1);
		return collaborators.length > 0 ? collaborators[0] : null;
	}

	@Override
	public Collaborator[] getCollaborators() {
		return getCollaborators(Collaborator.LOGIN_FIELD_IDX, true, false);
	}

	@Override
	public Collaborator[] getActiveCollaborators(int orderFieldIndex,
			boolean ascendantSort) {
		return getCollaborators(orderFieldIndex, ascendantSort, true);
	}

	private Collaborator[] getCollaborators(int orderFieldIndex,
			boolean ascendantSort, boolean onlyActive) {
		String orderName;
		switch (orderFieldIndex) {
		case Collaborator.ID_FIELD_IDX:
			orderName = "id"; //$NON-NLS-1$
			break;
		case Collaborator.LOGIN_FIELD_IDX:
			orderName = "login"; //$NON-NLS-1$
			break;
		case Collaborator.FIRST_NAME_FIELD_IDX:
			orderName = "firstName"; //$NON-NLS-1$
			break;
		case Collaborator.LAST_NAME_FIELD_IDX:
			orderName = "lastName"; //$NON-NLS-1$
			break;
		case Collaborator.IS_ACTIVE_FIELD_IDX:
			orderName = "isActive"; //$NON-NLS-1$
			break;
		default:
			throw new DAOException(
					Strings.getString("DbMgr.errors.UNKNOWN_FIELD_INDEX", orderFieldIndex)); //$NON-NLS-1$
		}
		Object[] orderByClause = new Object[] { 
				ascendantSort 
					? new AscendantOrderByClause(orderName) 
					: new DescendantOrderByClause(orderName)
		};
		if (onlyActive) {
			return collaboratorDAO.select(
					new String[] { "isActive" }, 
					new Object[] { Boolean.TRUE }, 
					orderByClause, -1);
		} else {
			return collaboratorDAO.select(null, null, orderByClause, -1);
		}
	}


	@Override
	public Collaborator[] getCollaborators(int orderFieldIndex,
			boolean ascendantSort) {
		return getCollaborators(orderFieldIndex, ascendantSort,
				false);
	}


	@Override
	public long getContributionsSum(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws ModelException {
		// Control sur la date
		verifyInterval(fromDate, toDate);
		// Récupération du total
		return contributionDAO.getContributionsSum(contributor, task, fromDate, toDate);
	}


	@Override
	public int getContributionsCount(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws ModelException {
		// Control sur la date
		verifyInterval(fromDate, toDate);
		// Récupération du compte
		return contributionDAO.getContributionsCount(contributor, task, fromDate, toDate);
	}

	@Override
	public Contribution[] getContributions(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws 	ModelException {
		// Vérification de la tache (le chemin de la tache doit être le bon
		// pour que le calcul le soit)
		if (task != null) {
			verifyTaskPath(task);
		}

		// Control sur la date
		verifyInterval(fromDate, toDate);

		// Retour du résultat
		return contributionDAO.getContributions(contributor, task, fromDate, toDate);
	}

	@Override
	public Collaborator[] getContributors(Task task, Calendar fromDate,
			Calendar toDate) throws ModelException {
		verifyInterval(fromDate, toDate);
		return collaboratorDAO.getContributors(task, fromDate, toDate);
	}

	/**
	 * Checks whether the given interval is relevant or not.
	 * 
	 * @param fromDate
	 *            start of the date interval.
	 * @param toDate
	 *            end of the date interval.
	 * @throws ModelException
	 *             thrown if the interval is invalid.
	 */
	private void verifyInterval(Calendar fromDate, Calendar toDate)
			throws ModelException {
		verify("FROM_DATE_MUST_BE_BEFORE_TO_DATE", //$NON-NLS-1$
			fromDate == null 
				|| toDate == null
				|| !fromDate.getTime().after(toDate.getTime()));
	}

	private Map<Long, TaskContributions> createTaskContributions(
			Collaborator contributor, Task task, Calendar fromDate,
			Calendar toDate) {
		Contribution[] contributions = contributionDAO.getContributions(contributor,
				task, fromDate, toDate);
		
		int daysCount = DateHelper.countDaysBetween(fromDate, toDate) + 1;
		
		// Rangement des contributions par identifiant de tache
		// (as the tsk parameter can be omitted => in this case, several
		// tasks might be returned)
		Map<Long, TaskContributions> result = new HashMap<Long, TaskContributions>();
		
		Function<Long, TaskContributions> creator = id -> {
			TaskContributions part = new TaskContributions();
			part.setContributions(new Contribution[daysCount]);
			return part;
		};
		for (Contribution contribution : contributions) {
			TaskContributions part = result
					.computeIfAbsent(contribution.getTaskId(), creator);
			
			Calendar contributionDate = new GregorianCalendar(
					contribution.getYear(), 
					contribution.getMonth() - 1,
					contribution.getDay());
			int idx = DateHelper.countDaysBetween(fromDate, contributionDate);
			part.getContributions()[idx] = contribution;
		}
		return result;
	}
	
	
	@Override
	public IntervalContributions getIntervalContributions(
			Collaborator contributor, Task task, 
			Calendar fromDate, Calendar toDate) throws ModelException {
		// If the contributor is missing, error....
		verify("CONTRIBUTOR_MUST_BE_SPECIFIED", contributor != null);

		// Control sur la date
		verifyInterval(fromDate, toDate);

		// Récupération des contributions
		Map<Long, TaskContributions> taskContribs = 
				createTaskContributions(contributor, task, fromDate, toDate);

		// Task retrieval and sort
		Task[] tasks = getTasks(taskContribs.keySet().stream()
				.mapToLong(it -> it)
				.toArray());
		Arrays.sort(tasks, TASK_PATH_SORTER);
		
		Map<String, Task> hierarchies = taskDAO.getAllParents(Arrays.asList(tasks));

		// Result building
		IntervalContributions result = new IntervalContributions();
		result.setFromDate(fromDate);
		result.setToDate(toDate);
		result.setTaskContributions(new TaskContributions[tasks.length]);
		
		for (int i = 0; i < tasks.length; i++) {
			Task aTask = tasks[i];
			TaskContributions taskContrib = taskContribs
					.get(aTask.getId());
			taskContrib.setTask(aTask);
			taskContrib.setTaskCodePath(Tasks.buildTaskCodePath(aTask, hierarchies));
			taskContrib.setClosed(!Tasks.isModifiable(aTask, hierarchies));
			result.getTaskContributions()[i] = taskContrib;
		}

		return result;
	}

	@Override
	public Duration[] getDurations() {
		return durationDAO.select(null, null,
				new Object[] { new AscendantOrderByClause("id") }, -1);
	}

	@Override
	public Duration[] getActiveDurations() {
		return durationDAO.select(
				new String[] { "isActive" }, new Object[] { Boolean.TRUE },
				new Object[] { new AscendantOrderByClause("id") }, -1);
	}

	@Override
	public Duration getDuration(long durationId) {
		return durationDAO.selectByPK(new Object[] { durationId });
	}

	public Task getParentTask(Task task) {

		String parentTaskFullPath = task.getPath();
		// Si le chemin est vide, la tache parent est nulle (tache racine)
		if (parentTaskFullPath == null || parentTaskFullPath.isEmpty()) {
			return null;
		}

		// Extraction du chemin et du numéro de la tache recherchée
		log.debug("Fullpath='" + parentTaskFullPath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		String path = parentTaskFullPath.substring(0,
				parentTaskFullPath.length() - 2);
		byte number = StringHelper.toByte(parentTaskFullPath
				.substring(parentTaskFullPath.length() - 2));
		log.debug(" => path=" + path); //$NON-NLS-1$
		log.debug(" => number=" + number); //$NON-NLS-1$

		return getTask(path, number);

	}

	@Override
	public Task[] getSubTasks(Long parentTaskId, String filter) {
		Task parentTask = parentTaskId != null ? getTask(parentTaskId)
				: null;
		String fullpath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
		return taskDAO.getSubTasks(fullpath, filter);
	}

	@Override
	public Task getFirstTaskMatching(String filter) {
		return taskDAO.getFirstTaskMatching(filter);
	}

	public Task[] getSubTasks(Long parentTaskId) {
		// Récupération des sous tâches
		Task parentTask = parentTaskId != null ? getTask(parentTaskId)
				: null;

		return getSubTasks(parentTask);
	}

	private Task[] getSubTasks(Task parentTask) {
		// Récupération du chemin à partir de la tache parent
		String fullpath = parentTask == null ? "" : parentTask.getFullPath(); //$NON-NLS-1$
		log.debug("Looking for tasks with path='" + fullpath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		return taskDAO.select(new String[] { PATH_ATTRIBUTE }, new Object[] { fullpath }, new Object[] { new AscendantOrderByClause("number") }, -1);
	}

	public Task getTask(long taskId) {
		return taskDAO.selectByPK(new Object[] { taskId });
	}

	@Override
	public int getRootTasksCount() {
		return (int) taskDAO.count(new String[] { PATH_ATTRIBUTE }, new Object[] { "" });
	}

	@Override
	public Task[] getTasks(TaskSearchFilter filter) {
		long[] taskIds = taskDAO.getTaskIds(filter);
		Task[] tasks = getTasks(taskIds);

		Arrays.sort(tasks, TASK_PATH_SORTER);

		// Retour du résultat
		return tasks;
	}

	@Override
	public Task getTask(String taskPath, String taskCode) {
		Task[] tasks = taskDAO.select(
				new String[] { PATH_ATTRIBUTE, CODE_ATTRIBUTE }, 
				new Object[] { taskPath, taskCode }, 
				null, -1);
		return tasks.length > 0 ? tasks[0] : null;
	}

	@Override
	public Task getTaskByCodePath(final String codePath) throws ModelException {
		log.info("getTaskByCodePath(" + codePath + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		verify("INVALID_TASK_CODE_PATH", codePath.startsWith("/")); //$NON-NLS-1$ 

		// Recherche de la tache
		String subpath = codePath.trim().substring(1);
		log.debug("Processing task path '" + subpath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		Task task = null;
		while (subpath.length() > 0) {
			int idx = subpath.indexOf('/');
			String taskCode = idx >= 0 ? subpath.substring(0, idx) : subpath;
			String taskPath = task != null ? task.getFullPath() : ""; //$NON-NLS-1$
			subpath = idx >= 0 ? subpath.substring(idx + 1) : ""; //$NON-NLS-1$
			task = getTask(taskPath, taskCode);
			verify("UNKNOWN_TASK_CODE_PATH", task != null, codePath);  //$NON-NLS-1$
		}
		log.debug("Found " + task); //$NON-NLS-1$

		// Retour du résultat
		return task;
	}

	@Override
	public Task[] getContributedTasks(Collaborator contributor,
			Calendar fromDate, Calendar toDate) {
		long[] taskIds = taskDAO.getContributedTaskIds(contributor, fromDate, toDate);
		return getTasks(taskIds);
	}

	@Override
	public Task[] getTasksByCodePath(String[] codePaths) throws ModelException {
		// Recherche des taches
		Task[] tasks = new Task[codePaths.length];
		for (int i = 0; i < codePaths.length; i++) {
			String codePath = codePaths[i].trim();
			log.debug("Searching task path '" + codePath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			Task task = getTaskByCodePath(codePath);
			// Enregistrement dans le tableau

			verify("UNKNOWN_TASK", task != null, codePath); //$NON-NLS-1$
			tasks[i] = task;
		}

		// Retour du résultat
		return tasks;
	}

	@Override
	public TaskSums getTaskSums(long taskId, Calendar fromDate, Calendar toDate)
			throws ModelException {
		return getSubTasksSums(taskId, null, fromDate, toDate).get(0);
	}

	@Override
	public List<TaskSums> getSubTasksSums(Task parentTask, Calendar fromDate,
			Calendar toDate) throws ModelException {
		// Vérification de la tache (le chemin de la tache doit être le bon
		// pour que le calcul le soit)
		if (parentTask != null) {
			verifyTaskPath(parentTask);
		}
		// Compute parent task path
		String tasksPath = parentTask != null ? parentTask.getFullPath() : "";
		
		return getSubTasksSums(null, tasksPath, fromDate, toDate);
	}
	
	private List<TaskSums> getSubTasksSums(Long taskId, String tasksPath, Calendar fromDate,
			Calendar toDate) {
		// Compute sums
		List<TaskSums> tasksSums = taskDAO.getTasksSums(taskId, tasksPath);

		// Add contributions
		Map<Long, TaskContributionsSums> contributionSums = contributionDAO.getTasksSums(taskId, tasksPath, fromDate, toDate);
		for (TaskSums taskSums : tasksSums) {
			long theTaskId = taskSums.getTask().getId();
			taskSums.setContributionsSums(contributionSums.get(theTaskId));
		}
		
		// If a start date has been specified, initially consumed must be fixed by adding past contributions
		if (fromDate != null) {
			Calendar date = (Calendar) fromDate.clone();
			date.add(Calendar.DATE, -1);
			Map<Long, TaskContributionsSums> pastContributionsSums = contributionDAO.getTasksSums(taskId, tasksPath, null, date);
			for (TaskSums taskSums : tasksSums) {
				long theTaskId = taskSums.getTask().getId();
				taskSums.setInitiallyConsumedSum(taskSums.getInitiallyConsumedSum() + pastContributionsSums.get(theTaskId).getConsumedSum());
			}
		}
		
		// If an end date has been specified, estimated time to complete must be fixed by adding future contributions
		if (toDate != null) {
			Calendar date = (Calendar) toDate.clone();
			date.add(Calendar.DATE, +1);
			Map<Long, TaskContributionsSums> futureContributionsSums = contributionDAO.getTasksSums(taskId, tasksPath, date, null);
			for (TaskSums taskSums : tasksSums) {
				long theTaskId = taskSums.getTask().getId();
				try {
					taskSums.setTodoSum(taskSums.getTodoSum() + futureContributionsSums.get(theTaskId).getConsumedSum());
				} catch (NullPointerException e) {
					throw e;
				}
			}
		}
		
		// Return the result
		return tasksSums;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Whe dealing with several tasks, this method is not effective.
	 * </p>
	 */
	@Override
	public String getTaskCodePath(Task task) throws ModelException {
		// Le chemin de la tache et son numéro ne doivent pas avoir changés
		// pour pouvoir invoquer cette méthode (la modification des
		// attributs
		// n'est autorisée que pour les champs autres que le chemin et le
		// numéro.
		verifyTaskPath(task);

		// Construction du chemin
		return buildTaskCodePath(task);
	}

	/**
	 * Construit le chemin de la tâche à partir des codes de tache.
	 * 
	 * @param task
	 *            la tache dont on veut connaître le chemin.
	 * @return le chemin.
	 */
	private String buildTaskCodePath(Task task) {
		Map<String, Task> parents = taskDAO.getAllParents(Collections.singletonList(task));
		return Tasks.buildTaskCodePath(task, parents);
	}

	@Override
	public void moveDownTask(Task task) throws ModelException {
		// Le chemin de la tache et son numéro ne doivent pas avoir changés
		// pour pouvoir invoquer cette méthode (la modification des
		// attributs
		// n'est autorisée que pour les champs autres que le chemin et le
		// numéro.
		verifyTaskPath(task);

		// Recherche de la tache à descendre (incrémentation du numéro)
		byte taskToMoveUpNumber = (byte) (task.getNumber() + 1);
		Task taskToMoveUp = getTask(task.getPath(), taskToMoveUpNumber);
		verify("TASK_CANNOT_BE_MOVED_DOWN", taskToMoveUp != null); //$NON-NLS-1$

		// Inversion des taches
		toggleTasks(task, taskToMoveUp);
	}

	@Override
	public void moveTaskUpOrDown(Task task, int newTaskNumber)
			throws ModelException {
		// Le chemin de la tache et son numéro ne doivent pas avoir changés
		// pour pouvoir invoquer cette méthode
		verifyTaskPath(task);

		// Pour que la méthode fonctionne, il faut que le nombre
		// cible soit différent du nombre courant
		if (task.getNumber() == newTaskNumber)
			throw new ModelException(
					"New task number is equal to current task number ; task not moved");

		// Récupération de la tache parent, et contrôle du modèle
		// (le numéro de destination ne peut être hors interval)
		Task parentTask = getParentTask(task);
		int subTasksCount = parentTask != null 
				? getSubTasksCount(parentTask.getId())
				: getRootTasksCount();
		if (newTaskNumber > subTasksCount || newTaskNumber < 1) {
			throw new ModelException("Invalid task number");
		}

		// Définition du sens de déplacement
		int stepSign = task.getNumber() > newTaskNumber ? -1 : 1;
		for (int i = task.getNumber() + stepSign; i != newTaskNumber + stepSign; i+=stepSign) {
			Task taskToToggle = getTask(task.getPath(), (byte) i);
			toggleTasks(task, taskToToggle);
			task.setNumber((byte) i);
		}
	}

	@Override
	public synchronized void moveTask(Task task, Task destParentTask)
			throws ModelException {
		/**
		 * Controles d'intégrité.
		 */

		// Le chemin de la tache et son numéro ne doivent pas avoir changés
		// pour pouvoir invoquer cette méthode (la modification des
		// attributs
		// n'est autorisée que pour les champs autres que le chemin et le
		// numéro.
		verifyTaskPath(task);
		if (destParentTask != null) {
			verifyTaskPath(destParentTask);
		}

		// Control : la tache de destination ne doit pas être
		// une tache fille de la tache à déplacer
		Task cursor = destParentTask;
		while (cursor != null) {
			verify("TASK_CANNOT_BE_MOVED_UNDER_ITSELF", !cursor.equals(task)); //$NON-NLS-1$
			cursor = getParentTask(cursor);
		}

		// Une tache ne peut admettre une sous-tache que si elle
		// n'est pas déja associée à un consommé
		if (destParentTask != null) {
			checkAcceptsSubtasks(destParentTask);
		}

		// Le code de la tache à déplacer ne doit pas être en conflit
		// avec un code d'une autre tache fille de la tache parent
		// de destination
		String destPath = destParentTask != null 
				? destParentTask.getFullPath()
				: ""; //$NON-NLS-1$
		verify("TASK_CODE_EXIST_AT_DESTINATION", //$NON-NLS-1$
				getTask(destPath, task.getCode()) == null, task.getCode()); 


		/**
		 * Déplacement de la tache.
		 */

		// Récupération de la tache parent et des sous-taches
		// avant modification de son numéro et de son chemin
		String initialTaskFullPath = task.getFullPath();
		Task srcParentTask = getParentTask(task);
		Task[] subTasksToMove = getSubTasks(task);

		// Déplacement de la tache
		byte number = taskDAO.newTaskNumber(destPath);
		task.setPath(destPath);
		task.setNumber(number);
		taskDAO.update(task);

		// Déplacement des sous-taches
		changeTasksPaths(subTasksToMove, initialTaskFullPath.length(),
				task.getFullPath());

		// Reconstruction des numéros de tâches d'où la tâche provenait
		// et qui a laissé un 'trou' en étant déplacée
		rebuildSubtasksNumbers(srcParentTask);
	}

	@Override
	public void moveUpTask(Task task) throws ModelException {
		// Le chemin de la tache et son numéro ne doivent pas avoir changés
		// pour pouvoir invoquer cette méthode (la modification des
		// attributs
		// n'est autorisée que pour les champs autres que le chemin et le
		// numéro.
		verifyTaskPath(task);

		// Recherche de la tache à monter (décrémentation du numéro)
		byte taskToMoveDownNumber = (byte) (task.getNumber() - 1);
		Task taskToMoveDown = getTask(task.getPath(), taskToMoveDownNumber);
		verify("TASK_CANNOT_BE_MOVED_UP", taskToMoveDown != null); //$NON-NLS-1$

		// Inversion des taches
		toggleTasks(task, taskToMoveDown);
	}

	/**
	 * Reconstruit les numéros de taches pour un chemin donné (chemin complet de
	 * la tache parent considérée).
	 * 
	 * @param parentTask
	 *            la tache parent.
	 */
	private void rebuildSubtasksNumbers(Task parentTask) {
		// Récupération des sous-taches
		Task[] tasks = getSubTasks(parentTask);
		for (int i = 0; i < tasks.length; i++) {
			Task task = tasks[i];
			byte taskNumber = task.getNumber();
			byte expectedNumber = (byte) (i + 1);
			if (taskNumber != expectedNumber) {
				Task[] subTasks = getSubTasks(task);
				task.setNumber(expectedNumber);
				String fullPath = task.getFullPath();
				changeTasksPaths(subTasks, fullPath.length(), fullPath);
				taskDAO.update(task);
			}
		}
	}


	@Override
	public void removeCollaborator(Collaborator collaborator)
			throws ModelException {
		// Vérification que le collaborateur n'est pas utilisé
		long contribsNb = getContributionsCount(collaborator, null, null, null);
		if (contribsNb != 0)
			throw new ModelException(
					Strings.getString(
							"ModelMgr.errros.COLLABORATOR_WITH_CONTRIBUTIONS_CANNOT_BE_REMOVED", contribsNb)); //$NON-NLS-1$ //$NON-NLS-2$

		// Suppression du collaborateur
		collaboratorDAO.delete(new String[] { "id" }, new Object[] { collaborator.getId() });
	}


	@Override
	public void removeContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws ModelException {
		// Faut-il mettre à jour automatiquement le RAF de la tache ?
		if (!updateEstimatedTimeToComlete) {
			// Suppression de la contribution
			contributionDAO.delete(contribution);
		} else {
			// Récupération des éléments de la contribution
			Collaborator contributor = getCollaborator(contribution
					.getContributorId());
			Task task = getTask(contribution.getTaskId());
			// Récupération de la contribution correspondante en base
			Contribution[] contributions = contributionDAO.getContributions(contributor,
					task, contribution.getDate(), contribution.getDate());
			if (contributions.length == 0) {
				// Si la contribution n'existait pas, il n'y a rien à faire
				// de plus
			} else { // Sinon, il y a forcément une seule contribution
				
				// On vérifie que la donnée en base est en phase avec
				// l'entrant
				// pour s'assurer qu'on ne va pas incrémenter le RAF de la
				// tache
				// avec une valeur incohérente
				verify("CONTRIBUTION_UPDATE_DETECTED", //$NON-NLS-1$
						contribution.getDurationId() == contributions[0].getDurationId());

				// Suppression de la contribution
				contributionDAO.delete(contribution);

				// Mise à jour du RAF de la tache
				task.setTodo(task.getTodo() + contribution.getDurationId());
				taskDAO.update(task);
			}
		}
	}


	@Override
	public void removeContributions(Contribution[] contributions) {
		// Suppression de la contribution
		for (Contribution contribution : contributions) {
			contributionDAO.delete(contribution);
		}
	}


	@Override
	public void removeDuration(Duration duration) throws ModelException {
		// Vérification de l'existance
		verify("DURATION_DOES_NOT_EXIST", durationExists(duration)); //$NON-NLS-1$

		// Vérification de la non utilisation de la durée
		boolean isUsed = contributionDAO.count(new String[] { "durationId" }, new Object[] { duration.getId()}) > 0;
		verify("UNMOVEABLE_DURATION", !isUsed); //$NON-NLS-1$

		// Suppression
		durationDAO.delete(duration);
	}

	@Override
	public synchronized void removeTask(Task task) throws ModelException {
		// Vérification de l'adéquation des attributs de la tache avec les
		// données en base
		verifyTaskPath(task);

		// Vérification que la tache n'est pas utilisé
		long contribsNb = getContributionsCount(null, task, null, null);
		verify("TASK_HAS_SUBTASKS", contribsNb == 0, contribsNb); //$NON-NLS-1$ 

		// Récupération de la tâche parent pour reconstruction des
		// numéros de taches
		Task parentTask = getParentTask(task);

		// Delete sub tasks
		taskDAO.delete(new String[] { PATH_ATTRIBUTE }, new Object[] { new LikeStatement(task.getFullPath() + "%") });

		// Delete the task
		taskDAO.delete(task);

		// Reconstruction des numéros de taches
		rebuildSubtasksNumbers(parentTask);

	}

	/**
	 * Inverse deux taches dans l'arborescence des taches.
	 * 
	 * @param task1
	 *            la 1° tache.
	 * @param task2
	 *            la 2nde tache.
	 */
	private void toggleTasks(Task task1, Task task2) {
		byte task1InitialNumber = task1.getNumber();
		byte task2InitialNumber = task2.getNumber();
		String task1InitialFullpath = task1.getFullPath();
		String task2InitialFullpath = task2.getFullPath();

		// Récupération des taches filles de ces 2 taches
		Task[] task1subTasks = getSubTasks(task1);
		Task[] task2subTasks = getSubTasks(task2);

		// Changement des numéros de la tache 1 avec une valeur fictive
		task1.setNumber((byte) 0);
		taskDAO.update(task1);
		changeTasksPaths(task1subTasks, task1InitialFullpath.length(),
				task1.getFullPath());

		// Changement des numéros de la tache 2
		task2.setNumber(task1InitialNumber);
		taskDAO.update(task2);
		changeTasksPaths(task2subTasks, task2InitialFullpath.length(),
				task2.getFullPath());

		// Changement des numéros de la tache 1
		task1.setNumber(task2InitialNumber);
		taskDAO.update(task1);
		changeTasksPaths(task1subTasks, task1InitialFullpath.length(),
				task1.getFullPath());
	}

	@Override
	public Collaborator updateCollaborator(Collaborator collaborator)
			throws ModelException {
		// Control de l'unicité du login
		verifyUniqueLogin(collaborator);

		// Mise à jour des données
		return collaboratorDAO.update(collaborator);
	}

	@Override
	public Duration updateDuration(Duration duration) {
		return durationDAO.update(duration);
	}

	@Override
	public Contribution updateContribution(Contribution contribution,
			boolean updateEstimatedTimeToComlete) throws ModelException {
		verifyExistingDuration(contribution);

		Contribution result = null;
		// Faut-il mettre à jour automatiquement le RAF de la tache ?
		if (!updateEstimatedTimeToComlete) {
			// Mise à jour des données
			result = contributionDAO.update(contribution);
		} else {
			// Récupération des éléments de la contribution
			Collaborator contributor = getCollaborator(contribution
					.getContributorId());
			Task task = getTask(contribution.getTaskId());
			// Récupération de la contribution correspondante en base
			Contribution[] contributions = contributionDAO.getContributions(contributor,
					task, contribution.getDate(), contribution.getDate());

			// Si la contribution n'existe pas, c'est qu'il y a
			// déphasage entre les données de l'appelant et la BDD
			verify("CONTRIBUTION_DELETION_DETECTED", contributions.length != 0); //$NON-NLS-1$

			long oldDuration = contributions[0].getDurationId();
			long newDuration = contribution.getDurationId();

			// Mise à jour de la contribution
			result = contributionDAO.update(contribution);

			// Mise à jour du RAF de la tache
			long newEtc = task.getTodo() + oldDuration - newDuration;
			task.setTodo(newEtc > 0 ? newEtc : 0);
			taskDAO.update(task);
			
		}

		// Retour du résultat
		return result;
	}

	@Override
	public Contribution[] changeContributionTask(Contribution[] contributions,
			Task newContributionTask) throws ModelException {
		// La tache ne peut accepter une contribution que
		// si elle n'admet aucune sous-tache
		verify("A_TASK_WITH_SUBTASKS_CANNOT_ACCEPT_CONTRIBUTIONS", //$NON-NLS-1$
			getSubTasksCount(newContributionTask.getId()) == 0);

		// Mise à jour des identifiants de tâche
		for (int i = 0; i < contributions.length; i++) {
			Contribution contribution = contributions[i];
			contributionDAO.delete(contribution);
			contribution.setTaskId(newContributionTask.getId());
			contributionDAO.insert(contribution);
		}

		// Retour de la tache modifiée
		return contributions;
	}

	@Override
	public Duration updateDuration(Duration duration, Duration newDuration)
			throws ModelException {
		// Si la nouvelle durée est égale à l'ancienne, il n'y a rien
		// à faire de plus!...
		if (!newDuration.equals(duration)) {
			// Tentative de suppression de la durée
			removeDuration(duration);

			// Insertion de la nouvelle durée
			createDuration(newDuration);
		}
		// Retour de la tache modifiée
		return newDuration;
	}

	@Override
	public Task updateTask(Task task) throws ModelException {
		// Le chemin de la tache et son numéro ne doivent pas avoir changés
		// pour pouvoir invoquer cette méthode (la modification des
		// attributs
		// n'est autorisée que pour les champs autres que le chemin et le
		// numéro.
		verifyTaskPath(task);

		// Check sur l'unicité du code pour le chemin considéré
		assertUniquePath(getParentTask(task), task);

		// Mise à jour des données
		task = taskDAO.update(task);

		// Retour de la tache modifiée
		return task;
	}

	private Task getTask(String taskPath, byte taskNumber) {
		Task[] tasks = taskDAO.select(
				new String[] { PATH_ATTRIBUTE, "number" }, 
				new Object[] { taskPath, taskNumber }, 
				null, -1);
		return tasks.length > 0 ? tasks[0] : null;
	}

	@Override
	public Task[] getTasks(long[] tasksIds) {
		if (tasksIds == null || tasksIds.length == 0) {
			return new Task[0];
		}
		
		// The task id array is cut in sub arrays of maximum 250 tasks
		List<Task> result = new ArrayList<Task>(tasksIds.length);
		for (int i = 0; i < tasksIds.length; i += 250) {
			Object[] subArray = new Object[Math.min(250, tasksIds.length - i)];
			for (int j = 0; j < subArray.length; j++) {
				subArray[j] = tasksIds[i + j];
			}
			
			Task[] tasks = taskDAO.select(new String[] { "id" }, 
					new Object[] { new InStatement(subArray) }, 
					new Object[] { new AscendantOrderByClause("number") }, -1);
			result.addAll(Arrays.asList(tasks));
		}

		
		// Retour du résultat
		return result.toArray(Task[]::new);
	}

	private static final Comparator<Task> SORTER_BY_PATH = (Task t1, Task t2) -> {
		String path1 = t1.getFullPath();
		String path2 = t2.getFullPath();
		int len = Math.min(path1.length(), path2.length());
		int c = path1.substring(0, len).compareTo(path2.substring(0, len));
		return (c != 0) 
				? c
				: path1.length() - path2.length();
	};
	
	
	@Override
	public byte[] exportToExcel(Long parentTaskId) throws IOException, ModelException {
		try (Workbook wbk = new HSSFWorkbook()) {
	
			Sheet sheet = wbk.createSheet();
			sheet.createFreezePane(0, 1);
			// Header style
			CellStyle headerCellStyle = wbk.createCellStyle();
			headerCellStyle.setFillForegroundColor(HSSFColorPredefined.GREY_25_PERCENT.getIndex());
			headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
			headerCellStyle.setBorderBottom(BorderStyle.THIN);
			headerCellStyle.setBorderLeft(headerCellStyle.getBorderBottom());
			headerCellStyle.setBorderRight(headerCellStyle.getBorderBottom());
			headerCellStyle.setBorderTop(headerCellStyle.getBorderBottom());
			// Header
			Row header = sheet.createRow(0);
			int idx = 0;
			for (String columnName : new String[] { 
					PATH_ATTRIBUTE, 
					CODE_ATTRIBUTE, 
					"name", 
					BUDGET_ATTRIBUTE, 
					"initiallyConsumed", 
					"todo", 
					"comment" }) {
				Cell cell = header.createCell(idx++);
				cell.setCellStyle(headerCellStyle);
				cell.setCellValue(columnName);
			}
			// Retrieve tasks
			Task parentTask = parentTaskId != null ? getTask(parentTaskId) : null;
			Task[] tasks = null;
			if (parentTaskId == null) {
				tasks = taskDAO.selectAll();
			} else {
				tasks = taskDAO.select(
						new String[] { PATH_ATTRIBUTE }, 
						new Object[] { new LikeStatement(parentTask.getFullPath() + "%") }, 
						null, -1);
			}
			// Sort
			Arrays.sort(tasks, SORTER_BY_PATH);
			
			// Output
			CellStyle bodyCellStyle = wbk.createCellStyle();
			bodyCellStyle.setBorderBottom(BorderStyle.THIN);
			bodyCellStyle.setBorderLeft(bodyCellStyle.getBorderBottom());
			bodyCellStyle.setBorderRight(bodyCellStyle.getBorderBottom());
			bodyCellStyle.setBorderTop(bodyCellStyle.getBorderBottom());
			Map<String, String> pathToTaskCodePathMap = new HashMap<String, String>();
			for (Task task : tasks) {
				
				// Compute task code path
				String taskPath = task.getPath();
				String parentTaskCodePath = pathToTaskCodePathMap.get(taskPath);
				String taskCodePath = (parentTaskCodePath != null ? parentTaskCodePath + '/' : "") + task.getCode();
				pathToTaskCodePathMap.put(task.getFullPath(), taskCodePath);
				
				// Append row
				Row row = sheet.createRow(sheet.getLastRowNum() + 1);
				idx = 0;
				for (Object v : new Object[] {
						(parentTaskCodePath != null ? parentTaskCodePath : ""),
						task.getCode(), 
						task.getName(), 
						task.getBudget(),
						task.getInitiallyConsumed(), 
						task.getTodo(),
						task.getComment() }) {
					Cell cell = row.createCell(idx++);
					if (v != null) {
						if (v instanceof String) {
							cell.setCellValue((String)v);
						} else {
							cell.setCellValue(((Long) v)/100d);
						}
					}
					cell.setCellStyle(bodyCellStyle);
				}
			}
			for (int colIdx = 0 ; colIdx <= 6; colIdx++) {
				sheet.autoSizeColumn(colIdx);
			}
			// Save the resource
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			wbk.write(out);
			return out.toByteArray();
		}
	}
	
	private static void setAttributeValue(Task task, XLSCell cell, boolean numeric) throws ModelException {
		try {
			Object value = cell.getValue();
			if (value != null) {
				if (numeric) {
					value = StringHelper.entryToHundredth(String.valueOf(value));
				}
				BeanUtilsBean2.getInstance().setProperty(task, cell.getColumnName(), value);
			}
		} catch (StringFormatException e) {
			throw new XLSModelException(cell.getCell(), "bad format (" + e.getMessage() + ")");
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new XLSModelException(cell.getCell(), "invalid content (" + e.getMessage() + ")");
		}
	}
	
	@Override
	public void importFromExcel(Long parentTaskId, InputStream xls) throws IOException, ModelException {
		final List<String> numericFieldNames = Arrays.asList(BUDGET_ATTRIBUTE, "initiallyConsumed", "todo");
		final TaskDAOCache taskCache = new TaskDAOCache(taskDAO);
		final String parentTaskCodePath = parentTaskId == null ? "" : taskCache.getCodePath(parentTaskId);
		XlsImportHelper.visit(xls, cells -> {
				
			if (!cells.containsKey(CODE_ATTRIBUTE)) {
				throw new ModelException("Sheet must contain a code column");
			}
			
			// Process other rows
			String theParentTaskCodePath = parentTaskCodePath;
			boolean allColumnsAreNull = true;
			Task newTask = factory.newTask();
			for (String columnName : cells.keySet()) {
				XLSCell xlsCell = cells.get(columnName);
				Object value = xlsCell.getValue();
				allColumnsAreNull &= (value == null || "".equals(String.valueOf(value).trim()));
				if (PATH_ATTRIBUTE.equals(columnName)) {
					if (value != null) {
						allColumnsAreNull = false;
						String relativePath = String.valueOf(value);
						if (!relativePath.isEmpty() && !relativePath.startsWith("/")) {
							relativePath = "/" + relativePath;
						}
						theParentTaskCodePath += relativePath;
					}
				} else {
					boolean numeric = numericFieldNames.contains(columnName);
					setAttributeValue(newTask, xlsCell, numeric);
				}
			}
			
			// Create task
			Task parentTask = taskCache.getByCodePath(theParentTaskCodePath);
			if (!"".equals(theParentTaskCodePath) && parentTask == null) {
				throw new ModelException("Unknown task path '" + theParentTaskCodePath + "'");
			}
			if (!allColumnsAreNull) {
				createTask(parentTask, newTask);
			}
		});
	}

	@Override
	public Collection<Integer> getContributionYears() {
		return contributionDAO.getContributionYears();
	}

	@Override
	public Report buildReport(Calendar start, ReportIntervalType intervalType,
			Integer intervalCount, Long rootTaskId, int taskDepth,
			boolean onlyKeepTasksWithContributions, boolean byContributor,
			boolean contributorCentricMode, long[] contributorIds)
			throws ModelException {

		return doBuildReport(start, intervalType, intervalCount,
				rootTaskId, taskDepth, onlyKeepTasksWithContributions,
				byContributor, contributorCentricMode, contributorIds,
				null, -1, false);

	}

	/**
	 * 
	 * @param maxIntervalCount
	 *            helps to detect EXCEL overlfows (when used in an EXCEL
	 *            context).
	 * @throws ColumnsOverflowException
	 *             throw if the max columns count is overflowed.
	 */
	private Report doBuildReport(Calendar start, ReportIntervalType intervalType,
			Integer intervalCount, Long rootTaskId, int taskDepth,
			boolean onlyKeepTasksWithContributions, boolean byContributor,
			boolean contributorCentricMode, long[] contributorIds,
			String[] orderContributorsBy, int maxIntervalCount, boolean dryRun)
			throws ModelException {
		
		// Fix task depth
		if (taskDepth < 0) {
			taskDepth = getMaxTaskDepthUnder(rootTaskId);
		}
		
		// If start date is omitted, compute a date
		Task rootTask = rootTaskId != null ? taskDAO.selectByPK(rootTaskId) : null;
		
		// If no start is given or no interval count is given, the existing database interval
		// is considered
		Calendar[] interval = null;
		if (start == null || intervalCount == null) {
			interval = contributionDAO.getContributionsInterval(rootTask != null ? rootTask.getFullPath() : null);
			// If no interval is found and if no start has been given, it not possible to continue
			if (start == null && interval == null) {
				throw new ModelException("No contributions found");
			}
		}

		// Ensure we have a start
		if (start == null) {
			start = interval[0];
		}
		prepareCalendarForReport(start, intervalType);

		// Ensure we have an interval count
		if (intervalCount == null) {
			Calendar end = interval != null 
					? interval[1] 
					: Calendar.getInstance();
			prepareCalendarForReport(end, intervalType);
			end.add(intervalType.getIntType(), 1);
			
			// Compute the delay
			switch (intervalType) {
			case YEAR :
				intervalCount = end.get(Calendar.YEAR) - start.get(Calendar.YEAR);
				break;
			case MONTH:
				intervalCount = (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 + (end.get(Calendar.MONTH) - start.get(Calendar.MONTH));
				break;
			case WEEK:
				intervalCount = DateHelper.countDaysBetween(start, end) / 7;
				break;
			case DAY:
				intervalCount = DateHelper.countDaysBetween(start, end);
			}
		} else if (intervalCount < 0) {
			throw new ModelException("Invalid interval (negative value is not allowed)");
		} else if (intervalCount == 0) {
			throw new ModelException("Invalid interval (null value is not allowed)");
		}
		
		// Column count computation
		if (maxIntervalCount > 0 && intervalCount >= maxIntervalCount) {
			throw new ModelException("Too many generated columns :"
					+ intervalCount
					+ " (Try to change your report interval type)");
		}
		
		// Compute the report
		if (dryRun) {
			return null;
		}
		return reportDAO.buildReport(start, intervalType, intervalCount,
				rootTask, taskDepth, onlyKeepTasksWithContributions,
				byContributor, contributorCentricMode, contributorIds,
				orderContributorsBy);
	}

	@Override
	public Workbook buildReport(Calendar start,
			ReportIntervalType intervalType, Integer intervalCount,
			Long rootTaskId, int taskDepth,
			boolean onlyKeepTasksWithContributions, boolean byContributor,
			boolean contributorCentricMode, long[] contributorIds, 
			String[] columnIds, 
			boolean includeTotals, boolean dryRun)
			throws ModelException {
		Map<String, IReportColumnComputer> reportColumnComputers = new HashMap<String, IReportColumnComputer>(defaultReportColumnComputers); 
		List<IReportColumnComputer> columns = new ArrayList<IReportColumnComputer>();
		int taskFields = 0;
		List<String> collaboratorFields = new ArrayList<String>();
		if (columnIds != null) {
			for (String columnId : columnIds) {
				IReportColumnComputer column = reportColumnComputers.get(columnId);
				if (column == null) {
					column = new ReflectiveReportColumnComputer(columnId);
					reportColumnComputers.put(columnId, column);
				}
				columns.add(column);
				if (columnId.startsWith(ReflectiveReportColumnComputer.TASK_PREFIX)) {
					taskFields++;
				} else if (columnId.startsWith(ReflectiveReportColumnComputer.COLLABORATOR_PREFIX)) {
					int skip = ReflectiveReportColumnComputer.COLLABORATOR_PREFIX.length();
					collaboratorFields.add(columnId.substring(skip));
				} else {
					throw new IllegalArgumentException("Unknown field type '" + columnId + "'");
				}
			}
		}
		// Check attributes to include
		verify("BAD_REPORT_PARAMS_EMPTY_TASK_ATTRIBUTES", taskDepth == 0 || taskFields != 0);

		// If collaborator is expected, must specify at least one collaborator attribute
		// (except if one and only one collaborator identifier has been given)
		verify("BAD_REPORT_PARAMS_EMPTY_COLLABORATOR_ATTRIBUTES", 
				!byContributor 
				|| !collaboratorFields.isEmpty()
				|| contributorIds == null 
				|| contributorIds.length == 1
				);

		return buildReport(
				start, intervalType, intervalCount,
				rootTaskId, taskDepth,
				onlyKeepTasksWithContributions, byContributor,
				contributorCentricMode, contributorIds,
				columns.toArray(IReportColumnComputer[]::new),
				collaboratorFields.toArray(String[]::new),
				includeTotals, dryRun);
	}

	@Override
	public Workbook buildReport(
			Calendar start, ReportIntervalType intervalType, Integer intervalCount,
			Long rootTaskId, int taskDepth,
			boolean onlyKeepTasksWithContributions, boolean byContributor,
			boolean contributorCentricMode, long[] contributorIds,
			IReportColumnComputer[] columns, String[] orderContributorsBy,
			boolean includeTotals, boolean dryRun) throws ModelException {

		// In Excel 97 format, it is not possible to have more than 256
		// columns
		int maxIntervalCount = 256 - columns.length - 1;
		// Build raw report
		Report report = doBuildReport(start, intervalType, intervalCount,
				rootTaskId, taskDepth, onlyKeepTasksWithContributions,
				byContributor, contributorCentricMode, contributorIds,
				orderContributorsBy, maxIntervalCount, dryRun);

	
		// Stop here if dry run mode
		if (dryRun) {
			return null;
		}

		// Convert report to XLS
		String dateFormat = null;
		switch (intervalType) {
		case YEAR:
			dateFormat = "yyyy";
			break;
		case MONTH:
			dateFormat = "MM/yy";
			break;
		case WEEK:
			dateFormat = "'W'ww/YY";
			break;
		case DAY:
			dateFormat = "dd/MM";
			break;
		}
		SimpleDateFormat xlsSdf = new SimpleDateFormat(dateFormat);
		WorkbookBuilder wb = new WorkbookBuilder();
		Workbook workbook = wb.getWorkbook();
		Sheet sheet = workbook.createSheet("Report");
		Row headerRow = sheet.createRow(sheet.getLastRowNum());
		int colIdx = 0;
		for (IReportColumnComputer column : columns) {
			wb.asHeaderCellStyl(headerRow.createCell(colIdx++))
					.setCellValue(column.getName());
		}

		Collection<Calendar> dates = report.getDates();
		for (Calendar date : dates) {
			String week = xlsSdf.format(date.getTime());
			wb.asHeaderCellStyl(headerRow.createCell(colIdx))
				.setCellValue(week);
			colIdx++;
		}
		if (includeTotals) {
			wb.asHeaderCellStyl(headerRow.createCell(colIdx))
				.setCellValue("Total");
		}

		long[] columnSums = new long[dates.size()];
		ReportItem lastItem = null;
		for (ReportItem item : report.getItems()) {
			TaskSums contributedTask = item.getContributedTask();
			Row row = sheet.createRow(sheet.getLastRowNum() + 1);
			colIdx = 0;
			for (IReportColumnComputer column : columns) {
				Cell cell = wb.asBodyCellStyl(row.createCell(colIdx++));
				// Summable fields must only appear once and not for each  occurence
				// For example if a task has 200 as budget, this value must not appear
				// for every collaborator that contributes to it

				boolean ignoreCell = column.isSummable() 
					&& lastItem != null
					&& contributedTask != null
					&& lastItem.getContributedTask().getTask().getId() 
						== contributedTask.getTask().getId();
				
				if (!ignoreCell) {
					Object cellValue = column.compute(item);
					if (cellValue instanceof String) {
						cell.setCellValue((String) cellValue);
					} else if (cellValue instanceof Double) {
						cell.setCellValue((Double) cellValue);
						wb.asBodyRightAlignmentCellStyl(cell);
					} else if (cellValue instanceof Boolean) {
						cell.setCellValue((Boolean) cellValue);
					} else if (cellValue != null) {
						throw new IllegalStateException(
								"Unexpected cell type : "
										+ cellValue.getClass());
					}
				}
			}
			long sum = 0;
			for (int i = 0; i < dates.size(); i++) {
				long contributionSum = item.getContributionSum(i);
				Cell cell = wb.asBodyRightAlignmentCellStyl(row.createCell(colIdx++));
				if (contributionSum > 0) {
					sum += contributionSum;
					columnSums[i] += contributionSum;
					cell.setCellValue(contributionSum / 100d);
				}
			}
			if (includeTotals) {
				wb.asFooterCellStyle(row.createCell(colIdx++))
					.setCellValue(sum / 100d);
			}
			lastItem = item;
		}

		// Footer
		if (includeTotals) {
			colIdx = columns.length;
			Row row = sheet.createRow(sheet.getLastRowNum() + 1);
			long globalSum = 0;
			for (int i = 0; i < dates.size(); i++) {
				long columnSum = columnSums[i];
				wb.asFooterCellStyle(row.createCell(colIdx++))
					.setCellValue(columnSum / 100d);
				globalSum += columnSum;
			}
			wb.asFooterCellStyle(row.createCell(colIdx++))
				.setCellValue(globalSum / 100d);
		}

		// Autosize code & name columns
		colIdx = 0;
		for (@SuppressWarnings("unused")
		IReportColumnComputer column : columns) {
			sheet.autoSizeColumn(colIdx++);
		}
		// Freeze
		sheet.createFreezePane(colIdx, 1);
		for (@SuppressWarnings("unused")
		Calendar date : dates) {
			sheet.setColumnWidth(colIdx, 1900);
			colIdx++;
		}
	
		return workbook;
	}
	
	private void prepareCalendarForReport(Calendar start, ReportIntervalType intervalType) {
		switch (intervalType) {
		case YEAR :
			start.set(Calendar.MONTH, 0);
			start.set(Calendar.DATE, 1);
			break;
		case MONTH:
			start.set(Calendar.DATE, 1);
			break;
		case WEEK:
			start = DateHelper.moveToFirstDayOfWeek(start);
			break;
		case DAY:
		}

		start.set(Calendar.HOUR_OF_DAY, 12);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
	}

	@Override
	public int getMaxTaskDepthUnder(Long rootTaskId) throws DAOException {
		String path = "";
		if (rootTaskId != null) {
			Task rootTask = getTask(rootTaskId);
			path = rootTask.getFullPath();
		}
		// If the root task Id is a leaf task, the SQL request returns 0 (as there are no child task)
		int maxTaskDepthUnder = taskDAO.getMaxTaskDepthUnder(path);
		return maxTaskDepthUnder > 0 
			? maxTaskDepthUnder - (path.length() / 2) 
			: 0;
	}

	@Override
	public Calendar[] getContributionsInterval(Long rootTaskId) {
		String rootTaskPath = null;
		if (rootTaskId != null) {
			Task rootTask = getTask(rootTaskId);
			if (rootTask != null) {
				rootTaskPath = rootTask.getFullPath();
			}
		}
		return contributionDAO.getContributionsInterval(rootTaskPath);
	}

	@Override
	public ReportCfg[] getReportCfgs(String category, Long ownerId)
			throws ModelException {
		checkReportCfgCategory(category);
		return reportCfgDAO.select(
					new String[] { "category", "ownerId" }, 
					new Object[] { category, ownerId }, 
					new Object[] { "name" }, -1);
	}

	/**
	 * Checks if a category is non null.
	 * 
	 * @param category
	 *            the category to check.
	 * @throws ModelException
	 *             if the category is null.
	 */
	private void checkReportCfgCategory(String category) throws ModelException {
		if (category == null) {
			throw new ModelException("Category cannot be null");
		}
	}

	@Override
	public ReportCfg createReportCfg(ReportCfg reportCfg) throws ModelException {
		checkReportCfgCategory(reportCfg.getCategory());
		if (reportCfg.getName() == null) {
			throw new ModelException("Name cannot be null");
		}
		return reportCfgDAO.insert(reportCfg);
	}

	@Override
	public void removeReportCfg(long id) {
		reportCfgDAO.deleteByPK(id);
	}

	@Override
	public void updateReportCfg(ReportCfg reportCfg) {
		reportCfgDAO.update(reportCfg);
	}

	@Override
	public ReportCfg getReportCfg(long id) {
		return reportCfgDAO.selectByPK(id);
	}

}

