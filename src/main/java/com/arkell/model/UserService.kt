package com.arkell.model

import com.arkell.entity.Category
import com.arkell.entity.UserEntity
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.exception.ElementNotFoundException
import com.arkell.entity.geo.City
import com.arkell.entity.interaction.MailSubscription
import com.arkell.model.auth.AuthReasonService
import com.arkell.model.auth.AuthService
import com.arkell.repo.MailSubscriptionRepo
import com.arkell.repo.SpecificationHelper
import com.arkell.repo.UserRepo
import com.arkell.util.Locks
import com.arkell.util.objects.Excludes
import com.arkell.util.objects.ObjectFromMapUpdater
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UserService(
		private val authService: AuthService,
		@Lazy private val authReasonService: AuthReasonService,
		private val categoryModel: CategoryModel,
		private val mailSubscriptionRepo: MailSubscriptionRepo,
		@Lazy private val partnerModel: PartnerModel,
		@Lazy private val offerModel: OfferModel,
		override val repository: UserRepo) : UpdateAction<UserEntity>() {

	// TODO Cache this
	override fun getById(id: String): UserEntity =
			authService.getUserOrNull(id) ?: super.getById(id)

	fun list(page: Int, pageSize: Int, type: Array<AuthPermission>?, mail: String?): Page<UserEntity> {
		val filter = SpecificationHelper<UserEntity>()

		filter.textIgnoreCase("mail", mail)

		type?.let {
			filter.where { root, _, cb ->
				cb.or(*it.map {
					cb.equal(root.get<AuthPermission>("accessLevel"), it)
				}.toTypedArray())
			}
		}

		return filter.page(page, pageSize).result(repository)
	}

	fun getByMail(mail: String): UserEntity? = repository.findByMail(mail)

	fun createByMail(mail: String, accessType: AuthPermission): UserEntity {
		return UserEntity(mail).also { repository.save(it) }
	}

	fun createByMailAndPhone(mail: String, phone: String?, accessType: AuthPermission): UserEntity {
		return UserEntity(mail).apply { this.phone = phone }.also { repository.save(it) }
	}

	fun setUserAccessType(userId: String, accessLevel: AuthPermission) = update(userId) {
		it.accessLevel = accessLevel
	}

	override fun edit(id: String, update: UserEntity.() -> Unit): UserEntity {
		val user = getById(id)
		synchronized(this) {
			update(user)
			repository.save(user)
		}
		return user
	}

	fun updateInfo(id: String, birthday: LocalDate? = null, gender: String? = null,
	               data: Map<String, String> = mapOf()) = edit(id) {
		ObjectFromMapUpdater(this, data).exclude(*Excludes.user).modify()
		birthday?.also { this.birthday = it }
		gender?.also { this.gender = it }
	}

	@Throws(Exception::class)
	fun changeAccessType(userId: String, type: AuthPermission, author: UserEntity) = update(userId) {
		if (author.accessLevel.level < type.level) {
			throw IllegalAccessException("You can't do this. Have no permission.")
		}
		it.accessLevel = type
	}

	@Throws(Exception::class)
	override fun update(id: String, action: (UserEntity) -> Unit): UserEntity {
		val user = authService.getUserOrNull(id)
				?: repository.findById(id).orElseThrow { ElementNotFoundException("User $id not found") } //repository.findById(id).orElse(notFound("user::$id"))
		Locks.withBlock(user.id) {
			action(user)
			user.updated = System.currentTimeMillis()
			repository.save(user)
		}
		return user
	}

	fun changeMail(id: String, oldMail: String, newMail: String) = update(id) {

		if (it.mail != oldMail) {
			throw IllegalArgumentException("Wrong oldMail passed")
		}

		authReasonService.changeMail(oldMail, newMail)
		mailSubscriptionRepo.findByMail(oldMail).forEach {
			it.mail = newMail
			mailSubscriptionRepo.save(it)
		}

		it.mail = newMail
	}

	fun subscribeToNewsLetter(userId: String?, mail: String, category: Category?, city: City?, gender: UserEntity.Gender?): MailSubscription {
		if (userId == null && getByMail(mail) != null) {
			throw IllegalArgumentException("This mail is attached to other user, so you cannot set it as mail" +
					" (your token is missing or leads to no user)")
		}

		getSubscribesByMail(mail).filter {
			(category != null && it.category == null) && (city != null && it.city == null)
		}.forEach {
			mailSubscriptionRepo.delete(it)
		}

		val subscription = mailSubscriptionRepo.save(MailSubscription(mail).also {
			it.category = category
			it.city = city
			it.region = city?.parentRegion
			gender?.run { it.gender = this }
		})
		userId?.let {
			update(it) {
				it.subscription = subscription
				it.newsSubscribed = true
				try {
					subscription.gender = getGender(it.gender)
					mailSubscriptionRepo.save(subscription)
				} catch (e: Exception) {
				}
			}
		}
		return subscription
	}

	fun getSubscribesByMail(mail: String): List<MailSubscription> {
		return mailSubscriptionRepo.findByMail(mail)
	}

	fun getGender(gender: String): UserEntity.Gender {
		return when (gender.toLowerCase()) {
			"male" -> UserEntity.Gender.MALE
			"female" -> UserEntity.Gender.FEMALE
			else -> throw IllegalArgumentException("There are only 2 genders")
		}
	}

	fun createWithData(birthday: String, gender: String, accessType: AuthPermission?, data: Map<String, String>): UserEntity {
		val user = createByMail(data["mail"]!!, accessType ?: AuthPermission.CLIENT)
		return autoEdit(user.id, data) {
			accessType?.let { this.accessLevel = it }
			this.birthday = LocalDate.parse(birthday)
			this.gender = when (gender.toLowerCase()) {
				"male" -> "male"
				"female" -> "female"
				else -> throw IllegalArgumentException("There are only 2 genders")
			}
		}
	}

	fun unsubscribeFromLetters(mail: String): Boolean {
		mailSubscriptionRepo.findByMail(mail).forEach {
			repository.findBySubscription(it)?.also {
				it.subscription = null
				it.newsSubscribed = false
				repository.save(it)
			}
			mailSubscriptionRepo.deleteById(it.id)
		}
		return true
	}

	fun getSubscriptions(page: Int, pageSize: Int, cityId: String? = null, categoryId: String? = null): Page<MailSubscription> {
		return mailSubscriptionRepo.findAll(PageRequest.of(page, pageSize))
		//			.filter { !(cityId != null && it.city?.id != cityId) }
		//			.filter { !(categoryId != null && it.category?.id != categoryId) }
	}

	/**
	 * I use DB-get to ensure that category exists
	 */
	fun addFavoriteCategory(userId: String, categoryId: String) = update(userId) {
		it.favoriteCategoryIds.add(categoryModel.getById(categoryId).id)
	}

	fun setFavoriteCategories(userId: String, categories: Array<String>) = update(userId) {
		it.favoriteCategoryIds = categoryModel.getByIds(categories.toList()).map { it.id }.toMutableSet()
	}

	fun getFavoriteCategories(userId: String) = categoryModel.getByIds(getById(userId).favoriteCategoryIds,
			ignoreMissing = true)

	fun removeFavoriteCategory(userId: String, categoryId: String) = update(userId) {
		it.favoriteCategoryIds.remove(categoryId)
	}

	fun addFavoritePartner(userId: String, partnerId: String) = update(userId) {
		it.favoritePartnerIds.add(partnerModel.getById(partnerId).id)
	}

	fun getFavoritePartners(userId: String) = getById(userId).favoritePartnerIds

	fun getFavoritePartnersData(userId: String) = partnerModel.getByIds(getById(userId).favoritePartnerIds)

	fun removeFavoritePartner(userId: String, partnerId: String) = update(userId) {
		it.favoritePartnerIds.remove(partnerId)
	}

	fun addFavoriteOffer(userId: String, offerId: String) = update(userId) {
		it.favoriteOfferIds.add(offerModel.getById(offerId).id)
	}

	fun getFavoriteOffers(userId: String) = getById(userId).favoriteOfferIds

	@Transactional
	fun getFavoriteOffersData(userId: String) = offerModel.getByIds(getById(userId).favoriteOfferIds)

	fun removeFavoriteOffer(userId: String, offerId: String) = update(userId) {
		it.favoriteOfferIds.remove(offerId)
	}

}