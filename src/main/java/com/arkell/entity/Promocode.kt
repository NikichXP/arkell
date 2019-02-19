package com.arkell.entity

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Promocode(
		@Id override var id: String,
		var offerId: String) : Saveable() {

	@Deprecated("Do not use this. For JPA only.")
	constructor() : this(UUID.randomUUID().toString(), "-1")

	var isPublic = true
	var claimed = false
	var owner: String? = null

	var partnerId: String? = null

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true

	fun claim(mail: String) = apply {
		owner = mail
		claimed = true
	}

}