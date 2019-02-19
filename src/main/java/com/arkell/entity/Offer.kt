package com.arkell.entity

import com.arkell.util.IDGenerator
import javax.persistence.*

@Entity
class Offer(var title: String,
            var partnerId: String,
            var offerType: OfferType,
            var startDate: Long,
            var endDate: Long
) : VisibleSaveable() {

	constructor(partner: Partner, offerType: OfferType)
			: this("", partner, offerType, System.currentTimeMillis(), System.currentTimeMillis() * 2) {
		this.featuredApp = partner.featuredApp
		this.featuredWeb = partner.featuredWeb
	}

	constructor () : this(title = "", partnerId = "", offerType = OfferType.DISCOUNT,
			startDate = System.currentTimeMillis(), endDate = System.currentTimeMillis() + 3600_000L * 24 * 365)

	constructor(title: String, partner: Partner, offerType: OfferType,
	            startDate: Long, endDate: Long) : this(title, partner.id, offerType, startDate, endDate) {
		this.categories = partner.categories
		this.mainCategory = partner.mainCategory
	}

	@Id
	override var id: String = IDGenerator.longId()

	var standartAmount: Double? = null
	var goldAmount: Double? = null
	var privateAmount: Double? = null
	var premiumAmount: Double? = null

	var currency: String? = "%"
		set(value) {
			field = value ?: "%"
		}

	var previewAmount: Double? = 0.0

	@ManyToMany(fetch = FetchType.EAGER)
	var categories: MutableList<Category> = mutableListOf()
	@ManyToOne
	var mainCategory: Category? = null

	@Column(columnDefinition = "TEXT")
	var terms: String? = null

	var image: String? = null
	var imageApp: String? = null
	var banner: String? = null
	var bannerApp: String? = null

	var flierImg: String? = null
	var qrCodeImg: String? = null
	var barcode: String? = null
	var upTo: Boolean = false
	var priority: Int? = 10

	var adminMail: String? = null
	var promocodeAlertCount: Int? = null

	var realisation: Realisation? = null
	var highlight: Boolean? = false
	var isBig: Boolean? = false
	var isGlobal: Boolean? = false
	var onlyClient: Boolean? = false
	var url: String? = null
	var urlEnabled: Boolean? = null
		set(value) {
			field = value ?: false
		}

	override var created: Long? = System.currentTimeMillis()
		set(value) {
			field = value ?: System.currentTimeMillis()
		}
	override var updated: Long? = System.currentTimeMillis()
		set(value) {
			field = value ?: System.currentTimeMillis()
		}

	override var publishedWeb: Long? = System.currentTimeMillis()
	override var publishedApp: Long? = System.currentTimeMillis()
	override var featuredApp: Boolean? = false
	override var featuredWeb: Boolean? = false
	override var showApp: Boolean? = false
	override var showWeb: Boolean? = false


	enum class OfferType(val string: String) {
		CASHBACK("CASHBACK"), DISCOUNT("DISCOUNT"), GIFT("GIFT"), RUB("RUB"), POINTS("POINTS")
	}

	enum class Realisation(val type: String) {
		DISCOUNT("discount"), PROMOCODE("promoCode"), BARCODE("barCode"), QR("qr"),
		FLIER("flier")
	}
}

/*
Ссылка на партнёра
Тип Скидки - подарок, кэшбек, скидка
Числовое значение - не для подарка
Единица измерения %/$
Признак «До»
Условия получения Скидки*
Дата начала действия
Дата окончания действия
 */
