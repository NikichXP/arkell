package com.arkell.api.admin

import com.arkell.entity.Banner
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.misc.Platform
import com.arkell.model.BannerModel
import com.arkell.util.getParamData
import com.arkell.util.toLocalDateTime
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@Auth(value = AuthPermission.ADMIN)
@RestController
@RequestMapping("/api/admin/banner")
class AdminBannerAPI(
		private val bannerModel: BannerModel) {

	@PostMapping("/create")
	fun create(@RequestParam cityId: String?, request: HttpServletRequest, @RequestParam regionId: String?,
	           @RequestParam startDate: Long, @RequestParam endDate: Long): Banner {
		return bannerModel.createBanner(cityId = cityId, data = request.getParamData(), regionId = regionId,
				startDate = startDate.toLocalDateTime(), endDate = endDate.toLocalDateTime())
	}


	@GetMapping("/list", "/list/v2")
	fun list(@RequestParam page: Int, @RequestParam pageSize: Int?, @RequestParam cityId: String?,
	         @RequestParam regionId: String?, @RequestParam showHidden: Boolean?, @RequestParam showInactive: Boolean?,
	         @RequestParam string: String?, @RequestParam sort: String?, @RequestParam platform: Platform?): Page<Banner> {
		return bannerModel.listBanners(page = page, pageSize = pageSize ?: 20, cityId = cityId, showHidden = showHidden,
				sort = sort, showInActive = showInactive, regionId = regionId, string = string, platform = platform
				?: Platform.app)
	}

	@PostMapping("/edit")
	fun edit(@RequestParam id: String, @RequestParam startDate: Long?, @RequestParam endDate: Long?,
	         request: HttpServletRequest, @RequestParam cityId: String?, @RequestParam regionId: String?): Banner {
		return bannerModel.editBanner(id = id, startDate = startDate, endDate = endDate, data = request.getParamData(),
				cityId = cityId, regionId = regionId)
	}

	@PostMapping("/delete")
	fun delete(@RequestParam id: String): Boolean {
		return bannerModel.deleteById(id)
	}
}