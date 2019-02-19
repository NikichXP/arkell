package com.arkell.util

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream

class SheetsModelProxy(val workbook: XSSFWorkbook) {

	constructor(file: File) : this(XSSFWorkbook(FileInputStream(file)))

	fun getSheet(index: Int): List<List<String>> {
		val mySheet: XSSFSheet = workbook.getSheetAt(index); // Get iterator to all the rows in current sheet
		val rowIterator: Iterator<Row> = mySheet.iterator(); // Traversing over each row of XLSX file

		var table = mutableListOf<MutableList<String>>()

		while (rowIterator.hasNext()) {
			val cellIterator = rowIterator.next().cellIterator() // For each row, iterate through each columns
			val row = mutableListOf<String>()
			while (cellIterator.hasNext()) {
				row.add(cellIterator.next().let {
					return@let when (it.cellType) {
						Cell.CELL_TYPE_STRING -> it.stringCellValue
						Cell.CELL_TYPE_NUMERIC -> it.numericCellValue.let {
							if (it % 1 == 0.0) {
								it.toInt().toString()
							} else {
								it.toString()
							}
						}
						Cell.CELL_TYPE_BOOLEAN -> it.booleanCellValue.toString()
						else -> it.toString()
					}
				}.trim())
			}
			table.add(row)
		}
		table = table.drop(1).toMutableList()
		return table
	}
}