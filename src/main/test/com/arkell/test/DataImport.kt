package com.arkell.test

import com.arkell.api.AuthAPI
import com.arkell.entity.*
import com.arkell.entity.auth.AuthReason
import com.arkell.entity.geo.*
import com.arkell.entity.interaction.Feedback
import com.arkell.export.ExportCoreUtil
import com.arkell.model.GeoModel
import com.arkell.model.OfferModel
import com.arkell.model.PartnerModel
import com.arkell.model.file.FileModel
import com.arkell.model.internal.MailService
import com.arkell.repo.*
import com.arkell.repo.auth.AuthReasonRepo
import com.arkell.util.*
import com.google.gson.Gson
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.File
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

@ExtendWith(SpringExtension::class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DataImport {
	
	@Autowired
	private lateinit var authAPI: AuthAPI
	@Autowired
	private lateinit var mailService: MailService
	@Autowired
	private lateinit var geoModel: GeoModel
	@Autowired
	private lateinit var categoryRepo: CategoryRepo
	@Autowired
	private lateinit var partnerModel: PartnerModel
	@Autowired
	private lateinit var offerRepo: OfferRepo
	@Autowired
	private lateinit var fileModel: FileModel
	@Autowired
	private lateinit var offerModel: OfferModel
	@Autowired
	private lateinit var exportCoreUtil: ExportCoreUtil
	
	val xlsDoc = SheetsModelProxy(File("D:/SDV_ data.xlsx"))
	
	
	@Autowired
	private lateinit var authReasonRepo: AuthReasonRepo
	@Autowired
	private lateinit var userRepo: UserRepo
	@Autowired
	private lateinit var bannerRepo: BannerRepo
	@Autowired
	private lateinit var configPairRepo: ConfigPairRepo
	@Autowired
	private lateinit var specialProjectRepo: SpecialProjectRepo
	@Autowired
	private lateinit var feedbackRepo: FeedbackRepo
	@Autowired
	private lateinit var newsRepo: NewsRepo
	
	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate
	@Autowired
	private lateinit var ordinalIDGetter: OrdinalIDGetter
	
	@Test
	fun createTableJson() {
		
		val pairs = listOf("offer" to "terms", "news" to "description", "partner" to "about", "mailbroadcast" to "content",
				"specialproject" to "description")
		
		for (pair in pairs) {
			val (table, field) = pair
			
			val map = mutableMapOf<String, String>()
			
			// select   id,   convert_from(loread(lo_open(terms :: int, x'40000' :: int), x'40000' :: int), 'UTF-8') AS terms from offer
			jdbcTemplate.queryForList("select id, convert_from(loread(lo_open($field :: int, x'40000' :: int), x'40000' :: int), 'UTF-8') AS $field from $table where $field notnull limit 100000").forEach {
				map[it["id"].toString()] = it[field].toString()
			}
			
			val json = Gson().toJson(map)
			val file = File("D:/export-$table-$field.json")
			file.createNewFile()
			
			file.writeText(json)
		}
		
	}
	
	@Test
	fun fullExport() {
		
		//		repoToClassMap().forEach { k, v ->
		//			try {
		//				val file: File = exportCoreUtil.export<Any>(repo = k as JpaRepository<Any, String>, clazz = v as Class<Any>)
		//				file.absolutePath.print("Created: ")
		//			} catch (e: Exception) {
		//				println(v.name + " failed.")
		//			}
		//		}
		//
	}
	
	@Test
	fun test() {
		println(ordinalIDGetter.getByTable("partner"))
	}
	
	@Test
	fun fullImport() {
		//		exportCoreUtil.import(geoModel.regionOps.repository)
		//		exportCoreUtil.import(geoModel.cityOps.repository)
		//		exportCoreUtil.import(geoModel.placeOps.repository)
		//		exportCoreUtil.import(categoryRepo)
		//		exportCoreUtil.import(configPairRepo)
		//		exportCoreUtil.import(partnerModel.repository)
		//		exportCoreUtil.import(geoModel.objectLocationOps.repository)
		//		exportCoreUtil.import(offerRepo)
		//		exportCoreUtil.import(userRepo)
		//		exportCoreUtil.import(authReasonRepo)
		//		exportCoreUtil.import(specialProjectRepo)
		//		exportCoreUtil.import(feedbackRepo)
		//		exportCoreUtil.import(bannerRepo)
		//		exportCoreUtil.import(newsRepo)
	}
	
	@Test
	fun importMalls() {
		val data = SheetsModelProxy(File("D:/SALE_SC data.xlsx")).getSheet(0)
		
		data.map {
			Mall().apply {
				id = it[0]
				title = it[1]
				cityId = it[3]
			}
		}.forEach {
			geoModel.mallOps.repository.save(it)
		}
	}
	
	fun repoToClassMap() = mapOf<JpaRepository<*, String>, Class<*>>(
			partnerModel.repository to Partner::class.java,
			offerRepo to Offer::class.java,
			geoModel.regionOps.repository to Region::class.java,
			geoModel.cityOps.repository to City::class.java,
			geoModel.placeOps.repository to Place::class.java,
			geoModel.objectLocationOps.repository to ObjectLocation::class.java,
			userRepo to UserEntity::class.java,
			authReasonRepo to AuthReason::class.java,
			bannerRepo to Banner::class.java,
			categoryRepo to Category::class.java,
			configPairRepo to ConfigPair::class.java,
			specialProjectRepo to SpecialProject::class.java,
			feedbackRepo to Feedback::class.java
	)
	
	//	@Test
	//	fun testExport() {
	//		val file = exportCoreUtil.export(categoryRepo, v)
	//		exportCoreUtil.import(categoryRepo, file)
	//	}
	
	
	@Test
	fun testExportingData() {
		
		exportCoreUtil.fullExport(null)
		
		//		val workbook = XSSFWorkbook()
		//		val schemas = mutableMapOf<String, MutableSet<String>>()
		//
		//		jdbcTemplate.query("select * from information_schema.tables", {
		//			schemas.getOrPut(it.getString("table_schema"), { mutableSetOf() }).add(
		//					it.getString("table_name")
		//			)
		//			//			println(it.getString("table_schema") + " --- " + it.getString("table_name"))
		//		})
		//
		//		val tables = schemas.filterValues { it.containsAll(listOf("partner", "offer", "place")) }.values.first()
		//
		//		for (table in tables) {
		//
		//			val sheet = workbook.createSheet(table)
		//			var rowNum = 0
		//
		//			jdbcTemplate.query("select * from $table limit 1", { rs ->
		//				val row = sheet.createRow(rowNum++)
		//				for ((colNum, i) in (1..rs.metaData.columnCount).withIndex()) {
		//					val cell = row.createCell(colNum)
		//					cell.setCellValue(rs.metaData.getColumnName(i))
		//				}
		//			})
		//
		//			jdbcTemplate.query("select * from $table", { rs ->
		//				val row = sheet.createRow(rowNum++)
		//				for ((colNum, i) in (1..rs.metaData.columnCount).withIndex()) {
		//					val cell = row.createCell(colNum)
		//					cell.setCellValue(rs.getObject(rs.metaData.getColumnName(i))?.toString())
		//				}
		//			})
		//		}
		//
		//		val file = File("D:/test.xlsx")
		//		if (file.exists()) {
		//			file.delete()
		//		}
		//		file.createNewFile()
		//		file.outputStream()
		//		workbook.also {
		//			it.write(file.outputStream())
		//			it.close()
		//		}
		
	}
	
	@Test
	fun updateFromTxt3() {
		
		val lines = File("D:/activePartners.txt").readLines()
			.map { it.split("\t")[0] to it.split("\t")[1].toBoolean() }.drop(105)
		
		var i = 0
		for ((id, status) in lines) {
			
			println("Processing line ${++i}/${lines.size}")
			try {
				
				partnerModel.edit(id) {
					showApp = status
					showWeb = status
				}
				
				offerModel.findByPartnerId(id).forEach {
					it.showWeb = status
					it.showApp = status
					offerRepo.save(it)
				}
			} catch (e: Exception) {
			}
		}
	}
	
	@Test
	fun updateUrls() {
		//		jdbcTemplate.update("update offer set urlenabled = true")
		jdbcTemplate.query("select title from partner where url isnull;", {
			val title = it.getString("title")
			val url = title.map { latin(it) }.reduceOrElse("") { a, b -> "$a$b" }.toLowerCase()
			println("Title - $title, URL - $url")
			try {
				jdbcTemplate.update("update partner set url = '$url' where title = '$title'")
			} catch (e: Exception) {
			}
		})
	}
	
	infix fun latin(char: Char): String {
		when (char.toUpperCase()) {
			'А'  -> return "a"
			'Б'  -> return "b"
			'В'  -> return "v"
			'Г'  -> return "g"
			'Д'  -> return "d"
			'Е'  -> return "e"
			'Ё'  -> return "je"
			'Ж'  -> return "zh"
			'З'  -> return "z"
			'И'  -> return "i"
			'Й'  -> return "y"
			'К'  -> return "k"
			'Л'  -> return "l"
			'М'  -> return "m"
			'Н'  -> return "n"
			'О'  -> return "o"
			'П'  -> return "p"
			'Р'  -> return "r"
			'С'  -> return "s"
			'Т'  -> return "t"
			'У'  -> return "u"
			'Ф'  -> return "f"
			'Х'  -> return "kh"
			'Ц'  -> return "c"
			'Ч'  -> return "ch"
			'Ш'  -> return "sh"
			'Щ'  -> return "sch"
			'Ъ'  -> return ""
			'Ы'  -> return "y"
			'Ь'  -> return ""
			'Э'  -> return "e"
			'Ю'  -> return "yu"
			'Я'  -> return "ya"
			' '  -> return "_"
			'№'  -> return "no"
			':'  -> return ""
			'#', '?', '/', ',', '&', '*', '"', ';', '«', '»', '\'', '\\', '’', '%',
			'.'  -> return "-"
			else -> return char.toString()
		}
	}
	
	@Test
	fun updateOfferAmounts() {
		for (i in 0..9999) {
			println("Page $i")
			offerRepo.findAll(PageRequest.of(i, 10)).forEach {
				it.apply {
					//					standartAmount = amounts?.get(0)
					//					goldAmount = amounts?.get(1)
					//					premiumAmount = amounts?.get(2)
					//					privateAmount = amounts?.get(3)
				}
				launch { offerRepo.save(it) }
			}
		}
	}
	
	@Test
	fun updateLocationCategories() {
		val filter = SpecificationHelper<ObjectLocation>()
		filter.where { root, _, cb ->
			cb.and(
					cb.or(
							cb.isNull(root.get<Category>("category"))
							//							cb.equal(root.get<String>("marker"), "")
					),
					cb.equal(root.get<Boolean>("isReal"), true))
		}
		
		do {
			val page = filter.page(0, 10).result(geoModel.objectLocationOps.repository)
			page.totalElements.print("Objects left: ")
			page.forEach {
				try {
					val partner = partnerModel.getById(it.partnerId!!)
					it.setPartner(partner)
					geoModel.objectLocationOps.save(it)
				} catch (e: Exception) {
					//					it.marker = ""
					println("Partner ${it.partnerId} doesn't exist")
					geoModel.objectLocationOps.deleteById(it.id)
				}
			}
		} while (page.size > 0)
	}
	
	@Test
	fun findMissingPartners() {
		
		val pool = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
		
		File("D:/activePartners.txt").readLines()
			.map { it.split("\t")[0] }
			.sortedBy { it }
			.map { async(pool) { partnerModel.repository.existsById(it) } to it }
			.map { it.first.blockAwait() to it.second }
			.forEach {
				if (!it.first) {
					println(it.second)
				}
			}
	}
	
	@Test
	fun updateAllSaveablesV2() {
		
		jdbcTemplate.query("select offer_id, categories_id from offer_category", {
			jdbcTemplate.update("insert into offer_categoriesid (offer_id, categoriesid) values ('${it.getString("offer_id")}', '${it.getString("categories_id")}')"
			)
		})
		
		//
		//		val entities = listOf("offer", "partner", "banner", "specialproject", "news")
		//
		//		for (table in entities) {
		//			jdbcTemplate.update("update $table set showapp = visible where showapp is null ")
		//			jdbcTemplate.update("update $table set showweb = visible where showweb is null ")
		//			jdbcTemplate.update("update $table set publishedapp = ${System.currentTimeMillis()} where publishedapp is null ")
		//			jdbcTemplate.update("update $table set publishedweb = ${System.currentTimeMillis()} where publishedweb is null ")
		//		}
		
	}
	
	@Test
	fun importRegion() {
		val regions = File("D:/regions.txt").readLines().drop(1).map {
			val arr = it.split('\t')
			Region().apply {
				id = arr[0]
				name = arr[1]
			}
		}
		
		val existing = geoModel.regionOps.listRegions(true)
		
		regions.forEach {
			if (!existing.any { region -> region.name == it.name }) {
				geoModel.regionOps.repository.save(it)
			}
		}
	}
	
	@Test
	fun importCity() {
		val cities = File("D:/city.txt").readLines().drop(1).map {
			val arr = it.split('\t')
			City().apply {
				id = arr[0]
				name = arr[1]
				parentRegion = geoModel.regionOps.getById(arr[2])
			}
		}
		
		val existing = geoModel.cityOps.repository.findAll()
		
		cities.forEach {
			if (!existing.any { city -> city.name == it.name }) {
				geoModel.cityOps.repository.save(it)
				geoModel.regionOps.getById(it.parentRegion.id).apply {
					this.cities.add(it.id)
					geoModel.regionOps.repository.save(this)
				}
			}
		}
	}
	
	@Test
	fun importPlace() {
		val cities = geoModel.cityOps.repository.findAll()
		
		val metro = File("D:/metro.txt").readLines().drop(1).map {
			val arr = it.split('\t')
			Place().apply {
				id = arr[0]
				name = arr[1]
				parentCity = cities.find { it.id == arr[6] }!!
				point.x = arr[7].toDouble()
				point.y = arr[8].toDouble()
			}
		}
		
		val exist = geoModel.placeOps.repository.findAll()
		
		metro.forEach {
			if (!exist.any { place -> place.name == it.name && place.parentCity.id == it.parentCity.id }) {
				geoModel.placeOps.repository.save(it)
				geoModel.cityOps.repository.save(it.parentCity)
			}
		}
	}
	
	@Test
	fun `add not real location to all cities`() {
		while (!this::geoModel.isInitialized) {
			Thread.sleep(100)
		}
		val cities = geoModel.cityOps.repository.findAll()
		val places = geoModel.placeOps.repository.findAll()
		cities.forEach { city ->
			if (places.filter { it.parentCity.id == city.id }.none { !it.isReal }) {
				val place = Place(name = "Онлайн", point = GeoPoint(0.0, 0.0), parentCity = city)
				place.isReal = false
				geoModel.placeOps.repository.save(place)
				city.places.add(place.id)
				geoModel.cityOps.repository.save(city)
			}
		}
	}
	
	fun getStreetType(input: String) = when (input) {
		"1"  -> "проспект"
		"2"  -> "улица"
		"4"  -> "переулок"
		"7"  -> "шосе"
	//		"2" -> "улица"
	//		"2" -> "улица"
	//		"2" -> "улица"
	//		"2" -> "улица"
		else -> "улица"
	}
	
	@Test
	fun modifyPlaces() {
		geoModel.placeOps.repository.findByName("Онлайн").filter { it.isReal }.forEach {
			it.isReal = false
			geoModel.placeOps.repository.save(it)
		}
	}
	
	fun createUsers() {
		try {
			authAPI.registerByMail("abc@xyz.com", null, "1234")
			authAPI.verify("abc@xyz.com")
		} catch (e: Exception) {
		}
		for (i in 0..10) {
			try {
				authAPI.registerByMail("admin$i@sdv.com", null, "123456")
				authAPI.verify("admin$i@sdv.com")
			} catch (e: Exception) {
			}
		}
	}
	
	@Test
	fun updateLocations() {
		val repo = geoModel.objectLocationOps.repository
		
		do {
			SpecificationHelper<ObjectLocation>().where { root, _, cb ->
				cb.isNull(root.get<String>("regionId"))
			}.page(0, pageSize = 10).sort(Sort.Direction.ASC, "id").result(repo).also {
				it.totalElements.print("Elements left: ")
				it.forEach {
					it.updateIds()
					repo.save(it)
				}
			}
		} while (true)
	}
	
	@Test
	fun `eliminate url duplicates`() {
		
		do {
			
			val set = mutableSetOf<Map<String, *>>()
			
			var retry = false
			jdbcTemplate.queryForList("select url, id from offer").filter { it["url"] != null }
				.forEach {
					if (set.any { o -> it["url"] == o["url"] }) {
						it["url"]?.print()
						retry = true
						jdbcTemplate.execute("update offer set url = '${it["url"]}0' where id like '${it["id"]}'")
					} else {
						set.add(it)
					}
				}
		} while (retry)
		
	}
	
	@Test
	fun retestEveryEvent() {
		val spec = SpecificationHelper<Offer>()
		spec.where { root, criteriaQuery, cb ->
			val partnerRoot = criteriaQuery.from(Partner::class.java)
			return@where cb.and(
					cb.equal(root.get<String>("partnerId"), partnerRoot.get<String>("id")),
					cb.notEqual(root.get<Boolean>("visible"), partnerRoot.get<Boolean>("visible"))
			)
		}
		
		var page = 0
		
		do {
			val result: Page<Offer> = spec.page(0, 10).result(offerRepo)
			println("${result.totalElements} left")
			result.forEach {
				it.visible = !it.visible!!
				offerRepo.save(it)
			}
			page++
		} while (result.hasNext())
		
	}
	
	// 10
	
	@Test
	fun updatePercentage() {
		val data = SheetsModelProxy(File("D:/SDV_ data.xlsx")).getSheet(10)
		
		(0..10500).forEach { id ->
			
			if (!partnerModel.repository.existsById(id.toString())) {
				return@forEach
			}
			
			val list = data.filter { it[0].toInt() == id }
			
			println("Update partner's offers $id / 10436")
			offerModel.findByPartnerId(id.toString()).forEach { offer ->
				//				offer.amounts = arrayOf(0.0, 0.0, 0.0, 0.0)
				//				offer.amounts!![0] = list.find { it[1] == "10" }?.get(2)?.toDouble() ?: 0.0
				//				offer.amounts!![1] = list.find { it[1] == "20" }?.get(2)?.toDouble() ?: 0.0
				//				offer.amounts!![2] = list.find { it[1] == "30" }?.get(2)?.toDouble() ?: 0.0
				//				offer.amounts!![3] = list.find { it[1] == "40" }?.get(2)?.toDouble() ?: 0.0
				//				offer.amounts?.let { amount ->
				//					offer.upTo = amount[0] == amount[1] && amount[2] == amount[3] && amount[1] == amount[2]
				//				}
				offerRepo.save(offer)
			}
		}
	}
	
	@Test
	fun updateLogo() {
		
		val xlsDoc = SheetsModelProxy(File("D:/SDV_ data.xlsx"))
		val sdvToPartner = xlsDoc.getSheet(7) // row [5]
		
		var ctr = 0
		
		var page = 0
		var hasNext = true
		while (hasNext) {
			val result = partnerModel.repository.findAll(PageRequest.of(page, 10))
			result.forEach { partner ->
				val row = sdvToPartner.find { it[0] == partner.id } ?: throw IllegalArgumentException()
				if (!File(row[5].replace("/common/img", "D:")).exists()) {
					println("Offer ${++ctr}/${result.totalElements}: no logo")
					partner.logo = "logo.jpg"
					partner.visible = false
					offerModel.findByPartnerId(partner.id).forEach {
						it.visible = false
						offerRepo.save(it)
					}
					partnerModel.repository.save(partner)
				} else {
					println("Offer ${++ctr}/${result.totalElements}: has logo")
				}
			}
			hasNext = result.hasNext()
			page++
		}
	}
	
	@Test
	fun updateUpTo() {
		var page = 60
		var hasNext = true
		while (hasNext) {
			val result = offerRepo.findAll(PageRequest.of(page, 20))
			"Page $page/${result.totalPages}".print()
			result.forEach {
				//				val amount = it.amounts ?: return@forEach
				
				//				if (amount[0] == amount[1] && amount[2] == amount[3] && amount[1] == amount[2]) {
				//					return@forEach
				//				}
				
				it.upTo = true
				offerRepo.save(it)
			}
			hasNext = result.hasNext()
			page++
		}
	}
	
	@Test
	fun updateDescription() {
		val xlsDoc = SheetsModelProxy(File("D:/SDV_ data.xlsx"))
		val sdvToPartner = xlsDoc.getSheet(7)
		
		var ptr = 0
		
		do {
			val page = partnerModel.repository.findAll(PageRequest.of(ptr, 10))
			ptr++
			"Page $ptr/${page.totalPages}".print()
			page.forEach {
				sdvToPartner.find { line -> line[0] == it.id }?.apply {
					if (it.about != null && it.about?.length ?: 0 > 0) {
						return@apply
					}
					if (this[3].isEmpty()) {
						return@apply
					}
					it.about = this[3]
					partnerModel.repository.save(it)
				}
			}
		} while (page.hasNext())
	}
	
	@Test
	fun importData() {
		
		createUsers()
		
		"Import data start".print()
		importRegion()
		"Import regions done".print()
		importCity()
		"Import city done".print()
		importPlace()
		"Import places done".print()
		`add not real location to all cities`()
		"All cities have virtual location. Now: import categories.".print()
		
		val xlsDoc = SheetsModelProxy(File("D:/SDV_ data.xlsx"))
		val categoryList = SheetsModelProxy(File("D:/SALE_PARTNER_TYPE data.xlsx"))
			.getSheet(0).map {
				categoryRepo.save(Category().apply {
					id = it[0]
					name = it[1]
				})
			}
		
		"read data".print()
		val places = geoModel.placeOps.repository.findAll()
		val cities = geoModel.cityOps.repository.findAll()
		
		"read xml".print()
		val partners = mutableListOf<Partner>()
		val placesToPartner = xlsDoc.getSheet(2)
		val partnerToMetro = xlsDoc.getSheet(3)
		val sdvToPartner = xlsDoc.getSheet(7)
		val partnerToCategory = xlsDoc.getSheet(9)
		
		val rand = Random()
		var counter = 0
		val uploads = mutableListOf<Deferred<Any>>()
		
		fun findNotRealPlaceInCity(cityName: String): Place {
			return places.filter { it.parentCity.name == cityName }.find { !it.isReal } ?: let {
				geoModel.placeOps.repository.save(
						Place("Онлайн", cities.find { it.name == cityName }!!, GeoPoint(0.0, 0.0))
				).also { places.add(it) }
			}
		}
		
		for (row in sdvToPartner) {
			
			if (partnerModel.repository.existsById(row[0])) {
				println("Partner ${row[0]}\t do exist")
				counter++
				continue
			}
			println("Partner ${row[0]}\tnot exist")
			val partner = Partner().apply {
				id = row[0]
				title = row[1]
				website = row[2]
				about = row[3]
				visible = row[6].trim() == "1"
				
				partnerToCategory.filter { it[0] == id }.forEach {
					val category = categoryList.find { category -> category.id == it[1] }!!
					categories.add(category)
					if (mainCategory == null) {
						mainCategory = category
					}
				}
			}
			
			val logoFile = File(row[5].replace("/common/img", "D:"))
			if (logoFile.exists()) {
				partner.logo = "logo/" + logoFile.absolutePath.substringAfterLast('/').substringAfterLast('\\')
			} else {
				partner.logo = "default.jpg"
				partner.visible = false
			}
			//			val logo = "logo/" + logoFile.absolutePath.substringAfterLast('/').substringAfterLast('\\')
			//			if (partner.logo != logo) {
			//				try {
			//					//uploads.add(async { fileModel.uploadLocalFile(logoFile, logo) })
			//					partner.logo = logo
			//				} catch (e: Exception) {
			//					println("file for partner ${partner.id} not found.")
			//				}
			//			}
			
			val locations = mutableListOf<ObjectLocation>()
			
			//find all partners rows
			placesToPartner.filter { it[1] == partner.id }.forEach {
				val location = ObjectLocation().apply {
					id = it[0]
					try {
						point = GeoPoint(it[19].toDouble(), it[20].toDouble())
					} catch (e: Exception) {
						println(it)
						System.exit(1)
					}
					
					place = partnerToMetro.find { it[0] == id }?.let {
						places.find { place -> place.id == it[1] }
					} ?: findNotRealPlaceInCity(it[6])
					
					partnerId = partner.id
					
					streetName = it[9]
					streetType = getStreetType(it[8])
					building = it[10]
					buildingSection = (it[11] + " " + it[12]).trim()
					territory = (it[13] + " " + it[14]).trim()
					contactInfo = it[15]
					workHours = it[17]
				}
				partner.locations.add(location.id)
				locations.add(location)
			}
			
			val offers = mutableListOf<Offer>()
			offers.add(Offer(title = row[1],
					partner = partner,
					offerType = Offer.OfferType.DISCOUNT,
					startDate = LocalDateTime.now().toLong(),
					endDate = LocalDateTime.now().plusYears(2).toLong()).apply {
				visible = partner.visible
				//				amounts = arrayOf(0.0, 0.0, 0.0, 0.0)
				//				var prev = 1
				//				(0..3).forEach { i ->
				//					prev = rand.int(prev..(10 + i * 2 + (i / 3) * 2))
				//					amounts!![i] = prev.toDouble()
				//				}
				//				upTo = ((amounts!![0] == amounts!![1]) && (amounts!![2] == amounts!![3])) && (amounts!![1] == amounts!![2])
				terms = if (row[4].isNotEmpty()) row[4] else "."
			})
			
			try {
				println("Writing partner: ${counter++}/${sdvToPartner.size}")
				locations.forEach { geoModel.objectLocationOps.save(it) }
				partnerModel.repository.save(partner)
				offers.forEach { offerRepo.save(it) }
			} catch (e: Exception) {
				e.printStackTrace()
			}
			
		}
		
		uploads.forEach { it.blockAwait() }
		
	}
	
	@Test
	fun updateTestText() {
		
		val list = File("C:/ids.txt").readLines()
			.map {
				val offer = offerModel.repository.findById(it).get()
				offer.terms = "Скидка предоставляется при оплате картой Райффайзенбанка"
				return@map async {
					offerModel.repository.save(offer)
					println("$it saved")
				}
			}
		
		list.forEach {
			it.blockAwait()
		}
		
	}
	
	val semaphore = Semaphore(10)
	
	@Test
	fun importLogo() {
		val data = xlsDoc.getSheet(7)
		
		data.parallelStream().forEach { array ->
			semaphore.acquire()
			println("Start mod: " + array[0])
			
			val partner = try {
				partnerModel.getById(array[0])
			} catch (e: Exception) {
				null
			}
			partner ?: let {
				semaphore.release()
				return@forEach
			}
			val logoFile = File(array[5].replace("/common/img", "D:"))
			val logo = logoFile.absolutePath.substringAfterLast('/').substringAfterLast('\\')
			if (partner.logo != logo) {
				try {
					partner.logo = fileModel.uploadLocalFile(logoFile, "logo/${logo}")
					partnerModel.repository.save(partner)
				} catch (e: Exception) {
					println("file for partner ${partner.id} not found.")
				}
			}
			println("End mod: " + partner.id)
			semaphore.release()
		}
	}
	
	@Test
	fun generateRandomAmounts() {
		
		val rand = Random()
		
		var page = 0
		var hasNext = true
		while (hasNext) {
			val result = offerRepo.findAll(PageRequest.of(page, 20))
			"Page $page/${result.totalPages}".print()
			result.forEach {
				//				if (it.amounts == null || it.amounts!!.size != 4) {
				//					it.amounts = arrayOf(0.0, 0.0, 0.0, 0.0)
				//					var prev = 1
				//					(0..3).forEach { i ->
				//						prev = rand.int(prev..(10 + i * 2 + (i / 3) * 2))
				//						it.amounts!![i] = prev.toDouble()
				//					}
				//					offerRepo.save(it)
				//				}
			}
			hasNext = result.hasNext()
			page++
		}
		
	}
	
	
}