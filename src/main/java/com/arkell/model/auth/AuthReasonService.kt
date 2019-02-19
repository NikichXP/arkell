package com.arkell.model.auth

import com.arkell.entity.UserEntity
import com.arkell.entity.auth.*
import com.arkell.entity.exception.CustomExceptionCode
import com.arkell.entity.exception.ElementNotFoundException
import com.arkell.model.MailTextsService
import com.arkell.model.UserService
import com.arkell.model.internal.ConfigService
import com.arkell.repo.auth.AuthReasonRepo
import com.arkell.repo.auth.MailVerifyRepo
import com.arkell.repo.auth.PasswordRestoreRepo
import com.google.common.hash.Hashing
import kotlinx.coroutines.experimental.launch
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import javax.annotation.PostConstruct

@Service
class AuthReasonService(
		private val authReasonRepo: AuthReasonRepo,
		private val mailVerifyRepo: MailVerifyRepo,
		private val passwordRestoreRepo: PasswordRestoreRepo,
		private val userService: UserService,
		private val mailTextsService: MailTextsService,
		private val configService: ConfigService) {

	private var passwordRestoreTimeout: Long = 0

	@PostConstruct
	fun postConstruct() { // TODO Extract this to other proxy class
		passwordRestoreTimeout = configService.getParam("authController",
				"passwordRestoreTimeout", (24 * 60 * 60).toString()).toLong()
	}

	fun getByMailAndPass(mail: String, pass: String): AuthReason {

		val result = authReasonRepo.findById(encode(mail))
			.filter { it.authData == encode(pass) }
			.orElseThrow { CustomExceptionCode(403, "Неверный логин/пароль") }

		if (result.user.accessLevel == AuthPermission.BLOCKED) {
			throw CustomExceptionCode(801, "Учетная запись неактивна. Проверьте свою почту.")
		}

		return result
	}

	fun registerByMailAndPass(mail: String, phone: String?, pass: String): AuthReason {
		if (authReasonRepo.findById(encode(mail)).isPresent) {
			throw IllegalArgumentException("На эту почту уже зарегистрирована запись.")
		}
		val user = userService.createByMailAndPhone(mail, phone, AuthPermission.BLOCKED)

		launch { userService.repository.save(user) }

		createVerifyMail(user.id, user.mail)

		return createAuthReason(mail, pass, user)
	}

	fun createAuthReason(mail: String, pass: String, user: UserEntity): AuthReason {
		return authReasonRepo.save(AuthReason(
				id = encode(mail),
				authSource = AuthSourceType.MAIL_PASS,
				authData = encode(pass),
				mail = mail,
				user = user))
	}

	fun getByMail(mail: String): AuthReason? = authReasonRepo.findById(encode(mail)).orElse(null)

	fun changeMail(oldMail: String, newMail: String) {

		val reason = getByMail(oldMail)
				?: throw IllegalArgumentException("Wrong mail supplied to authReasonService.changeMail: '$oldMail'")

		if (getByMail(newMail) != null) {
			throw IllegalArgumentException("New mail belongs to other user: authReasonService.changeMail: '$newMail")
		}

		authReasonRepo.delete(reason)
		reason.id = encode(newMail)
		reason.mail = newMail
		authReasonRepo.save(reason)
	}

	fun createVerifyMail(userId: String, mail: String) {
		val verify = MailVerify(userId = userId, mail = mail)

		//		if (mail.endsWith("test")) {
		//		verify.id = mail //.replace("@", "-at-").replace(".", "-dot-") // TODO Test delete this
		//		}

		launch { mailTextsService.sendVerificationMail(verify) }
		launch { mailVerifyRepo.save(verify) }
	}

	//	fun resendEmail(mail: String) {
	//		// TODO do this
	//	}

	fun verifyMail(code: String): Boolean {
		val verify = mailVerifyRepo.findById(code).orElseThrow { throw ElementNotFoundException("Code is invalid") }
		userService.setUserAccessType(verify.userId, AuthPermission.CLIENT)
		launch { mailVerifyRepo.deleteById(verify.id) }
		return true
	}

	/**
	 * @param oldPass is for password restore method
	 */
	fun changePass(mail: String, newPass: String, oldPass: String? = null) {
		val authReason = authReasonRepo.findById(encode(mail)).orElseThrow {
			CustomExceptionCode(405, "Mail is wrong")
		}

		if (oldPass != null && authReason.authData != encode(oldPass)) {
			throw CustomExceptionCode(403, "Passwords doesn't match")
		}

		authReasonRepo.save(authReason.apply { this.authData = encode(newPass) })
	}

	private fun encode(string: String): String = Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString()

	fun requestPasswordRestore(mail: String): PasswordRestore {
		val user = userService.getByMail(mail) ?: throw CustomExceptionCode(402, "User with this email " +
				"doesn't exist.")

		passwordRestoreRepo.findByUserId(user.id).ifPresent {
			passwordRestoreRepo.deleteById(it.id)
		}

		return PasswordRestore(userId = user.id, userMail = mail)
			.also { passwordRestoreRepo.save(it) }
			.also { mailTextsService.sendMailResetMail(it.id, it.userMail) }
	}

	fun submitPasswordRestore(mail: String, code: String, pass: String): Boolean {
		val restore = passwordRestoreRepo.findById(code).orElseThrow { ElementNotFoundException("This code is illegal.") }
		changePass(restore.userMail, newPass = pass)
		return true
	}
}
	