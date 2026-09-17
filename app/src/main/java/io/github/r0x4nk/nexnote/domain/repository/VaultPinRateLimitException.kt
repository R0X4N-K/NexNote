package io.github.r0x4nk.nexnote.domain.repository

/** A PIN verification was refused before running the password derivation. */
class VaultPinRateLimitException(val retryAfterMillis: Long) :
    IllegalStateException("Too many PIN attempts. Try again later.")
