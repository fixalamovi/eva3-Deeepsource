package com.example.deepsource

import com.example.deepsource.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class BackendResponseTest {
    private fun String.toResponseBody(): ResponseBody = ResponseBody.create(null, this)
    @Test fun successfulResponseReturnsItsBody() = runBlocking {
        assertEquals("ok", backendCall { Response.success("ok") })
    }
    @Test fun emptySuccessBodyIsAnError() = runBlocking {
        assertEquals("El servidor devolvió una respuesta vacía",
            runCatching { backendCall { Response.success<String>(null) } }.exceptionOrNull()?.message)
    }
    @Test fun structuredErrorPreservesStatusAndMessage() = runBlocking {
        val error = runCatching { backendCall<String> {
            Response.error(409, "{\"message\":\"La tarea tiene registros\"}".toResponseBody())
        } }.exceptionOrNull() as BackendException
        assertEquals(409, error.status)
        assertEquals("La tarea tiene registros", error.message)
    }
    @Test fun malformedAndMissingMessagesUseTheSafeFallback() = runBlocking {
        for (body in listOf("<html>error</html>", "{}", "{\"message\":null}",
            "{\"message\":\"  \"}", "{\"message\":{\"debug\":true}}", "{\"message\":42}")) {
            val error = runCatching { backendCall<String> { Response.error(500, body.toResponseBody()) } }.exceptionOrNull()
            assertTrue(error is BackendException)
            assertEquals("El servidor no pudo completar la operación", error?.message)
        }
    }
    @Test fun ioFailureHasAnActionableMessage() = runBlocking {
        val error = runCatching { backendCall<String> { throw IOException("private detail") } }.exceptionOrNull()
        assertTrue(error!!.message!!.contains("Revisa la conexión"))
        assertFalse(error.message!!.contains("private detail"))
    }
    @Test fun cancellationIsNeverConvertedToConnectionFailure() = runBlocking {
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, runCatching { backendCall<String> { throw cancellation } }.exceptionOrNull())
    }
    @Test fun unitResponseAcceptsNoContent() = runBlocking {
        backendUnit { Response.success<Unit>(204, null) }
    }
    @Test fun unitResponsePreservesServerError() = runBlocking {
        val error = runCatching { backendUnit {
            Response.error(401, "{\"message\":\"Sesión vencida\"}".toResponseBody())
        } }.exceptionOrNull() as BackendException
        assertEquals(401, error.status)
        assertEquals("Sesión vencida", error.message)
    }
    @Test fun unitResponseUsesSafeFallbackForInvalidJson() = runBlocking {
        assertEquals("El servidor no pudo completar la operación", runCatching {
            backendUnit { Response.error(503, "not-json".toResponseBody()) }
        }.exceptionOrNull()?.message)
    }
    @Test fun unitResponseMapsConnectionFailure() = runBlocking {
        assertTrue(runCatching { backendUnit { throw IOException() } }.exceptionOrNull()!!.message!!.contains("conectar"))
    }
    @Test fun unitResponsePropagatesCancellation() = runBlocking {
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, runCatching { backendUnit { throw cancellation } }.exceptionOrNull())
    }
    @Test fun programmingErrorsAreNotHiddenAsConnectionFailures() = runBlocking {
        val error = IllegalArgumentException("invalid request")
        assertSame(error, runCatching { backendCall<String> { throw error } }.exceptionOrNull())
    }
}
