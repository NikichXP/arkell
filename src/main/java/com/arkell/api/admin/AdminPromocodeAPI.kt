package com.arkell.api.admin

import com.arkell.entity.Promocode
import com.arkell.model.PromocodeService
import com.arkell.model.file.FileModel
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/admin/promocode")
class AdminPromocodeAPI(
		val promocodeService: PromocodeService,
		val fileModel: FileModel) {

	@GetMapping("/{id}")
	fun getById(@PathVariable id: String): Promocode {
		return promocodeService.getById(id)
	}

	/**
	 * Lists promocodes
	 * @param claimed if set shows all the claimed/free codes
	 * @param offerId filters to offerId
	 * @param public show public codes
	 */
	@GetMapping("/list")
	fun list(@RequestParam page: Int, @RequestParam pageSize: Int?, @RequestParam offerId: String?,
	         @RequestParam claimed: Boolean?, @RequestParam public: Boolean?, @RequestParam partnerId: String?): Page<Promocode> {
		return promocodeService.list(page = page, pageSize = pageSize ?: 100, offerId = offerId, claimed = claimed,
				public = public, partnerId = partnerId)
	}

	/**
	 * Adds promocodes to offer.
	 *
	 * @param isPublic default = true
	 */
	@PostMapping("/add")
	fun addCodes(@RequestParam offerId: String, @RequestParam codes: Array<String>,
	             @RequestParam isPublic: Boolean?): Pair<String, String?> {
		return promocodeService.insert(offerId, codes = codes.asList(), isPublic = isPublic ?: true)
	}

	/**
	 * Upload a file with data
	 * @param request must contain part "file" with any filename provided
	 */
	@PostMapping("/upload")
	fun upload(request: HttpServletRequest, @RequestParam offerId: String, @RequestParam isPublic: Boolean?): Map<String, Any?> {
		val part = request.getPart("file")
		return promocodeService.parseFile(file = fileModel.uploadToTempStorage(part),
				offerId = offerId, isPublic = isPublic ?: true).let {
			mutableMapOf("status" to it.first, "jobId" to it.second).filter { it.value != null }
		}
	}

	@GetMapping("/jobs")
	fun jobs(): List<String> {
		return promocodeService.jobs.keys().toList()
	}

	@GetMapping("/job/{id}")
	fun statusByJobId(@PathVariable id: String): Map<String, Any> {
		return (promocodeService.jobs[id] ?: throw IllegalArgumentException("No job with this id")).let {
			val a = it.split("/")[0].toInt().toDouble()
			val b = it.split("/")[1].toInt().toDouble()

			return@let mapOf("progress" to it, "percent" to (a / b) * 100)
		}
	}

	/**
	 * Deletes promocode. ID is the code.
	 */
	@PostMapping("/delete/{id}")
	fun deleteCode(@PathVariable id: String): Boolean {
		return promocodeService.deleteById(id)
	}

}