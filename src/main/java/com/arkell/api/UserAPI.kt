package com.arkell.api

import com.arkell.entity.Category
import com.arkell.entity.Offer
import com.arkell.entity.Partner
import com.arkell.entity.UserEntity
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.interaction.MailSubscription
import com.arkell.entity.misc.Platform
import com.arkell.model.CategoryModel
import com.arkell.model.FeedModel
import com.arkell.model.GeoModel
import com.arkell.model.UserService
import com.arkell.model.auth.AuthService
import com.arkell.util.getParamData
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import javax.servlet.http.HttpServletRequest

//@Auth(AuthPermission.USER)
@RestController
@RequestMapping("/api/user")
class UserAPI(
		private val userService: UserService,
		private val authService: AuthService,
		private val geoModel: GeoModel,
		private val categoryModel: CategoryModel,
		private val feedModel: FeedModel) {

	/**
	 * Get user info by auth token.
	 * @param token required to be sent as a request header
	 */
	@Auth(value = AuthPermission.USER)
	@GetMapping("/me")
	fun me(@RequestHeader token: String): UserEntity {
		return authService.getUser(token)
	}

	/**
	 * Sets user info. Apply only args that provided.
	 * @param birthday must be like 2018-07-31
	 * @param gender is "male" or "female", case insensitive.
	 *
	 */
	@PostMapping("/info")
	fun updateInfo(@RequestHeader token: String, @RequestParam birthday: String? = null,
	               @RequestParam gender: String? = null, request: HttpServletRequest): UserEntity {
		return userService.updateInfo(id = authService.getUser(token).id, birthday = birthday?.let { LocalDate.parse(it) },
				gender = gender?.let {
					when (it.toLowerCase()) {
						"male" -> "male"
						"female" -> "female"
						else -> throw IllegalArgumentException("There are only 2 genders")
					}
				}, data = request.getParamData())
	}

	// page пусть будет. мало ли.

	/**
	 * Gets response with 2 arrays: one is news, other is partners
	 */
	@GetMapping("/feed")
	fun feed(@RequestHeader token: String, @RequestParam platform: Platform?): Map<String, Any> {
		return feedModel.getNewsFeed(token, platform ?: Platform.app)
	}

	/**
	 * Subscribe to news
	 * @param token is optional. if not set - user wouldn't see subscription in profile
	 * @param mail user's email. if other user already have this email - exception will be thrown
	 * @param cityId optional, if not set - you will recieve news from all the cities (or not? hz)
	 * @param categoryId preferred category.
	 */
	@PostMapping("/subscribe/news")
	fun newsSubscribe(@RequestHeader token: String?, @RequestParam mail: String, @RequestParam cityId: String?,
	                  @RequestParam categoryId: String?, @RequestParam gender: String?): MailSubscription {
		return userService.subscribeToNewsLetter(userId = token?.let { authService.getUser(it).id }, mail = mail,
				city = cityId?.let { geoModel.cityOps.getById(it) }, gender = gender?.let { UserEntity.Gender.valueOf(it) },
				category = categoryId?.let { categoryModel.getById(it) })
	}

	/**
	 * Unsubscribe from news. Just send your mail and it is all.
	 */
	@PostMapping("/unsubscribe/news")
	fun newsUnSubscribe(@RequestParam mail: String): Boolean {
		return userService.unsubscribeFromLetters(mail)
	}

	/**
	 * Get list of favorite categories
	 */
	@GetMapping("/favorite/category")
	fun getFavoriteCategories(@RequestHeader token: String): List<Category> {
		return userService.getFavoriteCategories(authService.getUser(token).id)
	}

	/**
	 * Add user a favorite category
	 * @param id is category ID
	 */
	@PostMapping("/favorite/category/add")
	fun addFavoriteCategory(@RequestHeader token: String, @RequestParam id: String): UserEntity {
		return userService.addFavoriteCategory(authService.getUser(token).id, id)
	}

	/**
	 * Add user a favorite category
	 * @param id список ID категорий, разделённый запятыми.
	 */
	@PostMapping("/favorite/category/set")
	fun setFavoriteCategories(@RequestHeader token: String, @RequestParam id: Array<String>?, @RequestParam ids: String?): UserEntity {
		val resultSet = mutableSetOf<String>()
		id?.forEach { resultSet.add(it) }
		ids?.let { it.split(",").forEach { resultSet.add(it) } }
		return userService.setFavoriteCategories(authService.getUser(token).id, resultSet.toTypedArray())
	}

	/**
	 * Deletes favorite category from list
	 */
	@PostMapping("/favorite/category/delete")
	fun deleteFavoriteCategory(@RequestHeader token: String, @RequestParam id: String): UserEntity {
		return userService.removeFavoriteCategory(authService.getUser(token).id, id)
	}

	/**
	 * Get list of favorite partners
	 */
	@GetMapping("/favorite/partner")
	fun getFavoritePartners(@RequestHeader token: String): MutableSet<String> {
		return userService.getFavoritePartners(authService.getUser(token).id)
	}

	/**
	 * Gets list of favorite partners (entities)
	 */
	@GetMapping("/favorite/partner/data")
	fun getFavoritePartnersData(@RequestHeader token: String): List<Partner> {
		return userService.getFavoritePartnersData(authService.getUser(token).id)
	}

	/**
	 * Add user a favorite partner
	 * @param id is partner ID
	 */
	@PostMapping("/favorite/partner")
	fun addFavoritePartner(@RequestHeader token: String, @RequestParam id: String): UserEntity {
		return userService.addFavoritePartner(authService.getUser(token).id, id)
	}

	/**
	 * Deletes favorite partner from list
	 */
	@DeleteMapping("/favorite/partner/delete")
	fun deleteFavoritePartner(@RequestHeader token: String, @RequestParam id: String): UserEntity {
		return userService.removeFavoritePartner(authService.getUser(token).id, id)
	}

	/**
	 * Get list of favorite offers
	 */
	@GetMapping("/favorite/offer")
	fun getFavoriteOffers(@RequestHeader token: String): MutableSet<String> {
		return userService.getFavoriteOffers(authService.getUser(token).id)
	}

	/**
	 * Gets list of favorite offers (entities)
	 */
	@GetMapping("/favorite/offer/data")
	fun getFavoriteOffersData(@RequestHeader token: String): List<Offer> {
		return userService.getFavoriteOffersData(authService.getUser(token).id)
	}

	/**
	 * Add user a favorite offer
	 * @param id is Offer ID
	 */
	@PostMapping("/favorite/offer")
	fun addFavoriteOffer(@RequestHeader token: String, @RequestParam id: String): UserEntity {
		return userService.addFavoriteOffer(authService.getUser(token).id, id)
	}

	/**
	 * Deletes favorite offer from list
	 */
	@DeleteMapping("/favorite/offer/delete")
	fun deleteFavoriteOffer(@RequestHeader token: String, @RequestParam id: String): UserEntity {
		return userService.removeFavoriteOffer(authService.getUser(token).id, id)
	}
}