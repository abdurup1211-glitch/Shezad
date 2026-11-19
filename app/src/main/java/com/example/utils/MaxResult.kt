package com.example.utils

/**
 * Standard sealed operation result wrapper used across MAX modules and repositories.
 */
sealed class MaxResult<out T> {
  data object Loading : MaxResult<Nothing>()

  data class Success<out T>(val data: T) : MaxResult<T>()

  data class Error(
    val message: String,
    val cause: Throwable? = null,
  ) : MaxResult<Nothing>()
}
