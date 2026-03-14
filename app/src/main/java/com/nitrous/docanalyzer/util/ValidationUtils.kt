package com.nitrous.docanalyzer.util

import android.util.Patterns

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }

    fun isValidPassword(password: String): Boolean {
        // Minimum 8, Maximum 128, no leading/trailing whitespace
        return password.length in 8..128 && password.trim() == password
    }

    fun isValidOtp(otp: String): Boolean {
        return otp.isNotEmpty() && otp.length == 6 && otp.all { it.isDigit() }
    }
}
