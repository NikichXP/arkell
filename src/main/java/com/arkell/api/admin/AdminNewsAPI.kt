package com.arkell.api.admin

import com.arkell.entity.News
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.model.NewsModel
import com.arkell.util.getParamData
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@Auth(value = AuthPermission.ADMIN)
@RestController
@RequestMapping("/api/admin/news")
class AdminNewsAPI(
		private val newsModel: NewsModel) {

	@PostMapping("/create")
	fun create(@RequestParam priority: Int?, @RequestParam regionsIds: List<String>?, @RequestParam citiesIds: List<String>?,
	           @RequestParam beginDate: Long?, @RequestParam endDate: Long?, @RequestParam partners: Array<String>?,
	           @RequestParam offers: Array<String>?, request: HttpServletRequest): News {
		return newsModel.create(priority = priority, regionsIds = regionsIds, citiesIds = citiesIds, beginDate = beginDate,
				endDate = endDate, partners = partners, offers = offers, data = request.getParamData())
	}

	/**
	 * Edit news by ID
	 * @param beginDate is timestamp (in milliseconds, not just seconds)
	 * @param endDate is the same as beginDate
	 * @param partnerId if "null" is sent - removes the partner. otherwise, set partner by ID
	 * @param offerId same as partnerId
	 */
	@PostMapping("/edit/{id}")
	fun edit(@PathVariable id: String, @RequestParam regionsIds: List<String>?, @RequestParam citiesIds: List<String>?,
	         request: HttpServletRequest, @RequestParam beginDate: Long?, @RequestParam endDate: Long?): News {
		return newsModel.edit(id = id, regionsIds = regionsIds, citiesIds = citiesIds,
				data = request.getParamData(), beginDate = beginDate,
				endDate = endDate)
	}

	@PostMapping("/data")
	fun setOffersAndPartners(@RequestParam id: String, @RequestParam partners: Array<String>?,
	                         @RequestParam offers: Array<String>?): News {
		return newsModel.setPartnersAndOffers(id, partners, offers)
	}

	@PostMapping("/partner/add")
	fun addPartner(@RequestParam newsId: String, @RequestParam partnerId: String): News {
		return newsModel.addPartner(newsId, partnerId)
	}

	@PostMapping("/partner/delete")
	fun deletePartner(@RequestParam newsId: String, @RequestParam partnerId: String): News {
		return newsModel.removePartner(newsId, partnerId)
	}

	@PostMapping("/offer/add")
	fun addOffer(@RequestParam newsId: String, @RequestParam offerId: String): News {
		return newsModel.addOffer(newsId, offerId)
	}

	@PostMapping("/offer/delete")
	fun deleteOffer(@RequestParam newsId: String, @RequestParam offerId: String): News {
		return newsModel.removeOffer(newsId, offerId)
	}

	@PostMapping("/delete/{id}")
	fun deleteByid(@PathVariable id: String): Boolean {
		return newsModel.deleteById(id)
	}
}