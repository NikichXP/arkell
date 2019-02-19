package com.arkell.repo.auth

import com.arkell.entity.auth.AuthReason
import com.arkell.entity.auth.MailVerify
import com.arkell.entity.auth.PasswordRestore
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.*

interface AuthReasonRepo : JpaRepository<AuthReason, String>, JpaSpecificationExecutor<AuthReason> {

	fun findByMailIgnoreCase(mail: String): AuthReason?

}

interface MailVerifyRepo : JpaRepository<MailVerify, String> {

}

interface PasswordRestoreRepo : JpaRepository<PasswordRestore, String> {

	fun findByUserId(userId: String): Optional<PasswordRestore>

}