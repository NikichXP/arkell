package com.arkell.api.admin

import com.arkell.entity.Partner
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.interaction.PartnerSubmit
import com.arkell.model.PartnerModel
import com.arkell.util.Ret
import com.arkell.util.getParamData
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@Auth(value = AuthPermission.ADMIN)
@RestController
@RequestMapping("/api/admin/partner/")
class AdminPartnerAPI(
		private val partnerModel: PartnerModel) {

	/**
	 * Creates a partner. Main difference is that this method does not submit partner form.
	 * It creates a partner, which is already active.
	 * @param newStatus is new priority for this partner (>0 to make it active)
	 * @param category is brand's category (food, mobile phone, clothes etc.)
	 */
	@PostMapping("/create")
	fun create(@RequestParam newStatus: Int?, request: HttpServletRequest, @RequestParam category: String,
	           @RequestParam cityId: String?): Partner {
		return partnerModel.createPartner(cityId = cityId,
				data = request.parameterMap.mapValues { it.value[0] }, discountCategory = category).also {
			partnerModel.changeStatus(it.id, newStatus ?: 1)
		}
	}

	/**
	 * @param request is every param you want to edit
	 */
	@PostMapping("/edit")
	fun edit(@RequestParam id: String, @RequestParam visible: Boolean?, request: HttpServletRequest,
	         @RequestParam cityId: String?): Partner {
		return partnerModel.editPartner(id, visible = visible, cityId = cityId,
				params = request.parameterMap.mapValues { e -> e.value[0] })
	}

	/**
	 * Make partner global. Global partners are shown in any place.
	 * @param status true - partner will be global, false - partner will be not global. it's that simple.
	 */
	@PostMapping("/makeGlobal")
	fun makeGlobal(@RequestParam id: String, status: Boolean): Partner {
		return if (status) partnerModel.makeGlobalPartner(id) else partnerModel.makePartnerUnGlobal(id)
	}

	@PostMapping("/online")
	fun setOnlinePoints(@RequestParam partnerId: String, @RequestParam cities: List<String>): ResponseEntity<*> {
		return partnerModel.setOnlinePoints(partnerId, cities)
	}

	@GetMapping("/submit/list")
	fun submitList(): List<PartnerSubmit> {
		return partnerModel.listSubmits()
	}

	@GetMapping("/submit/{id}")
	fun submitById(@PathVariable id: String): PartnerSubmit {
		return partnerModel.getSubmitById(id)
	}

	@PostMapping("/submit/delete")
	fun submitDelete(@RequestParam id: String): String {
		return partnerModel.deleteSubmit(id)
	}

	@PostMapping("/contact/add")
	fun contactAdd(@RequestParam id: String, @RequestParam name: String, @RequestParam phone: String,
	               @RequestParam mail: String, @RequestParam position: String): Partner {
		return partnerModel.addContactPerson(id, name, phone, mail, position)
	}

	@PostMapping("/contact/edit")
	fun contactEdit(@RequestParam id: String, @RequestParam contactId: String, request: HttpServletRequest): Partner {
		return partnerModel.editContactPerson(id, contactId, request.getParamData())
	}

	@PostMapping("/contact/delete")
	fun contactDelete(@RequestParam id: String, @RequestParam contactId: String): Partner {
		return partnerModel.removeContactPerson(id, contactId)
	}

	/**
	 * Add location to partner
	 *
	 * @param places это все места, что необходимы.
	 * Если нет ни одного элемента то будет браться нереальное место из cityId
	 * Все добавленные места добавляются в сущность objectLocation, индексируются и по ним можно искать.
	 * Все добавленные места должны быть из одного города (иначе будет возвращена ошибка).
	 * @param mallId ID торгового центра.
	 * @param cityId нужен только если places пуст
	 * @return сущность партнёра
	 * 405 - если мест нет и город пуст
	 */
	@PostMapping("/address/add")
	fun addAddress(@RequestParam partnerId: String, @RequestParam places: Array<String>?, @RequestParam cityId: String?,
	               @RequestParam mallId: String?, request: HttpServletRequest): Partner {
		return partnerModel.addPartnerLocation(id = partnerId, places = places
				?: arrayOf(), cityId = cityId, mallId = mallId,
				data = request.getParamData())
	}

	/**
	 * Delete location by location id from partner
	 * @param id partner ID
	 */
	@PostMapping("/address/delete")
	fun deleteAddress(@RequestParam id: String, @RequestParam locationId: String): Partner {
		return partnerModel.deletePartnerLocation(id, locationId)
	}

	/**
	 * Warning: if this wouldn't work I'll change List -> Array.
	 * @param id is partner id
	 * @param mainCategory categoryId to be set as main category (if categories doesn't contain it -> this category would
	 * be added to list)
	 * @param add list of categories ID's to add
	 * @param delete list of categories ID's to delete
	 */
	@PostMapping("/category/edit")
	fun categoryEdit(@RequestParam id: String, @RequestParam mainCategory: String?, @RequestParam add: List<String>?,
	                 @RequestParam delete: List<String>?): Partner {
		return partnerModel.editPartnerCategories(id = id, mainCategoryId = mainCategory,
				add = add ?: listOf(), delete = delete ?: listOf())
	}

	/**
	 * Approve of disapprove partner
	 * @param status is new status of partner: 0 is banned/unconfirmed, 1 is highest priority, up to the sky.
	 */
	@PostMapping("/changeStatus")
	fun changeStatus(@RequestParam id: String, @RequestParam status: Int): ResponseEntity<*> {
		return Ret.ok(partnerModel.changeStatus(id, status))
	}

	@PostMapping("/delete")
	fun delete(@RequestParam id: String): Boolean {
		return partnerModel.deletePartner(id)
	}

}
