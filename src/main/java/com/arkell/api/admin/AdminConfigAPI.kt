package com.arkell.api.admin

import com.arkell.entity.Catalog
import com.arkell.entity.Category
import com.arkell.entity.ConfigPair
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.interaction.MailSubscription
import com.arkell.entity.misc.CardOffer
import com.arkell.model.CatalogModel
import com.arkell.model.CategoryModel
import com.arkell.model.ConfigModel
import com.arkell.model.InteractionsModel
import com.arkell.model.misc.CardOfferModel
import com.arkell.util.getParamData
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

/**
 * Some test description
 */
@Auth(value = AuthPermission.ADMIN)
@RestController
@RequestMapping("/api/admin/config")
class AdminConfigAPI(
		private val configModel: ConfigModel,
		private val interactionsModel: InteractionsModel,
		private val categoryModel: CategoryModel,
		private val catalogModel: CatalogModel,
		private val cardOfferModel: CardOfferModel) {

	@PostMapping("/data")
	fun setData(@RequestParam name: String, @RequestParam param: String, @RequestParam value: String): ConfigPair {
		return configModel.configService.addParam(name, param, value)
	}

	@GetMapping("/data")
	fun getData(@RequestParam name: String): ConfigPair {
		return configModel.configService.getConfig(name)
	}

	/**
	 * Set the mail for feedback
	 * @return 200 OK if feedback is set, with feedback entity in response.
	 */
	@PostMapping("/feedbackmail")
	fun setFeedbackMail(@RequestParam mail: String): String {
		return configModel.also { it.feedbackMail = mail }.feedbackMail
	}

	/**
	 * Set url for card register form
	 */
	@Deprecated("Use /card/add and others instead")
	@PostMapping("/card/url")
	fun setCardUrl(@RequestParam url: String): String {
		return configModel.also { it.cardUrl = url }.cardUrl
	}

	@PostMapping("/card/add")
	fun addCardInfo(request: HttpServletRequest): CardOffer {
		return cardOfferModel.addCardOffer(request.getParamData())
	}

	@GetMapping("/card/list")
	fun listCards(): List<CardOffer> {
		return cardOfferModel.list()
	}

	@PostMapping("/card/edit/{id}")
	fun editCard(@PathVariable id: String, request: HttpServletRequest): CardOffer {
		return cardOfferModel.edit(id, request.getParamData())
	}

	@PostMapping("/card/delete/{id}")
	fun deleteCard(@PathVariable id: String): Boolean {
		return cardOfferModel.deleteById(id)
	}

	/**
	 * Modifies about-map, replaces existing key or adding new one if absent
	 */
	@PostMapping("/about")
	fun setAboutText(@RequestParam param: String, @RequestParam text: String): MutableMap<String, *> {
		return configModel.addAbout(param, text)
	}

	/**
	 * Deletes entry in about-map
	 */
	@DeleteMapping("/about")
	fun about(@RequestParam param: String): Map<String, *> {
		return configModel.removeAbout(param)
	}

	@PostMapping("/marker/add")
	fun addMarker(@RequestParam image: String): MutableList<String> {
		return configModel.addMarker(image)
	}

	@PostMapping("/marker/delete")
	fun deleteMarker(@RequestParam image: String): MutableList<String> {
		return configModel.removeMarker(image)
	}

	/**
	 * List of mails, that have subscribed. Should I change the URL?
	 */
	@GetMapping("/mail/subscriptions")
	fun mailSubscriptions(@RequestParam page: Int, @RequestParam pageSize: Int?): Page<MailSubscription> {
		return interactionsModel.mailSubscriptions(page, pageSize ?: 20)
	}

	@PostMapping("/mail/header")
	fun setMailHeaderFooter(@RequestParam header: String?, @RequestParam footer: String?): ConfigPair {
		header?.let { configModel.configService.addParam(name = "mail-templates", param = "header", value = it) }
		footer?.let { configModel.configService.addParam(name = "mail-templates", param = "footer", value = it) }
		return configModel.configService.getConfig("mail-templates")
	}

	@GetMapping("/mail/header")
	fun getMailHeaderFooter(): ConfigPair {
		return configModel.configService.getConfig("mail-templates")
	}

	@GetMapping("/category/all")
	fun allCategory(): List<Category> {
		return categoryModel.listCategories(page = 0, pageSize = 1000, showHidden = true).content
	}

	/**
	 * @param defaultImg is array of images set as default
	 */
	@PostMapping("/category/create")
	fun createCategory(@RequestParam name: String, @RequestParam icon: String,
	                   @RequestParam defaultImg: Array<String>, request: HttpServletRequest): Category {
		return categoryModel.createCategory(request.parameterMap.mapValues { it.value[0] }, name = name,
				icon = icon, defaultImg = defaultImg)
	}

	/**
	 * Edit category. If you need to edit some field - send this param. If it is not sent - it wouldn't be modified.
	 * @param id is id of category to be edited
	 * @param defaultImg is default image for offer if it hasn't image. default for app AND web.
	 */
	@PostMapping("/category/edit")
	fun editCategory(@RequestParam id: String, request: HttpServletRequest, @RequestParam defaultImg: Array<String>?,
	                 @RequestParam imagesWeb: Array<String>?, @RequestParam imagesApp: Array<String>?): Category {
		return categoryModel.editCategory(id, data = request.parameterMap.mapValues { it.value[0] }, images = defaultImg,
				imagesApp = imagesApp, imagesWeb = imagesWeb)
	}

	/**
	 * Delete category and transfers all the objects to other category
	 */
	@PostMapping("/category/delete")
	fun categoryDelete(@RequestParam id: String, @RequestParam toId: String): Boolean {
		categoryModel.deleteCategory(id, toId)
		return true
	}

	/**
	 * Sets category to visible/invisible
	 */
	@PostMapping("/category/visible")
	fun visible(@RequestParam id: String, @RequestParam status: Boolean): Category {
		return categoryModel.edit(id) { visible = status }
	}

	@Auth(AuthPermission.NONE)
	@GetMapping("/catalog/list")
	fun catalogList(): List<Catalog> {
		return catalogModel.list()
	}

	@Auth(AuthPermission.NONE)
	@GetMapping("/catalog/{id}")
	fun catalogById(@PathVariable id: String): Catalog {
		return catalogModel.getById(id)
	}

	@Auth(AuthPermission.NONE)
	@GetMapping("/catalog/name/{name}")
	fun catalogByName(@PathVariable name: String): Catalog {
		return catalogModel.getByTitle(name)
	}

	@PostMapping("/catalog/addData")
	fun catalogAddData(@RequestParam id: String?, @RequestParam name: String?, @RequestParam data: String): Catalog {
		return catalogModel.addData(id, name, data)
	}

	@PostMapping("/catalog/deleteData")
	fun catalogDeleteData(@RequestParam id: String?, @RequestParam name: String?, @RequestParam data: String): Catalog {
		return catalogModel.deleteData(id, name, data)
	}

	@PostMapping("/catalog/delete")
	fun catalogDelete(@RequestParam id: String): Boolean {
		return catalogModel.deleteById(id)
	}

}