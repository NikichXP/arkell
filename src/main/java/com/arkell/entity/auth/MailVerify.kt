package com.arkell.entity.auth

import com.arkell.util.IDGenerator
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class MailVerify(
		var userId: String, var mail: String) {

	@Id
	var id: String = IDGenerator.shortId().substring(0..5).toLowerCase()

	constructor() : this("", "")

}