package com.arkell.api

import com.arkell.entity.geo.*
import com.arkell.entity.misc.SimplePoint
import com.arkell.model.GeoModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/geo")
class GeoAPI(
		private val geoModel: GeoModel) {

	@GetMapping("/region/{id}")
	fun regionById(@PathVariable id: String): Region {
		return geoModel.regionOps.getById(id)
	}

	/**
	 * List all the regions
	 * @param showHidden for some reason, ok
	 */
	@GetMapping("/regions")
	fun regions(@RequestParam showHidden: Boolean?, @RequestParam page: Int, @RequestParam pageSize: Int?): Page<Region> {
		val list = geoModel.regionOps
			.listRegions(showHidden ?: false) //.filter { if (showHidden != true) it.cities.size > 0 else true }
			.sortedBy { it.id }

		val pageResult = PageImpl(list.drop(page * (pageSize ?: 20)).take(pageSize ?: 20),
				PageRequest.of(page, pageSize ?: 20), list.size.toLong())

		return pageResult
	}

	@GetMapping("/city/{id}")
	fun cityById(@PathVariable id: String): City {
		return geoModel.cityOps.getById(id)
	}

	@GetMapping("/city/closest")
	fun cityClosest(@RequestParam geoX: Double, @RequestParam geoY: Double): City {
		return geoModel.cityOps.closestCity(geoX, geoY)
	}

	/**
	 * List all the cities. Can be filtered by the region
	 * @param regionId is optional, if set - filters cities with that region
	 */
	@GetMapping("/cities")
	fun cities(@RequestParam regionId: String?, @RequestParam showHidden: Boolean?, @RequestParam title: String?,
	           @RequestParam page: Int, @RequestParam pageSize: Int?): Page<City> {

		val visible = showHidden?.not() ?: false

		return geoModel.cityOps.listCities(regionId, visible = visible, title = title, page = page,
				pageSize = pageSize ?: 20)
	}

	@GetMapping("/place/{id}")
	fun placeById(@PathVariable id: String): Place {
		return geoModel.placeOps.getById(id)
	}

	/**
	 * List all the places inside the city, like districts, metro stations etc.
	 * @param cityId if set, filters by id all the results
	 */
	@GetMapping("/places")
	fun places(@RequestParam cityId: String?, @RequestParam regionId: String?, @RequestParam showHidden: Boolean?,
	           @RequestParam title: String?): List<Place> {
		return geoModel.placeOps.listPlaces(cityId, regionId, showHidden = showHidden ?: false, title = title)
	}

	@GetMapping("/places/count")
	fun placesCount(@RequestParam ids: Array<String>): Map<String, *> {
		return geoModel.placeOps.getLocationsCount(ids)
	}

	/**
	 * Get all the places from all the cities
	 */
	@GetMapping("/places/all")
	fun allPlaces(@RequestParam page: Int, @RequestParam pageSize: Int?, @RequestParam type: String?,
	              @RequestParam hasLocations: Boolean?, @RequestParam regionId: String?, @RequestParam title: String?,
	              @RequestParam cityId: String?, @RequestParam showHidden: Boolean?): Page<Place> {
		return geoModel.placeOps.findAll(page, pageSize ?: 20, hasLocations = hasLocations, type = type,
				cityId = cityId, regionId = regionId, title = title, showHidden = showHidden ?: false)
	}

	@GetMapping("/mall/{id}")
	fun mallById(@PathVariable id: String): Mall = geoModel.mallOps.getById(id)

	@GetMapping("/mall/list")
	fun mallList(@RequestParam placeId: String?, @RequestParam cityId: String?, @RequestParam page: Int,
	             @RequestParam pageSize: Int?, @RequestParam showHidden: Boolean?): Page<Mall> {
		return geoModel.mallOps.list(placeId, cityId, page, pageSize ?: 20, showHidden = showHidden ?: false)
	}

	@GetMapping("/location/{id}")
	fun locationById(@PathVariable id: String): ObjectLocation {
		return geoModel.objectLocationOps.getById(id)
	}

	/**
	 * Get simplified locations from DB
	 */
	@GetMapping("/locations/simple")
	fun locationsSimple(@RequestParam cityId: String?, @RequestParam partnerId: String?,
	                    @RequestParam title: String?): List<*> {

		val list = geoModel.objectLocationOps.simpleLocations(cityId = cityId, partnerId = partnerId, title = title)
			.sortedBy { it.lat }.toMutableList()

		val margin = 0.001

		fun move(point: SimplePoint) {
			if (Math.random() > 0.5) {
				if (Math.random() > 0.5) {
					point.lat -= margin
				} else {
					point.lat += margin
				}
			} else {
				if (Math.random() > 0.5) {
					point.lon -= margin
				} else {
					point.lon += margin
				}
			}
		}

		for (i in 0..list.size - 2) {
			if (Math.abs(list[i].lat - list[i + 1].lat) < margin && Math.abs(list[i].lon - list[i + 1].lon) < margin) {
				do {
					move(list[i])
				} while (list.filter { Math.abs(it.lat - list[i].lat) < margin }
							.filter { Math.abs(it.lon - list[i].lon) < margin }
							.filter { it.id != list[i].id }
							.any())
			}
		}

		return list.map { arrayOf(it.id, it.cityId, it.lat, it.lon, it.marker, it.categoryId) }
	}
}