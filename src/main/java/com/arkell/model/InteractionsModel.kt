package com.arkell.model

import com.arkell.entity.interaction.MailSubscription
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service

@Service
class InteractionsModel(
		private val configModel: ConfigModel) {

	fun getFeedbackMail() = configModel.feedbackMail
	fun setFeedbackMail(mail: String): String = mail.apply {
		if (!this.matches(emailRegex)) {
			throw IllegalArgumentException("Mail is invalid")
		}
		configModel.feedbackMail = this
	}

	fun subscribeMail(mail: String) {
		TODO("mongoTemplate.save(MailSubscription(mail))")
	}

	fun unSubscribeMail(mail: String) {
		TODO("mongoTemplate.remove(mongoTemplate.findById<MailSubscription>(mail)!!)")
	}

	fun mailSubscriptions(page: Int, pageSize: Int): Page<MailSubscription> {
		TODO("not implemented")
	}

	companion object {
		val emailRegex = Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^" +
				"_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]" +
				"|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])" +
				"?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?" +
				"[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]" +
				":(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-" +
				"\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")
	}

}