package com.arkell.api.admin

import com.arkell.entity.UserEntity
import com.arkell.entity.interaction.MailSubscription
import com.arkell.model.*
import com.arkell.model.file.FileModel
import com.arkell.repo.MailSubscriptionRepo
import com.arkell.util.SheetsModelProxy
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import org.springframework.web.bind.annotation.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/admin/import")
class AdminImportAPI(
		val fileModel: FileModel,
		val offerModel: OfferModel,
		val newsModel: NewsModel,
		val partnerModel: PartnerModel,
		val specialProjectModel: SpecialProjectModel,
		val mailSubscriptionRepo: MailSubscriptionRepo,
		val geoModel: GeoModel) {

	val jobs = ConcurrentHashMap<String, UpdateJob>()

	@GetMapping("/status")
	fun status(): Map<*, *> {
		return jobs.mapValues { "${it.value.ctr.get()}/${it.value.size}" }
	}

	/**
	 * @param request в запросе должен быть файл file.
	 * в файле 1 столбец - ID записи, второй - ФИО, третий - мейл, четвертый - ID региона.
	 */
	@PostMapping("/start/subscribes")
	fun startSubscribe(request: HttpServletRequest): String {
		val file = fileModel.uploadToTempStorage(request.getPart("file"))
		val sheet = SheetsModelProxy(file).getSheet(0)

		val regions = geoModel.regionOps.listRegions(true)

		val ctr = AtomicInteger(0)

		jobs["mailsubscribe"] = UpdateJob(
				ctr = ctr,
				size = sheet.size,
				task = launch {
					sheet.forEach {
						MailSubscription(it[2]).apply {
							name = it[1]
							region = regions.find { region -> region.id == it[3] }
							if (it[4].contains('f', true)) {
								gender = UserEntity.Gender.FEMALE
							}
						}.also {
							mailSubscriptionRepo.save(it)
						}
						ctr.incrementAndGet()
					}
				}
		)

		return "Job started"
	}

	@PostMapping("/start")
	fun import(request: HttpServletRequest, @RequestParam entity: String): String {

		/*listOf("offer" to "terms", "news" to "description", "partner" to "about", "specialproject" to "description")*/

		if (jobs.get(entity) != null) {
			throw IllegalStateException("Job is already started: progress = ${jobs[entity]!!.ctr.get()}/${jobs[entity]!!.size - 1}")
		}

		val file = fileModel.uploadToTempStorage(request.getPart("file"))

		val data = JsonParser().parse(file.readLines().reduce { a, b -> a + b })
			.asJsonObject.entrySet()

		val action: (Map.Entry<String, JsonElement>) -> Unit = when (entity) {
			"offer" -> ({
				val offer = offerModel.getById(it.key)
				offer.terms = it.value.asString
				launch { offerModel.save(offer) }
			})
			"news" -> ({
				val news = newsModel.getById(it.key)
				news.description = it.value.asString
				launch { newsModel.save(news) }
			})
			"partner" -> ({
				val partner = partnerModel.getById(it.key)
				partner.about = it.value.asString
				launch { partnerModel.save(partner) }
			})
			"specialproject" -> ({
				val specialProject = specialProjectModel.getById(it.key)
				specialProject.description = it.value.asString
				launch { specialProjectModel.save(specialProject) }
			})
			else -> throw IllegalArgumentException("Wrong entity name: can be offer, news, partner, specialproject")
		}

		val ctr = AtomicInteger(0)
		val size = data.size

		jobs[entity] = UpdateJob(
				ctr = ctr,
				size = size,
				task = launch {
					data.forEach {
						action(it)
						ctr.incrementAndGet()
					}
				}
		)

		return "Job started"
	}

	class UpdateJob(
			val task: Job,
			val ctr: AtomicInteger,
			val size: Int
	) {

		var error = false

		fun isDone(): Boolean {
			if (ctr.get() < size - 1) {
				return false
			}
			if (task.isCompleted) {
				return true
			}
			return true
		}

	}

}