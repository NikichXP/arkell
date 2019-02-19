package com.arkell.api

import com.arkell.entity.Banner
import com.arkell.entity.misc.Platform
import com.arkell.model.BannerModel
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/banner")
class BannerAPI(
		private val bannerModel: BannerModel) {

	/**
	 * Warning: in future this will return paged result
	 */
	@GetMapping("/list")
	fun list(@RequestParam page: Int?, @RequestParam pageSize: Int?, @RequestParam cityId: String?,
	         @RequestParam regionId: String?, platform: Platform?): List<Banner> {
		return bannerModel.listBanners(page = page ?: 0, pageSize = pageSize ?: 20, cityId = cityId,
				regionId = regionId, platform = platform ?: Platform.app).toList()
	}

	@GetMapping("/{id}")
	fun getById(@PathVariable id: String): Banner {
		return bannerModel.getById(id)
	}

}