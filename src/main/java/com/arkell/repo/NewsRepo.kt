package com.arkell.repo

import com.arkell.entity.News
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface NewsRepo : JpaRepository<News, String>, JpaSpecificationExecutor<News> {

	override fun findAll(pageable: Pageable): Page<News>
	fun getByUrl(url: String): News?

}