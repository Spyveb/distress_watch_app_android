package com.app.wearapp.ApiCall

import com.app.wearapp.BaseApplication
import com.app.wearapp.UserAuth.Auth
import okhttp3.Interceptor
import okhttp3.Response

class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${Auth.getToken(BaseApplication.context)}")
            .addHeader("Accept", "application/json")
            .build()

        return chain.proceed(request)
    }
}