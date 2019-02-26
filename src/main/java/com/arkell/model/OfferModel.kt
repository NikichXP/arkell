package com.arkell.model

import com.arkell.entity.Category
import com.arkell.entity.Offer
import com.arkell.entity.Partner
import com.arkell.entity.geo.ObjectLocation
import com.arkell.entity.misc.Display
import com.arkell.entity.misc.Platform
import com.arkell.repo.OfferRepo
import com.arkell.repo.PlatformFeaturedSpecificationHelper
import com.arkell.repo.SpecificationHelper
import com.arkell.util.blockAwait
import com.arkell.util.nullIfFalse
import com.arkell.util.objects.Excludes
import com.arkell.util.objects.ObjectFromMapUpdater
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jboss.logging.Logger
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.criteria.Root

@Service
class OfferModel(
		private val geoModel: GeoModel,
		override val repository: OfferRepo,
		private val jdbcTemplate: JdbcTemplate,
		private val partnerModel: PartnerModel,
		@Lazy private val newsModel: NewsModel,
		@Lazy private val specialProjectModel: SpecialProjectModel,
		private val categoryModel: CategoryModel) : UpdateAction<Offer>() {

	private val logger = Logger.getLogger(this::class.java)

	fun create(data: Map<String, String> = mapOf(), partnerId: String, categoryId: String, offerType: Offer.OfferType,
	           amount: Double? = null, amounts: Array<Double>? = null, realisation: String?): Offer {

		val partner = partnerModel.getById(partnerId)
		return Offer(
				partner = partner,
				offerType = offerType
		).apply {

			ObjectFromMapUpdater(this, data).modify()

			this.mainCategory = categoryId.let { categoryModel.getById(it) }
			this.categories.add(this.mainCategory!!)
			this.realisation = realisation?.let { getRealisationFromString(it) }
			this.previewAmount = previewAmount ?: 0.0

			setUrl(this, data["url"])

			repository.save(this)
		}
	}

	private fun setUrl(offer: Offer, url: String?) {

		fun findAnyUrl(url: String) =
				jdbcTemplate.queryForList("select id from offer where url = '$url'")
						.map { it["id"]!! as String }

		fun checkUrlId(list: List<String>): Boolean {
			return when {
				list.isEmpty() -> true
				list.size == 1
						&& list[0] == offer.id -> true
				else -> false
			}
		}

		if (url == null) {
			return
		} else {

			if (checkUrlId(findAnyUrl(url))) {
				offer.url = url
				return
			}

			var ctr = 0

			while (!checkUrlId(findAnyUrl(url + ctr))) {
				ctr++
			}

			offer.url = url + ctr
		}
	}

	fun editOfferCategories(id: String, set: List<String>? = null,
	                        mainCategoryId: String?) = edit(id) {
		mainCategoryId?.let { this.mainCategory = categoryModel.getById(it) }
		set?.let {
			if (it.isEmpty()) {
				return@let
			}
			this.categories = it.map { categoryModel.getById(it) }.toMutableList()
		}
	}

	fun editOffer(offerId: String, data: Map<String, String>, visible: Boolean? = null,
	              display: Display? = null, previewAmount: Double? = null, offerType: Offer.OfferType?,
	              priority: Int?, featured: Boolean? = null, realisation: String?) = edit(offerId) {

		ObjectFromMapUpdater(this, data)
				.exclude(*Excludes.default)
				.modify()

		realisation?.let { this.realisation = getRealisationFromString(it) }
		offerType?.let { this.offerType = it }
		previewAmount?.let { this.previewAmount = if (it >= 0.0) it else null }

		if (data["featuredApp"] ?: data["featuredWeb"] != null) {
			repository.save(this)
			val offer = this
			launch { partnerModel.updateFeatured(offer.partnerId) }
		}

		setUrl(this, data["url"])

		updated = System.currentTimeMillis()
	}

	@Transactional
	fun listByTitle(title: String) = repository.findByTitleContainingIgnoreCase(title)

	fun listAll(page: Int, pageSize: Int): Page<Offer> = repository.findAll(PageRequest.of(page, pageSize))

	@Transactional
	fun findByPartnerId(id: String): List<Offer> = repository.findByPartnerId(id)

	fun getByUrl(url: String) = repository.findByUrl(url) ?: notFound(url)

	fun find(categories: Array<String>? = null, cityId: String? = null, sort: String? = null, regionId: String? = null,
	         places: Array<String>? = null, title: String? = null, featured: Boolean? = null,
	         showHidden: Boolean = false, onlyClient: Boolean = false, isOnlineShop: Boolean? = null,
	         showGlobal: Boolean? = null, platform: Platform, offerType: Offer.OfferType? = null,
	         partnerId: String? = null, cardType: String? = null,
	         page: Int?, pageSize: Int, offset: Int?): Page<Offer> {

		val filter = SpecificationHelper<Offer>()
		filter.textIgnoreCase("title", title)
		filter.with(
				"onlyClient" to onlyClient.nullIfFalse(),
				"offerType" to offerType,
				"partnerId" to partnerId
		)

		cardType?.also {
			if (listOf("standart", "gold", "private", "platinum").none { a -> it == a }) {
				throw IllegalArgumentException("only standart, gold, private, platinum are card types")
			}
			filter.where { root, _, cb -> cb.isNotNull(root.get<Double>("${it}Amount")) }
		}

		filter.where(PlatformFeaturedSpecificationHelper.getFeaturedFilters(platform, featured, showHidden))
		if (!showHidden) {
			filter.where { root, _, cb -> cb.gt(root["endDate"], System.currentTimeMillis()) }
		}

		isOnlineShop?.let {
			filter.where { root, criteriaQuery, cb ->
				val partnerRoot = criteriaQuery.from(Partner::class.java)
				return@where cb.and(
						cb.equal(root.get<String>("partnerId"), partnerRoot.get<String>("id")),
						cb.equal(partnerRoot.get<String>("isOnlineShop"), it)
				)
			}
		}

		// app don't recieve zero amount
		//		if (platform == Platform.app) {
		//			filter.where { root, _, cb ->
		//				cb.notEqual(root.get<Double>("previewAmount"), 0.0)
		//			}
		//		}

		categories?.let {
			filter.where { root, _, cb ->
				val list = jdbcTemplate.queryForList("select offer_id from offer_category where categories_id in ("
						+ it.map { "'$it'" }.reduce { a, b -> "$a, $b" } +
						")", String::class.java)

				return@where root.get<String>("id").`in`(list)
			}
		}

		if (cityId != null || places != null || regionId != null) {
			val id = regionId ?: cityId
			filter.where { root, criteriaQuery, cb ->
				criteriaQuery.distinct(true)
				val locationRoot: Root<ObjectLocation> = criteriaQuery.from(ObjectLocation::class.java)

				return@where if (places == null) {
					cb.and(
							cb.equal(root.get<String>("partnerId"), locationRoot.get<String>("partnerId")),
							cb.equal(locationRoot.get<String>(regionId?.let { "regionId" } ?: "cityId"), id)
					)
				} else {
					cb.and(
							cb.equal(root.get<String>("partnerId"), locationRoot.get<String>("partnerId")),
							locationRoot.get<String>("placeId").`in`(*places)
					)
				}
			}
		} else if (showGlobal == true) {
			filter.where { root, criteriaQuery, cb ->
				val partnerRoot = criteriaQuery.from(Partner::class.java)
				return@where cb.and(
						cb.equal(root.get<String>("partnerId"), partnerRoot.get<String>("id")),
						cb.equal(partnerRoot.get<Boolean>("isGlobal"), true)
				)
			}
		}

		filter.apply {
			if (page != null) {
				page(page, pageSize)
			} else if (offset != null) {
				offset(offset, pageSize)
			} else {
				throw IllegalStateException("page or offset must be set")
			}
		}.sort = when (sort) {
			"created" -> Sort.by(Sort.Direction.DESC, "created")
			"updated" -> Sort.by(Sort.Direction.DESC, "updated")
			else -> Sort.by(Sort.Direction.ASC, "priority")
		}

		return filter.result(repository)
	}

	fun categoryCount(regionId: String?): List<*> {

		if (regionId == null) {
			return partnerModel.globalCategoryCount()
		}

		return categoryModel.allVisible().map { category ->
			category to repository.count { root, query, cb ->
				val locRoot = query.from(ObjectLocation::class.java)
				return@count cb.and(
						cb.equal(root.get<Category>("mainCategory"), category),
						cb.and(
								cb.equal(root.get<String>("partnerId"), locRoot.get<String>("partnerId")),
								cb.equal(locRoot.get<String>("regionId"), regionId)
						)
				)
			}
		}.filter { it.second > 0 }
	}


	fun deleteOffer(id: String) {
		val news = async {
			newsModel.listBy(actual = false, pageSize = 1, page = 0, offerId = id, showHidden = true,
					platform = Platform.admin).content
		}
		val projects = async {
			specialProjectModel.list(showHidden = false, page = 0, pageSize = 1, offerId = id,
					platform = Platform.admin, range = null).content
		}
		if (news.blockAwait().size > 0) {
			throw IllegalArgumentException("Существует новость с id ${news.blockAwait()[0].id}, которая ссылается на этот оффер.")
		}
		if (projects.blockAwait().size > 0) {
			throw IllegalArgumentException("Существует проект с id ${projects.blockAwait()[0].id}, который ссылается на этот оффер.")
		}
		repository.deleteById(id)
	}

	fun getOfferLocations(id: String): List<ObjectLocation> {
		val offer = getById(id)
		return partnerModel.getPartnerLocations(offer.partnerId)
	}

	fun byLocation(locationId: String): List<Offer> {
		return findByPartnerId(geoModel.objectLocationOps.getById(locationId).partnerId!!)
	}

	private fun getRealisationFromString(str: String): Offer.Realisation? {
		if (str == "null") {
			return null
		}
		return Offer.Realisation.valueOf(str)
	}
}
