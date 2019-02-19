package com.arkell.config

import com.arkell.config.viewmodifier.BasicViewModifier
import com.arkell.entity.exception.CustomExceptionCode
import com.arkell.entity.exception.NotLoggedInException
import com.arkell.util.Ret
import com.arkell.util.print
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class ControllerAdvisor(
		private val viewModifier: BasicViewModifier) : ResponseBodyAdvice<Any> {

	val ignoredClasses = setOf(IOException::class, ArithmeticException::class,
			MissingServletRequestParameterException::class, HttpRequestMethodNotSupportedException::class)

	override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>) = true

	override fun beforeBodyWrite(body: Any, returnType: MethodParameter, selectedContentType: MediaType,
	                             selectedConverterType: Class<out HttpMessageConverter<*>>,
	                             request: ServerHttpRequest, response: ServerHttpResponse): Any {
		response.headers.add("content-type", "application/json; charset=UTF-8")
		if (response.headers["Access-Control-Allow-Origin"] == null) response.headers.accessControlAllowOrigin = "*"

//		body.javaClass.name.print("Classname: ")

		val ret = viewModifier.modify(body, request)
		return returnForMap(viewModifier.onPreBuild(ret, request) as Map<*, *>)
	}

	fun returnForMap(returnEntity: Map<*, *>): Map<*, *> {
		return if (
				((returnEntity["status"] as? String)?.toLowerCase() == "ok" && returnEntity["data"] != null) ||
				((returnEntity["status"] as? String)?.toLowerCase() == "error" && returnEntity["message"] != null)
		) {
			returnEntity
		} else {
			mapOf<String, Any?>(
					"data" to returnEntity,
					"status" to "ok"
			)
		}
	}

	@ExceptionHandler(Exception::class)
	fun handle(ex: Exception, request: HttpServletRequest): ResponseEntity<*> {
		if (request.requestURL.toString().contains("localhost")) {
			ex.printStackTrace()
			return Ret.error(800, "Exception")
		}
		val sb = StringBuilder()
			.append(request.remoteAddr)
			.append(" @ ")
			.append(Optional.ofNullable(request.getHeader("user-agent")).orElse("unknown device"))
			.append(" : ")
			.append(request.requestURL)
			.append("  ->  {")
			.append(request.parameterMap
				.keys.stream()
				.map { key -> key + ":" + request.getParameter(key) }
				.reduce { s1, s2 -> "$s1, $s2" }
				.orElse(""))
			.append("}\n")
			.append(ex.javaClass.name).append("  ->  ")
			.append(ex.localizedMessage).append("\n")
		ex.stackTrace
			.filter { it.className.startsWith("com.avant") }
			.forEach {
				sb.append(it.className).append(" : ").append(it.methodName).append(" @ line ")
					.append(it.lineNumber).append("\n")
			}

		if (!ignoredClasses.contains(ex::class)) {
			// TODO logging to telegram
		}

		val returnCode: Int = when (ex) {
			is NullPointerException -> 503
			is IllegalArgumentException -> 406
			is MissingServletRequestParameterException,
			is HttpRequestMethodNotSupportedException,
			is FileNotFoundException,
			is ServletRequestBindingException,
			is IOException -> 404
			is NotLoggedInException -> 401
			is IllegalAccessError,
			is IllegalAccessException -> 403
			is IllegalStateException -> 409
			is CustomExceptionCode -> ex.code
			else -> 503
		}

		if (returnCode == 503) {
//			ex.printStackTrace()
			sb.print()
			//			if (Math.random() < 0.1) {
			//				return Ret.error(418, "I’m a teapot")
			//			}
			return Ret.error(503, sb.toString())
		} else {
			// println(sb)
		}

		return Ret.error(returnCode, ex.message ?: "No message available.")
	}

}