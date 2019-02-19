package com.arkell.test

import com.arkell.api.OfferAPI
import com.arkell.api.PartnerAPI
import com.arkell.api.admin.AdminNewsAPI
import com.arkell.api.admin.AdminOfferAPI
import com.arkell.api.admin.AdminPartnerAPI
import com.arkell.entity.Offer
import com.arkell.entity.Partner
import com.arkell.model.*
import com.arkell.repo.OfferRepo
import org.junit.FixMethodOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runners.MethodSorters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
internal class PartnerAndOfferTest {
	
	@Autowired
	private lateinit var partnerModel: PartnerModel
	@Autowired
	private lateinit var partnerAPI: PartnerAPI
	@Autowired
	private lateinit var adminPartnerAPI: AdminPartnerAPI
	@Autowired
	private lateinit var adminOfferAPI: AdminOfferAPI
	@Autowired
	private lateinit var adminNewsAPI: AdminNewsAPI
	@Autowired
	private lateinit var offerModel: OfferModel
	@Autowired
	private lateinit var newsModel: NewsModel
	@Autowired
	private lateinit var offerAPI: OfferAPI
	@Autowired
	private lateinit var offerRepo: OfferRepo
	@Autowired
	private lateinit var geoModel: GeoModel
	@Autowired
	private lateinit var categoryModel: CategoryModel
	
	@Test
	fun `t01 create partner and change his status to approved`() {
		//		val title = "Test partner 001"
		//		var partner = partnerAPI.submit(title = title, legalName = "FP Test 001 Inc", INN = "1421234141",
		//				organisationForm = "FP", contactPersons = "Andryi Hrolenko", phone = "38050-555-12345",
		//				email = "test001.fict.me", website = "https://www.google.com", category = categoryModel.listCategories(true)[0].id,
		//				placesId = arrayOf(geoModel.placeOps.findByName("Republic Stadium", "Foo Bar").id),
		//				geoY = null, geoX = null, shopsCount = 1, sellType = "Retail")
		//
		//		assertEquals(partner.status, Partner.Status.PENDING)
		//
		//		adminPartnerAPI.changeStatus(partner.id, 2)
		//
		//		partner = partnerModel.getById(partner.id)
		//
		//		assertEquals(partner.status, Partner.Status.APPROVED)
		//		assertEquals(partner.priority, 2)
		//		assertNotNull(partnerModel.getByTitle(title))
		//
		//		partnerModel.deleteById(partner.id)
		//
		//		assertThrows<ElementNotFoundException> { partnerModel.getById(partner.id) }
	}
	
	@Test
	fun `t02 create partner with offers`() {
		//		val partner = createPartner {}
		//
		//		val place = geoModel.placeOps.listPlaces(null).random()
		//
		//		val offer = adminOfferAPI.create(title = "Get discount with us!", partnerId = partner.id,
		//				categoryId = partner.category.id, offerType = Offer.OfferType.CASHBACK.string, image = "default.jpg",
		//				startDate = LocalDateTime.now().plusDays(3).toLong(),
		//				endDate = LocalDateTime.now().plusDays(10).toLong(),
		//				address = null, amount = (Math.random() * 100.0).roundToInt().toDouble() / 10.0, placeId = place.id,
		//				geoX = null, geoY = null, amounts = null, banner = "default.jpg")
		//
		//		assertNotNull(offerModel.getById(offer.id))
		//
		//		assert(Offer.OfferType.CASHBACK == offer.offerType)
		//		assertTrue { offer.startDate.isAfter(LocalDateTime.now().plusDays(3).minusMinutes(1)) }
		//		assertTrue { offer.endDate.isBefore(LocalDateTime.now().plusDays(10).plusHours(4)) }
		//
		//		offerModel.deleteById(offer.id)
		//		partnerModel.deleteById(partner.id)
	}
	
	@Test
	fun testFilterByCategories() {
		//		val categories = categoryModel.listCategories(false)
		//		val places = geoModel.placeOps.listPlaces(null)
		//
		//		val result = partnerModel.filterPartners(categories = null, cityId = null, placeId = places[0].id,
		//				title = null, sort = null, page = 0, pageSize = 50).forEach {
		//			//			assertTrue(it.locations.any { it.place.id == places[0].id })
		//		}
		
	}
	
	//	@Test
	fun `t03 generate test data`() {
		val title = "John Doe test inc."
		val partner = try {
			partnerModel.getByTitle(title)
		} catch (e: Exception) {
			createPartner {
				this.title = "John Doe test inc."
			}
		}
		createOffer(partner) {
			offerType = Offer.OfferType.DISCOUNT
			//			geoLocation = ObjectLocation(x = Math.random() * 50, y = Math.random() * 50,
			//					place = geoModel.placeOps.listPlaces(
			//							geoModel.cityOps.listCities(null).random().id
			//					).random())
		}
		createOffer(partner) {
			offerType = Offer.OfferType.CASHBACK
		}
		createOffer(partner) {
			offerType = Offer.OfferType.GIFT
//			amounts = arrayOf(10.0, 11.0, 12.0, 13.0)
		}
	}
	
