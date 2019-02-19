package com.arkell.auth

import com.arkell.entity.UserEntity
import com.arkell.entity.auth.Auth
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.exception.CustomExceptionCode
import com.arkell.model.auth.AuthService
import com.arkell.util.print
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AccessInterceptor(
		private val authService: AuthService) : HandlerInterceptorAdapter() {

	override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any?): Boolean {

		// route https instead of http
		//		if (request.requestURL.toString().startsWith("http:")
		//				&& !request.requestURL.toString().contains("localhost")) {
		//			response.sendRedirect("https" + request.requestURL.toString().substring(4))
		//			return false
		//		}

		//Logging
		(request.requestURL.toString() + "  ->  { token: ${request.getHeader("token")} } " +
				"- { agent: ${request.getHeader("user-agent")} } " +
				"- {" +
				request.parameterMap
					.keys.stream()
					.map { key -> key + ":" + request.getParameter(key) }
					.reduce { s1, s2 -> "$s1, $s2" }.orElse("")
				+ "}").run { if (this.length > 500) this.substring(0..500) else this }.print()

		val method: Method = (handler as? HandlerMethod)?.method ?: throw CustomExceptionCode(404, "Wrong mapping")

		val auth: Auth = method.getAnnotation(Auth::class.java)
				?: method.declaringClass.getAnnotation(Auth::class.java)
				?: return true

		if (auth.value == AuthPermission.NONE) {
			return true
		}

		val user: UserEntity = getUser(request) ?: return writeError(response)

		println("Check auth: ${user.id} -- ${user.mail}")

		return if (user.accessLevel.level >= auth.value.level) {
			true
		} else {
			writeError(response)
		}

		//		auth.value.level
		//		user.accessType
		//
		//		var ret = false
		//
		//		if (auth.value == AuthPermission.ADMIN) {
		//			ret = user.accessType == AccessType.MODERATOR || user.accessType == AccessType.ADMIN
		//		}
		//
		//		if (auth.value == AuthPermission.USER) {
		//			ret = true
		//		}

		//		return if (ret) ret else writeError(response)
	}

	private fun getUser(request: HttpServletRequest): UserEntity? {
		return request.getHeader("token")?.let { authService.getUserByTokenOrNull(it) }
				?: request.getParameter("token")?.let { authService.getUserByTokenOrNull(it) }
	}

	fun writeError(response: HttpServletResponse): Boolean {
		response.writer.write("""	{"status":"error", "message":"Auth failed."}""")
		response.addHeader("Content-Type", "application/json;charset=UTF-8")
		response.status = 401
		response.writer.close()
		return false
	}

}