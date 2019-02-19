package com.arkell.entity

import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.interaction.MailSubscription
import com.arkell.util.IDGenerator
import java.time.LocalDate
import javax.persistence.*

@Entity
class UserEntity(var mail: String) : Saveable() {

	constructor() : this("")

	@Id
	override var id: String = IDGenerator.longId()
	var accessLevel: AuthPermission = AuthPermission.BLOCKED

	var name: String? = null
	var surname: String? = null
	var middleName: String? = null

	var birthday: LocalDate = LocalDate.of(1970, 1, 1)
	var gender: String = "male"
	var newsSubscribed: Boolean = false

	var isClient: Boolean? = false
		get() = field ?: false
		set(value) {
			field = value ?: false
		}

	var cnum: String? = null

	var phone: String? = null

	@ElementCollection(fetch = FetchType.EAGER)
	var favoriteCategoryIds = mutableSetOf<String>()
	@ElementCollection(fetch = FetchType.EAGER)
	var favoritePartnerIds = mutableSetOf<String>()
	@ElementCollection(fetch = FetchType.EAGER)
	var favoriteOfferIds = mutableSetOf<String>()

	@OneToOne
	var subscription: MailSubscription? = null

	override var created: Long? = System.currentTimeMillis()
		set(value) {
			field = value ?: System.currentTimeMillis()
		}
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = false

	enum class Gender(val gender: String) {
		MALE("male"), FEMALE("female")
	}

}