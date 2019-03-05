package com.arkell.api

import com.arkell.entity.Offer
import com.arkell.entity.exception.ElementNotFoundException
import com.arkell.entity.geo.ObjectLocation
import com.arkell.entity.misc.Platform
import com.arkell.model.OfferModel
import com.arkell.model.auth.AuthService
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/offer")
class OfferAPI(
		private val offerModel: OfferModel,
		private val authService: AuthService) {

	@GetMapping("/{id}")
	fun getById(@PathVariable id: String): Offer {
		return offerModel.getById(id)
	}

	@GetMapping("/ids")
	fun ids(ids: Array<String>): List<Offer> {
		return offerModel.getByIds(ids.asList())
	}

	/**
	 * Get all offers by partner id
	 * @param id partner id
	 */
	@GetMapping("/partner")
	fun partner(@RequestParam id: String): List<Offer> {
		return offerModel.findByPartnerId(id)
	}

	@GetMapping("/locations")
	fun locations(@RequestParam id: String): List<ObjectLocation> {
		return offerModel.getOfferLocations(id)
	}

	/**
	 * Отображает предложения по заданным фильтрам. Не слать одновременно placeId, cityId или regionId,
	 * это понизит производительность запроса.
	 *
	 * @param categories список категорий, надеюсь работает.
	 * @param cityId работает!
	 * @param title фрагмент имени, в любом регистре.
	 * @param showHidden если true, показывает скрытые офферы. Иначе - выводит только visible офферы
	 * @param sort параметр сортировки. может быть "created", "updated", "priority" - по созданию, удалению и приоритету.
	 * @param isOnlineShop показать клиентов, которые интернет-магазин/не-интернет-магазин (или все если не передан)
	 * @param showGlobal вывести глобальные офферы. работает пока что только при условии, если не указан регион/город/место
	 * @param platform app, web, admin
	 * @param offerType can be CASHBACK, DISCOUNT, GIFT
	 */
	@GetMapping("/list")
	fun list(@RequestParam page: Int?, @RequestParam pageSize: Int?, @RequestParam offset: Int?,
	         @RequestParam title: String?, @RequestParam featured: Boolean?,
	         @RequestParam placeId: Array<String>?, @RequestParam categories: Array<String>?, @RequestParam cityId: String?,
	         @RequestParam regionId: String?, @RequestParam sort: String?, @RequestParam showHidden: Boolean?,
	         @RequestParam onlyClient: Boolean?, @RequestParam isOnlineShop: Boolean?, @RequestParam showGlobal: Boolean?,
	         @RequestParam platform: Platform?, @RequestParam offerType: String?, @RequestParam partnerId: String?,
	         @RequestParam cardType: String?): Page<Offer> {

		if (page != null) {
			return offerModel.find(
					categories = categories,
					partnerId = partnerId,
					title = title,
					sort = sort,
					page = page,
					offset = offset,
					regionId = regionId,
					cityId = cityId,
					places = placeId,
					pageSize = pageSize ?: 20,
					showHidden = showHidden ?: false,
					featured = featured,
					onlyClient = onlyClient ?: false,
					isOnlineShop = isOnlineShop,
					showGlobal = showGlobal,
					platform = platform ?: Platform.app,
					offerType = offerType?.let { Offer.OfferType.valueOf(it) },
					cardType = cardType)
		}

		if (offset == null) {
			throw IllegalArgumentException("Params 'page' AND 'offset' cannot be both null")
		}

		var addition = 0

		var pageResult: Page<Offer>

		do {
			pageResult = offerModel.find(
					categories = categories,
					partnerId = partnerId,
					title = title,
					sort = sort,
					page = null,
					offset = offset,
					regionId = regionId,
					cityId = cityId,
					places = placeId,
					pageSize = (pageSize ?: 20) + addition,
					showHidden = showHidden ?: false,
					featured = featured,
					onlyClient = onlyClient ?: false,
					isOnlineShop = isOnlineShop,
					showGlobal = showGlobal,
					platform = platform ?: Platform.app,
					offerType = offerType?.let { Offer.OfferType.valueOf(it) },
					cardType = cardType)

			val sum = pageResult.map { if (it.isBig == true) 4 else 1 }.sum()

			if (pageResult.size < (pageSize ?: 20) + addition) {
				break
			}

			addition += 4 - (sum % 4)

		} while (sum % 4 != 0)

		return pageResult
	}

	@GetMapping("/recommended")
	fun recommended(@RequestHeader token: String, @RequestParam platform: Platform, @RequestParam page: Int,
	                @RequestParam pageSize: Int?, @RequestParam featured: Boolean?): Page<Offer> {
		val user = authService.getUser(token)

		fun <T> Array<T>.nullIfEmpty(): Array<T>? = if (this.isEmpty()) {
			null
		} else {
			this
		}

		return offerModel.find(platform = platform, featured = featured, page = page, pageSize = pageSize ?: 7,
				categories = user.favoriteCategoryIds.toTypedArray().nullIfEmpty(), offset = null)
	}

	@GetMapping("/url/{url}")
	fun getByUrl(@PathVariable url: String): Offer {
		val ret = offerModel.getByUrl(url)
		if (ret.urlEnabled == true || ret.showWeb == true) {
			return ret
		} else {
			throw ElementNotFoundException("Url link is forbidden for this element.")
		}
	}

	@GetMapping("/title")
	fun title(@RequestParam title: String): List<Offer> {
		return offerModel.listByTitle(title)
	}

	/**
	 * Показывает, сколько у той или иной категории есть офферов в регионе. Не считает глобальные офферы.
	 * Возвращает количество офферов у каждой категории.
	 */
	@GetMapping("/category/count")
	fun count(@RequestParam regionId: String?): List<*> {
		return offerModel.categoryCount(regionId)
	}

	/**
	 * У партнёра есть и локация и скидки. Этот метод вернёт все скидки по ID локации транзитивно через партнёра
	 */
	@GetMapping("/located")
	fun located(@RequestParam locationId: String): List<Offer> {
		return offerModel.byLocation(locationId)
	}
}

