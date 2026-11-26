package com.example.deepsource.data.remote

import com.example.deepsource.BuildConfig
import com.example.deepsource.domain.Session
import kotlinx.coroutines.CancellationException
import com.google.gson.JsonParser
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class BackendException(val status: Int, message: String) : IllegalStateException(message)
internal class BackendConnectionException(service: String?, cause: IOException) :
    IllegalStateException(
        if (service == null) "No pudimos conectar con el servidor. Revisa la conexión y vuelve a intentarlo."
        else "No pudimos conectar con $service. Inicia los microservicios y vuelve a intentarlo.",
        cause
    )

// Gson ya es parte de Retrofit. Este análisis también funciona en pruebas JVM.
private fun serverMessage(raw: String): String? = runCatching {
    val element = JsonParser.parseString(raw).asJsonObject.get("message")
    if (element != null && element.isJsonPrimitive && element.asJsonPrimitive.isString)
        element.asString.takeIf { it.isNotBlank() } else null
}.getOrNull()

internal object BackendClient {
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    }
    private fun retrofit(url: String) = Retrofit.Builder().baseUrl(url)
        .client(client).addConverterFactory(GsonConverterFactory.create()).build()
    val projects: ProjectsApi by lazy { retrofit(BuildConfig.PROJECTS_API_URL).create(ProjectsApi::class.java) }
    val records: RecordsApi by lazy { retrofit(BuildConfig.RECORDS_API_URL).create(RecordsApi::class.java) }
    fun auth(session: Session): String {
        check(session.token.startsWith("ds_")) { "Vuelve a iniciar sesión para conectar con el servidor" }
        return "Bearer " + session.token
    }
}
internal suspend fun <T> backendCall(service: String? = null, block: suspend () -> Response<T>): T {
    val result = try { block() }
    catch (e: CancellationException) { throw e }
    catch (e: IOException) { throw BackendConnectionException(service, e) }
    if (!result.isSuccessful) {
        val raw = result.errorBody()?.use { it.string() }.orEmpty()
        throw BackendException(result.code(), serverMessage(raw) ?: "El servidor no pudo completar la operación")
    }
    return result.body() ?: error("El servidor devolvió una respuesta vacía")
}
internal suspend fun backendUnit(block: suspend () -> Response<Unit>) {
    val result = try { block() }
    catch (e: CancellationException) { throw e }
    catch (_: IOException) { error("No pudimos conectar con el servidor. Inténtalo nuevamente.") }
    if (!result.isSuccessful) {
        val raw = result.errorBody()?.use { it.string() }.orEmpty()
        throw BackendException(result.code(), serverMessage(raw)
            ?: "El servidor no pudo completar la operación")
    }
}
