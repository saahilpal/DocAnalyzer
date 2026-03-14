package com.nitrous.docanalyzer.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nitrous.docanalyzer.auth.data.AuthRepository
import com.nitrous.docanalyzer.network.NetworkResult
import com.nitrous.docanalyzer.util.ValidationUtils
import com.nitrous.docanalyzer.ui.theme.Strings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val otp: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val verificationRequired: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null,
    val otpError: String? = null,
    val confirmPasswordError: String? = null,
    val isSubmitted: Boolean = false,
    val resendCountdown: Int = 0,
    val isResending: Boolean = false
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isAuthenticated = repository.isLoggedIn()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, emailError = null, error = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue, passwordError = null, error = null) }
    }

    fun onNameChange(newValue: String) {
        _uiState.update { it.copy(name = newValue, nameError = null, error = null) }
    }

    fun onOtpChange(newValue: String) {
        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
            _uiState.update { it.copy(otp = newValue, otpError = null, error = null) }
        }
    }

    fun onConfirmPasswordChange(newValue: String) {
        _uiState.update { it.copy(confirmPassword = newValue, confirmPasswordError = null, error = null) }
    }

    /**
     * Resets all authentication fields and errors.
     * Used for clearing state when navigating between auth screens.
     */
    fun resetForm() {
        _uiState.update {
            it.copy(
                email = "",
                password = "",
                name = "",
                otp = "",
                confirmPassword = "",
                emailError = null,
                passwordError = null,
                nameError = null,
                otpError = null,
                confirmPasswordError = null,
                isSubmitted = false,
                error = null,
                successMessage = null
            )
        }
    }

    private fun validateInputs(isRegister: Boolean): Boolean {
        val email = _uiState.value.email
        val password = _uiState.value.password
        val name = _uiState.value.name

        val emailError = if (!ValidationUtils.isValidEmail(email)) "Invalid email format" else null
        val passwordError = if (password.length < 8) "Password must be at least 8 characters" else null

        return if (isRegister) {
            val nameError = if (name.isBlank()) "Please enter your name" else null
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    nameError = nameError
                )
            }
            emailError == null && passwordError == null && nameError == null
        } else {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    nameError = null
                )
            }
            emailError == null && passwordError == null
        }
    }

    fun login() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isSubmitted = true) }
        if (!validateInputs(false)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.login(_uiState.value.email.trim(), _uiState.value.password)
            handleAuthResult(result)
        }
    }

    fun register(onSuccess: () -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isSubmitted = true) }
        if (!validateInputs(true)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.register(
                _uiState.value.name.trim(),
                _uiState.value.email.trim(),
                _uiState.value.password
            )
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Verification code sent to your email") }
                    onSuccess()
                }
                is NetworkResult.ApiError -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false, error = "Registration failed. Please try again.") }
                }
            }
        }
    }

    fun verifyOtp(onSuccess: () -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isSubmitted = true) }
        if (_uiState.value.otp.length != 6) {
            _uiState.update { it.copy(otpError = "Verification code must be 6 digits") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.verifyOtp(_uiState.value.email.trim(), _uiState.value.otp)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Account verified successfully", verificationRequired = false) }
                    // Automatically re-execute login upon successful OTP verification
                    login()
                    onSuccess()
                }
                is NetworkResult.ApiError -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false, error = "Verification failed. Please check the code.") }
                }
            }
        }
    }

    fun resendOtp() {
        if (_uiState.value.resendCountdown > 0 || _uiState.value.isResending) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, error = null) }
            val result = repository.sendOtp(_uiState.value.email.trim())
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isResending = false, successMessage = "New verification code sent to your email") }
                    startResendCountdown()
                }
                is NetworkResult.ApiError -> {
                    _uiState.update { it.copy(isResending = false, error = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isResending = false, error = "Failed to resend code") }
                }
            }
        }
    }

    fun resendResetOtp() {
        if (_uiState.value.resendCountdown > 0 || _uiState.value.isResending) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, error = null) }
            val result = repository.requestReset(_uiState.value.email.trim())
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isResending = false, successMessage = "New verification code sent to your email") }
                    startResendCountdown()
                }
                is NetworkResult.ApiError -> {
                    _uiState.update { it.copy(isResending = false, error = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isResending = false, error = "Failed to resend code") }
                }
            }
        }
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(resendCountdown = 30) }
        countdownJob = viewModelScope.launch {
            while (_uiState.value.resendCountdown > 0) {
                delay(1000)
                _uiState.update { it.copy(resendCountdown = it.resendCountdown - 1) }
            }
        }
    }

    private fun handleAuthResult(result: NetworkResult<*>) {
        when (result) {
            is NetworkResult.Success -> {
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true, verificationRequired = false) }
            }
            is NetworkResult.ApiError -> {
                if (result.code == "INACTIVE_ACCOUNT") {
                    // Automatically trigger send-otp and shift to verification mode
                    viewModelScope.launch {
                        repository.sendOtp(_uiState.value.email.trim())
                        _uiState.update { it.copy(isLoading = false, verificationRequired = true, error = null) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            is NetworkResult.Exception -> {
                _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
            is NetworkResult.RateLimit -> {
                _uiState.update { it.copy(isLoading = false, error = "Too many attempts. Please try again later.") }
            }
            else -> {
                _uiState.update { it.copy(isLoading = false, error = "Authentication failed") }
            }
        }
    }

    fun requestReset(onSuccess: (String?) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.requestReset(_uiState.value.email.trim())) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess("Code sent to your email")
                }
                is NetworkResult.ApiError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false, error = "Request failed. Please try again.") }
            }
        }
    }

    fun resetPassword(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isSubmitted = true) }
        
        val otpError = if (_uiState.value.otp.length != 6) "OTP must be 6 digits" else null
        val passwordError = if (_uiState.value.password.length < 8) "Password must be at least 8 characters" else null
        
        if (otpError != null || passwordError != null) {
            _uiState.update { it.copy(otpError = otpError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.resetPassword(
                _uiState.value.email.trim(),
                _uiState.value.otp,
                _uiState.value.password
            )
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Password updated successfully") }
                    onSuccess()
                }
                is NetworkResult.ApiError -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false, error = "Failed to update password") }
            }
        }
    }

    fun logout() {
        _uiState.update { it.copy(isAuthenticated = false, isLoading = false) }
        repository.logout()
    }
    
    fun clearStatus() {
        _uiState.update { it.copy(error = null, successMessage = null, isSubmitted = false) }
    }

    fun clearInputs(keepEmail: Boolean = false) {
        _uiState.update { currentState ->
            currentState.copy(
                email = if (keepEmail) currentState.email else "",
                password = "",
                name = "",
                otp = "",
                confirmPassword = "",
                emailError = if (keepEmail) currentState.emailError else null,
                passwordError = null,
                nameError = null,
                otpError = null,
                confirmPasswordError = null,
                isSubmitted = false,
                error = null,
                successMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
