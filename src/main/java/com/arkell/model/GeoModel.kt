package com.arkell.model

import com.arkell.entity.exception.ElementNotFoundException
import com.arkell.entity.geo.*
import com.arkell.entity.misc.SimplePoint
import com.arkell.repo.SpecificationHelper
import com.arkell.repo.geo.*
import com.arkell.util.OrdinalIDGetter
import com.arkell.util.blockAwait
import com.arkell.util.nullIfFalse
import com.arkell.util.objects.ObjectFromMapUpdater
import com.arkell.util.random
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class GeoModel(
		val regionOps: RegionOps,
		val cityOps: CityOps,
		val placeOps: PlaceOps,
		val objectLocationOps: ObjectLocationOps,
		val mallOps: MallOps) {

	@PostConstruct
	fun checkThatAllCitiesHaveFakeLocation() {
		launch {
			val cities = cityOps.repository.findAll()
			val places = placeOps.repository.findAll()
			cities.forEach { city ->
				if (places.asSequence().filter { it.parentCity.id == city.id }.none { !it.isReal }) {
					val place = Place(name = "Онлайн", point = GeoPoint(0.0, 0.0), parentCity = city)
					place.isReal = false
					city.places.add(place.id)
					launch { placeOps.repository.save(place) }
					launch { cityOps.repository.save(city) }
				}
			}
		}
	}

}

@Service
class ObjectLocationOps(
		private val placeOps: PlaceOps,
		private val jdbcTemplate: JdbcTemplate,
		override val repository: ObjectLocationRepo,
		private val mallOps: MallOps
) : UpdateAction<ObjectLocation>() {

	fun findByPlaces(vararg placesIds: String): List<ObjectLocation> {
		return placesIds.flatMap { findByPlace(it) }
	}

	fun findByPlace(placeId: String): List<ObjectLocation> {
		return repository.findByPlace(placeOps.getById(placeId))
	}

	fun findServiceLocations(partnerId: String): List<ObjectLocation> {
		return repository.findByIsServiceAndPartnerId(true, partnerId)
	}

	fun deleteServiceLocations(partnerId: String) {

		val jobs = mutableListOf<Deferred<*>>()

		findServiceLocations(partnerId).forEach {
			jobs += async { repository.deleteById(it.id) }
		}

		jobs.forEach { it.blockAwait() }
	}

	fun editLocation(id: String, x: Double? = null, y: Double? = null, newPlaces: Array<String>?,
	                 mallId: String? = null, data: Map<String, String>) = autoUpdate(id, data) {
		x?.apply { it.point.x = this }
		y?.apply { it.point.y = this }
		newPlaces?.apply { it.places = placeOps.getByIds(this.asList()).toMutableList() }
		mallId?.apply { it.mall = mallOps.getById(this) }
	}

	fun list(cityId: String? = null, placeId: String? = null, showService: Boolean? = null, showHidden: Boolean? = null,
	         partnerId: String? = null, page: Int, pageSize: Int?, address: String? = null, sort: String? = null,
	         online: Boolean? = null):
			Page<ObjectLocation> {

		val filter = SpecificationHelper<ObjectLocation>()

		filter.with(
				"cityId" to cityId,
				"placeId" to placeId,
				"partnerId" to partnerId,
				"isReal" to online?.not()
		)

		if (showService != true) {
			filter.where { root, _, cb -> cb.notEqual(root.get<Boolean>("isService"), true) }
		}

		if (address != null) {
			filter.where { root, _, cb ->
				return@where cb.or(
						cb.like(cb.lower(root.get<String>("streetName")), "%${address.toLowerCase()}%"),
						cb.like(cb.lower(root.get<String>("territory")), "%${address.toLowerCase()}%"),
						cb.like(cb.lower(root.get<String>("district")), "%${address.toLowerCase()}%"),
						cb.like(cb.lower(root.get<String>("postCode")), "%${address.toLowerCase()}%")
				)
			}
		}

		if (showHidden != true) {
			filter.where { root, _, cb -> cb.notEqual(root.get<Boolean>("visible"), false) }
		}

		sort?.let { filter.sort(Sort.Direction.DESC, it) }

		return filter.page(page, pageSize ?: 20).result(repository)
	}

	fun countByPlace(placeId: String): Long {
		return repository.count { root, _, cb ->
			cb.equal(root.get<String>("placeId"), placeId)
		}
	}

	fun simpleLocations(cityId: String?, partnerId: String? = null, title: String?): List<SimplePoint> {

		val ret = mutableListOf<SimplePoint>()

		jdbcTemplate.query("select objectlocation.id, cityid, x, y, marker, category_id, partner.showweb, " +
				"partner.title from objectlocation left join partner on objectlocation.partnerid = partner.id " +
				"where partner.showweb = true and " +
				if (cityId != null) {
					"cityid = '$cityId' and "
				} else {
					""
				} +
				if (title != null) {
					" lower(partner.title) like '%${title.toLowerCase()}%' and "
				} else {
					""
				} +
				if (partnerId != null) {
					"partnerid = '$partnerId' and "
				} else {
					""
				} + "isreal = true") {
			ret.add(SimplePoint(id = it.getString("id"), cityId = it.getString("cityid"),
					lat = it.getDouble("x"), lon = it.getDouble("y"),
					marker = it.getString("marker"), categoryId = it.getString("category_id"),
					partnerName = it.getString("title")))
		}

		return ret
	}

	fun findByPartner(id: String): List<ObjectLocation> {
		val filter = SpecificationHelper<ObjectLocation>()

		filter.where { root, _, cb ->
			cb.equal(root.get<String>("partnerId"), id)
		}

		return filter.resultUnPaged(repository)
	}

	fun createUnRealLocation(partnerId: String, place: Place) {
		val location = ObjectLocation(place)
		location.partnerId = partnerId
		location.updateIds()
		repository.save(location)
	}

}

