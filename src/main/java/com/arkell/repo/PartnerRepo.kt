package com.arkell.repo

import com.arkell.entity.Partner
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface PartnerRepo : JpaRepository<Partner, String>, JpaSpecificationExecutor<Partner> {

	@Transactional
	override fun <S : Partner?> save(entity: S): S

	fun findByTitle(name: String): List<Partner>
	fun findByTitleContainingIgnoreCase(title: String): List<Partner>
	fun findByUrl(url: String): Partner?

}
