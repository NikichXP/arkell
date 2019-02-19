package com.arkell.entity.interaction

import com.arkell.entity.*
import com.arkell.entity.geo.*
import com.arkell.util.IDGenerator
import javax.persistence.*

@Entity
class MailSubscription(var mail: String) {

	constructor() : this("")

	@Id
	var id: String = IDGenerator.longId()

	@ManyToOne
	var category: Category? = null
	@ManyToOne
	var city: City? = null
	@ManyToOne
	var region: Region? = null

	var name: String? = null

	var gender: UserEntity.Gender = UserEntity.Gender.MALE

}