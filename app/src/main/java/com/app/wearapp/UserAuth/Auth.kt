package com.app.wearapp.UserAuth

import android.content.Context
import com.app.banoun.UserAuth.mainPrefsName


class Auth {

    companion object {
        private const val tokenKeyName = "token"
        private const val idKeyName = "id"
        private const val nameKeyName = "name"


        fun setToken(context: Context, value: String?) {
            val sharedPref = context.getSharedPreferences(tokenKeyName, Context.MODE_PRIVATE)
            sharedPref.edit().putString(mainPrefsName, value)
                .apply()
        }

        fun getToken(context: Context?): String? {
            val sharedPref = context?.getSharedPreferences(tokenKeyName, Context.MODE_PRIVATE)
            return sharedPref?.getString(mainPrefsName, null)
        }

        fun setId(context: Context ,value: String) {
            val sharedPref = context.getSharedPreferences(idKeyName, Context.MODE_PRIVATE)
            sharedPref.edit().putString(mainPrefsName, value)
                .apply()
        }

        fun getId(context: Context?): String? {
            val sharedPref = context?.getSharedPreferences(nameKeyName, Context.MODE_PRIVATE)
            return sharedPref?.getString(mainPrefsName, "")
        }

        fun setName(context: Context ,value: String) {
            val sharedPref = context.getSharedPreferences(nameKeyName, Context.MODE_PRIVATE)
            sharedPref.edit().putString(mainPrefsName, value)
                .apply()
        }

        fun getName(context: Context?): String? {
            val sharedPref = context?.getSharedPreferences(idKeyName, Context.MODE_PRIVATE)
            return sharedPref?.getString(mainPrefsName, "")
        }



        fun clearAllPreferences(context: Context) {

            setToken(context,null)
            setId(context,"")
            setName(context,"")
            val pref1 = context.getSharedPreferences(mainPrefsName, Context.MODE_PRIVATE)
            pref1.edit().clear().apply()
        }
    }
}