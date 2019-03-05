package com.arkell.api.admin

import com.arkell.entity.UserEntity
import com.arkell.entity.geo.GeoPoint
import com.arkell.entity.geo.ObjectLocation
import com.arkell.entity.geo.Place
import com.arkell.entity.interaction.MailSubscription
import com.arkell.model.*
import com.arkell.model.file.FileModel
import com.arkell.repo.MailSubscriptionRepo
import com.arkell.util.SheetsModelProxy
import com.arkell.util.randomOrNull
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

	/**
	 * Import places from XLSX file.
	 * Rows:
	 * 0 - id
	 * 1 - name
	 * 2 - city id
	 * 3, 4 - x, y geo coords
	 *
	 * optionals:
	 * 5 - type (default is 'metro') -- only if not empty
	 * 6 - logo
	 */
	@PostMapping("/start/places")
	fun startPlaces(request: HttpServletRequest): String {
		val file = fileModel.uploadToTempStorage(request.getPart("file"))
		val sheet = SheetsModelProxy(file).getSheet(0)

		val cities = geoModel.cityOps.repository.findAll()

		val repo = geoModel.placeOps.repository

		val ctr = AtomicInteger(0)

		jobs["cities"] = UpdateJob(
				ctr = ctr,
				size = sheet.size,
				task = launch {
					sheet.forEach {
						Place(name = it[1], parentCity = cities.find { city -> city.id == it[2] }!!,
								point = GeoPoint(it[3].toDouble(), it[4].toDouble())).apply {
							id = it[0]

							try {
								if (it[5].trim().isNotBlank()) {
									type = it[5]
								}
								if (it[6].trim().isNotBlank()) {
									logo = it[6]
								}
							} catch (e: Exception) {
							}
						}.also {
							repo.save(it)
						}
						ctr.incrementAndGet()
					}
				}
		)

		return "Job started"
	}

	/**
	 * Import places from XLSX file.
	 * Rows:
	 * 0 - not used (can be row number)
	 * 1 - place id
	 *
	 * not very necessary:
	 *
	 * 2 - streetName
	 * 3 - streetType
	 * 4 - building
	 * 5 - buildingSection
	 * 6 - territory
	 * 7 - postCode
	 * 8 - district
	 * 9 - comment
	 * 10 - work hours
	 * 11 - address string
	 * 12 - contact info
	 *
	 * optionals:
	 *
	 * 13, 14 - x, y geo coords
	 * 15 - marker on map -- is it used anywhere?
	 *
	 */
	@PostMapping("/start/objectlocation")
	fun startObjectLocationImport(@RequestParam partnerId: String, request: HttpServletRequest): String {
		val file = fileModel.uploadToTempStorage(request.getPart("file"))
		val sheet = SheetsModelProxy(file).getSheet(0)

		val places = geoModel.placeOps

		val ctr = AtomicInteger(0)

		jobs["cities"] = UpdateJob(
				ctr = ctr,
				size = sheet.size,
				task = launch {

					fun String.nullIfEmpty(): String? {
						return if (this.isBlank()) {
							null
						} else {
							this
						}
					}

					val partner = partnerModel.getById(partnerId)

					sheet.map { list ->
						if (list.size < 15) {
							val ret = list.toMutableList()
							while (ret.size < 15) {
								ret.add("")
							}
							return@map ret
						} else {
							list
						}
					}.forEach {
						try {
							launch {
								val place = places.findByName(it[1]).randomOrNull() ?: places.getById(it[1])

								val x = it[13].toDoubleOrNull()
								val y = it[14].toDoubleOrNull()

								val point = x?.let { GeoPoint(x, y!!) }

								ObjectLocation(place, point ?: place.point).apply {
									setPartner(partner)

									streetName = it[2].nullIfEmpty()
									streetType = it[3].nullIfEmpty()
									building = it[4].nullIfEmpty()
									buildingSection = it[5].nullIfEmpty()
									territory = it[6].nullIfEmpty()
									postCode = it[7].nullIfEmpty()
									district = it[8].nullIfEmpty()
									comment = it[9].nullIfEmpty()
									workHours = it[10].nullIfEmpty()
									addressString = it[11].nullIfEmpty()
									contactInfo = it[12].nullIfEmpty()

									it[15].nullIfEmpty()?.let {
										marker = it
									}

									geoModel.objectLocationOps.save(this)
								}
							}
						} catch (e: Exception) {
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

