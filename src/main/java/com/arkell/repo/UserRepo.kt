package com.arkell.repo

import com.arkell.entity.UserEntity
import com.arkell.entity.interaction.MailSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface UserRepo : JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {

	fun findByMail(mail: String): UserEntity?
	fun findBySubscription(subscription: MailSubscription): UserEntity?

}
