package com.arkell.model

import com.arkell.entity.geo.City
import com.arkell.entity.interaction.Feedback
import com.arkell.repo.FeedbackRepo
import org.springframework.stereotype.Service

@Service
class FeedbackModel(
		private val feedbackRepo: FeedbackRepo) {

	fun createFeedback(theme: String, fullName: String, email: String, city: City?, phone: String, message: String,
	                   feedbackType: Feedback.FeedbackType, partnerName: String?, shopAddress: String?): Feedback {
		return Feedback(theme = theme, fullName = fullName, email = email, message = message, feedbackType = feedbackType,
				phone = phone, city = city)
			.apply {
				partnerName?.let { this.partnerName = partnerName }
				shopAddress?.let { this.shopAddress = shopAddress }
			}.also {
				feedbackRepo.save(it)
			}
	}

	fun listFeedback(): List<Feedback> = feedbackRepo.findAll()
	fun deleteById(id: String): Boolean = feedbackRepo.deleteById(id).let { true }


}