package com.arkell.model

import com.arkell.entity.Offer
import com.arkell.entity.Partner
import com.arkell.entity.SpecialProject
import com.arkell.entity.misc.Platform
import com.arkell.repo.PlatformFeaturedSpecificationHelper
import com.arkell.repo.SpecialProjectRepo
import com.arkell.repo.SpecificationHelper
import com.arkell.util.objects.Excludes
import com.arkell.util.objects.ObjectFromMapUpdater
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SpecialProjectModel(
		override val repository: SpecialProjectRepo,
		private val categoryModel: CategoryModel,
		@Lazy
		private val offerModel: OfferModel,
		@Lazy
		private val partnerModel: PartnerModel,
		private val geoModel: GeoModel) : UpdateAction<SpecialProject>() {

	fun create(priority: Int?, categoryId: String?, data: Map<String, String> = mapOf(), regions: List<String>?,
	           cities: List<String>?): SpecialProject =
			repository.save(SpecialProject().apply {
				ObjectFromMapUpdater(this, data).exclude(*Excludes.default).modify()
				this.priority = priority ?: 20
				cities?.let { this.cities = geoModel.cityOps.getByIds(it).toMutableList() }
				regions?.let { this.regions = geoModel.regionOps.getByIds(it).toMutableList() }
				category = categoryId?.let { categoryModel.getById(it) }
			})

	fun list(showHidden: Boolean? = null, page: Int, pageSize: Int, sort: String? = null, offerId: String? = null,
	         title: String? = null, showInactive: Boolean? = null, platform: Platform, range: LongRange?,
	         featured: Boolean? = null, cityId: String? = null, regionId: String? = null): Page<SpecialProject> {

		val filter = SpecificationHelper<SpecialProject>()
				.textIgnoreCase("title", title)

		filter.where(PlatformFeaturedSpecificationHelper.getFeaturedFilters(platform, featured, showHidden))
		if (showHidden != true) {
			filter.where { root, _, cb -> cb.gt(root["endDate"], System.currentTimeMillis()) }
		}

		if (range != null) {
			filter.where { root, _, cb ->
				cb.between(root.get<Long>("startDate"), range.start, range.endInclusive)
				//				cb.and(cb.lt(root.get<Long>("startDate"), range.endInclusive),
				//						cb.gt(root.get<Long>("startDate"), range.start))
			}
		} else if (showInactive != true) {
			filter.where { root, _, cb ->
				cb.and(cb.lt(root.get<Long>("startDate"), System.currentTimeMillis()),
						cb.gt(root.get<Long>("endDate"), System.currentTimeMillis()))
			}
		}

		cityId?.let {
			filter.where { root, _, cb -> cb.isMember(geoModel.cityOps.getById(it), root.get("cities")) }
		}

		regionId?.let {
			filter.where { root, _, cb -> cb.isMember(geoModel.regionOps.getById(it), root.get("regions")) }
		}

		offerId?.let {
			filter.where { root, _, cb ->
				cb.isMember(it, root.get<List<String>>("offerList"))
			}
		}

		filter.page(page, pageSize).sort = when (sort) {
			"created" -> Sort.by(Sort.Direction.DESC, "created")
			"updated" -> Sort.by(Sort.Direction.DESC, "updated")
			else -> Sort.by(Sort.Direction.ASC, "priority")
		}
		return filter.result(repository)
	}

	@Transactional
	fun getByUrl(url: String): SpecialProject {
		return repository.getByUrl(url) ?: notFound(url)
	}

	fun partnerList(id: String): List<Partner> = partnerModel.getByIds(getById(id).partnerList)
	fun offerList(id: String): List<Offer> = offerModel.getByIds(getById(id).offerList)

	fun getOfferList(projectId: String): List<Offer> {
		val project = getById(projectId)
		return project.partnerList.flatMap { offerModel.findByPartnerId(it) }
	}

	fun addPartner(id: String, partnerId: String) = update(id) {
		val partner = partnerModel.getById(partnerId)
		it.partnerList.add(partner.id)
	}

	fun removePartner(id: String, partnerId: String) = update(id) {
		it.partnerList.remove(partnerId)
	}

	fun addOffer(id: String, offerId: String) = update(id) {
		val offer = offerModel.getById(offerId)
		it.offerList.add(offerId)
	}

	fun removeOffer(id: String, offerId: String) = update(id) {
		it.offerList.remove(offerId)
	}

	fun setData(id: String, partners: Array<String>?, offers: Array<String>?, regions: List<String>?,
	            cities: List<String>?) = edit(id) {
		partners?.let { this.partnerList = it.map { partnerModel.getById(it) }.map { it.id }.toMutableList() }
		offers?.let { this.offerList = it.map { offerModel.getById(it) }.map { it.id }.toMutableList() }
		cities?.let { this.cities = geoModel.cityOps.getByIds(it).toMutableList() }
		regions?.let { this.regions = geoModel.regionOps.getByIds(it).toMutableList() }
	}

	fun removePartnerFromEverything(partnerId: String) {
		repository.findByPartnerListContains(partnerId).forEach {
			it.partnerList.remove(partnerId)
			repository.save(it)
		}
	}

	fun update(id: String, data: Map<String, String>) = autoEdit(id, data) {
		// hello there
	}

	override fun deleteById(id: String) = repository.deleteById(id).let { true }


}