@Service
class MallOps(
		override val repository: MallRepo,
		@Lazy val placeOps: PlaceOps,
		@Lazy val cityOps: CityOps) : UpdateAction<Mall>() {

	fun create(places: Array<String>?, point: GeoPoint?, data: Map<String, String>): Mall = repository.save(
			Mall().apply {
				ObjectFromMapUpdater(this, data).modify()
				point?.let { this.point = it }
			}
	).also {
		if (places != null) {
			setPlaces(it.id, places)
		}
	}

	fun edit(id: String, point: GeoPoint?, places: Array<String>?, data: Map<String, String>) = autoEdit(id, data) {
		point?.let { this.point = it }
	}.also {
		if (places != null) {
			setPlaces(id, places)
		}
	}

	fun setPlaces(id: String, list: Array<String>) = edit(id) {
		places = list.map { placeOps.getById(it) }.toMutableList()
		cityId = places.first().parentCity.id
	}

	fun list(placeId: String?, cityId: String?, page: Int, pageSize: Int, showHidden: Boolean): Page<Mall> {
		val filter = SpecificationHelper<Mall>()

		if (!showHidden) {
			filter.where { root, _, cb -> cb.notEqual(root.get<Boolean>("visible"), false) }
		}

		if (placeId != null) {
			placeId.let { placeOps.getById(it) }.let {
				filter.where { root, _, cb ->
					cb.isMember(it, root.get<List<Place>>("places"))
				}
			}
		} else if (cityId != null) {
			cityId.let { cityOps.getById(it) }.let {
				filter.where { root, _, cb ->
					cb.equal(root.get<String>("cityId"), it.id)
				}
			}
		}
		return filter.page(page, pageSize).sort(Sort.Direction.ASC, "title").result(repository)
	}

}

