package com.arkell.util.objects

object ObjectToMapConverter {

	fun convert(obj: Any): MutableMap<String, Any?> =
			if (obj is Map<*, *>) {
				obj.mapKeys { it.toString() }.toMutableMap()
			} else {
				mutableMapOf(*obj::class.java.declaredFields
					.filter {
						return@filter try {
							obj::class.java.getDeclaredMethod("get" + it.name.capitalize()).invoke(obj)
							true
						} catch (e: Exception) {
							try {
								obj::class.java.getDeclaredMethod(it.name).invoke(obj)
								true
							} catch (e2: Exception) {
								false
							}
						}
					}
					.map {
						return@map try {
							it.name to obj::class.java.getDeclaredMethod("get" + it.name.capitalize()).invoke(obj)
						} catch (e: Exception) {
							it.name to obj::class.java.getDeclaredMethod(it.name).invoke(obj)
						}
					}.toTypedArray())
			}
}
