package com.arkell.repo

import com.arkell.entity.MailBroadcast
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface MailBroadcastRepo : JpaRepository<MailBroadcast, String>, JpaSpecificationExecutor<MailBroadcast> {

	fun findByDateBetween(start: Long, end: Long): List<MailBroadcast>

}