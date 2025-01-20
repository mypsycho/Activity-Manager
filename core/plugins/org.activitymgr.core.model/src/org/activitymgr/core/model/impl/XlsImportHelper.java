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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.model.XLSModelException;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Excel helper methods.
 * @author jbrazeau
 */
public class XlsImportHelper {
	
	public static class XLSCell {
		private final String columnName;
		private final Cell cell;
		private final Object value;
		public XLSCell(String columnName, Cell cell, Object value) {
			this.columnName = columnName;
			this.cell = cell;
			this.value = value;
		}
	
		public String getColumnName() {
			return columnName;
		}
		
		public Cell getCell() {
			return cell;
		}
		
		public Object getValue() {
			return value;
		}
	
	}
	
	public static interface IXLSHandler {
		
		void handleRow(Map<String, XLSCell> cells) throws ModelException;
		
	}

	public static <T> void visit(InputStream xls, IXLSHandler handler) throws IOException, ModelException {
		BeanUtilsBean.setInstance(new BeanUtilsBean2());
		
		try (Workbook wbk = new HSSFWorkbook(xls)) {
			if (wbk.getNumberOfSheets() == 0) {
				throw new ModelException("Workbook must contain at least one sheet");
			}
			Sheet sheet = wbk.getSheetAt(0);
			if (sheet.getLastRowNum() == 0) {
				throw new ModelException("Sheet must contain a header row and at least a content row");
			}
	
			// Process header row
			Map<Integer, String> columnNamesIndex = new LinkedHashMap<Integer, String>();
			Row headerRow = sheet.getRow(0);
			for (int i = 0; i<headerRow.getLastCellNum(); i++) {
				Cell cell = headerRow.getCell(i);
				if (cell != null) {
					String colmunName = cell.getStringCellValue().trim();
					if (!"".equals(colmunName)) {
						columnNamesIndex.put(i, colmunName);
					}
				}
			}
			
			// Process each row
			Map<String, XLSCell> map = new LinkedHashMap<String, XlsImportHelper.XLSCell>();
			for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
				map.clear();
				Row row = sheet.getRow(rowIdx);
				for (int colIdx : columnNamesIndex.keySet()) {
					String columnName = columnNamesIndex.get(colIdx);
					Cell cell = row.getCell(colIdx);
					if (cell != null) {
						Object value = null;
						// Retrieve type
						CellType cellType = cell.getCellType();
						if (cellType == CellType.FORMULA) {
							cellType = cell.getCachedFormulaResultType();
						}
						switch (cellType) {
						case BLANK :
						case _NONE :
						case FORMULA : // already interpreted
							// Do nothing
							break;
						case BOOLEAN :
							value = cell.getBooleanCellValue();
							break;
						case STRING :
							value = cell.getStringCellValue();
							break;
						case NUMERIC :
							value = cell.getNumericCellValue();
							break;
						case ERROR :
							throw new XLSModelException(cell, "Cell contains an error");
						}
						map.put(columnName, new XLSCell(columnName, cell, value));
					}
				}
				try {
					// Handle the row
					handler.handleRow(map);
				} catch (XLSModelException e) { // Simply rethrow
					throw e;
				} catch (ModelException e) {
					// Encapsulate and link to the first cell
					throw new XLSModelException(row.getCell(0), e);
				}
			}
		}
	}

}
