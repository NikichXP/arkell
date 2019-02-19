package com.arkell.repo

import com.arkell.entity.interaction.MailSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface MailSubscriptionRepo : JpaRepository<MailSubscription, String>, JpaSpecificationExecutor<MailSubscription> {

	fun findByMail(mail: String): List<MailSubscription>

}