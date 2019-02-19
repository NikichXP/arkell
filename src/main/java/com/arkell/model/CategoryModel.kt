package com.arkell.model

import com.arkell.entity.Category
import com.arkell.repo.CategoryRepo
import com.arkell.repo.SpecificationHelper
import com.arkell.util.blockAwait
import com.arkell.util.objects.Excludes
import com.arkell.util.objects.ObjectFromMapUpdater
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class CategoryModel(
		override val repository: CategoryRepo,
		private val jdbcTemplate: JdbcTemplate) : UpdateAction<Category>() {

	fun createCategory(data: Map<String, String>, name: String, icon: String, defaultImg: Array<String>): Category =
			Category(name = name, icon = icon).also {
				it.defaultImg = defaultImg.toMutableList()

				ObjectFromMapUpdater(it, data)
					.exclude(*Excludes.default)
					.modify()

				repository.save(it)
			}

	fun getByName(name: String): Category = repository.findByName(name) ?: notFound()

	fun listCategories(page: Int, pageSize: Int, showHidden: Boolean?): Page<Category> {
		val filter = SpecificationHelper<Category>()

		if (showHidden != true) {
			filter.with("visible" to true)
		}

		return filter.page(page, pageSize).sort(Sort.Direction.ASC, "title").result(repository)
	}

	fun editCategory(id: String, data: Map<String, String>, images: Array<String>?, imagesApp: Array<String>?,
	                 imagesWeb: Array<String>?) = autoUpdate(id, data) {
		images?.apply { it.defaultImg = this.toMutableList() }
		(imagesApp ?: images)?.apply { it.imagesApp = this.toMutableList() }
		(imagesWeb ?: images)?.apply { it.imagesWeb = this.toMutableList() }
	}

	fun deleteCategory(id: String, toId: String) {

		getById(toId)

		val tasks = mutableListOf<Deferred<String>>()

		tasks += async {
			try {
				jdbcTemplate.update("update mailbroadcast set category_id= ? where category_id = ? ", toId, id)
				jdbcTemplate.update("update objectlocation set category_id= ? where category_id = ? ", toId, id)
				return@async ""
			} catch (e: Exception) {
				return@async "1"
			}
		}

		tasks += async {
			try {
				jdbcTemplate.update("update offer set maincategory_id = ? where maincategory_id = ? ", toId, id)
				jdbcTemplate.update("update offer_category set categories_id = ? where categories_id = ? ", toId, id)
				jdbcTemplate.update("update offer_categoriesid set categoriesid = ? where categoriesid = ? ", toId, id)
				return@async ""
			} catch (e: Exception) {
				return@async "2"
			}
		}

		tasks += async {
			try {
				jdbcTemplate.update("update partner set maincategory_id = ? where maincategory_id = ? ", toId, id)
				jdbcTemplate.update("update partner_category set categories_id = ? where categories_id = ? ", toId, id)
				return@async ""
			} catch (e: Exception) {
				return@async "3"
			}
		}

		tasks += async {
			try {
				jdbcTemplate.update("update specialproject set category_id= ? where category_id = ? ", toId, id)
				jdbcTemplate.update("update userentity_favoritecategoryids set favoritecategoryids= ? where favoritecategoryids = ? ", toId, id)
				return@async ""
			} catch (e: Exception) {
				return@async "4"
			}
		}

		val s = tasks.map { it.blockAwait() }.reduce { a, b -> "$a$b" }

		if (s.isNotEmpty()) {
			throw IllegalStateException("Failed to delete category $id and merge to $toId: stage[$s]")
		}

		repository.deleteById(id).let { true }
	}

	/**
	 * TODO Do this optimized: cached or another shit
	 */
	fun getByIds(categoriesIds: Iterable<String>, ignoreMissing: Boolean = false): List<Category> {
		return categoriesIds.map {
			return@map try {
				getById(it)
			} catch (e: Exception) {
				if (ignoreMissing) {
					null
				} else {
					throw e
				}
			}
		}.filterNotNull()
	}

	fun editCategoryImages(id: String, add: Array<String>, delete: Array<String>) = edit(id) {
		defaultImg.removeAll(delete)
		defaultImg.addAll(add)
	}

	fun allVisible(): List<Category> {
		return repository.findByVisible(true)
	}

}