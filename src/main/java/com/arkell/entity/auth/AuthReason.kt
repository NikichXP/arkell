package com.arkell.entity.auth

import com.arkell.entity.Saveable
import com.arkell.entity.UserEntity
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
data class AuthReason(@Id var id: String,
                      var authSource: AuthSourceType = AuthSourceType.MAIL_PASS,
                      var authData: String,
                      var mail: String = "",
                      @ManyToOne
                      var user: UserEntity) {

	constructor() : this("", AuthSourceType.MAIL_PASS, "", "", UserEntity())

}