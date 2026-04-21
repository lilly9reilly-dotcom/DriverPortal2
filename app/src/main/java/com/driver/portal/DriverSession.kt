package com.driver.portal

import android.content.Context

object DriverSession {

    private const val PREF_NAME = "driver_session"
    private const val KEY_NAME = "driver_name"
    private const val KEY_PHONE = "driver_phone"
    private const val KEY_CAR = "car_number"

    fun saveDriver(context: Context, name: String, phone: String, car: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
            .putString(KEY_CAR, car)
            .apply()
    }

    fun getDriverName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    fun getDriverPhone(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PHONE, "") ?: ""
    }

    fun getCarNumber(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CAR, "") ?: ""
    }

    fun isLoggedIn(context: Context): Boolean {
        return getDriverName(context).isNotEmpty()
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}