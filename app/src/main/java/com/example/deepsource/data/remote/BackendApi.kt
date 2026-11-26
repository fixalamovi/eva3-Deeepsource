package com.example.deepsource.data.remote

import com.example.deepsource.domain.*
import retrofit2.Response
import retrofit2.http.*
import java.time.LocalDate

internal data class GoogleLoginChallenge(val clientId: String, val nonce: String)
internal data class GoogleLoginBody(val idToken: String)
internal data class LoginBody(val email: String, val password: String)
internal data class RegisterBody(val name: String, val email: String, val password: String)
internal data class NameBody(val name: String)
internal data class PasswordBody(val current: String, val next: String)
internal data class ProjectBody(val name: String, val client: String, val estimateMinutes: Int)
internal data class TaskBody(val name: String)
internal data class EntryBody(val projectId: String, val taskId: String, val day: String, val seconds: Long, val source: String)
internal data class RemoteEntry(
    val id: Long, val uid: String, val userName: String, val projectId: String,
    val day: String, val seconds: Long, val task: String, val source: String, val taskId: String
) {
    fun domain() = TimeEntry(id, uid, userName, projectId, LocalDate.parse(day), seconds, task, source, taskId)
}
internal interface ProjectsApi {
    @POST("api/auth/google/challenge") suspend fun googleChallenge(): Response<GoogleLoginChallenge>
    @POST("api/auth/google") suspend fun loginWithGoogle(@Body body: GoogleLoginBody): Response<Session>
    @POST("api/auth/login") suspend fun login(@Body body: LoginBody): Response<Session>
    @POST("api/auth/register") suspend fun register(@Body body: RegisterBody): Response<Session>
    @POST("api/auth/logout") suspend fun logout(@Header("Authorization") auth: String): Response<Unit>
    @PUT("api/auth/profile") suspend fun profile(@Header("Authorization") auth: String, @Body body: NameBody): Response<Session>
    @PUT("api/auth/password") suspend fun password(@Header("Authorization") auth: String, @Body body: PasswordBody): Response<Unit>
    @GET("api/proyectos") suspend fun projects(@Header("Authorization") auth: String): Response<List<Project>>
    @GET("api/tareas") suspend fun tasks(@Header("Authorization") auth: String): Response<List<ProjectTask>>
    @POST("api/proyectos") suspend fun createProject(@Header("Authorization") auth: String, @Body body: ProjectBody): Response<Project>
    @POST("api/proyectos/{id}/tareas") suspend fun createTask(@Header("Authorization") auth: String, @Path("id") id: String, @Body body: TaskBody): Response<ProjectTask>
}
internal interface RecordsApi {
    @GET("api/registros") suspend fun entries(@Header("Authorization") auth: String): Response<List<RemoteEntry>>
    @POST("api/registros") suspend fun create(@Header("Authorization") auth: String, @Header("Idempotency-Key") key: String, @Body body: EntryBody): Response<RemoteEntry>
    @PUT("api/registros/{id}") suspend fun update(@Header("Authorization") auth: String, @Path("id") id: Long, @Body body: EntryBody): Response<RemoteEntry>
    @DELETE("api/registros/{id}") suspend fun delete(@Header("Authorization") auth: String, @Path("id") id: Long): Response<Unit>
}
