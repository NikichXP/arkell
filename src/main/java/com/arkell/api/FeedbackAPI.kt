package com.arkell.api

import com.arkell.entity.interaction.Feedback
import com.arkell.model.FeedbackModel
import com.arkell.model.GeoModel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/feedback")
class FeedbackAPI(
		private val feedbackModel: FeedbackModel,
		private val geoModel: GeoModel) {

	/**
	 * Send a feedback message
	 * @param feedbackType can be either a 'question', 'suggest' or 'issue' (ignore case)
	 */
	@PostMapping("/send")
	fun send(@RequestParam theme: String, @RequestParam fullName: String, @RequestParam email: String,
	         @RequestParam cityId: String?, @RequestParam phone: String, @RequestParam message: String,
	         @RequestParam feedbackType: String, @RequestParam partnerName: String?,
	         @RequestParam shopAddress: String?): Feedback {
		val fType = when (feedbackType.toLowerCase()) {
			"question" -> Feedback.FeedbackType.QUESTION
			"issue" -> Feedback.FeedbackType.ISSUE
			"suggest" -> Feedback.FeedbackType.SUGGEST
			else -> throw IllegalArgumentException("Send Question or Issue")
		}

		return feedbackModel.createFeedback(theme, fullName, email, cityId?.let { geoModel.cityOps.getById(it) }, phone, message, fType,
				partnerName, shopAddress)
	}

}