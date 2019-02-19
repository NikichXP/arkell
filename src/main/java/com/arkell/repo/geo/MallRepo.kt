package com.arkell.repo.geo

import com.arkell.entity.geo.Mall
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface MallRepo : JpaRepository<Mall, String>, JpaSpecificationExecutor<Mall> {
}