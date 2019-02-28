package com.arkell.api.admin

import com.arkell.entity.Offer
import com.arkell.entity.SpecialProject
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.model.SpecialProjectModel
import com.arkell.util.getParamData
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@Auth(value = AuthPermission.ADMIN)
@RestController
@RequestMapping("/api/admin/project")
class AdminSpecialProjectAPI(
		private val specialProjectModel: SpecialProjectModel) {

	@PostMapping("/create")
	fun create(@RequestParam priority: Int?, @RequestParam categoryId: String?, @RequestParam cities: List<String>?,
	           @RequestParam regions: List<String>?, request: HttpServletRequest): SpecialProject {
		return specialProjectModel.create(priority = priority, categoryId = categoryId,
				cities = cities, regions = regions, data = request.getParamData())
	}

	@PostMapping("/data")
	fun setData(@RequestParam id: String, @RequestParam partners: Array<String>?,
	            @RequestParam offers: Array<String>?, @RequestParam cities: List<String>?,
	            @RequestParam regions: List<String>?): SpecialProject {
		return specialProjectModel.setData(id, partners, cities = cities, regions = regions, offers = offers)
	}

	@PostMapping("/partner/add")
	fun addPartner(@RequestParam projectId: String, @RequestParam partnerId: String): SpecialProject {
		return specialProjectModel.addPartner(projectId, partnerId)
	}

	@PostMapping("/partner/delete")
	fun deletePartner(@RequestParam projectId: String, @RequestParam partnerId: String): SpecialProject {
		return specialProjectModel.removePartner(projectId, partnerId)
	}

	@PostMapping("/offer/add")
	fun addOffer(@RequestParam projectId: String, @RequestParam offerId: String): SpecialProject {
		return specialProjectModel.addOffer(projectId, offerId)
	}

	@PostMapping("/offer/delete")
	fun deleteOffer(@RequestParam projectId: String, @RequestParam offerId: String): SpecialProject {
		return specialProjectModel.removeOffer(projectId, offerId)
	}

	@GetMapping("/offers")
	fun offers(@RequestParam projectId: String): List<Offer> {
		return specialProjectModel.getOfferList(projectId)
	}

	@PostMapping("/update/{id}")
	fun update(@PathVariable id: String, request: HttpServletRequest): SpecialProject {
		return specialProjectModel.update(id, request.getParamData())
	}

	@PostMapping("/delete/{id}")
	fun delete(@PathVariable id: String): Boolean {
		return specialProjectModel.deleteById(id)
	}

}