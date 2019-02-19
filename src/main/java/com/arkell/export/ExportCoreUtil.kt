package com.arkell.export

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jboss.logging.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.io.File

@Service
class ExportCoreUtil(
		private val jdbcTemplate: JdbcTemplate) {

	private final val postgresInformationSchemaTable = "information_schema.tables"

	fun getTableList(): List<String> {
		val schemas = mutableMapOf<String, MutableSet<String>>()

		jdbcTemplate.query("select * from $postgresInformationSchemaTable", {
			schemas.getOrPut(it.getString("table_schema"), { mutableSetOf() }).add(
					it.getString("table_name")
			)
		})

		return schemas.filterValues { it.containsAll(listOf("partner", "offer", "place")) }.values.first().toList()
	}

	fun getTableHeaders(table: String): List<String> {

		val ret = mutableListOf<String>()

		jdbcTemplate.query("select * from $table limit 1", { rs ->
			for (i in 1..rs.metaData.columnCount) {
				ret.add(rs.metaData.getColumnName(i))
			}
		})

		return ret
	}

	fun getTableData(table: String, where: String? = null): List<List<String>> {
		val ret = mutableListOf<List<String>>()

		var sql = "select * from $table"

		if (where != null) {
			sql += " where $where"
		} else {
//			sql += " limit 100"
		}

		jdbcTemplate.query(sql, { rs ->
			// TODO Remove limit 100

			val line = mutableListOf<String>()

			for (i in 1..rs.metaData.columnCount) {
				line.add(rs.getObject(i)?.toString() ?: "") //rs.metaData.getColumnName(i)
			}

			ret.add(line)
		})

		return ret
	}

	fun writeToFile(file: File, data: List<Pair<String, List<List<String>>>>) {
		val workbook = XSSFWorkbook()

		if (file.exists()) {
			file.delete()
		}

		for ((tableName, values) in data) {
			val sheet = workbook.createSheet(tableName)

			for ((rowNum, line) in values.withIndex()) {
				val row = sheet.createRow(rowNum)

				for ((colNum, e) in line.withIndex()) {
					val cell = row.createCell(colNum)
					cell.setCellValue(e)
				}
			}
		}

		file.createNewFile()
		file.outputStream()
		workbook.also {
			it.write(file.outputStream())
			it.close()
		}

	}

	fun exportAdmins() {
		val file = File(System.getProperty("user.dir") + "/admins.xlsx")
		val table = getTableData("userentity", "accesstype > 1")
		writeToFile(file, listOf("admins" to table))
	}

	fun exportPlaces(filter: String? = null) {
		val file = File(System.getProperty("user.dir") + "/places.xlsx")
		val table = getTableData("place", filter?.let { "type = '$it'" })
		writeToFile(file, listOf("admins" to table))
	}

	fun fullExport(tables_: List<String>? = null) {
		val tables = tables_ ?: getTableList()
		val logger = Logger.getLogger(this::class.java)

		try {
			writeToFile(File(System.getProperty("user.dir") + "/backup.xlsx"), tables.map { table ->
				logger.info("export table $table")
				table to mutableListOf(getTableHeaders(table)).apply { addAll(getTableData(table)) }
			})
		} catch (e: Exception) {
			e.printStackTrace()
		}
		logger.info("Export competed.")
	}


}