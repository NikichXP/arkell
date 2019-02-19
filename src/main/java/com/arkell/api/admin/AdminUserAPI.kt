package com.arkell.api.admin

import com.arkell.entity.UserEntity
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.interaction.MailSubscription
import com.arkell.model.UserService
import com.arkell.model.auth.AuthReasonService
import com.arkell.model.auth.AuthService
import com.arkell.util.getParamData
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import javax.servlet.http.HttpServletRequest

@Auth(value = AuthPermission.MODERATOR)
@RestController
@RequestMapping("/api/admin/user")
class AdminUserAPI(
		private val authReasonService: AuthReasonService,
		private val authService: AuthService,
		private val userService: UserService) {

	@GetMapping("/{id}")
	fun id(@PathVariable id: String): UserEntity {
		return userService.getById(id)
	}

	/**
	 * @param accessLevel is user access level, can be: ADMIN, MODERATOR, CLIENT, USER, BLOCKED
	 */
	@PostMapping("/create")
	fun create(@RequestParam birthday: String, @RequestParam gender: String, @RequestParam accessLevel: AuthPermission?,
	           @RequestParam password: String, @RequestParam mail: String, request: HttpServletRequest): UserEntity {
		val user = userService.createWithData(birthday, gender, accessLevel, request.getParamData())
		authReasonService.createAuthReason(mail, password, user)
		return user
	}

	/**
	 * Sets user info. Apply only args that provided.
	 * @param birthday must be like 2018-07-31
	 * @param gender is "male" or "female", case insensitive.
	 */
	@PostMapping("/info")
	fun updateInfo(@RequestParam birthday: String? = null, @RequestParam gender: String? = null,
	               @RequestParam userId: String, request: HttpServletRequest): UserEntity {

		return userService.updateInfo(id = userId, birthday = birthday?.let { LocalDate.parse(it) },
				data = request.getParamData(),
				gender = gender?.let {
					when (it.toLowerCase()) {
						"male" -> "male"
						"female" -> "female"
						else -> throw IllegalArgumentException("There are only 2 genders")
					}
				})
	}

	/**
	 * @param type can be: ADMIN, MODERATOR, CLIENT, USER, BLOCKED
	 * @param mail case insensible search in part of email.
	 */
	@GetMapping("/list")
	fun list(@RequestParam page: Int, @RequestParam pageSize: Int?, @RequestParam type: Array<AuthPermission>?,
	         @RequestParam mail: String?): Page<UserEntity> {
		return userService.list(page = page, pageSize = pageSize ?: 20, type = type, mail = mail)
	}

	/**
	 * @param type can be: ADMIN, MODERATOR, CLIENT, USER, BLOCKED
	 */
	@Auth(AuthPermission.MODERATOR)
	@PostMapping("/access")
	fun access(@RequestParam userId: String, @RequestParam token: String, @RequestParam type: AuthPermission): UserEntity {
		val author = authService.getUser(token)
		return userService.changeAccessType(userId, author = author, type = type)
	}

	@GetMapping("/subscriptionList")
	fun subscriptionList(@RequestParam page: Int, @RequestParam pageSize: Int?): Page<MailSubscription> {
		return userService.getSubscriptions(page, pageSize ?: 20)
	}

	@PostMapping("/changeMail")
	fun changeMail(@RequestParam id: String, @RequestParam oldMail: String, @RequestParam newMail: String): UserEntity {
		return userService.changeMail(id, oldMail, newMail)
	}

}