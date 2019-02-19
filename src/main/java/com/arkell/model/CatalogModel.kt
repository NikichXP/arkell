package com.arkell.model

import com.arkell.entity.Catalog
import com.arkell.repo.CatalogRepo
import org.springframework.stereotype.Service

@Service
class CatalogModel(
		override val repository: CatalogRepo) : UpdateAction<Catalog>() {

	fun create(title: String): Catalog = repository.save(Catalog().apply {
		this.title = title
	})

	fun getByTitle(title: String): Catalog = repository.getByTitle(title) ?: create(title)

	fun addData(id: String?, title: String?, vararg data: String) = edit(id
			?: title?.let { getByTitle(it).id }
			?: notFound().id) {
		data.forEach {
			if (!this.data.contains(it)) this.data.add(it)
		}
	}

	fun deleteData(id: String?, title: String?, vararg data: String) = edit(id
			?: title?.let { getByTitle(it).id }
			?: notFound().id) { this.data.removeAll(data) }

	fun list(): List<Catalog> {
		return repository.findAll()
	}

}