package com.arkell.api

import com.arkell.entity.Partner
import com.arkell.entity.exception.ElementNotFoundException
import com.arkell.entity.geo.ObjectLocation
import com.arkell.entity.interaction.PartnerSubmit
import com.arkell.entity.misc.Platform
import com.arkell.model.PartnerModel
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/partner")
class PartnerAPI(
		private val partnerModel: PartnerModel) {

	/**
	 * This will force locations get in request
	 */
	@GetMapping("/{id}")
	fun getById(@PathVariable id: String, @RequestParam forceLocations: Boolean?): Partner {
		return partnerModel.getById(id)
	}

	/**
	 * Submits a form for the partnership.
	 * @param title is brand name (Coca Cola)
	 * @param legalName is legal name (Coca Cola Beverages Ukraine Ltd.)
	 * @param INN is unique tax identifier (123456778654321)
	 * @param organisationForm is type of organisation (ЗАО, ПП, ЧП, ООО)
	 * @param contactPerson is contact string (Grey S., 123. Colorado str, Virginia)
	 * @param email is email or mailing address if not possible
	 * @param sellType is "Retail" or "Online", case INsensitive
	 * @param website is website
	 * @param shopsCount is Int
	 * @param placeId is parent place (see GeoAPI)
	 * @param geoX is geographical X of default position of all offers. Optional.
	 * @param geoY same as X. Optional.
	 */
	@PostMapping("/submit")
	fun submit(@RequestParam title: String, @RequestParam legalName: String, @RequestParam INN: String?,
	           @RequestParam organisationForm: String, @RequestParam contactPerson: String?, @RequestParam phone: String,
	           @RequestParam email: String?, @RequestParam sellType: String, @RequestParam cityId: String?,
	           @RequestParam website: String?, @RequestParam shopsCount: Int?
	): PartnerSubmit {
		return partnerModel.submit(title = title, legalName = legalName, INN = INN, cityId = cityId,
				organisationForm = organisationForm, contactPerson = contactPerson, phone = phone, email = email,
				sellType = sellType, website = website, shopsCount = shopsCount)
	}

	/**
	 * Filter partners and their offers by criterias.
	 * @param title is case-insensitive title
	 * @param forceLocations will force locations to fetch and to be included in response. Most likely
	 * to overload server and cause timeouts.
	 */
	@GetMapping("/list", "/filter")
	fun listOrFilter(@RequestParam categories: Array<String>?, @RequestParam cityId: String?, @RequestParam placeId: String?,
	                 @RequestParam regionId: String?, @RequestParam platform: Platform?,
	                 @RequestParam sort: String?, @RequestParam page: Int, @RequestParam pageSize: Int?,
	                 @RequestParam forceLocations: Boolean?, @RequestParam showHidden: Boolean?,
	                 @RequestParam title: String?, @RequestParam featured: Boolean?, @RequestParam isOnline: Boolean?,
	                 @RequestParam noOffers: Boolean?, @RequestParam noPoints: Boolean?): Page<Partner> {
		return partnerModel.filterPartners(categories = categories, cityId = cityId, placeId = placeId,
				regionId = regionId, page = page, pageSize = pageSize ?: 20, sort = sort, title = title,
				showHidden = showHidden ?: false, featured = featured, isOnline = isOnline, noPoints = noPoints,
				noOffers = noOffers, platform = platform ?: Platform.app)
	}

	@GetMapping("/url/{url}")
	fun getByUrl(@PathVariable url: String): Partner {
		val ret = partnerModel.getByUrl(url)
		if (ret.urlEnabled == true || ret.showWeb == true) {
			return ret
		} else {
			throw ElementNotFoundException("Url link is forbidden for this element.")
		}
	}

	@GetMapping("/locations/{id}")
	fun locations(@PathVariable id: String): List<ObjectLocation> {
		return partnerModel.getPartnerLocations(id)
	}

	@GetMapping("/located")
	fun located(@RequestParam locationId: String): Partner {
		return partnerModel.byLocation(locationId)
	}

	/**
	 * Get by title or ID
	 */
	@GetMapping("/title")
	fun findByTitle(@RequestParam title: String): List<Partner> {
		return partnerModel.findByName(title)
	}

}