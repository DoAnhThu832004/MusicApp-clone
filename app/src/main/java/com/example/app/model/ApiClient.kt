package com.example.app.model

import android.content.Context
import com.example.app.model.request.RefreshRequest
import com.example.app.model.response.AuthenticationResponse
import com.example.app.viewmodel.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import retrofit2.http.Body
import retrofit2.http.POST

// Ví dụ IP máy tính của bạn là 192.168.1.15
private const val BASE_URL = "http://192.168.1.79:8080/identity/"

interface AuthApi {
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): retrofit2.Response<AuthenticationResponse>
}

object ApiClient {

    fun build(context: Context): ApiService {
        val sessionManager = SessionManager(context)

        // Interceptor: thêm Authorization header nếu có
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val path = original.url.encodedPath
            val isPublic = (path.endsWith("/auth/token") && original.method == "POST") ||
                    (path.endsWith("/users") && original.method == "POST")

            if (isPublic) return@Interceptor chain.proceed(original)

            val token = sessionManager.getAccessTokenSync()
            val requestBuilder = original.newBuilder()
            if (!token.isNullOrEmpty()) requestBuilder.addHeader("Authorization", "Bearer $token")

            chain.proceed(requestBuilder.build())
        }

        // OkHttpClient without auth (for refresh)
        val clientNoAuth = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        // Retrofit without auth used by authenticator
        val retrofitNoAuth = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(clientNoAuth)
            .build()

        val authApi = retrofitNoAuth.create(AuthApi::class.java)

        // Authenticator: khi gặp 401, sẽ gọi refresh token (synchronous)
        val tokenAuthenticator = Authenticator { route, response ->
            // Avoid infinite loops
            if (responseCount(response) >= 2) return@Authenticator null

            val refresh = sessionManager.getRefreshTokenSync() ?: return@Authenticator null

            // Call refresh synchronously inside runBlocking style (OkHttp Authenticator already on background thread)
            try {
                val refreshReq = RefreshRequest(refresh)
                val refreshResponse = runCatching {
                    // Use retrofitNoAuth to call refresh (suspend -> use .execute via service built with Call)
                    // Simpler: use authApi via coroutine is not available here; use synchronous OkHttp call instead
                    val gson = Gson()
                    val requestBody = RequestBody.create(
                        "application/json".toMediaType(),
                        gson.toJson(refreshReq)
                    )
                    val req = Request.Builder()
                        .url(BASE_URL + "auth/refresh")
                        .post(requestBody)
                        .build()
                    clientNoAuth.newCall(req).execute()
                }.getOrNull()

                if (refreshResponse != null && refreshResponse.isSuccessful) {
                    val body = refreshResponse.body?.string()
                    val authResp = Gson().fromJson(body, AuthenticationResponse::class.java)
                    val newAccess = authResp.result.token
                    val newRefresh = authResp.result.token // nếu backend tách access/refresh, parse accordingly

                    if (!newAccess.isNullOrEmpty()) {
                        // Lưu token mới (suspend) bằng runBlocking
                        runBlocking {
                            sessionManager.saveSession(newAccess, newRefresh)
                        }
                        // Retry original request with new token
                        return@Authenticator response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccess")
                            .build()
                    }
                }
            } catch (e: Exception) {
                // ignore -> trả về null để chain 401 lên UI
            }
            null
        }

        val okClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okClient)
            .build()

        return retrofit.create(ApiService::class.java)
    }

    fun getAiChatRepository(context: Context): com.example.app.model.repository.AiChatRepository {
        val apiService = build(context)
        return com.example.app.model.repository.AiChatRepository(apiService)
    }

    private fun responseCount(response: Response): Int {
        var res: Response? = response
        var result = 1
        while (res?.priorResponse != null) {
            result++
            res = res.priorResponse
        }
        return result
    }
}
