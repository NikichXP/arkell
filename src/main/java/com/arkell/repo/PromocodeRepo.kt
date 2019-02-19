package com.arkell.repo

import com.arkell.entity.Promocode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface PromocodeRepo : JpaRepository<Promocode, String>, JpaSpecificationExecutor<Promocode> {

	fun findByOwnerAndOfferId(owner: String, offerId: String): Promocode?

}