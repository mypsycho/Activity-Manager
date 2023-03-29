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
package org.activitymgr.core.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

public class WorkbookBuilder {
	
	private Workbook workbook = new HSSFWorkbook();
	private CellStyle headerCellStyle;
	private CellStyle bodyCellStyle;
	private CellStyle bodyRightAlignmentCellStyle;
	private CellStyle footerCellStyle;
	
	public WorkbookBuilder() {
		// Création du style des cellules
		bodyCellStyle = workbook.createCellStyle();
		bodyCellStyle.setBorderBottom(BorderStyle.THIN);
		bodyCellStyle.setBorderLeft(bodyCellStyle.getBorderBottom());
		bodyCellStyle.setBorderRight(bodyCellStyle.getBorderBottom());
		bodyCellStyle.setBorderTop(bodyCellStyle.getBorderBottom());

		// Création du style de l'entête
		headerCellStyle = workbook.createCellStyle();
		headerCellStyle.cloneStyleFrom(bodyCellStyle);
		headerCellStyle.setFillForegroundColor(HSSFColorPredefined.GREY_40_PERCENT.getIndex());
		headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerCellStyle.setAlignment(HorizontalAlignment.LEFT);

		// Création du style des cellules alignées à droite
		bodyRightAlignmentCellStyle = workbook.createCellStyle();
		bodyRightAlignmentCellStyle.cloneStyleFrom(bodyCellStyle);
		bodyRightAlignmentCellStyle.setAlignment(HorizontalAlignment.RIGHT);

		// Création du style des cellules alignées à gauche
		footerCellStyle = workbook.createCellStyle();
		footerCellStyle.cloneStyleFrom(bodyRightAlignmentCellStyle);
		footerCellStyle.setFillForegroundColor(HSSFColorPredefined.GREY_25_PERCENT.getIndex());
		footerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	}
	
	public Workbook getWorkbook() {
		return workbook;
	}
	
	public CellStyle getBodyCellStyle() {
		return bodyCellStyle;
	}
	
	public CellStyle getHeaderCellStyle() {
		return headerCellStyle;
	}

	public Cell asHeaderCellStyl(Cell cell) {
		cell.setCellStyle(headerCellStyle);
		return cell;
	}

	public Cell asBodyCellStyl(Cell cell) {
		cell.setCellStyle(bodyCellStyle);
		return cell;
	}

	public Cell asBodyRightAlignmentCellStyl(Cell cell) {
		cell.setCellStyle(bodyRightAlignmentCellStyle);
		return cell;
	}

	public Cell asFooterCellStyle(Cell cell) {
		cell.setCellStyle(footerCellStyle);
		return cell;
	}
}
