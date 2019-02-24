package com.arkell.api.admin

import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.geo.*
import com.arkell.model.GeoModel
import com.arkell.util.getParamData
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@Auth(value = AuthPermission.ADMIN)
@RestController
@RequestMapping("/api/admin/geo")
class AdminGeoAPI(
		private val geoModel: GeoModel) {

	/**
	 * Creates region with given name
	 */
	@PostMapping("/region/create")
	fun regionCreate(@RequestParam name: String): Region {
		return geoModel.regionOps.createRegion(name)
	}

	/**
	 * Edits the name of region
	 */
	@PostMapping("/region/edit/{id}")
	fun regionEdit(@PathVariable id: String, @RequestParam name: String?, @RequestParam visible: Boolean?): Region {
		return geoModel.regionOps.editRegion(id, name, visible)
	}

	/**
	 * Creates city with given name in region by ID
	 */
	@PostMapping("/city/create")
	fun cityCreate(@RequestParam name: String, @RequestParam regionId: String, @RequestParam geoX: Double?,
	               @RequestParam geoY: Double?): City {
		return geoModel.cityOps.createCity(name, regionId, geoX, geoY)
	}

	/**
	 * Edits city information
	 * @param regionId if set, changes region of the city.
	 */
	@PostMapping("/city/edit/{id}")
	fun cityEdit(@PathVariable id: String, @RequestParam name: String?, @RequestParam geoX: Double?,
	             @RequestParam geoY: Double?, @RequestParam regionId: String?, request: HttpServletRequest): City {
		return geoModel.cityOps.updateCity(id, regionId = regionId, name = name, geoX = geoX, geoY = geoY,
				data = request.getParamData())
	}

	/**
	 * Creates place in city (like trade center or metro station)
	 * @param isReal optional: send false if place is supposed to be a virtual place, for internet-shops etc.
	 * @param geoX is geographical X of place (latitude)
	 * @param geoY is longitude
	 */
	@PostMapping("/place/create")
	fun createPlace(@RequestParam name: String, @RequestParam cityId: String, @RequestParam isReal: Boolean?,
	                @RequestParam geoX: Double, @RequestParam geoY: Double, @RequestParam type: String,
	                @RequestParam logo: String? = null): Place {
		return geoModel.placeOps.createPlace(name = name, cityId = cityId, isReal = isReal ?: true,
				point = GeoPoint(geoX, geoY), type = type, logo = logo)
	}

	/**
	 * Edits place's info
	 * @param geoX if provided, geoY must be provided as well
	 * @param geoY doesn't affects if geoX is not provided
	 */
	@PostMapping("/place/edit/{id}")
	fun placeEdit(@PathVariable id: String, @RequestParam name: String?, @RequestParam cityId: String?,
	              @RequestParam geoX: Double?, @RequestParam geoY: Double?, @RequestParam isReal: Boolean?,
	              @RequestParam type: String?, @RequestParam logo: String?, @RequestParam visible: Boolean?): Place {
		return geoModel.placeOps.updatePlace(id = id, name = name, isReal = isReal, type = type,
				point = geoX?.let { GeoPoint(it, geoY!!) }, logo = logo, visible = visible)
	}

	@PostMapping("/mall/create")
	fun mallCreate(request: HttpServletRequest, @RequestParam geoX: Double?,
	               @RequestParam geoY: Double?, @RequestParam places: Array<String>?): Mall {
		return geoModel.mallOps.create(places = places, point = geoX?.let { GeoPoint(it, geoY!!) },
				data = request.getParamData())
	}

	@PostMapping("/mall/edit")
	fun mallEdit(@RequestParam id: String, @RequestParam placeId: String?, request: HttpServletRequest,
	             @RequestParam geoX: Double?, @RequestParam geoY: Double?, @RequestParam places: Array<String>?): Mall {
		return geoModel.mallOps.edit(id = id, places = places, point = geoX?.let { GeoPoint(it, geoY!!) },
				data = request.getParamData())
	}

	/**
	 * Please be careful before deleting this
	 */
	@PostMapping("/mall/delete")
	fun mallDelete(@RequestParam id: String): Boolean = geoModel.mallOps.deleteById(id)

	/**
	 * Get locations page, can be filtered by cityId of placeId
	 * @param address search any match (ignore case) in district, streetName or territory
	 * @param online true - только онлайн-точки, false - только физические, не передан = все
	 */
	@GetMapping("/locations", "/locations/list")
	fun locations(@RequestParam cityId: String?, @RequestParam placeId: String?, @RequestParam showService: Boolean?,
	              @RequestParam showHidden: Boolean?, @RequestParam partnerId: String?, @RequestParam page: Int,
	              @RequestParam pageSize: Int?, @RequestParam sort: String?, @RequestParam address: String?,
	              @RequestParam online: Boolean?): Page<ObjectLocation> {
		return geoModel.objectLocationOps.list(cityId = cityId, placeId = placeId, showService = showService,
				showHidden = showHidden, page = page, pageSize = pageSize, partnerId = partnerId, sort = sort,
				address = address, online = online)
	}

	/**
	 * Edit any location data
	 * @param request any other data
	 * @param places то же самое, как и при добавлении этих параметров в сущность партнёрской точки.
	 */
	@PostMapping("/location")
	fun editLocation(@RequestParam id: String, @RequestParam geoX: Double?, @RequestParam geoY: Double?,
	                 @RequestParam places: Array<String>?, @RequestParam mallId: String?,
	                 request: HttpServletRequest): ObjectLocation {
		return geoModel.objectLocationOps.editLocation(id, geoX, geoY, places, mallId, request.getParamData())
	}
}