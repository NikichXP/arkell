package com.arkell.api

import com.arkell.entity.auth.AccessToken
import com.arkell.entity.auth.AuthReason
import com.arkell.entity.auth.RefreshToken
import com.arkell.model.auth.AuthReasonService
import com.arkell.model.auth.AuthService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthAPI(
		private val authService: AuthService,
		private val authReasonService: AuthReasonService) {

	/**
	 * Get accessToken in exchange for refreshToken
	 * @param token is RefreshToken
	 *
	 * @return AccessToken entity.
	 * 901 refresh token is invalid (actually it's code 1001, swagger don't like codes that are >999)
	 */
	@PostMapping("/access")
	fun access(@RequestParam token: String): AccessToken {
		return authService.createAccessToken(token)
	}

	/**
	 * Updates your access token: sets timeout to 1 hour again. Use it if you don't want to obtain new access token.
	 * Valid only when access token is valid.
	 */
	@PostMapping("/update")
	fun update(@RequestParam token: String): AccessToken {
		return authService.renewToken(token)
	}

	/**
	 * Register by mail and pass
	 * @param phone temporary it is optional
	 * @return temporary returns AuthReason entity for debugging ease.
	 */
	@PostMapping("/register/mail")
	fun registerByMail(@RequestParam mail: String, @RequestParam phone: String?, @RequestParam pass: String): AuthReason {
		return authReasonService.registerByMailAndPass(mail, phone, pass)
	}

	/**
	 * Login using existing mail and pass
	 * @param isMobile send true is you are mobile device
	 * @return RefreshToken entity.
	 */
	@PostMapping("/login/mail")
	fun loginByMail(@RequestParam mail: String, @RequestParam pass: String, @RequestParam isMobile: Boolean?): RefreshToken {
		return authService.login(authReasonService.getByMailAndPass(mail, pass))
	}

	/**
	 * Change password (as usually users do)
	 * @param oldPass is current password
	 * @return true if operation was successful
	 * 403 if oldPass doesn't match with existing
	 */
	@PostMapping("/pass/change")
	fun changePassword(@RequestHeader token: String, @RequestParam oldPass: String, @RequestParam newPass: String): Boolean {
		authReasonService.changePass(authService.getUser(token).mail, oldPass = oldPass, newPass = newPass)
		return true
	}

	/**
	 * Verify the mail by the code.
	 * !!! Currently, for dev reasons, verification code is your mail.
	 * @param code is sent to user's email address
	 */
	@PostMapping("/verify")
	fun verify(@RequestParam code: String): Boolean {
		return authReasonService.verifyMail(code)
	}

	/**
	 * Reset password by mail
	 * @param mail is user's email
	 * @return 200 OK if mail is sent,
	 *  409 - IllegalStateException if there is already an active password restore request,
	 *  404 - NotFoundException - user with this mail doesn't exist
	 */
	@PostMapping("/reset/mail")
	fun resetByMail(@RequestParam mail: String): String {
		authReasonService.requestPasswordRestore(mail)
		return "ok"
	}

	/**
	 * Submits the password with code from mail.
	 * @return 200 if OK, 404 if code sent is illegal
	 */
	@PostMapping("/reset/submit")
	fun resetSubmit(@RequestParam mail: String, @RequestParam code: String, @RequestParam pass: String): Boolean {
		return authReasonService.submitPasswordRestore(mail, code, pass)
	}

}