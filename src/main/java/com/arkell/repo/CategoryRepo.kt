package com.arkell.repo

import com.arkell.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface CategoryRepo : JpaRepository<Category, String>, JpaSpecificationExecutor<Category> {

	fun findByName(name: String): Category?
	fun findByVisible(visible: Boolean): List<Category>
}