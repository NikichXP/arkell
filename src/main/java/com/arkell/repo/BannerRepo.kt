package com.arkell.repo

import com.arkell.entity.Banner
import com.arkell.entity.geo.City
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.LocalDateTime

interface BannerRepo : JpaRepository<Banner, String>, JpaSpecificationExecutor<Banner> {

	fun findByStartDateBeforeAndEndDateAfterAndCity(startDate: LocalDateTime, endDate: LocalDateTime, city: City): List<Banner>

	fun findByStartDateBeforeAndEndDateAfter(startDate: LocalDateTime, endDate: LocalDateTime): List<Banner>

}
