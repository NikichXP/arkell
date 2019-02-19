package com.arkell.util.objects

import org.jboss.logging.Logger
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

class ObjectFromMapUpdater<T : Any>(val entity: T) {

	var map = mutableMapOf<String, String>()

	constructor(entity: T, map: Map<String, String>) : this(entity) {
		this.map = map.toMutableMap()
	}

	fun data(map: Map<String, String>) = apply {
		map.forEach { k, v -> this.map[k] = v }
	}

	fun exclude(vararg params: String) = apply {
		params.forEach { map.remove(it) }
	}

	fun modify(): T = entity.apply {
		map.filterKeys { it != "id" }
			.forEach { param, value ->
				try {

					//					println("field $param")

					val field = this::class.memberProperties.find { it.name.toLowerCase() == param.toLowerCase() }
							?: return@forEach

					//					println("field $param found")

					val setter = (field as? KMutableProperty<*> ?: return@forEach).setter

					//					println("field $param got setter ${field.javaGetter?.returnType?.simpleName}")

					when (field.javaGetter?.returnType?.simpleName?.toLowerCase()) {
						"boolean" -> setter.call(this, value.toBoolean())
						"string" -> setter.call(this, value)
						"double" -> setter.call(this, value.toDouble())
						"long" -> setter.call(this, value.toLong())
						"integer", "int" -> setter.call(this, value.toInt())
						else -> {
							//							println("field $param else loop")
						}
					}
				} catch (e: Exception) {
					Logger.getLogger(this::class.java)
						.error("Error updating entity of ${this::class.java.name}: [\"$param\" - \"$value\"] failed")
					// ну и хер с ними
				}
			}
	}

}