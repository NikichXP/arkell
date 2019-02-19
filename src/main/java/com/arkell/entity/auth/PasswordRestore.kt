package com.arkell.entity.auth

import com.arkell.util.IDGenerator
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class PasswordRestore(@Id var id: String = IDGenerator.base64Code(32),
                      var userId: String, var userMail: String) {

	constructor() : this("", "", "")

}