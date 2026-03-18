package org.activitymgr.core.dto.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.activitymgr.core.orm.IConverter;
import org.activitymgr.core.util.StringHelper;

public class TaskNumberConverter implements IConverter<Short> {

	@Override
	public void bind(PreparedStatement stmt, int index, Short value)
			throws SQLException {
		stmt.setString(index, StringHelper.toHex2Chars(value));
	}

	@Override
	public Short readValue(ResultSet rs, int index) throws SQLException {
		return StringHelper.hexToNumber(rs.getString(index));
	}

	@Override
	public int getSQLType() {
		return Types.VARCHAR/*(2)*/; 
	}

}