@Service
class RegionOps(
		override val repository: RegionRepo,
		@Lazy val cityOps: CityOps,
		val ordinalIDGetter: OrdinalIDGetter) : UpdateAction<Region>() {

	fun listRegions(showHidden: Boolean): List<Region> {
		return repository.findAll().let {
			return@let if (!showHidden) it.filter { it.visible == true }
			else it
		}
	}

	fun createRegion(name: String): Region =
			try {
				getByName(name)
			} catch (e: ElementNotFoundException) {
				Region(name).also {
					it.id = ordinalIDGetter.getByTable("region").toString()
					repository.save(it)
				}
			}

	fun getByName(name: String): Region = repository.findByName(name) ?: notFound()

	fun editRegion(id: String, name: String?, visible: Boolean? = null) = update(id) {
		name?.apply { it.name = this }
		visible?.apply { it.visible = this }
	}

	fun addCity(city: City) = update(city.parentRegion.id) {
		it.cities.add(city.id)
	}

	fun removeCity(regionId: String, city: City) = update(regionId) {
		it.cities.remove(city.id)
	}

	fun changeRegion(cityId: String, newRegionId: String) = changeRegion(cityOps.getById(cityId), newRegionId)
		.also { city -> cityOps.save(city) }

	fun changeRegion(city: City, newRegionId: String): City {
		val prevRegion = city.parentRegion
		val newRegion = this.getById(newRegionId)
		city.parentRegion = newRegion
		launch { removeCity(prevRegion.id, city) }
		launch { addCity(city) }
		return city
	}
}

@Service
class CityOps(
		override val repository: CityRepo,
		private val jdbcTemplate: JdbcTemplate,
		val regionOps: RegionOps,
		val ordinalIDGetter: OrdinalIDGetter) : UpdateAction<City>() {

	fun listCities(regionId: String?, visible: Boolean, title: String? = null,
	               page: Int, pageSize: Int): Page<City> {

		return SpecificationHelper<City>()
			.with("parentRegion" to regionId?.let { regionOps.getById(it) })
			.with("visible" to visible.nullIfFalse())
			.textIgnoreCase("name", title)
			.page(page, pageSize)
			.result(repository)
	}

	fun createCity(name: String, regionId: String, geoX: Double?, geoY: Double?): City = try {
		findByName(name = name, regionId = regionId)
	} catch (e: Exception) {
		City(name = name, parentRegion = regionOps.getById(regionId))
			.also {
				it.id = ordinalIDGetter.getByTable("city").toString()
				it.geoX = geoX
				it.geoY = geoY
				repository.save(it)
				regionOps.addCity(it)
			}
	}

	fun findByName(name: String): List<City> = repository.findByName(name)

	fun findByName(name: String, regionId: String): City {
		val region = try {
			regionOps.getById(regionId)
		} catch (e: Exception) {
			e.printStackTrace()
			Region("")
		}
		return repository.findByNameAndParentRegion(name, region) ?: notFound()
	}


	fun updateCity(cityId: String, regionId: String? = null, name: String? = null, geoX: Double? = null,
	               geoY: Double? = null, data: Map<String, String>) = autoEdit(cityId, data) {
		name?.let { this.name = it }
		regionId?.let {
			regionOps.changeRegion(this, it)
		}
		geoX?.let {
			this.geoX = it
		}
		geoY?.let {
			this.geoY = it
		}
	}

	fun closestCity(geoX: Double, geoY: Double): City = jdbcTemplate
		.queryForList("select id, geox, geoy from city")
		.map {
			(it["id"] as String) to (it["geoX"] as? Double to it["geoY"] as? Double)
		}.sortedBy {
			val (x, y) = it.second
			val a = geoX - (x ?: -1.0)
			val b = geoY - (y ?: -1.0)
			return@sortedBy a * a + b * b
		}.first().let { getById(it.first) }

	fun addPlace(place: Place) = update(place.parentCity.id) {
		it.places.add(place.id)
	}

	fun removePlace(cityId: String, place: Place) = update(cityId) {
		it.places.remove(place.id)
	}

}

