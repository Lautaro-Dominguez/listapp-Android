package ar.edu.itba.listapp.data.network

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object NetworkModule {

    private lateinit var sessionManager: SessionManager

    // provider to fetch token dynamically
    @Volatile
    private var authTokenProvider: (() -> String?)? = null

    fun initialize(context: Context) {
        sessionManager = SessionManager(context)
    }

    fun setAuthTokenProvider(provider: () -> String?) {
        authTokenProvider = provider
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val headersInterceptor = okhttp3.Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")

        // Get token either from provider or SessionManager as fallback
        val tokenFromProvider = authTokenProvider?.invoke()
        val tokenFromSession = if (::sessionManager.isInitialized) sessionManager.loadAuthToken() else null
        val token = tokenFromProvider ?: tokenFromSession
        if (!token.isNullOrBlank()) {
            val headerValue = if (token.startsWith("Bearer ")) token else "Bearer $token"
            requestBuilder.header("Authorization", headerValue)
        }

        chain.proceed(requestBuilder.build())
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }

    val productService: ProductService by lazy {
        retrofit.create(ProductService::class.java)
    }

    val pantryService: PantryService by lazy {
        retrofit.create(PantryService::class.java)
    }

    val listService: ListService by lazy {
        retrofit.create(ListService::class.java)
    }
}
