package com.app.wearapp.ApiCall

import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @FormUrlEncoded
    @POST("create_sos_emergency_case")
    suspend fun sendData(@FieldMap toString: Map<String, String>): Response<ApiResponse>

    @POST("check_token")
    suspend fun checkToken(): Response<ApiResponse>

}