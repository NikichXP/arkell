package com.arkell.api

import com.arkell.entity.TestRepo
import com.arkell.entity.exception.CustomExceptionCode
import com.arkell.model.internal.MailService
import com.arkell.repo.CategoryRepo
import com.arkell.util.DocsCreator
import com.arkell.util.EntityDocsCreator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File

@RestController
@RequestMapping("/api/test")
class TestAPI(
		val mailService: MailService,
		val categoryRepo: CategoryRepo,
		val testRepo: TestRepo) {

	@GetMapping("/env")
	fun userDir(): String {
		return System.getProperty("user.dir")
	}

	@GetMapping("/ping")
	fun ping() = "ok"

	@RequestMapping("/mail")
	fun mail(@RequestParam mail: String?): String {
		mailService.sendMail("Test mail", "Test 12345 OK", mail ?: "nikichx2@gmail.com")
		return "ok"
	}

	@GetMapping("/getDocs")
	fun test(): Set<EntityDocsCreator.EntityInfo> {
		val names = File(System.getProperty("user.dir") + "/src/main/java/com/arkell/api").listFiles()
			.filter { it.name != "AbstractAPI.kt" }
			.flatMap { if (it.isFile) listOf(it) else it.listFiles().toList() }
			.map { DocsCreator.getDocs(it, "<br>") }
			.flatMap { it.methods }
			.map { it.returnType }
		return EntityDocsCreator.test(names.toSet())
	}

	@GetMapping("/error/{code}")
	fun errorCode(@PathVariable code: Int): ResponseEntity<*> {
		throw CustomExceptionCode(code, "meeeessage!")
	}

	@GetMapping("/easter_egg/authors/secret")
	fun easterEggAuthors(): Any {
		return mapOf(
				"team lead" to mapOf(
						"name" to "Andrey Hrolenko",
						"tg_username" to "@andixp"
				),
				"back-end dev" to mapOf(
						"name" to "Nikita Ruzhevsky",
						"tg_id" to "34080460",
						"tg_username" to "@NikichXP/@iSyncOS"
				),
				"android dev" to listOf(
						mapOf(
								"name" to "Nikita Kraev",
								"tg_username" to "@kitttn"
						),
						mapOf(
								"name" to "Pavel Sarpov",
								"tg_username" to "@Oldbosun"
						)
				),
				"iOS dev" to mapOf(
						"name" to "Alexey Milakhin",
						"tg_username" to "@alexeymilakhin"
				),
				"front-end dev" to listOf(
						mapOf(
								"name" to "Yevhenii Loginov",
								"tg_username" to "@ev_loginov"
						),
						mapOf(
								"name" to "Sergey Maystrenko",
								"tg_username" to "@sergeymays"
						)
				),
				"testing" to mapOf(
						"tg_username" to "@cygnusx",
						"isAnon" to true
				)
		)
	}

}