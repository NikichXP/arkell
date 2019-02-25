package com.arkell.model

import com.arkell.entity.MailBroadcast
import com.arkell.entity.UserEntity
import com.arkell.entity.auth.MailVerify
import com.arkell.model.internal.ConfigService
import com.arkell.model.internal.MailService
import com.arkell.repo.MailBroadcastRepo
import com.arkell.util.objects.Excludes
import com.arkell.util.objects.ObjectFromMapUpdater
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class MailTextsService(
		private val mailService: MailService,
		private val geoModel: GeoModel,
		private val newsModel: NewsModel,
		private val offerModel: OfferModel,
		private val specialProjectModel: SpecialProjectModel,
		private val mailBroadcastService: MailBroadcastService,
		private val categoryModel: CategoryModel,
		override val repository: MailBroadcastRepo,
		private val configService: ConfigService,
		private val jdbcTemplate: JdbcTemplate) : UpdateAction<MailBroadcast>() {

	@Scheduled(fixedDelay = 60_000L)
	fun scheduleBroadcast() {

		val lastCheck = configService.getParam("mail-scheduler", "msg.lastCheck", "0").toLong()

		val endTime = System.currentTimeMillis()

		repository.findAll { root, _, cb ->
			cb.between(root.get<Long>("date"), lastCheck, endTime)
		}.forEach {
			mailBroadcastService.broadcast(it)
		}

		configService.addParam("mail-scheduler", "msg.lastCheck", endTime.toString())

	}

	fun sendVerificationMail(verify: MailVerify) {
		mailService.sendMail("Подтверждение почты",
				"Здравствуйте!\n" +
						"\n" +
						"Вы зарегистирировались в программе привилегий Скидки для Вас!\n" +
						"\n" +
						"Ваш код подтверждения: ${verify.id}",
				verify.mail)
	}

	fun sendMailResetMail(code: String, userMail: String) {
		mailService.sendMail("Восстановление пароля",
				"Здравствуйте!\n\nВаш код для восстановления пароля к учётной записи Скидки Для Вас: $code\n\n" +
						"Если это были не Вы, удалите это письмо.",
				userMail)
	}

	fun createMailBroadcast(region: String?, category: String?, newsList: List<String>?, offerList: List<String>?,
	                        projectList: List<String>?, gender: UserEntity.Gender? = null, cities: List<String>?,
	                        regions: List<String>?, data: Map<String, String> = mapOf()): MailBroadcast {
		val broadcast = MailBroadcast().apply {
			ObjectFromMapUpdater(this, data).exclude(*Excludes.default).modify()
			this.region = region?.let { geoModel.regionOps.getById(it) }
			this.newsList = newsList?.map { newsModel.getById(it).id }?.toMutableList() ?: mutableListOf()
			this.offerList = offerList?.map { offerModel.getById(it).id }?.toMutableList() ?: mutableListOf()
			this.projectList = projectList?.map { specialProjectModel.getById(it).id }?.toMutableList()
					?: mutableListOf()
			this.gender = gender
			this.category = category?.let { categoryModel.getById(it) }
			cities?.let {
				this.cities = geoModel.cityOps.getByIds(it)
			}
			regions?.let {
				this.regions = geoModel.regionOps.getByIds(it)
			}
		}

		repository.save(broadcast)

		if (broadcast.date ?: 0 < configService.getParam("mail-scheduler", "msg.lastCheck", "0").toLong()) {
			mailBroadcastService.broadcast(broadcast)
		}

		return broadcast
	}

	fun edit(id: String, data: Map<String, String>, newsList: List<String>?, offerList: List<String>?, date: Long?,
	         projectList: List<String>?, categoryId: String?, gender: UserEntity.Gender? = null,
	         cities: List<String>?, regions: List<String>?) = autoEdit(id, data) {
		newsList?.let { this.newsList = it.map { newsModel.getById(it).id }.toMutableList() }
		offerList?.let { this.offerList = it.map { offerModel.getById(it).id }.toMutableList() }
		projectList?.let { this.projectList = it.map { specialProjectModel.getById(it).id }.toMutableList() }
		categoryId?.let { category = categoryModel.getById(it) }
		date?.let { this.date = it }
		gender?.let { this.gender = it }

		cities?.let {
			this.cities = geoModel.cityOps.getByIds(it)
		}
		regions?.let {
			this.regions = geoModel.regionOps.getByIds(it)
		}

		if (date != null && date < configService.getParam("mail-scheduler", "msg.lastCheck", "0").toLong()) {
			mailBroadcastService.broadcast(this)
		}
	}

	fun list(page: Int, pageSize: Int): Page<MailBroadcast> {
		return repository.findAll(PageRequest.of(page, pageSize))
	}

	fun getOfferList(id: String) = offerModel.getByIds(getById(id).offerList)
	fun getProjectList(id: String) = specialProjectModel.getByIds(getById(id).projectList)
	fun getNewsList(id: String) = newsModel.getByIds(getById(id).newsList)

	@PostConstruct
	fun postConstruct() {
		jdbcTemplate.query("select id, region_id from mailbroadcast where region_id notnull") {
			jdbcTemplate.update("insert into mailbroadcast_region(mailbroadcast_id, regions_id) values " +
					"('${it.getString("id")}', '${it.getString("region_id")}')")

			jdbcTemplate.update("update mailbroadcast set region_id = null where id = '${it.getString("id")}'")

		}
	}
}