package com.arkell.api.admin

import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.interaction.Feedback
import com.arkell.model.FeedbackModel
import org.springframework.web.bind.annotation.*

@Auth(value = AuthPermission.ADMIN)
@RestController
@RequestMapping("/api/admin/feedback")
class AdminFeedbackAPI(
		private val feedbackModel: FeedbackModel) {

	@GetMapping("/list")
	fun list(): List<Feedback> {
		return feedbackModel.listFeedback()
	}

	@PostMapping("/delete")
	fun delete(@RequestParam id: String): Boolean {
		return feedbackModel.deleteById(id)
	}
}