package com.arkell.model

import com.arkell.entity.Promocode
import com.arkell.entity.UserEntity
import com.arkell.entity.exception.CustomExceptionCode
import com.arkell.model.internal.MailService
import com.arkell.repo.PromocodeRepo
import com.arkell.repo.SpecificationHelper
import com.arkell.util.Locks
import com.arkell.util.SheetsModelProxy
import kotlinx.coroutines.experimental.launch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

@Service
class PromocodeService(
		val offerModel: OfferModel,
		val mailService: MailService,
		override val repository: PromocodeRepo) : UpdateAction<Promocode>() {

	val offerIds = ConcurrentSkipListSet<String>()

	val jobs = ConcurrentHashMap<String, String>()

	@Scheduled(fixedDelay = 1_000 * 60 * 10)
	fun checkAlerts() {
		offerIds.map { offerModel.getById(it) }.forEach {

			val count = countPrivateCodes(it.id)

			if (countPrivateCodes(it.id) <= it.promocodeAlertCount ?: 0) {
				it.adminMail?.run {
					mailService.sendMail("Количество промокодов критично мало",
							"Добрый день! На предложении партнёра ${it.id} под названием \"${it.title}\" " +
									"осталось мало промокодов ($count) при наличии барьера в " +
									"${it.promocodeAlertCount ?: 0} кодов. Добавьте новых, пожалуйста.\n\n" +
									"С уважением,\nВаша заботливая система.")
				}
			}

			offerIds.remove(it.id)
		}
	}


	fun parseFile(offerId: String, file: File, isPublic: Boolean = false): Pair<String, String?> {
		val lines = when (file.extension.toLowerCase()) {
			"txt" -> getTxtFileContent(file)
			"xls", "xlsx" -> getXlsFileContent(file)
			else -> throw IllegalArgumentException("File not supported")
		}
		return insert(offerId, lines, isPublic)
	}

	private fun getXlsFileContent(file: File): List<String> {
		return SheetsModelProxy(file).getSheet(0).map { it[0] }
	}

	private fun getTxtFileContent(file: File): List<String> {
		return file.readLines()
	}

	fun insert(offerId: String, codes: List<String>, isPublic: Boolean = false): Pair<String, String?> {

		val offer = offerModel.getById(offerId)

		val entities = codes
			.map { Promocode(it, offer.id) }
			.onEach {
				it.isPublic = isPublic
				it.partnerId = offer.partnerId
			}

		if (entities.size < 500) {
			repository.saveAll(entities)
			return "done" to null
		}

		val packs = mutableListOf<List<Promocode>>()
		val uuid = UUID.randomUUID().toString()

		var i = 0

		while ((i * 100) <= entities.size) {
			packs.add(entities.drop(i * 100).take(100))
			i++
		}

		var j = 0

		launch {
			packs.forEach {
				jobs[uuid] = "$j/$i"
				j++
				repository.saveAll(it)
			}
		}

		return "working" to uuid
	}

	fun list(page: Int, pageSize: Int, offerId: String? = null, claimed: Boolean? = null,
	         public: Boolean? = null, partnerId: String? = null): Page<Promocode> {
		val filter = SpecificationHelper<Promocode>()

		filter.with("offerId" to offerId, "partnerId" to partnerId,
				"claimed" to claimed, "isPublic" to public)

		return filter.page(page, pageSize).sort(Sort.Direction.DESC, "created").result(repository)
	}

	fun getUserCodes(user: UserEntity) = getUserCodes(user.mail)

	fun getUserCodes(mail: String): List<Promocode> {
		return repository.findAll { root, _, cb -> cb.equal(root.get<String>("owner"), mail) }
	}

	@Throws(CustomExceptionCode::class)
	fun getAndClaimCode(offerId: String, user: UserEntity?) = getAndClaimCode(offerId, user?.mail)

	@Throws(CustomExceptionCode::class)
	fun getAndClaimCode(offerId: String, mail: String?): Promocode {

		var code = mail?.let { repository.findByOwnerAndOfferId(it, offerId) }

		if (code != null) {
			return code
		}

		var filter = SpecificationHelper<Promocode>()
		filter.with("isPublic" to true, "offerId" to offerId)
		code = filter.page(0, 1).result(repository).firstOrNull()

		if (code != null) {
			return code
		}

		mail ?: throw CustomExceptionCode(805, "There are no public promocodes in offer $offerId")

		Locks.withBlock(offerId) {
			filter = SpecificationHelper()
			filter.where { root, _, cb ->
				cb.and(
						cb.isNull(root.get<String>("owner")),
						cb.equal(root.get<String>("offerId"), offerId)
				)
			}
			filter.page(0, 1).result(repository).firstOrNull()?.let {
				offerIds.add(it.offerId)
				code = repository.save(it.claim(mail))
			}
		}

		return code ?: throw CustomExceptionCode(806, "There are no available promocodes in offer $offerId")
	}

	@Throws(CustomExceptionCode::class)
	fun getCode(offerId: String, mail: String?): Promocode {
		val filter = SpecificationHelper<Promocode>()

		filter.with("isPublic" to true)

		return filter.page(0, 1).result(repository).firstOrNull() ?: run {
			mail ?: throw IllegalStateException("There are no public promocodes in offer $offerId")
			return@run filter.with("isPublic" to false, "claimed" to false).result(repository).firstOrNull()
		} ?: throw IllegalStateException("There are no available promocodes in offer $offerId")
	}

	@Throws(CustomExceptionCode::class)
	fun claim(user: UserEntity, code: String) = claim(user.mail, code)

	@Throws(CustomExceptionCode::class)
	fun claim(mail: String, code: String) = edit(code) {

		if (isPublic) {
			throw CustomExceptionCode(804, "Cannot claim public code")
		}

		if (claimed && mail != owner) {
			throw IllegalAccessException("This code is claimed by other user")
		}

		claimed = true
		owner = mail
	}

	fun countPrivateCodes(offerId: String): Int {
		return repository.count { root, _, cb ->
			cb.and(
					cb.equal(root.get<String>("offerId"), offerId),
					cb.equal(root.get<Boolean>("isPublic"), false)
			)
		}.toInt()
	}

	@Throws(CustomExceptionCode::class)
	fun sendToMail(mail: String, offerId: String): String {
		val code = getAndClaimCode(offerId = offerId, mail = mail)
		val offer = offerModel.getById(offerId)
		mailService.sendMail("Ваш промокод", getPromocodeText(offer.title, code.id), mail)
		return "ok"
	}

	private fun getPromocodeText(title: String, code: String): String {
		return File(System.getProperty("user.dir") + "/mail-promocode.html").readLines()
			.reduce { a, b -> a + b }
			.replace("{{КОМПАНИЯ}}", title)
			.replace("{{ПРОМОКОД}}", code)
	}
}