@Service
class PlaceOps(
		override val repository: PlaceRepo,
		@Lazy val locationOps: ObjectLocationOps,
		val cityOps: CityOps,
		val regionOps: RegionOps,
		val jdbcTemplate: JdbcTemplate) : UpdateAction<Place>() {

	fun listPlaces(cityId: String?, regionId: String?, showHidden: Boolean, title: String? = null): List<Place> {

		val filter = SpecificationHelper<Place>()

		filter
			.with("parentCity" to cityId?.let { cityOps.getById(it) })
			.textIgnoreCase("name", title)
			.sort(Sort.Direction.ASC, "name")

		regionId?.let {
			filter.where { root, _, cb ->
				cb.equal(root.get<City>("parentCity").get<Region>("parentRegion"), regionOps.getById(it))
			}
		}

		if (!showHidden) {
			filter.where { root, _, cb ->
				cb.or(
						cb.isTrue(root.get<Boolean>("visible")),
						cb.isNull(root.get<Boolean>("visible")))
			}
		}

		return filter.resultUnPaged(repository)
	}

	fun findByNameMatch(string: String): List<Place> {
		return repository.findByNameContaining(string)
	}

	fun findAllUnrealPlaces(): List<Place> = repository.findByIsReal(false)

	fun findAnyUnRealPlace(): Place = repository.findByIsReal(false).random()
	fun findAnyUnRealPlace(city: City): Place = repository.findAll { root, _, cb ->
		cb.equal(root.get<City>("parentCity"), city)
	}.random()

	fun findByName(placeName: String) = repository.findByName(placeName)

	fun findByName(placeName: String, cityName: String): Place {
		val cities = cityOps.findByName(cityName)

		if (cities.isEmpty()) {
			return notFound("result list for city name $cityName is empty")
		}

		val placeList = cities.map {
			repository.findByNameIgnoreCaseAndParentCity(placeName, it)
		}.filter { it != null }.map { it!! }.toList()

		if (placeList.isEmpty() || placeList.size > 1) {
			return notFound("placelist.size == ${placeList.size}")
		}

		return placeList.first()
	}

	fun findByNameExact(placeName: String, cityId: String): Place {
		return repository.findByNameIgnoreCaseAndParentCity(placeName, cityOps.getById(cityId)) ?: notFound()
	}

	fun createPlace(name: String, cityId: String, point: GeoPoint, isReal: Boolean, type: String, logo: String?): Place {
		return try {
			findByNameExact(name, cityId)
		} catch (e: ElementNotFoundException) {
			repository.save(Place(name = name, parentCity = cityOps.getById(cityId), point = point)
				.also {
					it.isReal = isReal
					cityOps.addPlace(it)
					it.type = type
					it.logo = logo
				})
		}
	}

	fun updatePlace(id: String, name: String?, isReal: Boolean?, point: GeoPoint?, type: String?, logo: String?, visible: Boolean?) = update(id) {
		name?.apply { it.name = this }
		point?.apply { it.point = this }
		isReal?.apply { it.isReal = this }
		type?.apply { it.type = this }
		logo?.apply { it.logo = this }
		visible?.apply { it.visible = this }
	}

	fun findAll(page: Int, pageSize: Int, hasLocations: Boolean?, type: String? = null,
	            cityId: String? = null, regionId: String? = null, title: String? = null, showHidden: Boolean): Page<Place> {

		val filter = SpecificationHelper<Place>().with("type" to type)
			.textIgnoreCase("name", title)

		if (!showHidden) {
			filter.with("visible" to true)
		}

		hasLocations?.let {
			filter.where { root, criteriaQuery, cb ->
				val locationRoot = criteriaQuery.from(ObjectLocation::class.java)
				val predicate = cb.equal(locationRoot.get<String>("placeId"), root.get<String>("id"))
				return@where cb.notEqual(cb.count(predicate), 0)
			}
		}

		regionId?.let { regionOps.getById(it) }?.apply {
			filter.where { root, _, cb ->
				cb.equal(root.get<City>("parentCity").get<Region>("parentRegion"), this)
			}
		}

		cityId?.let { cityOps.getById(it) }?.apply {
			filter.where { root, _, cb ->
				cb.equal(root.get<City>("parentCity"), this)
			}
		}

		return filter.page(page, pageSize).sort(Sort.Direction.ASC, "name").result(repository)
	}

	fun findClosest(x: Double, y: Double): Place {

		val distances = mutableListOf<Pair<String, Double>>()

		jdbcTemplate.query("select id, x, y from place where isReal = true") {

			val x1 = it.getDouble("x") - x
			val y1 = it.getDouble("y") - y

			distances.add(it.getString("id") to Math.sqrt(x1*x1 + y1*y1))
		}

		return getById(distances.minBy { it.second }!!.first)
	}

	fun getLocationsCount(ids: Array<String>): Map<String, Long> {
		val ret = mutableMapOf<String, Long>()
		ids.forEach { ret[it] = locationOps.countByPlace(it) }
		return ret
	}

	fun findByCityId(id: String): List<Place> {
		val city = cityOps.getById(id)
		return repository.findByParentCity(city)
	}


}