package com.driver.portal

import android.content.Context

object DriverSession {

    private const val PREF_NAME = "driver_session"
    private const val KEY_NAME = "driver_name"
    private const val KEY_PHONE = "driver_phone"
    private const val KEY_CAR = "car_number"

    // Persistent prefs — NOT cleared on logout
    private const val PERSISTENT_PREF = "driver_persistent"
    private const val KEY_LOCKED_CAR = "locked_car_number"
    private const val KEY_LAST_ARCHIVED_MONTH = "last_archived_month"
    private const val KEY_LAST_ARCHIVED_YEAR = "last_archived_year"

    fun saveDriver(context: Context, name: String, phone: String, car: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
            .putString(KEY_CAR, car)
            .apply()
        // Lock car number permanently (survives logout)
        lockCarNumber(context, car)
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
        return getCarNumber(context).isNotEmpty()
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        // Persistent prefs (locked car, archive period) are NOT cleared
    }

    // ======== Car Lock ========
    fun lockCarNumber(context: Context, car: String) {
        val prefs = context.getSharedPreferences(PERSISTENT_PREF, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOCKED_CAR, car).apply()
    }

    fun getLockedCarNumber(context: Context): String {
        val prefs = context.getSharedPreferences(PERSISTENT_PREF, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOCKED_CAR, "") ?: ""
    }

    // ======== Monthly Archive ========
    fun getLastArchivedMonth(context: Context): Int {
        val prefs = context.getSharedPreferences(PERSISTENT_PREF, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_ARCHIVED_MONTH, -1)
    }

    fun getLastArchivedYear(context: Context): Int {
        val prefs = context.getSharedPreferences(PERSISTENT_PREF, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_ARCHIVED_YEAR, -1)
    }

    fun setLastArchivedPeriod(context: Context, month: Int, year: Int) {
        val prefs = context.getSharedPreferences(PERSISTENT_PREF, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_LAST_ARCHIVED_MONTH, month)
            .putInt(KEY_LAST_ARCHIVED_YEAR, year)
            .apply()
    }
}