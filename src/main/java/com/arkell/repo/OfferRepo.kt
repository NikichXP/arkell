package com.arkell.repo

import com.arkell.entity.Category
import com.arkell.entity.Offer
import com.arkell.entity.Partner
import com.arkell.entity.geo.ObjectLocation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Repository
@Transactional(isolation = Isolation.READ_COMMITTED)
interface OfferRepo : JpaRepository<Offer, String>, JpaSpecificationExecutor<Offer> {

	fun findByPartnerId(partnerId: String): List<Offer>
	fun findByTitleContainingIgnoreCase(title: String): List<Offer>
	fun findByUrl(url: String): Offer?

}