	//	@Test
	fun `t04 generate partner news`() {
		val title = "John Doe test inc."
		val partner = try {
			partnerModel.getByTitle(title)
		} catch (e: Exception) {
			createPartner {
				this.title = "John Doe test inc."
			}
		}
		//		createNews(partner) {
		//			beginDate = LocalDateTime.now().minusHours(20).toLong()
		//			this.title = "Про Lorem Ipsum"
		//			description = "Одному тестировщику был слишком скучен Lorem Ipsum. Он хотел, что бы в его текстах было хоть " +
		//					"что-либо интересное. Так и появились эти тексты. Спойлер: потом появится генератор случайных слов, " +
		//					"но его плоды будут скрыты от посторонних глаз."
		//			//					"В общем, решили два друга устроить махач ради лапши. И началась великая лапшичная война. " +
		//			//					"Разумеется, никакого кровопролития не было. Увы, всё было гораздо хуже. " +
		//			//					"Друзья не на шутку на друг друга обиделись и начали подбрасывать мусор в лапшу друг друга. " +
		//			//					"Переведя 5 килограм лапши, они решили, что более не следует враждовать. " +
		//			//					"Они заварили ещё лапши, сели рядом и смачно жрали эту треклятую лапшу, " +
		//			//					"потому что они китайцы и воевать, портя еду - экономически нецелесообразно. " +
		//			//					"По окончанию трапезы один из друзей был зарезан другим."
		//		}
	}
	
	@Test
	fun `t05 where i try to test optional queries`() {
		val date = LocalDateTime.now()
		val title = "John Doe test inc."
		val partner = try {
			partnerModel.getByTitle(title)
		} catch (e: Exception) {
			createPartner {
				this.title = "John Doe test inc."
			}
		}
		//		val categories = categoryModel.listCategories(true)
		//		(0..4).forEach { i ->
		//			categories.forEach { category ->
		//				createOffer(partner) {
		//					this.category = category
		//					this.startDate = date.plusDays(i.toLong())
		//					this.endDate = date.plusDays(3L + i)
		//				}
		//			}
		//		}
		
		//		val result = offerModel.find(
		//				//				categories = categories.slice(0..1).map { it.id }.toTypedArray(),
		//				//				showHidden = false,
		//				//				title = "цеНтр",
		//				cityId = "34",
		//				page = 0, pageSize = 20).content
		
		//		for (it in result) {
		//			System.err.println(it.title)
		//			//			assertTrue { it.title.contains("центр", ignoreCase = true) }
		//		}
		//
		//		System.err.println("Offers size: " + result.size)
		//		categories.forEach { category ->
		//			System.err.println("Category: ${category.name} &set: " +
		//					offerRepo.findByCategoryAndStartDateAfter(category, Optional.of(date.plusDays(2))).size)
		//			System.err.println("Category: ${category.name} null: " +
		//					offerRepo.findByCategoryAndStartDateAfter(category, Optional.empty()).size)
		//		}
	}
	
	/**
	 * Requires test 01 to work correctly
	 */
	fun createPartner(apply: Partner.() -> Unit): Partner {
		return Partner()
		//		val uuid = UUID.randomUUID().toString().substring(0..5)
		//		val partner = partnerModel.createPartner(title = "Test $uuid",
		//				legalName = "FP Test $uuid Inc", INN = "1421234141",
		//				organisationForm = "FP", contactPerson = "John Doe", phone = "+38-050-555-1234", email = "$uuid@fict.me",
		//				website = "https://www.google.com", discountCategory = categoryModel.listCategories(true)[0].id,
		//				placesId = arrayOf(geoModel.placeOps.findByName("Republic Stadium", "Foo Bar").id))
		//		partnerModel.changeStatus(partner.id, 2)
		//		return partnerModel.update(partner.id) {
		//			apply(it)
		//		}
	}
	
	/**
	 * Requires test 02 to work correctly
	 */
	fun createOffer(partner: Partner, apply: Offer.() -> Unit): Offer {
		val offer = offerModel.create(
				partnerId = partner.id, categoryId = partner.categories[0].id, offerType = Offer.OfferType.CASHBACK,
				amounts = if (Math.random() > 0.5) arrayOf(8.0, 10.0, 12.0, 15.0) else null,
				realisation = "PROMOCODE"
		)
		return offerModel.update(offer.id) {
			it.apply {
				title = "Get discount with us!"
				image = "default.jpg"
				banner = "default.jpg"
				terms = "Lol, this is test"
				upTo = Math.random() > 0.5
				previewAmount = 5.0
				priority = 10
			}
			apply(it)
		}
	}
	
	//	fun createNews(partner: Partner, apply: News.() -> Unit): News {
	//		val news = adminNewsAPI.create(title = "Test news", description = "Test news description",
	//				terms = "You get in, you get gift.",
	//				image = "default.jpg", workTerms = "When the shop is open", beginDate = LocalDateTime.now().toLong(),
	//				endDate = LocalDateTime.now().plusDays(7).toLong(), partnerId = partner.id, visible = true,
	//				banner = "default.jpg", offerId = null, voucher = null, priority = 20, regionId = null, cityId = null)
	//		return newsModel.edit(news.id, apply)
	//	}
}


