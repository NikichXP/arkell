package com.arkell.api

import com.arkell.entity.Offer
import com.arkell.entity.Partner
import com.arkell.entity.SpecialProject
import com.arkell.entity.exception.ElementNotFoundException
import com.arkell.entity.misc.Platform
import com.arkell.model.SpecialProjectModel
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/project")
class SpecialProjectAPI(
		private val specialProjectModel: SpecialProjectModel) {

	@GetMapping("/{id}")
	fun getById(@PathVariable id: String): SpecialProject = specialProjectModel.getById(id)

	/**
	 * Shows special projects in paged view.
	 * Notice that this method doesn't return partners entities, only id's.
	 * @param showHidden is optional. default is true, when true - don't show events outside of current dates
	 * @param sort startDate, endDate, priority, title (наверное), id
	 */
	@GetMapping("/list")
	fun list(@RequestParam showHidden: Boolean?, @RequestParam showInactive: Boolean?, @RequestParam page: Int,
	         @RequestParam pageSize: Int?, @RequestParam sort: String?, @RequestParam title: String?,
	         @RequestParam platform: Platform?, @RequestParam rangeFrom: Long?, @RequestParam rangeTo: Long?,
	         @RequestParam featured: Boolean?): Page<SpecialProject> {
		return specialProjectModel.list(showHidden = showHidden, page = page, title = title, showInactive = showInactive,
				pageSize = pageSize ?: 20, sort = sort, platform = platform ?: Platform.app, featured = featured,
				range = rangeFrom?.let { it..rangeTo!! })
	}

	@GetMapping("/url/{url}")
	fun getByUrl(@PathVariable url: String): SpecialProject {
		val ret = specialProjectModel.getByUrl(url)
		if (ret.urlEnabled == true || ret.showWeb == true) {
			return ret
		} else {
			throw ElementNotFoundException("Url link is forbidden for this element.")
		}
	}

	/**
	 * Get all the partner's entities from project by id.
	 * Not pageable!
	 */
	@GetMapping("/partners")
	fun partners(@RequestParam id: String): List<Partner> {
		return specialProjectModel.partnerList(id)
	}

	/**
	 * Get all the partner's entities from project by id.
	 * Not pageable!
	 */
	@GetMapping("/offers")
	fun offers(@RequestParam id: String): List<Offer> {
		return specialProjectModel.offerList(id)
	}


}