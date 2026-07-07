package app.aventurine.jetmapdemo.data.network

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Api(val host: String)

val KClass<*>.host: String
    get() = this.java.annotations.filterIsInstance<Api>().firstOrNull()?.host ?: ""