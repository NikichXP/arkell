package com.arkell.repo

import com.arkell.entity.Catalog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CatalogRepo : JpaRepository<Catalog, String> {

	fun getByTitle(title: String): Catalog?

}