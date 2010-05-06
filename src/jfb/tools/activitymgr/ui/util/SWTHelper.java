/*
 * Copyright (c) 2004, Jean-Fran�ois Brazeau. All rights reserved.
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
package jfb.tools.activitymgr.ui.util;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Classe offrant des services d'aide � l'utilisation de l'API SWT.
 */
public class SWTHelper {

	/** Logger */
	private static Logger log = Logger.getLogger(SWTHelper.class);

	/**
	 * Centre un popup par rapport � sa fen�tre parent.
	 * @param popupShell le popup.
	 */
	public static void centerPopup(Shell popupShell) {
		// D�finition de la positio du popup
		Point parentShellLocation = popupShell.getParent().getLocation();
		Point parentShellSize = popupShell.getParent().getSize();
		Point popupShellSize = popupShell.getSize();
		log.debug("parentShellSize = " + parentShellSize);
		log.debug("popupShellSize = " + popupShellSize);
		int x = parentShellLocation.x + (parentShellSize.x - popupShellSize.x)
				/ 2;
		int y = parentShellLocation.y + (parentShellSize.y - popupShellSize.y)
				/ 2;
		if (x < 0)
			x = 0;
		if (y < 0)
			y = 0;
		log.debug("x = " + x);
		log.debug("y = " + y);
		popupShell.setLocation(x, y);
		popupShell.setVisible(true);
	}

	/**
	 * Exporte un arbre SWT en fichier EXCEL.
	 * @param tree l'arbre � exporter.
	 * @throws UITechException lev� en cas de pb I/O lors de la sauvegarde du fichier EXCEL.
	 */
	public static void exportToWorkBook(Tree tree) throws UITechException{
		// Demande du nom de fichier
		FileDialog fd = new FileDialog(tree.getShell());
		fd.setFilterExtensions(new String[] { "*.xls", "*" });
		String fileName = fd.open();
		// Si le nom est sp�cifi�
		if (fileName != null) {
			try {
				HSSFWorkbook wb = toWorkBook(tree);
				FileOutputStream out = new FileOutputStream(fileName);
				wb.write(out);
				out.close();
			}
			catch (IOException e) {
				log.error("I/O exception", e);
				throw new UITechException("I/O exception while exporting.", e);
			}
		}
	}

	/**
	 * Convertit un arbre en classeur EXCEL.
	 * @param tree l'arbre � convertir.
	 * @return le classeur EXCEL.
	 */
	public static HSSFWorkbook toWorkBook(Tree tree) {
	    // Cr�ation du fichier EXCEL et du feuillet
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("export");
		sheet.createFreezePane(0, 1, 0, 1);
		sheet.setColumnWidth((short) 0, (short) 10000);
		// Cr�ation du style de l'ent�te
		HSSFCellStyle headerCellStyle = wb.createCellStyle();
		headerCellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		headerCellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		headerCellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		headerCellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		headerCellStyle.setBorderLeft(headerCellStyle.getBorderBottom());
		headerCellStyle.setBorderRight(headerCellStyle.getBorderBottom());
		headerCellStyle.setBorderTop(headerCellStyle.getBorderBottom());
		// Cr�ation de l'ent�te
		HSSFRow row = sheet.createRow((short) 0);
		TreeColumn[] columns = tree.getColumns();
		for (int i = 0; i < columns.length; i++) {
			TreeColumn column = columns[i];
			HSSFCell cell = row.createCell((short) i);
			cell.setCellValue(column.getText());
			cell.setCellStyle(headerCellStyle);
		}
		// Cr�ation du style des cellules
		HSSFCellStyle bodyCellStyle = wb.createCellStyle();
		bodyCellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		bodyCellStyle.setBorderLeft(bodyCellStyle.getBorderBottom());
		bodyCellStyle.setBorderRight(bodyCellStyle.getBorderBottom());
		bodyCellStyle.setBorderTop(bodyCellStyle.getBorderBottom());
		// Exportation des lignes du tableau
		TreeItem[] items = tree.getItems();
		appendToWorkbook("", sheet, bodyCellStyle, items, columns.length);
		// Retour du r�sultat
		return wb;
	}

	/**
	 * G�n�re des lignes dans le classeur EXCEL r�cursivement pour les �l�ments
	 * d'arbres sp�cifi�s et leurs �l�ments fils.
	 * @param indent l'indentiation � appliquer (plus la profondeur dans l'arbre
	 *    est �lev�e, plus l'indentation est longue).
	 * @param sheet le feuillet EXCEL.
	 * @param cellStyle le style de la cellule.
	 * @param treeItems les �lements.
	 * @param columnsNb le nombre de colonnes � exporter dans le feuillet. 
	 */
	private static void appendToWorkbook(String indent, HSSFSheet sheet, HSSFCellStyle cellStyle, TreeItem[] treeItems, int columnsNb) {
		log.debug("sheet.getLastRowNum() : " + sheet.getLastRowNum());
		int startRowNum = sheet.getLastRowNum() + 1;
		for (int i = 0; i<treeItems.length; i++) {
			TreeItem treeItem = treeItems[i];
			log.debug(" +-> TreeItem : " + i + ", expanded=" + 	treeItem.getExpanded() + ", data='" + treeItem.getData() + "'");
			if (treeItem.getData()!=null) {
				HSSFRow row = sheet.createRow((short) (sheet.getLastRowNum()+1));
				String rowName = treeItem.getText(0);
				log.debug("  +-> Row : '" + rowName + "'");
				HSSFCell cell = row.createCell((short) 0);
				cell.setCellValue(indent + rowName);
				cell.setCellStyle(cellStyle);
				for (int j=1; j<columnsNb; j++) {
					log.debug("  +-> Cell : " + j + ", '" + treeItem.getText(j) + "'");
					cell = row.createCell((short) j);
					cell.setCellStyle(cellStyle);
					String cellValue = treeItem.getText(j);
					try { cell.setCellValue(Integer.parseInt(cellValue)); }
					catch (NumberFormatException e0) {
						try { cell.setCellValue(Double.parseDouble(cellValue)); }
						catch (NumberFormatException e1) {
							cell.setCellValue(cellValue);
						}
					}
				}
				if (treeItem.getExpanded())
					appendToWorkbook(indent + "    ", sheet, cellStyle, treeItem.getItems(), columnsNb);
			}
		}
		int endRowNum = sheet.getLastRowNum();
		log.debug("startRowNum=" + startRowNum + ", endRowNum=" + endRowNum);
		if (!"".equals(indent) && (endRowNum-startRowNum>=1)) {
			log.debug(" -> grouped!");
			sheet.groupRow((short) startRowNum, (short) endRowNum);
		}
	}

}
