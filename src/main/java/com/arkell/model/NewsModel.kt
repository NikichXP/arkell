package com.arkell.model

import com.arkell.entity.News
import com.arkell.entity.Offer
import com.arkell.entity.Partner
import com.arkell.entity.misc.Platform
import com.arkell.repo.NewsRepo
import com.arkell.repo.PlatformFeaturedSpecificationHelper
import com.arkell.repo.SpecificationHelper
import com.arkell.util.objects.Excludes
import com.arkell.util.objects.ObjectFromMapUpdater
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class NewsModel(
		override val repository: NewsRepo,
		private val geoModel: GeoModel,
		private val offerModel: OfferModel,
		private val partnerModel: PartnerModel,

		//temporary, for bg-update job
		private val jdbcTemplate: JdbcTemplate) : UpdateAction<News>() {


	fun create(priority: Int? = null, citiesIds: List<String>?, regionsIds: List<String>?, data: Map<String, String>,
	           beginDate: Long? = null, endDate: Long? = null, offers: Array<String>?, partners: Array<String>?) =
			News().apply {

				ObjectFromMapUpdater(this, data).exclude(*Excludes.default).modify()

				citiesIds?.let {
					cities = geoModel.cityOps.getByIds(it).toMutableList()
					regions = cities.map { it.parentRegion }.toMutableList()
				}
				regionsIds?.let {
					regions = geoModel.regionOps.getByIds(it).toMutableList()
				}

				this.beginDate = beginDate ?: System.currentTimeMillis()
				this.endDate = endDate ?: System.currentTimeMillis()

				this.partners = (partners ?: arrayOf()).map { partnerModel.getById(it) }.map { it.id }.toMutableList()
				this.offers = (offers ?: arrayOf()).map { offerModel.getById(it) }.map { it.id }.toMutableList()

				repository.save(this)
			}


	fun setPartnersAndOffers(id: String, partners: Array<String>?, offers: Array<String>?) = edit(id) {
		partners?.let { this.partners = it.map { partnerModel.getById(it) }.map { it.id }.toMutableList() }
		offers?.let { this.offers = it.map { offerModel.getById(it) }.map { it.id }.toMutableList() }
	}

	fun partnerList(id: String): List<Partner> = partnerModel.getByIds(getById(id).partners)
	fun offerList(id: String): List<Offer> = offerModel.getByIds(getById(id).offers)

	fun addPartner(id: String, partnerId: String) = update(id) {
		val partner = partnerModel.getById(partnerId)
		it.partners.add(partner.id)
	}

	fun removePartner(id: String, partnerId: String) = update(id) {
		it.partners.remove(partnerId)
	}

	fun addOffer(id: String, offerId: String) = update(id) {
		val offer = offerModel.getById(offerId)
		it.offers.add(offerId)
	}

	fun removeOffer(id: String, offerId: String) = update(id) {
		it.offers.remove(offerId)
	}

	fun listBy(name: String? = null, partnerId: String? = null, actual: Boolean, pageSize: Int, page: Int,
	           sort: String? = null, offerId: String? = null, regionId: String? = null, showHidden: Boolean? = null,
	           cityId: String? = null, platform: Platform, featured: Boolean? = null, archive: Boolean? = null): Page<News> {

		val filter = SpecificationHelper<News>()
				.with("partner" to partnerId?.let { partnerModel.getById(it) })
				.textIgnoreCase("title", name)
				.page(page, pageSize)

		if (cityId != null || regionId != null) {
			filter.where { root, _, cb ->
				return@where cb.or(cityId?.let {
					cb.isMember(geoModel.cityOps.getById(it), root.get("cities"))
				} ?: regionId?.let {
					cb.isMember(geoModel.regionOps.getById(it), root.get("regions"))
				}
				?: throw IllegalStateException(), cb.isEmpty(root.get("cities")))
			}
		}

		offerId?.let {
			filter.where { root, _, cb ->
				cb.isMember(it, root.get<List<String>>("offers"))
			}
		}

		filter.where(PlatformFeaturedSpecificationHelper.getFeaturedFilters(platform, featured, showHidden))

		if (actual) {
			filter.where { root, _, cb -> cb.gt(root["endDate"], System.currentTimeMillis()) }
		} else if (archive == true) {
			filter.where { root, _, cb -> cb.lt(root["endDate"], System.currentTimeMillis()) }
		}

		return filter.sort(if (sort != null) Sort.Direction.DESC else Sort.Direction.ASC, (sort
				?: "priority"), "created").result(repository)
	}

	fun edit(id: String, citiesIds: List<String>?, regionsIds: List<String>?, beginDate: Long? = null, endDate: Long? = null,
	         data: Map<String, String>) =
			autoEdit(id, data, exclude = *Excludes.default.toMutableList().apply { add("cityId") }.toTypedArray()) {
				citiesIds?.let {
					cities = geoModel.cityOps.getByIds(it).toMutableList()
					regions = cities.map { it.parentRegion }.toMutableList()
				}
				regionsIds?.let {
					regions = geoModel.regionOps.getByIds(it).toMutableList()
				}
				this.beginDate = beginDate ?: this.beginDate
				this.endDate = endDate ?: this.endDate
			}

	fun setVisible(id: String) = update(id) {
		it.imageUrl ?: throw IllegalStateException("ImageURL is null")
		it.terms ?: throw IllegalStateException("terms is null")
		it.workTerms ?: throw IllegalStateException("workTerms is null")
		it.visible = true
	}

	fun listAll(page: Int, pageSize: Int): Page<News> = repository.findAll(PageRequest.of(page, pageSize))

	fun getByUrl(url: String): News {
		return repository.getByUrl(url) ?: notFound(url)
	}

	@PostConstruct
	fun migrateNews() {
		println("Migrate news start")

		var doIt: Boolean
		var ctr = 0

		do {
			doIt = false

			jdbcTemplate.query("select * from news where cityid notnull or regionid notnull limit 10") {

				try {
					if (it.getString("cityid") != null) {
						jdbcTemplate.update("insert into news_city(news_id, cities_id) " +
								"values ('${it.getString("id")}', '${it.getString("cityid")}')")
					}

					try {
						if (it.getString("regionid") != null) {
							jdbcTemplate.update("insert into news_region(news_id, regions_id) " +
									"values ('${it.getString("id")}', '${it.getString("regionid")}')")
						}
					} catch (ignored: Exception) {
					}

					jdbcTemplate.update("update news set cityid = null, regionid = null where id = '${it.getString("id")}'")

					doIt = true
					ctr++
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}

		} while (doIt)

		println("Migrate news end, done: $ctr")

	}
}