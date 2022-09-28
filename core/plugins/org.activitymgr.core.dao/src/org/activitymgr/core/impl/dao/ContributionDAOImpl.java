package org.activitymgr.core.impl.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.dao.AbstractORMDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.IContributionDAO;
import org.activitymgr.core.dao.IntervalRequestHelper;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskContributionsSums;

public class ContributionDAOImpl extends AbstractORMDAOImpl<Contribution> implements
		IContributionDAO {

	@Override
	public Contribution[] getContributions(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException {
		// Build the request
		String orderBy = "ctb_year, ctb_month, ctb_day, ctb_contributor";
		if (task != null) {
			orderBy += ", tsk_path, tsk_number";
		}
		
		try(PreparedStatement pStmt = buildContributionsRequest(task, contributor, 
					fromDate, toDate,
					getColumnNamesRequestFragment(null), orderBy)) {

			// Exécution de la requête
			ResultSet rs = pStmt.executeQuery();

			// Extraction du résultat
			List<Contribution> list = new ArrayList<Contribution>();
			while (rs.next()) {
				list.add(read(rs, 1));
			}
			return list.toArray(Contribution[]::new);

		} catch (SQLException e) {
			return critical(e, "CONTRIBUTIONS_SELECTION_FAILURE"); //$NON-NLS-1$
		}
	}


	@Override
	public long getContributionsSum(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException {
		try(PreparedStatement pStmt = buildContributionsRequest(task, contributor, 
				fromDate, toDate, 
				"sum(ctb_duration)", null)) { //$NON-NLS-1$
			return executeRequired(pStmt).getLong(1);
		} catch (SQLException e) {
			return critical(e, "CONTRIBUTIONS_SELECTION_FAILURE"); //$NON-NLS-1$
		}
	}


	@Override
	public int getContributionsCount(Collaborator contributor, Task task,
			Calendar fromDate, Calendar toDate) throws DAOException {
		try(PreparedStatement pStmt = buildContributionsRequest(task, contributor, 
				fromDate, toDate, 
				"count(ctb_duration)", null)) { //$NON-NLS-1$

			return executeRequired(pStmt).getInt(1);

		} catch (SQLException e) {
			return critical(e, "CONTRIBUTIONS_SELECTION_FAILURE");  //$NON-NLS-1$
		}
	}

	@Override
	public Map<Long, TaskContributionsSums> getTasksSums(Long taskId, String tasksPath, Calendar fromDate, Calendar toDate)
			throws DAOException {
		// At least one argument must be specified
		if (taskId != null && tasksPath != null) {
			throw new IllegalStateException("Both task Id and task path cannot be specified");
		}

		Map<Long, TaskContributionsSums> result = new HashMap<Long, TaskContributionsSums>();
		IntervalRequestHelper interval = new IntervalRequestHelper(fromDate, toDate);
		
		// Prepare the request
		StringBuilder request = new StringBuilder(
				"select pt.tsk_id, pt.tsk_number, sum(ctb_duration), count(ctb_duration) ");
		request.append("from TASK pt");
		request.append(" left join (TASK lt left join CONTRIBUTION on (ctb_task=lt.tsk_id");
		if (interval.hasIntervalCriteria()) {
			request.append(" and ");
			interval.appendIntervalCriteria(request);
		}
		request.append(")");
		
		request.append(") on (pt.tsk_id=lt.tsk_id or lt.tsk_path like concat(pt.tsk_path, pt.tsk_number, '%'))");
		request.append(" where ");
		request.append(taskId != null ? "pt.tsk_id" : "pt.tsk_path");
		request.append(" = ? group by pt.tsk_id order by pt.tsk_number");


		try(PreparedStatement pStmt = tx().prepareStatement(request.toString())) {

			int paramIdx = 1;
			if (interval.hasIntervalCriteria()) {
				paramIdx = interval.bindParameters(paramIdx, pStmt);
			}
			if (taskId != null) {
				pStmt.setLong(paramIdx++, taskId);
			} else {
				pStmt.setString(paramIdx++, tasksPath);
			}
			ResultSet rs = pStmt.executeQuery();

			// Retrieve the result
			while (rs.next()) {
				TaskContributionsSums sums = new TaskContributionsSums();
				sums.setTaskId(rs.getLong(1));
				sums.setConsumedSum(rs.getLong(3));
				sums.setContributionsNb(rs.getLong(4));
				result.put(sums.getTaskId(), sums);
			}

			
			// Return the result
			return result;
		} catch (SQLException e) {
			return critical(e, "TASK_SUMS_COMPUTATION_FAILURE",  //$NON-NLS-1$
					taskId != null ? taskId : tasksPath);
		}
	}
	/**
	 * Builds a request that selects contributions using a given task,
	 * contributor and date interval.
	 * 
	 * <p>
	 * All parameters are optional.
	 * </p>
	 * 
	 * @param task
	 *            a parent task of the contributions tasks.
	 * @param contributor
	 *            the contributor.
	 * @param fromDate
	 *            start date of the interval.
	 * @param toDate
	 *            end date of the interval.
	 * @param fieldsToSelect
	 *            fields to select.
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement buildContributionsRequest(Task task,
			Collaborator contributor, Calendar fromDate, Calendar toDate,
			String fieldsToSelect, String orderBy) throws SQLException {
		// Préparation de la requête
		StringBuilder request = new StringBuilder("select ")
				.append(fieldsToSelect)
				.append(" from CONTRIBUTION");
		if (task != null) {
			request.append(", TASK");
		}
		return buildIntervalRequest(request, contributor, task, fromDate,
				toDate, true, orderBy);
	}


	@Override
	public Collection<Integer> getContributionYears() {
		String query = "select distinct(ctb_year) as year from CONTRIBUTION order by year";
		try(PreparedStatement pStmt = tx().prepareStatement(query)) {

			// Exécution de le requête et extraction du résultat
			ResultSet rs = pStmt.executeQuery();
			
			Collection<Integer> years = new ArrayList<>();
			while (rs.next()) {
				years.add(rs.getInt(1));
			}

			return years;
		} catch (SQLException e) {
			return critical(e);
		}
	}
	

	@Override
	public Calendar[] getContributionsInterval(String taskPath) {
		boolean filterByTaskPath = taskPath != null && !"".equals(taskPath);
		// Build the SQL request
		String query = "select count(*), " //$NON-NLS-1$
				+ " min(ctb_year*10000+ctb_month*100+ctb_day)," //$NON-NLS-1$
				+ " max(ctb_year*10000+ctb_month*100+ctb_day)" //$NON-NLS-1$
				+ " from CONTRIBUTION"; //$NON-NLS-1$
		if (filterByTaskPath) {
			query += " join TASK on ctb_task=tsk_id where tsk_path like ? or concat(tsk_path, tsk_number)=?";
		}
		

		try(PreparedStatement pStmt = tx().prepareStatement(query)) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			
			if (filterByTaskPath) {
				pStmt.setString(1, taskPath + '%');
				pStmt.setString(2, taskPath);
			}
			// Exécution de le requête et extraction du résultat
			Calendar[] result = null;
			ResultSet rs = pStmt.executeQuery();
			if (rs.next()) {
				int contributionsCount = rs.getInt(1);
				// If there is no contribution, simply return null
				if (contributionsCount > 0) {
					
					// Else parse the result
					result = new Calendar[2];
					
					
					result[0] = new GregorianCalendar();
					result[0].setTime(sdf.parse(rs.getString(2)));
	
					result[1] = new GregorianCalendar();
					result[1].setTime(sdf.parse(rs.getString(3)));
				}
			}


			// Retour du résultat
			return result;
		} catch (SQLException | ParseException e) {
			return critical(e);
		}
	}

}