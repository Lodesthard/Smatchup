package com.example.smatchup.ui.auth

import androidx.annotation.StringRes
import com.example.smatchup.R
import com.example.smatchup.data.repository.AuthError

@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.FIELDS_REQUIRED -> R.string.auth_error_fields_required
    AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthError.PASSWORD_TOO_SHORT -> R.string.auth_error_password_too_short
    AuthError.PASSWORD_MISMATCH -> R.string.auth_error_password_mismatch
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.PSEUDO_TAKEN -> R.string.auth_error_pseudo_taken
    AuthError.EMAIL_TAKEN -> R.string.auth_error_email_taken
    AuthError.ACCOUNT_EXISTS -> R.string.auth_error_account_exists
    AuthError.UNKNOWN -> R.string.auth_error_unknown
}
