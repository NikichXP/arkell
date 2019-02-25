package com.arkell.api.admin

import com.arkell.entity.*
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.export.ExportCoreUtil
import com.arkell.model.MailTextsService
import com.arkell.util.getParamData
import com.google.api.client.util.IOUtils
import kotlinx.coroutines.experimental.launch
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Auth(value = AuthPermission.ADMIN)
@RestController
@RequestMapping("/api/admin/misc")
class AdminMiscAPI(
		private val exportCoreUtil: ExportCoreUtil,
		private val mailTextsService: MailTextsService) {

	/**
	 * Starts backup. Doesn't return data!
	 * @param tables is tables to put in xlsx-file. if not present - saves all the tables.
	 */
	@PostMapping("/backup/start")
	fun backupStart(@RequestParam tables: List<String>?): String {
		launch { exportCoreUtil.fullExport(tables) }
		return "Export started"
	}

	@GetMapping("/backup/tables")
	fun listBackupTables(): List<String> {
		return exportCoreUtil.getTableList()
	}

	/**
	 * Gets the last created backup. Currently in progress.
	 */
	@GetMapping("/backup/result")
	fun getBackupResult(response: HttpServletResponse) {
		response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
		response.setHeader("Content-disposition", "attachment; filename=backup-${System.currentTimeMillis()}.xlsx")
		response.addHeader("Access-Control-Allow-Methods", "GET")
		response.addHeader("Access-Control-Allow-Credentials", "*")
		response.addHeader("Access-Control-Max-Age", "3600")
		response.addHeader("Access-Control-Allow-Headers", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")

		IOUtils.copy(File(System.getProperty("user.dir") + "/backup.xlsx").inputStream(), response.outputStream)
	}

	@GetMapping("/backup/admins")
	fun backupAdmins(response: HttpServletResponse) {
		exportCoreUtil.exportAdmins()
		response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
		response.setHeader("Content-disposition", "attachment; filename=backup-${System.currentTimeMillis()}.xlsx")
		response.addHeader("Access-Control-Allow-Methods", "GET")
		response.addHeader("Access-Control-Allow-Credentials", "*")
		response.addHeader("Access-Control-Max-Age", "3600")
		response.addHeader("Access-Control-Allow-Headers", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")

		IOUtils.copy(File(System.getProperty("user.dir") + "/admins.xlsx").inputStream(), response.outputStream)
	}

	/**
	 * Backup places. Criteria is metro/mall
	 */
	@GetMapping("/backup/places")
	fun backupPlaces(@RequestParam type: String?, response: HttpServletResponse) {
		exportCoreUtil.exportAdmins()
		response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
		response.setHeader("Content-disposition", "attachment; filename=backup-${System.currentTimeMillis()}.xlsx")
		response.addHeader("Access-Control-Allow-Methods", "GET")
		response.addHeader("Access-Control-Allow-Credentials", "*")
		response.addHeader("Access-Control-Max-Age", "3600")
		response.addHeader("Access-Control-Allow-Headers", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")

		IOUtils.copy(File(System.getProperty("user.dir") + "/places.xlsx").inputStream(), response.outputStream)
	}

	/**
	 * This method will create mail broadcast
	 *
	 * @param gender can be "male" or "female" (ignore case)
	 */
	@PostMapping("/mail/create")
	fun mailCreate(@RequestParam region: String?, @RequestParam category: String?, @RequestParam newsList: List<String>?,
	               @RequestParam offerList: List<String>?, @RequestParam projectList: List<String>?,
	               @RequestParam gender: String?, request: HttpServletRequest): MailBroadcast {
		return mailTextsService.createMailBroadcast(
				region = region, category = category, newsList = newsList,
				offerList = offerList, projectList = projectList,
				gender = gender?.let {
					when (it.toLowerCase()) {
						"male" -> UserEntity.Gender.MALE
						"female" -> UserEntity.Gender.FEMALE
						else -> throw IllegalArgumentException("Gender can be 'male' or 'female' only")
					}
				},
				data = request.getParamData())
	}

	/**
	 *  @param gender can be "male" or "female" (ignore case)
	 */
	@PostMapping("/mail/edit")
	fun mailEdit(@RequestParam id: String, request: HttpServletRequest, @RequestParam region: String?,
	             @RequestParam category: String?, @RequestParam newsList: List<String>?, @RequestParam date: Long?,
	             @RequestParam offerList: List<String>?, @RequestParam projectList: List<String>?,
	             @RequestParam gender: String?, @RequestParam cities: List<String>?,
	             @RequestParam regions: List<String>?): MailBroadcast {
		return mailTextsService.edit(id = id, data = request.getParamData(), newsList = newsList, offerList = offerList,
				projectList = projectList, regionId = region, categoryId = category, date = date, regions = regions,
				cities = cities, gender = gender?.let {
			when (it.toLowerCase()) {
				"male" -> UserEntity.Gender.MALE
				"female" -> UserEntity.Gender.FEMALE
				else -> throw IllegalArgumentException("Gender can be 'male' or 'female' only")
			}
		})
	}

	@GetMapping("/mail/{id}/offers")
	fun mailOffers(@PathVariable id: String): List<Offer> {
		return mailTextsService.getOfferList(id)
	}

	@GetMapping("/mail/{id}/projects")
	fun mailProjects(@PathVariable id: String): List<SpecialProject> {
		return mailTextsService.getProjectList(id)
	}

	@GetMapping("/mail/{id}/news")
	fun mailNews(@PathVariable id: String): List<News> {
		return mailTextsService.getNewsList(id)
	}

	@GetMapping("/mail/list")
	fun mailList(@RequestParam page: Int, @RequestParam pageSize: Int?): Page<MailBroadcast> {
		return mailTextsService.list(page, pageSize ?: 4)
	}

	@PostMapping("/mail/delete")
	fun mailDelete(@RequestParam id: String): Boolean {
		return mailTextsService.deleteById(id)
	}


}