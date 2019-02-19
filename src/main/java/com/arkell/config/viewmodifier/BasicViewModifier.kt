package com.arkell.config.viewmodifier

import com.arkell.entity.*
import com.arkell.entity.auth.AuthReason
import com.arkell.entity.auth.RefreshToken
import com.arkell.model.GeoModel
import com.arkell.model.OfferModel
import com.arkell.model.PartnerModel
import com.arkell.model.auth.AuthService
import com.arkell.util.toLong
import com.arkell.util.toMap
import com.google.gson.Gson
import org.springframework.data.domain.Page
import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * This class do pre-write modification of class.
 */
@Service
class BasicViewModifier(
		private val partnerModel: PartnerModel,
		private val offerModel: OfferModel,
		private val geoModel: GeoModel,
		private val authService: AuthService) {

	val gson = Gson()

	@Suppress("UNCHECKED_CAST")
	final fun modify(entity: Any?, request: ServerHttpRequest): Any? =
			when (entity) {
				is Page<*> -> modifyPage(entity as Page<Any>, request)
				is Collection<*> -> modifyCollection(entity, request)
				is Map<*, *> -> modifyMap(entity, request)
				is Offer -> modifyOffer(entity, request)
				is Partner -> modifyPartner(entity, request)
				is UserEntity -> modifyUser(entity, request)
				is RefreshToken -> modifyToken(entity)
				is SpecialProject -> modifySpecialProject(entity, request)
				is Banner,
				is AuthReason -> entity.toMap()
				else -> entity
			}.let {
				if (it is Map<*, *>) {
					return@let it.mapValues { pair ->
						val value = pair.value
						return@mapValues when (value) {
							is LocalDateTime -> value.toLong()
							is LocalDate -> value.toString()
							else -> value
						}
					}
				} else {
					return@let it
				}
			}

	@Transactional
	fun modifySpecialProject(entity: SpecialProject, request: ServerHttpRequest): Any? {
		val map = entity.toMap()
		if (request.uri.toString().contains("forceEntities=true")) {
			map["offerEntities"] = modify(offerModel.getByIds(entity.offerList), request)
			map["partnerEntities"] = partnerModel
				.getByIds(entity.partnerList)
				.map { it.toMap() }
		}
		return map
	}

	private fun modifyUser(entity: UserEntity, request: ServerHttpRequest): Any? {
		val map = entity.toMap()
		map["gender"] = when (map["gender"]) {
			"male" -> UserEntity.Gender.MALE
			"female" -> UserEntity.Gender.FEMALE
			else -> null
		}
		map.mapValues { modify(it.value, request) }
		return map
	}

	private fun modifyToken(entity: RefreshToken): Any? {
		val map = entity.toMap()
		map["refreshToken"] = entity.id
		map.remove("id")
		return map
	}

	fun onPreBuild(entity: Any?, request: ServerHttpRequest): Any {
		return if (entity is Map<*, *>) {
			modifyMap(entity, request)
		} else {
			mapOf("data" to entity, "status" to "ok")
		}
	}

	fun modifyMap(returnEntity: Map<*, *>, request: ServerHttpRequest): Map<*, *> {
		return returnEntity.mapValues { modify(it.value, request) }
		//		return if (
		//				((ret["status"] as? String)?.toLowerCase() == "ok" && ret["data"] != null) ||
		//				((ret["status"] as? String)?.toLowerCase() == "error" && ret["message"] != null)
		//		) {
		//			ret
		//		} else {
		//			mapOf<String, Any?>(
		//					"data" to ret,
		//					"status" to "ok"
		//			)
		//		}
	}

	fun <T> modifyPage(page: Page<T>, request: ServerHttpRequest): Any {
		val map = HashMap<String, Any>()
		map["status"] = "ok"
		map["data"] = page.content.map { modify(it, request) }
		map["paging"] = mapOf(
				"page" to page.number,
				"pageSize" to page.numberOfElements,
				"maxPage" to page.totalPages,
				"size" to page.totalElements
		)
		return map
	}

	fun modifyCollection(list: Iterable<*>, request: ServerHttpRequest): Any = list
		.map {
			return@map if (it is String) {
				it
			} else {
				modify(it, request)
			}
		}

	fun modifyOffer(offer: Offer, request: ServerHttpRequest): Map<String, Any?> {
		val ret = offer.toMap()
		ret.remove("partnerId")
		ret["partner"] = partnerModel.getById(offer.partnerId).run {

			return@run modifyPartner(this, request).also {
				if (request.uri.toString().contains("forceLocations=true") ||
						request.headers.getValue("user-agent")[0].contains("ru.arkell.DiscountsForYou")) { //Raiffeisen/1.0.1 (ru.arkell.DiscountsForYou; build:3; iOS 11.4.1) Alamofire/4.7.2
					ret["locations"] = this.locations.map { geoModel.objectLocationOps.getById(it) }
				}
			}

		}
		ret["endDate"] = ret["endDate"]!!.let {
			if (it.toString().toLong() > System.currentTimeMillis() * 1.8) {
				null
			} else {
				it
			}
		}
		ret["amounts"] = arrayOf(
				offer.standartAmount,
				offer.goldAmount,
				offer.premiumAmount,
				offer.privateAmount
		)

		request.headers["token"]
			?.also {
				ret["isFavorite"] = authService.getUser(it[0])
					.let { it.favoritePartnerIds.contains(offer.partnerId) || it.favoriteOfferIds.contains(offer.id) }
			}
		return ret
	}

	fun modifyPartner(partner: Partner, request: ServerHttpRequest): Map<String, Any?> {
		val ret = partner.toMap()
		request.headers["token"]
			?.also { ret["isFavorite"] = authService.getUser(it[0]).favoritePartnerIds.contains(partner.id) }

		if (request.uri.toString().contains("forceLocations=true") ||
				request.headers.getValue("user-agent")[0].contains("ru.arkell.DiscountsForYou")) { //Raiffeisen/1.0.1 (ru.arkell.DiscountsForYou; build:3; iOS 11.4.1) Alamofire/4.7.2
			ret["locations"] = partner.locations.map { geoModel.objectLocationOps.getById(it) }
		}
		return ret
	}

}