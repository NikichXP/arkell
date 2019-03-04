package com.arkell.test

import com.arkell.model.CategoryModel
import com.arkell.repo.CategoryRepo
import org.junit.FixMethodOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runners.MethodSorters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
internal class CategoryTest {
	
	@Autowired
	private lateinit var categoryModel: CategoryModel
	
	@Autowired
	private lateinit var categoryRepo: CategoryRepo
	
	val name = "TestCategoryName"
	
	@Test
	fun t01createCategory() {
		//		categoryModel.createCategory(mapOf(), "Спорт и здоровье", "noimage.jpg", arrayOf("default.jpg"))
		//		categoryModel.createCategory("Финансы", "noimage.jpg", arrayOf("default.jpg"))
		//		categoryModel.createCategory("Тестирование", "noimage.jpg", "group-123.png", arrayOf("default.jpg"))
		//		categoryModel.createCategory("Новая категория", "noimage.jpg", "group-124.png", arrayOf("default.jpg"))
		//		categoryModel.createCategory("Мода", "noimage.jpg", "group-125.png", arrayOf("default.jpg"))
		//		categoryModel.createCategory("Электроника", "noimage.jpg", "group-126.png", arrayOf("default.jpg"))
		//		categoryModel.createCategory("Фокусы", "noimage.jpg", "group-127.png", arrayOf("default.jpg"))
		//		assertTrue {
		////			categoryModel.listCategories(true).any { it.name == name }
		//			true
		//		}
	}
	
	@Test
	fun t02editCategory() {
		val categoryId = categoryModel.getByName(name).id
		//		categoryModel.editCategory(categoryId, "newname123456", null, images = null)
		//		assertTrue { categoryModel.getById(categoryId).name == "newname123456" }
		//		categoryModel.editCategory(categoryId, name, "noimage2.jpg", images = null)
		assertTrue { categoryModel.getById(categoryId).let { it.name == name && it.icon == "noimage2.jpg" } }
	}
	
	@Test
	fun t03deleteCategory() {
//		val cat = categoryModel.getByName(name)
//		categoryModel.deleteCategory(cat.id)
//		assertThrows<ElementNotFoundException> { categoryModel.getByName(name) }
	}
	
	
}