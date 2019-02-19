package com.arkell.repo

import com.arkell.entity.interaction.Feedback
import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackRepo : JpaRepository<Feedback, String> {
}