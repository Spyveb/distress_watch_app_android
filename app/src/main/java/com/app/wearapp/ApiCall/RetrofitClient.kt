package com.app.wearapp.ApiCall

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://lightslategray-bear-191841.hostingersite.com/public/api/"

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HeaderInterceptor())
        .build()

    // Configure Gson
    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    // Configure Retrofit with OkHttpClient and Gson
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)  // Set the OkHttpClient
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}