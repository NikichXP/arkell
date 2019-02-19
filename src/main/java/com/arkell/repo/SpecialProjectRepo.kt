package com.arkell.repo

import com.arkell.entity.News
import com.arkell.entity.SpecialProject
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface SpecialProjectRepo : JpaRepository<SpecialProject, String>, JpaSpecificationExecutor<SpecialProject> {

	fun findByPartnerListContains(partnerId: String): List<SpecialProject>
	fun getByUrl(url: String): SpecialProject?
}
