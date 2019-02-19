package com.arkell.api

import com.arkell.entity.Category
import com.arkell.entity.misc.CardOffer
import com.arkell.model.CategoryModel
import com.arkell.model.ConfigModel
import com.arkell.model.InteractionsModel
import com.arkell.model.misc.CardOfferModel
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/config")
class ConfigAPI(
		private val configModel: ConfigModel,
		private val interactionsModel: InteractionsModel,
		private val categoryModel: CategoryModel,
		private val cardOfferModel: CardOfferModel) {

	/**
	 * Gets the feedback mail. Have no arguments.
	 */
	@GetMapping("/feedback/mail")
	fun feedbackMail(): String {
		return configModel.feedbackMail
	}

	/**
	 * Gets the 'request for a card' URL, have no arguments.
	 */
	@GetMapping("/card/url")
	fun cardUrl(): String {
		return configModel.cardUrl
	}

	/**
	 * Gets cards list (see api/admin/config/card/...)
	 */
	@GetMapping("/card/list")
	fun listCards(): List<CardOffer> {
		return cardOfferModel.list()
	}

	/**
	 * Return whole about-map
	 */
	@GetMapping("/about")
	fun getAboutMap(): Map<String, *> {
		return configModel.about
	}

	/**
	 * Return just value in about
	 */
	@GetMapping("/about/{field}")
	fun getAboutField(@PathVariable field: String): String {
		return configModel.about[field]!!
	}


	@GetMapping("/markers")
	fun markers(): List<String> = configModel.markerList

	/**
	 * Subscribe user to mail broadcasting.
	 * @param mail is user's mail.
	 */
	@PostMapping("/subscribeMail")
	fun subscribeMail(@RequestParam mail: String) {
		return interactionsModel.subscribeMail(mail)
	}

	/**
	 * Deletes user from mail broadcasting by mail
	 */
	@PostMapping("/unsubscribeMail")
	fun unsubscribeMail(@RequestParam mail: String) {
		return interactionsModel.unSubscribeMail(mail)
	}

	@GetMapping("/category/{id}")
	fun categoryById(@PathVariable id: String): Category {
		return categoryModel.getById(id)
	}

	/**
	 * Get categories list.
	 */
	@GetMapping("/categories")
	fun getCategories(@RequestParam page: Int?, @RequestParam pageSize: Int?, @RequestParam showHidden: Boolean?):
			Page<Category> {
		return categoryModel.listCategories(page ?: 0, pageSize ?: 20, showHidden)
	}

}