package com.arkell.entity.auth

import com.arkell.entity.UserEntity
import com.arkell.util.IDGenerator
import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
data class RefreshToken(@ManyToOne val user: UserEntity) {

	constructor() : this(UserEntity())

	@Id
	@JsonIgnore
	var id: String = IDGenerator.longId()
	val refreshToken: String get() = id

}