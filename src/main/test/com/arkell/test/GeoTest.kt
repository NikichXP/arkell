//package com.arkell.test
//
//import com.arkell.api.GeoAPI
//import com.arkell.api.admin.AdminGeoAPI
//import com.arkell.entity.geo.GeoPoint
//import com.arkell.model.CityOps
//import com.arkell.model.GeoModel
//import com.arkell.model.RegionOps
//import com.arkell.util.int
//import kotlinx.coroutines.experimental.launch
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.TestInstance
//import org.junit.jupiter.api.extension.ExtendWith
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.test.context.junit.jupiter.SpringExtension
//import java.util.*
//import kotlin.test.assertTrue
//
//@ExtendWith(SpringExtension::class)
//@SpringBootTest
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
////@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//internal class GeoTest {
//
//	@Autowired
//	private lateinit var geoModel: GeoModel
//	@Autowired
//	private lateinit var geoAPI: GeoAPI
//	@Autowired
//	private lateinit var adminGeoAPI: AdminGeoAPI
//
//	@Autowired
//	private lateinit var regionOps: RegionOps
//	@Autowired
//	private lateinit var cityOps: CityOps
//
//	@Test
//	fun t01_createDefaultData() {
//		val region = geoModel.regionOps.createRegion("Тестовый")
//		cityOps.createCity("Foo Bar", region.id, geoX = 0.0, geoY = 0.0)
//		val city = geoModel.cityOps.findByName("Foo Bar", region.id)
//
//		val logo = null
//
//		val places = arrayOf(
//				geoModel.placeOps.createPlace("Хогвартс", city.id, GeoPoint(1.0, 12.0), true, "metro", logo),
//				geoModel.placeOps.createPlace("Republic Stadium", city.id, GeoPoint(2.2, 15.3333), true, "metro", logo),
//				geoModel.placeOps.createPlace("Richmond Plaza", city.id, GeoPoint(3.4, 18.1), true, "metro", logo),
//				geoModel.placeOps.createPlace("Березняки", city.id, GeoPoint(4.6, 20.0), true, "metro", logo)
//		)
//	}
//
//	@Test
//	fun `t02 after some creations there is only one object with given name`() {
//		val region = adminGeoAPI.regionCreate("Somewhere")
//		adminGeoAPI.regionCreate("Somewhere")
//		assertTrue { geoAPI.regions(true, 0, 5_000_000).filter { it.name == "Somewhere" }.toList().size == 1 }
//
//		val city = adminGeoAPI.cityCreate("Джетсвилль", region.id, 0.0, 0.0)
//		adminGeoAPI.cityCreate("Джетсвилль", region.id, geoX = 0.0, geoY = 0.0)
//		assertTrue { geoAPI.cities(regionId = null, showHidden = true, title = null).filter { it.name == "Джетсвилль" }.size == 1 }
//
//		val places = listOf(
//				adminGeoAPI.placeCreate(name = "Foo", cityId = city.id, geoX = 35.55, geoY = 45.05, isReal = true, type = "metro"),
//				adminGeoAPI.placeCreate(name = "Foo", cityId = city.id, geoX = -5.55, geoY = -5.05, isReal = true, type = "metro"),
//				adminGeoAPI.placeCreate(name = "Bar", cityId = city.id, geoX = 35.05, geoY = 45.55, isReal = true, type = "metro"),
//				adminGeoAPI.placeCreate(name = "Bar", cityId = city.id, geoX = -5.55, geoY = -5.05, isReal = true, type = "metro")
//		)
//
//		assert(geoAPI.places(cityId = city.id, regionId = null, showHidden = true, title = null).size == 2)
//	}
//
//	@Test
//	fun `t03 where we test get with filters`() {
//		val region = adminGeoAPI.regionCreate("Богемия")
//		val city = adminGeoAPI.cityCreate(name = "Прага", regionId = region.id, geoX = 0.0, geoY = 0.0)
//		adminGeoAPI.cityCreate(name = "Брно", regionId = region.id, geoX = 0.0, geoY = 0.0)
//		adminGeoAPI.cityCreate(name = "Пльзень", regionId = region.id, geoX = 0.0, geoY = 0.0)
//		assert(geoAPI.cities(region.id, showHidden = true, title = null).size >= 3)
//
//		adminGeoAPI.placeCreate(name = "Praha 4", cityId = city.id, geoX = 35.55, geoY = 35.55, isReal = true, type = "metro")
//		adminGeoAPI.placeCreate(name = "Praha 5", cityId = city.id, geoX = 35.65, geoY = 35.65, isReal = true, type = "metro")
//		adminGeoAPI.placeCreate(name = "Praha 6", cityId = city.id, geoX = 35.75, geoY = 35.75, isReal = true, type = "metro")
//		assert(geoAPI.places(city.id, null, showHidden = true, title = null).size >= 3)
//	}
//
//	@Test
//	fun `t04 remove null in object locations`() {
//		geoModel.objectLocationOps.repository.findAll().forEach {
//			if (it.streetName == null) {
//				it.streetType = "улица"
//				it.building = Random().int(1..100).toString()
//				it.streetName = when (Random().int(1..5)) {
//					1    -> "Незнамо-где"
//					2    -> "Черной дыры"
//					3    -> "Исследователей болот"
//					4    -> "св. Гейба Ньюэлла"
//					5    -> "Линуса Торвальдса"
//					6    -> "Святых Бэкэндеров"
//					else -> "А что, так тоже можно?"
//				}
//				geoModel.objectLocationOps.repository.save(it)
//			}
//		}
//
//		//		mongoTemplate.findAll<Partner>().forEach { mongoTemplate.save(it) }
//		//		mongoTemplate.findAll<Offer>().forEach { mongoTemplate.save(it) }
//
//	}
//
//	@Test
//	fun `t05 restore integrity of places and cities collections`() {
//		val regions = geoModel.regionOps.repository.findAll()
//		val cities = geoModel.cityOps.repository.findAll()
//		val places = geoModel.placeOps.repository.findAll()
//
//		regions.forEach { region ->
//			cities.filter { city -> city.parentRegion.id == region.id }.map { it.id }.forEach { region.cities.add(it) }
//			launch { geoModel.regionOps.repository.save(region) }
//		}
//
//		cities.forEach { city ->
//			places.filter { place -> place.parentCity.id == city.id }.map { it.id }.forEach { city.places.add(it) }
//			launch { geoModel.cityOps.repository.save(city) }
//		}
//	}
//
//	@Test
//	fun `t06 all that cities will have location`() {
//		val cities = cityOps.listCities(null, showHidden = true)
//
//		for ((i, it) in cities.withIndex()) {
//			println("Start $i")
//			if (it.geoX != null && it.geoY != null) {
//				println("No need to edit $i")
//				continue
//			}
//			val places = geoModel.placeOps.findByCityId(it.id).filter { it.point.x > 10 && it.point.y > 10 }
//			try {
//				it.geoX = places.map { it.point.x }.average()
//				it.geoY = places.map { it.point.y }.average()
//				cityOps.save(it)
//				println("End $i")
//			} catch (e: Exception) {
//				println("Failed $i")
//			}
//		}
//	}
//}