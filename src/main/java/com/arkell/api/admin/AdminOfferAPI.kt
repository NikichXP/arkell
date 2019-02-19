package com.arkell.api.admin

import com.arkell.entity.Offer
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.misc.Display
import com.arkell.model.OfferModel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@Auth(value = AuthPermission.ADMIN)
@RestController
@RequestMapping("/api/admin/offer")
class AdminOfferAPI(
		private val offerModel: OfferModel) {

	/**
	 * Creates offer for a partner.
	 * TODO: check partner status, set featuredOffer
	 * @param offerType can be CASHBACK, DISCOUNT, GIFT
	 * @param amount is unified amount which will be mapped to 4-element array
	 * @param amounts is amounts of discounts. if sent, MUST be with size of 4, otherwise exception returns
	 */
	@PostMapping("/create")
	fun create(@RequestParam title: String, request: HttpServletRequest,
	           @RequestParam partnerId: String, @RequestParam categoryId: String, @RequestParam offerType: String,
	           @RequestParam amount: Double?, @RequestParam previewAmount: Double?,
	           @RequestParam banner: String?, @RequestParam amounts: Array<Double>?,
	           @RequestParam image: String?, @RequestParam terms: String?, @RequestParam upTo: Boolean?,
	           @RequestParam priority: Int?, @RequestParam realisation: String?): Offer {
		return offerModel.create(partnerId = partnerId, categoryId = categoryId, amount = amount, amounts = amounts,
				offerType = Offer.OfferType.valueOf(offerType), data = request.parameterMap.mapValues { it.value[0] },
				realisation = realisation)
	}


	/**
	 * Edits offer. If value not sent - corresponding value not modified
	 */
	@PostMapping("/edit")
	fun edit(@RequestParam id: String, @RequestParam offerType: String?, request: HttpServletRequest,
	         @RequestParam previewAmount: String?, @RequestParam display: String?, @RequestParam priority: Int?,
	         @RequestParam featured: Boolean?, @RequestParam realisation: String?): Offer {
		return offerModel.editOffer(offerId = id, offerType = offerType?.let { Offer.OfferType.valueOf(it) },
				previewAmount = previewAmount?.let { if (it.isNotEmpty()) it.toDouble() else -1.0 },
				data = request.parameterMap.mapValues { it.value[0] }, featured = featured,
				display = display?.let { Display.valueOf(it) }, priority = priority, realisation = realisation)

	}

	/**
	 * Warning: if this wouldn't work I'll change List -> Array.
	 * @param id is partner id
	 * @param mainCategory categoryId to be set as main category (if categories doesn't contain it -> this category would
	 * be added to list)
	 * @param set new list of categories ID's to delete
	 */
	@PostMapping("/category/edit")
	fun categoryEdit(@RequestParam id: String, @RequestParam mainCategory: String?, @RequestParam set: List<String>?): Offer {
		return offerModel.editOfferCategories(id = id, mainCategoryId = mainCategory, set = set)
	}

	@PostMapping("/delete")
	fun delete(@RequestParam id: String): Boolean {
		offerModel.deleteOffer(id)
		return true
	}

}
