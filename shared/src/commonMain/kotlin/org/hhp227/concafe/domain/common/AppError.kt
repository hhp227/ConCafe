package org.hhp227.concafe.domain.common

sealed interface AppError {
    data object Unauthorized : AppError

    data object PermissionDenied : AppError

    data object NotFound : AppError

    data class ValidationFailed(val reason: String) : AppError

    data class NetworkError(val message: String? = null) : AppError

    data class Unknown(val cause: String? = null) : AppError
}
