package org.activitymgr.core.impl.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activitymgr.core.dao.AbstractORMDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.ITaskDAO;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSearchFilter;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.util.StringHelper;
import org.apache.log4j.Logger;

/**
 * @author jbrazeau
 *
 */
public class TaskDAOImpl extends AbstractORMDAOImpl<Task> implements ITaskDAO {

	/** Logger */
	private static Logger log = Logger.getLogger(TaskDAOImpl.class);
	
	@Override
	public int getSubTasksCount(long parentTaskId) throws DAOException {
		
		String query = "select theTask.tsk_id, count(subTask.tsk_id)" //$NON-NLS-1$ 
				+ " from TASK as theTask" //$NON-NLS-1$ 
				+ " left join TASK as subTask" //$NON-NLS-1$ 
				+ " on subTask.tsk_path = concat(theTask.tsk_path, theTask.tsk_number)" //$NON-NLS-1$ 
				+ " where theTask.tsk_id=? group by theTask.tsk_id"; //$NON-NLS-1$ 
		try(PreparedStatement pStmt = tx().prepareStatement(query)) { 
			pStmt.setLong(1, parentTaskId);

			// Préparation du résultat
			return executeRequired(pStmt).getInt(2); //$NON-NLS-1$
		} catch (SQLException e) {
			return critical(e);
		}
	}

	@Override
	public Task[] getSubTasks(String parentTaskPath, String filter) {
		String query = "select distinct " + getColumnNamesRequestFragment("subtask")  //$NON-NLS-1$  //$NON-NLS-2$
			+ " from TASK as subtask" //$NON-NLS-1$
			+ "   inner join TASK filteredTask on (" //$NON-NLS-1$
			+ "     left(concat(filteredTask.tsk_path, filteredTask.tsk_number), length(subtask.tsk_path) + 2) " //$NON-NLS-1$
			+ "        = concat(subtask.tsk_path, subtask.tsk_number)" //$NON-NLS-1$
			+ "     or left(concat(subtask.tsk_path, subtask.tsk_number), length(filteredTask.tsk_path) + 2) " //$NON-NLS-1$
			+ "        = concat(filteredTask.tsk_path, filteredTask.tsk_number)" //$NON-NLS-1$
			+ " )" //$NON-NLS-1$
			+ " where" //$NON-NLS-1$
			+ "   subtask.tsk_path=?" //$NON-NLS-1$
			+ "   and (filteredTask.tsk_name like ? or filteredTask.tsk_code like ?)" //$NON-NLS-1$
			+ " order by subtask.tsk_number"; //$NON-NLS-1$

		try(PreparedStatement pStmt = tx().prepareStatement(query)) {
			
			// Request preparation
			pStmt.setString(1, parentTaskPath);
			String sqlFilter = "%" + filter + "%";
			pStmt.setString(2, sqlFilter);
			pStmt.setString(3, sqlFilter);

			// Exécution de la requête
			ResultSet rs = pStmt.executeQuery();

			// Préparation du résultat
			Collection<Task> result = new ArrayList<Task>();
			while (rs.next()) {
				result.add(read(rs, 1));
			}

			// Retour du résultat
			return (Task[]) result.toArray(new Task[result.size()]);
		} catch (SQLException e) {
			return critical(e);
		}
	}

	@Override
	public Task getFirstTaskMatching(String filter) {
		// select  distinct st.TSK_ID, st.TSK_PATH, st.TSK_NUMBER, st.TSK_CODE from task st inner join TASK t on left(concat(t.tsk_path, t.tsk_number), length(concat(st.tsk_path, st.tsk_number))) = concat(st.tsk_path, st.tsk_number) where st.tsk_path='01090304' and t.tsk_name like concat('%', 'CCAP', '%') order by st.tsk_number; 
		String query = "select " + getColumnNamesRequestFragment("t") //$NON-NLS-1$  //$NON-NLS-2$
			+ " from TASK as t" //$NON-NLS-1$
			+ " where t.tsk_name like ? or t.tsk_code like ?" //$NON-NLS-1$
			+ " order by t.tsk_path, t.tsk_number"; //$NON-NLS-1$
		try(PreparedStatement pStmt = tx().prepareStatement(query)) {

			// Request preparation
			String sqlFilter = "%" + filter + "%";  //$NON-NLS-1$  //$NON-NLS-2$
			pStmt.setString(1, sqlFilter);
			pStmt.setString(2, sqlFilter);

			// Exécution de la requête
			ResultSet rs = pStmt.executeQuery();

			return rs.next()?  read(rs, 1) : null;
		} catch (SQLException e) {
			return critical(e);
		}
	}

