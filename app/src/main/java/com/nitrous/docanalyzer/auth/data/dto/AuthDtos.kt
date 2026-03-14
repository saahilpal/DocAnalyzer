package com.nitrous.docanalyzer.auth.data.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("name") val name: String
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("type") val type: String // "registration" or "reset"
)

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresAt") val expiresAt: String,
    @SerializedName("user") val user: UserDto
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("created_at") val createdAtAlternative: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("updated_at") val updatedAtAlternative: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("is_active") val isActiveAlternative: Boolean? = null
) {
    val normalizedCreatedAt: String? get() = createdAt ?: createdAtAlternative
    val normalizedUpdatedAt: String? get() = updatedAt ?: updatedAtAlternative
    val normalizedIsActive: Boolean get() = isActive ?: isActiveAlternative ?: false
}

data class RegisterResponse(
    @SerializedName("message") val message: String
)

data class RequestResetRequest(
    @SerializedName("email") val email: String
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("newPassword") val newPassword: String
)
