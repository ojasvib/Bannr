package com.ojasvi.bannr.network

import ImageGenerationService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://credible-blowfish-tender.ngrok-free.app"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ImageGenerationService by lazy {
        retrofit.create(ImageGenerationService::class.java)
    }
}