package org.activitymgr.core.impl.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.activitymgr.core.dao.AbstractORMDAOImpl;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.ICollaboratorDAO;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.apache.log4j.Logger;

public class CollaboratorDAOImpl extends AbstractORMDAOImpl<Collaborator> implements
		ICollaboratorDAO {

	@Override
	public Collaborator[] getContributors(Task task, Calendar fromDate,
			Calendar toDate) throws DAOException {
		// Préparation de la requête
		StringBuilder request = new StringBuilder("select distinct (ctb_contributor), ");
		request.append(getColumnNamesRequestFragment(null));
		request.append(" from CONTRIBUTION, COLLABORATOR");
		if (task != null) {
			request.append(", TASK");
		}
		request.append(" where ctb_contributor=clb_id");
		
		try(PreparedStatement pStmt = 
				buildIntervalRequest(request, null /*no user*/, 
						task, fromDate, toDate, false, "clb_login")) {

			ResultSet rs = pStmt.executeQuery();

			// Recherche des sous-taches
			List<Collaborator> list = new ArrayList<>();
			while (rs.next()) {
				list.add(read(rs, 2));
			}

			// Retour du résultat
			return list.toArray(Collaborator[]::new);
		} catch (SQLException e) {
			return critical(e, "TASK_SELECTION_BY_COLLABORATOR_FAILURE"); //$NON-NLS-1$
		}
	}

}