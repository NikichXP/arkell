package com.arkell.entity.interaction

import com.arkell.entity.Saveable
import com.arkell.entity.geo.City
import com.arkell.util.IDGenerator
import java.time.LocalDateTime
import javax.persistence.*

@Entity
class Feedback(
		var theme: String,
		var fullName: String,
		var email: String,
		@ManyToOne
		var city: City?,
		var phone: String,
		@Column(columnDefinition = "TEXT")
		var message: String,
		var feedbackType: FeedbackType
) : Saveable() {

	constructor() : this("", "", "", City(), "", "", FeedbackType.QUESTION)

	@Id
	override var id: String = IDGenerator.longId()

	var partnerName: String? = null
	var shopAddress: String? = null
	var date: LocalDateTime = LocalDateTime.now()

	enum class FeedbackType {
		QUESTION, ISSUE,
		SUGGEST
	}

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = false
}

/*
Тема обращения
ФИО
email - обратный
Регион
Город
Номер телефона
Текст сообщения

если это отказ в услуге!
Название партнёра
Адрес магазина
Дата события
 */