	@Override
	public long[] getTaskIds(TaskSearchFilter filter) throws DAOException {

		// Préparation de la requête
		
		// Ajout du nom de champ
		String field;
		switch (filter.getFieldIndex()) {
		case TaskSearchFilter.TASK_NAME_FIELD_IDX:
			field = "tsk_name"; //$NON-NLS-1$
			break;
		case TaskSearchFilter.TASK_CODE_FIELD_IDX:
			field = "tsk_code"; //$NON-NLS-1$
			break;
		default:
			return critical(null, "UNKNOWN_FIELD_INDEX", //$NON-NLS-1$
					filter.getFieldIndex());
		}
		// Ajout du critère de comparaison
		String compare;
		switch (filter.getCriteriaIndex()) {
		case TaskSearchFilter.IS_EQUAL_TO_CRITERIA_IDX:
			compare = "="; //$NON-NLS-1$
			break;
		case TaskSearchFilter.STARTS_WITH_CRITERIA_IDX:
		case TaskSearchFilter.ENDS_WITH_CRITERIA_IDX:
		case TaskSearchFilter.CONTAINS_CRITERIA_IDX:
			compare = "like"; //$NON-NLS-1$
			break;
		default:
			return critical(null, "UNKNOWN_CRITERIA_INDEX", //$NON-NLS-1$
					filter.getCriteriaIndex());
		}
		
		String query = "select tsk_id from TASK where "  //$NON-NLS-1$
				+ field + " " + compare + " ?"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		// Préparation de la requête
		log.debug("Search request: " + query); //$NON-NLS-1$
		
		try(PreparedStatement pStmt = tx().prepareStatement(query)) {

			String parameter = null;
			switch (filter.getCriteriaIndex()) {
			case TaskSearchFilter.IS_EQUAL_TO_CRITERIA_IDX:
				parameter = filter.getFieldValue();
				break;
			case TaskSearchFilter.STARTS_WITH_CRITERIA_IDX:
				parameter = filter.getFieldValue() + "%"; //$NON-NLS-1$
				break;
			case TaskSearchFilter.ENDS_WITH_CRITERIA_IDX:
				parameter = "%" + filter.getFieldValue(); //$NON-NLS-1$
				break;
			case TaskSearchFilter.CONTAINS_CRITERIA_IDX:
				parameter = "%" + filter.getFieldValue() + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				break;
			default:
				return critical(null, "UNKNOWN_CRITERIA_INDEX", //$NON-NLS-1$
						filter.getCriteriaIndex());
			}
			log.debug("Search parameter: '" + parameter + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			pStmt.setString(1, parameter);

			// Exécution de la requête
			ResultSet rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Long> list = new ArrayList<Long>();
			while (rs.next()) {
				list.add(rs.getLong(1));
			}

			// Préparation du résultat
			long[] taskIds = new long[list.size()];
			for (int i = 0; i < taskIds.length; i++) {
				taskIds[i] = list.get(i);
			}
			return taskIds;
		} catch (SQLException e) {
			return critical(e, "TASKS_SELECTION_FAILURE");
		}
	}

	@Override
	public long[] getContributedTaskIds(Collaborator contributor, Calendar fromDate,
			Calendar toDate) throws DAOException {
		StringBuilder request = new StringBuilder(
				"select distinct ctb_task, tsk_path, tsk_number" //$NON-NLS-1$
				+ " from CONTRIBUTION, TASK where ctb_task=tsk_id");  //$NON-NLS-1$

		try(PreparedStatement pStmt = buildIntervalRequest(
				request, contributor, null /* no task */,
				fromDate, toDate, false, "tsk_path, tsk_number")) {
			// Préparation de la requête

			// Exécution de la requête
			ResultSet rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Long> list = new ArrayList<Long>();
			while (rs.next()) {
				list.add(rs.getLong(1));
			}
			
			// Retour du résultat
			long[] taskIds = new long[list.size()];
			for (int i = 0; i < taskIds.length; i++) {
				taskIds[i] = list.get(i);
			}
			return taskIds;
		} catch (SQLException e) {
			return critical(e, "TASK_SELECTION_BY_COLLABORATOR_FAILURE");
		}
	}


	@Override
	public byte newTaskNumber(String path) throws DAOException {

		// Recherche du max
		try(PreparedStatement pStmt = tx().prepareStatement(
				"select max(tsk_number) from TASK where tsk_path=?")) { //$NON-NLS-1$
			
			pStmt.setString(1, path);
			String maxStr = executeRequired(pStmt).getString(1);
			byte max = maxStr != null ? StringHelper.toByte(maxStr) : 0;
			log.debug("  => max= : " + max); //$NON-NLS-1$

			// Retour du résultat
			return (byte) (max + 1);
		} catch (SQLException e) {
			return critical(e, "TASK_NUMBER_COMPUTATION_FAILURE"); //$NON-NLS-1$
		}
	}

	@Override
	public List<TaskSums> getTasksSums(Long taskId, String tasksPath) throws DAOException {
		if (taskId != null && tasksPath != null) {
			throw new IllegalStateException("Both task Id and task path cannot be specified");
		}

		/* Budget, initialy consummed, etc sums computation 
		 * (all what is independant from contributions) */
		StringBuilder request = new StringBuilder("select")
				.append(" sum(leaftask.tsk_budget),")
				.append(" sum(leaftask.tsk_initial_cons),")
				.append(" sum(leaftask.tsk_todo),")
				.append(" count(leaftask.tsk_id), ")
				.append(getColumnNamesRequestFragment("maintask"))
				.append(" from TASK maintask, TASK leaftask ")
				.append("where ");
		
		if (taskId != null) { // Task id case
			request.append("maintask.tsk_id=?");
		} else if (tasksPath != null) { // Task path case
			request.append("maintask.tsk_path=?");
		}
		
		// XXX should be left join
		request.append(" and (maintask.tsk_id=leaftask.tsk_id or leaftask.tsk_path like concat(maintask.tsk_path, maintask.tsk_number, '%'))")
			.append(" group by maintask.tsk_id ")
			.append(" order by maintask.tsk_number");
		
		try(PreparedStatement pStmt = tx().prepareStatement(request.toString())) {
			
			if (taskId != null) {
				pStmt.setLong(1, taskId);
			} else if (tasksPath != null) {
				pStmt.setString(1, tasksPath);
			}
			ResultSet rs = pStmt.executeQuery();
			
			List<TaskSums> result = new ArrayList<TaskSums>();
			while (rs.next()) {
				TaskSums sums = new TaskSums();
				sums.setBudgetSum(rs.getLong(1));
				sums.setInitiallyConsumedSum(rs.getLong(2));
				sums.setTodoSum(rs.getLong(3));
				sums.setLeaf(rs.getLong(4) == 1);
				Task task = read(rs, 5);
				sums.setTask(task);
				result.add(sums);
			}
			
			return result;
		} catch (SQLException e) {
			return critical(e, "TASK_SUMS_COMPUTATION_FAILURE", //$NON-NLS-1$
					taskId != null ? taskId : tasksPath);
		}
	}

	
	@Override
	public int getMaxTaskDepthUnder(String path) throws DAOException {
		try(PreparedStatement pStmt = tx().prepareStatement(
				"select (max(length(tsk_path))/2+1) from TASK where tsk_path like ?")) {
			// Préparation de la requête
			pStmt.setString(1, path + "%");
			
			return executeRequired(pStmt).getInt(1);

		} catch (SQLException e) {
			return critical(e, "TASK_MAX_PATH_DEPTH_RETRIEVAL"); //$NON-NLS-1$
		}
	}
	
	
	protected static String[] getAllPaths(String path) {
		String[] result = new String[path.length()/2];
		for (int i = 0; i < result.length; i++) {
			result[i] = path.substring(0, path.length() - i*2);
		}
		return result;
	}
	
	@Override
	public Map<String, Task> getAllParents(Collection<Task> tasks) throws DAOException {
		if (tasks.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, Task> result = tasks.stream()
				.collect(Collectors.toMap(it -> it.getFullPath(), it -> it));
		Set<String> expecteds = tasks.stream()
				.flatMap(it -> Stream.of(getAllPaths(it.getPath())))
				.filter(it -> !result.containsKey(it))
				.collect(Collectors.toSet());
		if (expecteds.isEmpty()) {
			return result;
		}
		
		String query = "select " + getColumnNamesRequestFragment("t")
				+ " from TASK as t"
				+ " where concat(t.tsk_path, t.tsk_number) in ("
				+ expecteds.stream().map(it -> "?").collect(Collectors.joining(","))
				+ ")";
		
		try(PreparedStatement pStmt = tx().prepareStatement(query)) {
			int pIndex = 1;
			for (String path: expecteds) {
				pStmt.setString(pIndex++, path);
			}
			
			ResultSet rs = pStmt.executeQuery();
			while (rs.next()) {
				Task task = read(rs, 1);
				result.put(task.getFullPath(), task);
			}
			
			return result;
		} catch (SQLException e) {
			return critical(e, "TASKS_SELECTION_FAILURE"); //$NON-NLS-1$
		}
	}

}