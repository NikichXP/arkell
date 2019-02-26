package com.arkell.api

import com.arkell.entity.News
import com.arkell.entity.Offer
import com.arkell.entity.Partner
import com.arkell.entity.exception.ElementNotFoundException
import com.arkell.entity.misc.Platform
import com.arkell.model.NewsModel
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/news")
class NewsAPI(
		private val newsModel: NewsModel) {

	@GetMapping("/{id}")
	fun getById(@PathVariable id: String): News {
		return newsModel.getById(id)
	}

	@GetMapping("/url/{url}")
	fun getByUrl(@PathVariable url: String): News {
		val ret = newsModel.getByUrl(url)
		if (ret.urlEnabled == true || ret.showWeb == true) {
			return ret
		} else {
			throw ElementNotFoundException("Url link is forbidden for this element.")
		}
	}

	/**
	 * @param name Is name or part of name in news title
	 * @param actual show only actual news. default value is true. if false - you will also receive outdated and future news
	 * @param partnerId is id of existing partner
	 */
	@GetMapping("/list")
	fun list(@RequestParam name: String?, @RequestParam partnerId: String?, @RequestParam actual: Boolean?,
	         @RequestParam page: Int, @RequestParam pageSize: Int?, @RequestParam sort: String?,
	         @RequestParam cityId: String?, @RequestParam regionId: String?, @RequestParam showHidden: Boolean?,
	         @RequestParam platform: Platform?, @RequestParam featured: Boolean?,
	         @RequestParam archive: Boolean?): Page<News> {
		return newsModel.listBy(name = name, partnerId = partnerId, actual = actual ?: true, page = page, sort = sort,
				showHidden = showHidden, cityId = cityId, regionId = regionId, pageSize = pageSize ?: 20,
				platform = platform ?: Platform.app, featured = featured, archive = archive)
	}

	/**
	 * Get all the partner's entities from project by id.
	 * Not pageable!
	 */
	@GetMapping("/partners")
	fun partners(@RequestParam id: String): List<Partner> {
		return newsModel.partnerList(id)
	}

	/**
	 * Get all the partner's entities from project by id.
	 * Not pageable!
	 */
	@GetMapping("/offers")
	fun offers(@RequestParam id: String): List<Offer> {
		return newsModel.offerList(id)
	}

}