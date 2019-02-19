package com.arkell.util

object SwaggerGen {

	fun getSwaggerDoc(vararg docs: DocsCreator.ClassDeclaration): SwaggerDoc {

		val ret = SwaggerDoc()

		for (doc in docs) {

			ret.addTag(doc.name, doc.description)

			doc.methods.forEach {

				if (it.method.equals("any", true)) {
					it.method = "post"
				}

				ret.addPath(path = doc.path + it.path, method = it.method, apiName = doc.name,
						doc = (if (it.method.equals("post", true))
							SwaggerPostMethod(it) else SwaggerMethod(it)).modifyOperationId(doc.name))
			}
		}

		return ret
	}

}

class SwaggerDoc {

	var swagger = "2.0"
	var info = mapOf(
			"description" to "This is a sample server Petstore server.  You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, you can use the api key `special-key` to test the authorization filters.",
			"version" to "1.0.0",
			"title" to "Swagger Petstore"

	)
	var host = "test.domain.com"
	var basePath = "/api"
	var tags = mutableListOf<SwaggerTag>()

	//	var definitions = mapOf(
	//			"HttpServletRequest" to mapOf(
	//					"type" to "object",
	//					"properties" to mapOf<Any, Any>()
	//			))

	var paths = mutableMapOf<String, Map<String, SwaggerMethod>>()

	fun addPath(path: String, method: String, doc: SwaggerMethod, apiName: String) {
		paths[path] = mapOf(method.toLowerCase() to doc)
		doc.tags.add(apiName)
	}

	fun addTag(name: String, description: String) {
		this.tags.add(SwaggerTag(name, description))
	}
}

class SwaggerTag(var name: String, var description: String)

open class SwaggerMethod(function: DocsCreator.FunctionDeclaration) {

	var parameters = mutableListOf<Map<*, *>>()
	var summary = function.docs
	var operationId = function.name
	var tags = mutableListOf<String>()

	var responses = mutableMapOf<String, Any>()

	init {
		parameters = function.args
			.filter { it.type != "HttpServletResponse" }
			.filter { it.type != "HttpServletRequest" }
			.map { SwaggerArg(it) }
			.filter { it.type != "HttpServletResponse" }
			.filter { it.type != "HttpServletRequest" }
			.map { updateParam(it).getMap(false) }.toMutableList()

		function.returnCodes.forEach { k, v ->
			println("function: ${function.name}, return code $k, docs: $v")
			responses[k.toString()] = mapOf("description" to v)
		}

		responses["200"] = mapOf("description" to function.returns.let {
			function.returnType.print()
			if (it.isEmpty()) {
				"Type: ${function.returnType
					.replace('<', '(').replace('>', ')')}."
			} else {
				"Type: ${function.returnType
					.replace('<', '(').replace('>', ')')}; Docs: $it"
			}
		})
	}

	open fun updateParam(arg: SwaggerArg): SwaggerArg {
		if (arg.`in` == "body") {
			arg.`in` = "query"
		}
		return arg
	}

	fun modifyOperationId(name: String): SwaggerMethod {
		this.operationId = "$name.$operationId"
		return this
	}
}

class SwaggerPostMethod(function: DocsCreator.FunctionDeclaration) : SwaggerMethod(function) {

	var consumes = arrayOf(
			"multipart/form-data"
	)

	init {
		parameters = function.args
			.map { SwaggerArg(it) }
			.filter { it.type != "HttpServletResponse" }
			.map { updateParam(it).getMap(true) }.toMutableList()

		function.returnCodes.forEach { k, v ->
			println("function: ${function.name}, return code $k, docs: $v")
			responses[k.toString()] = mapOf("description" to v)
		}

		responses["200"] = mapOf("description" to function.returns.let {
			function.returnType.print()
			if (it.isEmpty()) {
				"Type: ${function.returnType
					.replace('<', '(').replace('>', ')')}."
			} else {
				"Type: ${function.returnType
					.replace('<', '(').replace('>', ')')}; Docs: $it"
			}
		})
	}

}

class SwaggerArg(
		var name: String,
		var `in`: String = "query",
		var description: String,
		var type: String,
		var required: Boolean
) {

	constructor(arg: DocsCreator.FunctionDeclaration.FunctionArg) :
			this(
					name = arg.name,
					description = arg.docs ?: "",
					type = arg.clazz,
					required = arg.required
			) {
		`in` = when (arg.type) {
			"@RequestParam" -> "query"
			"@PathVariable" -> "path"
			"@RequestHeader" -> "header"
			else -> "header"
		}
		if (type == "HttpServletRequest") {
			description += "is actually any other field of this entity you can send in request"
			`in` = "formData"
		}
		type = when (type) {
			"Int", "Long" -> "integer"
			"HttpServletRequest",
			"String" -> "string"
			"Boolean" -> "boolean"
			"Double" -> "number"

			"Platform" -> "string"
			"AuthPermission" -> "string"

			else -> type
		}
	}

	fun getMap(isPost: Boolean): Map<*, *> {
		val ret = this.toMap()
		if (isPost) {
			if (ret["in"] == "body") {
				ret["in"] = "formData"
			}
		}
		if (this.type.startsWith("List") || this.type.startsWith("Array")) {
			ret["type"] = "array"
			ret["items"] = mapOf("type" to "string")
		}
		return ret
	}

}