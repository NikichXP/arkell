package com.arkell.model

import com.arkell.entity.Category
import com.arkell.entity.Partner
import com.arkell.entity.exception.CustomExceptionCode
import com.arkell.entity.exception.ElementNotFoundException
import com.arkell.entity.geo.ObjectLocation
import com.arkell.entity.geo.Place
import com.arkell.entity.interaction.PartnerSubmit
import com.arkell.entity.interaction.PartnerSubmitRepo
import com.arkell.entity.misc.Platform
import com.arkell.repo.PartnerRepo
import com.arkell.repo.PlatformFeaturedSpecificationHelper
import com.arkell.repo.SpecificationHelper
import com.arkell.util.OrdinalIDGetter
import com.arkell.util.blockAwait
import com.arkell.util.objects.Excludes
import com.arkell.util.objects.ObjectFromMapUpdater
import com.arkell.util.random
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.annotation.PostConstruct
import javax.persistence.criteria.Path
import javax.persistence.criteria.Root

@Service
class PartnerModel(
		override val repository: PartnerRepo,
		@Lazy private val offerModel: OfferModel,
		private val categoryModel: CategoryModel,
		private val partnerSubmitRepo: PartnerSubmitRepo,
		private val specialProjectModel: SpecialProjectModel,
		private val geoModel: GeoModel,
		private val jdbcTemplate: JdbcTemplate,
		private val ordinalIDGetter: OrdinalIDGetter) : UpdateAction<Partner>() {

	@PostConstruct
	fun postConstruct() {
		val places = geoModel.placeOps.findAllUnrealPlaces()
		val partners = mutableListOf<String>()
		jdbcTemplate.query("select id from partner where isglobal = true") { partners.add(it.getString("id")) }

		partners.forEach { id ->
			val map = mutableMapOf<String, Boolean>()
			places.forEach { map[it.id] = false }
			jdbcTemplate.query("select partnerid, placeid from objectlocation where isReal = false and partnerid = '$id'") {
				map[it.getString("placeid")] = true
			}
			map.filterValues { !it }.forEach { placeId, _ ->
				makeServiceLocation(places.find { it.id == placeId }!!, getById(id))
			}
		}

		launch { migrateData() }
	}

	fun createPartner(data: Map<String, String> = mapOf(), discountCategory: String, cityId: String?): Partner {
		val partner = Partner()
				.apply {
					id = ordinalIDGetter.getByTable("partner").toString()
					mainCategory = categoryModel.getById(discountCategory)
					categories.add(mainCategory!!)

					city = cityId?.let { geoModel.cityOps.getById(it) }
				}

		ObjectFromMapUpdater(partner, data).exclude(*Excludes.default).modify()

		return repository.save(partner)
	}

	fun addPartnerLocation(id: String, places: Array<String>, cityId: String?,
	                       data: Map<String, String>, mallId: String?) = edit(id) {
		val mall = async { mallId?.let { geoModel.mallOps.getById(it) } }

		val location: ObjectLocation = if (places.isNotEmpty()) {
			ObjectLocation(geoModel.placeOps.getById(places[0])).also {
				it.addPlaces(geoModel.placeOps.getByIds(places.asList()))
			}
		} else {
			if (cityId == null) {
				throw CustomExceptionCode(405, "Дайте хотя бы одно место или город")
			} else {
				val city = geoModel.cityOps.getById(cityId)
				ObjectLocation(geoModel.placeOps.findAnyUnRealPlace(city))
			}
		}
		location.setPartner(this)
		location.mall = mall.blockAwait()
		ObjectFromMapUpdater(location, data).exclude(*Excludes.default).modify()
		geoModel.objectLocationOps.save(location)
		this.locations.add(location.id)
		this.hasLocations = true
	}

	fun addContactPerson(id: String, name: String, phone: String, mail: String, position: String) = edit(id) {
		this.contactPersons.add(Partner.ContactPerson(name = name, phone = phone, mail = mail, position = position))
	}

	fun editContactPerson(partnerId: String, personId: String, data: Map<String, String>) = edit(partnerId) {
		ObjectFromMapUpdater(this.contactPersons.find { it.id == personId }!!, data)
				.exclude(*Excludes.default)
				.modify()
	}

	fun removeContactPerson(partnerId: String, personId: String) = edit(partnerId) {
		this.contactPersons.removeIf { it.id == personId }
	}

	fun getPartnerLocations(partnerId: String): List<ObjectLocation> = getById(partnerId)
			.locations.map { geoModel.objectLocationOps.getById(it) }

	fun deletePartnerLocation(partnerId: String, locationId: String) = edit(partnerId) {
		locations.remove(locationId) //.removeIf { it.id == locationId }
		geoModel.objectLocationOps.deleteById(locationId)
		this.hasLocations = geoModel.objectLocationOps.list(partnerId = this.id, showService = false, page = 0,
				pageSize = 1).size > 0
	}

	fun editPartner(id: String, visible: Boolean? = null, params: Map<String, String>, cityId: String?) = edit(id) {
		ObjectFromMapUpdater(this, params).exclude(*Excludes.partner).modify()
		visible?.let { this.visible = it }
		cityId?.let { city = geoModel.cityOps.getById(it) }
	}

	fun editPartnerCategories(id: String, add: List<String> = listOf(), delete: List<String> = listOf(),
	                          mainCategoryId: String?) = edit(id) {
		mainCategoryId?.let { mainCategory = categoryModel.getById(it) }
		delete.forEach { id ->
			this.categories.removeIf { it.id == id }
		}
		add.map { categoryModel.getById(it) }.forEach { categories.add(it) }
		if (!categories.any { mainCategory?.id == it.id }) {
			mainCategory?.let { categories.add(it) }
		}
	}

	fun submit(title: String, legalName: String, INN: String?, organisationForm: String, contactPerson: String?,
	           phone: String, email: String?, sellType: String, website: String?, shopsCount: Int?, cityId: String?
	): PartnerSubmit {
		return partnerSubmitRepo.save(PartnerSubmit(title, legalName, INN, organisationForm, contactPerson, phone,
				email, sellType, website, shopsCount).apply {
			cityId?.let { this.city = geoModel.cityOps.getById(it) }
		})
	}

	fun changeStatus(id: String, status: Int) = update(id) {
		if (status <= 0) {
			it.priority = 0
			it.status = Partner.Status.BLOCKED
			it.visible = false
		} else {
			it.priority = status
			it.status = Partner.Status.APPROVED
			it.visible = true
		}
	}

	fun updateFeatured(id: String) = update(id) {
		it.featuredApp = 0L < offerModel.repository.count { root, _, cb ->
			cb.and(
					cb.equal(root.get<Boolean>("featuredApp"), true),
					cb.equal(root.get<String>("partnerId"), id)
			)
		}

		it.featuredWeb = 0L < offerModel.repository.count { root, _, cb ->
			cb.and(
					cb.equal(root.get<Boolean>("featuredWeb"), true),
					cb.equal(root.get<String>("partnerId"), id)
			)
		}
	}

	fun getByTitle(name: String) = repository.findByTitle(name).random()

	fun getByUrl(url: String) = repository.findByUrl(url) ?: notFound(url)

	fun listPartners(page: Int, pageSize: Int = 20): Page<Partner> = repository.findAll(PageRequest.of(page, pageSize))

	@Transactional
	fun findByName(name: String): List<Partner> {
		return repository.findByTitleContainingIgnoreCase(name).toMutableList().also {
			if (repository.existsById(name)) {
				it.add(repository.findById(name).get())
			}
		}
	}

	fun makeGlobalPartner(partnerId: String) = update(partnerId) {

		if (it.isGlobal == true) {
			return@update
		}

		val places = geoModel.placeOps.findAllUnrealPlaces().distinctBy { it.parentCity.id }

		val jobs = mutableListOf<Deferred<*>>()

		places.forEach { place ->
			jobs += async { makeServiceLocation(place, it) }
		}

		jobs.forEach { it.blockAwait() }

		it.isGlobal = true
	}


	fun makeServiceLocation(place: Place, partner: Partner): ObjectLocation {
		return geoModel.objectLocationOps.save(ObjectLocation(place).apply {
			this.setPartner(partner)
			isService = true
			workHours = null
		})
	}


	fun makePartnerUnGlobal(partnerId: String) = update(partnerId) {
		geoModel.objectLocationOps.deleteServiceLocations(partnerId)
		it.isGlobal = false
	}

	fun setOnlinePoints(partnerId: String, cities: List<String>) = update(partnerId) {
		val locations = geoModel.objectLocationOps.findByPartner(it.id)

		locations.filter { point -> point.isReal != true && !cities.any { it == point.cityId } }
				.forEach { location -> launch { geoModel.objectLocationOps.deleteById(location.id) } }

		geoModel.cityOps
				.getByIds(cities.minus(locations.map { it.cityId }.filterNotNull().toSet()))
				.map { geoModel.placeOps.findAnyUnRealPlace(it) }
				.forEach {
					geoModel.objectLocationOps.createUnRealLocation(partnerId, it)
				}
	}

	// TODO Online-shop, no points, no offers
	fun filterPartners(
			categories: Array<String>? = null, cityId: String?, placeId: String? = null, regionId: String? = null, title: String?,
			showHidden: Boolean = false, sort: String?, page: Int, pageSize: Int = 20, featured: Boolean? = null,
			noPoints: Boolean? = null, isOnline: Boolean? = null, noOffers: Boolean? = null, platform: Platform): Page<Partner> {

		val filter = SpecificationHelper<Partner>().page(page, pageSize)

		if (title != null) {
			filter.where { root, _, cb ->
				cb.or(
						cb.like(cb.lower(root.get<String>("title")), "%${title.toLowerCase()}%"),
						cb.like(cb.lower(root.get<String>("id")), "%${title.toLowerCase()}%")
				)
			}
		}

		if (isOnline == true) {
			filter.with("sellType" to "Online")
		}

		filter.where(PlatformFeaturedSpecificationHelper.getFeaturedFilters(platform, featured, showHidden))

		categories?.map { categoryModel.getById(it) }?.let {
			filter.where { root, _, cb ->
				cb.or(
						*(it.map { cb.isMember(it, root.get<List<Category>>("categories")) }.toTypedArray())
				)
			}
		}

		if (noPoints != null) {
			filter.with("hasLocations" to !noPoints)
		}

		//		if (noOffers == true) {
		//			filter.where { root, criteriaQuery, cb ->
		//				val offerRoot = criteriaQuery.from(Offer::class.java)
		//				return@where cb.equal(
		//						cb.count(criteriaQuery
		//							.subquery(Offer::class.java)
		//							.select(offerRoot)
		//							.where(
		//									cb.equal(root.get<String>("id"), offerRoot.get<String>("partnerId"))
		//							)
		//							.from(ObjectLocation::class.java)
		//						),
		//						0)
		//			}
		//		}

		if (cityId != null || placeId != null || regionId != null) {
			val id = regionId ?: cityId ?: placeId
			filter.where { root, criteriaQuery, cb ->
				criteriaQuery.distinct(true)
				val locationRoot: Root<ObjectLocation> = criteriaQuery.from(ObjectLocation::class.java)
				val searchPath: Path<String> = locationRoot.get<String>(when {
					regionId != null -> "regionId"
					cityId != null -> "cityId"
					placeId != null -> "placeId"
					else -> throw IllegalStateException("DAFUQ just happens?")
				})

				return@where cb.and(
						cb.equal(root.get<String>("id"), locationRoot.get<String>("partnerId")),
						cb.equal(searchPath, id))

			}
		}

		filter.sort = when (sort) {
			"created" -> Sort.by(Sort.Direction.DESC, "created")
			"updated" -> Sort.by(Sort.Direction.DESC, "updated")
			else -> Sort.by(Sort.Direction.ASC, "priority")
		}

		return filter.result(repository)
	}

	fun deletePartner(id: String): Boolean {
		val partner = getById(id)
		offerModel.findByPartnerId(partner.id).forEach {
			offerModel.deleteOffer(it.id)
		}
		specialProjectModel.removePartnerFromEverything(id)
		repository.deleteById(partner.id)
		return true
	}

	fun listSubmits(): List<PartnerSubmit> {
		return partnerSubmitRepo.findAll()
	}

	fun getSubmitById(id: String): PartnerSubmit {
		return partnerSubmitRepo.findById(id).orElseThrow { ElementNotFoundException(id) }
	}

	fun deleteSubmit(id: String): String {
		return partnerSubmitRepo.deleteById(id).let { "deleted" }
	}

	fun byLocation(locationId: String): Partner {
		return getById(geoModel.objectLocationOps.getById(locationId).partnerId!!)
	}

	fun globalCategoryCount(): List<*> {
		return categoryModel.allVisible().map { category ->
			category to repository.count { root, query, cb ->
				return@count cb.equal(root.get<Category>("mainCategory"), category)
			}
		}
	}


	fun migrateData() {

		println("Partner data migration start")

		var doIt: Boolean
		var ctr = 0

		do {
			doIt = false

			jdbcTemplate.query("select * from partner where city_id notnull limit 10") {

				try {
					if (it.getString("city_id") != null) {
						jdbcTemplate.update("insert into partner_city(partner_id, cities_id) " +
								"values ('${it.getString("id")}', '${it.getString("city_id")}')")
					}

					jdbcTemplate.update("update partner set city_id = null where id = '${it.getString("id")}'")

					doIt = true
					ctr++
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}

		} while (doIt)

		println("Migrate Partner end, done: $ctr")
	}


}