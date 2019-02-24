package com.arkell.model

import com.arkell.entity.Banner
import com.arkell.entity.geo.City
import com.arkell.entity.misc.Platform
import com.arkell.repo.BannerRepo
import com.arkell.repo.PlatformFeaturedSpecificationHelper
import com.arkell.repo.SpecificationHelper
import com.arkell.util.objects.Excludes
import com.arkell.util.objects.ObjectFromMapUpdater
import com.arkell.util.toLocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.lang.Exception
import java.time.LocalDateTime
import javax.annotation.PostConstruct

@Service
class BannerModel(
		override val repository: BannerRepo,
		private val geoModel: GeoModel,

		val jdbcTemplate: JdbcTemplate) : UpdateAction<Banner>() {

	fun createBanner(citiesIds: List<String>?, startDate: LocalDateTime?, endDate: LocalDateTime?, data: Map<String, String>,
	                 regionsIds: List<String>?): Banner {
		return repository.save(Banner().apply {
			ObjectFromMapUpdater(this, data).exclude(*Excludes.default).modify()
			startDate?.let { this.startDate = it }
			endDate?.let { this.endDate = it }
			citiesIds?.let {
				cities = geoModel.cityOps.getByIds(it).toMutableList()
				regions = cities.map { it.parentRegion }.toMutableList()
			}
			regionsIds?.let {
				regions = geoModel.regionOps.getByIds(it).toMutableList()
			}
		})
	}

	fun editBanner(id: String, startDate: Long?, endDate: Long?, citiesIds: List<String>?, regionsIds: List<String>?,
	               data: Map<String, String>) = autoEdit(
			id = id, params = data, exclude = *Excludes.default) {
		startDate?.let { this.startDate = it.toLocalDateTime() }
		endDate?.let { this.endDate = it.toLocalDateTime() }
		citiesIds?.let {
			cities = geoModel.cityOps.getByIds(it).toMutableList()
			regions = cities.map { it.parentRegion }.toMutableList()
		}
		regionsIds?.let {
			regions = geoModel.regionOps.getByIds(it).toMutableList()
		}
	}

	fun listBanners(page: Int, pageSize: Int = 20, cityId: String? = null, showHidden: Boolean? = null, sort: String? = null,
	                showInActive: Boolean? = null, regionId: String? = null, string: String? = null,
	                dateBefore: LocalDateTime? = null, dateAfter: LocalDateTime? = null, platform: Platform): Page<Banner> {

		val filter = SpecificationHelper<Banner>()

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

		filter.where(PlatformFeaturedSpecificationHelper.getFeaturedFilters(platform, showHidden = showHidden))

		if (showInActive != true) {
			filter.where { root, _, cb ->
				return@where cb.and(cb.lessThan(root.get<LocalDateTime>("startDate"), LocalDateTime.now()),
						cb.greaterThan(root.get<LocalDateTime>("endDate"), LocalDateTime.now()))
			}
		}

		if (string != null) {
			filter.where { root, _, cb ->
				cb.or(
						cb.like(cb.lower(root.get<String>("text")), "%${string.toLowerCase()}%"),
						cb.like(cb.lower(root.get<String>("title")), "%${string.toLowerCase()}%"),
						cb.like(cb.lower(root.get<String>("displayName")), "%${string.toLowerCase()}%")
				)
			}
		}

		return filter.sort(Sort.Direction.DESC, sort ?: "created").result(repository)
	}

	@PostConstruct
	fun migrateNews() {
		println("Migrate banners start")

		var doIt: Boolean
		var ctr = 0

		do {
			doIt = false

			jdbcTemplate.query("select * from banner where city_id notnull or region_id notnull limit 10") {

				try {
					if (it.getString("city_id") != null) {
						jdbcTemplate.update("insert into banner_city(banner_id, cities_id) " +
								"values ('${it.getString("id")}', '${it.getString("city_id")}')")
					}

					if (it.getString("region_id") != null) {
						jdbcTemplate.update("insert into banner_region(banner_id, regions_id) " +
								"values ('${it.getString("id")}', '${it.getString("region_id")}')")
					}

					jdbcTemplate.update("update banner set city_id = null, region_id = null where id = '${it.getString("id")}'")

					ctr++
					doIt = true
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}

		} while (doIt)

		println("Migrate banners end, done: $ctr")

	}

}