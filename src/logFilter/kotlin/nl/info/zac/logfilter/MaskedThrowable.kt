/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.logfilter

import java.util.IdentityHashMap

/**
 * A copy of a throwable with a masked message. It keeps the class name and the stack trace of the original, so that
 * a logged stack trace only differs from the original in the masked personal data.
 */
class MaskedThrowable(
    private val originalClassName: String,
    maskedMessage: String?
) : Throwable(maskedMessage) {
    override fun toString() = message?.let { "$originalClassName: $it" } ?: originalClassName

    override fun fillInStackTrace() = this
}

fun Throwable.toMaskedThrowable(): MaskedThrowable = toMaskedThrowable(IdentityHashMap())

private fun Throwable.toMaskedThrowable(maskedThrowables: IdentityHashMap<Throwable, MaskedThrowable>): MaskedThrowable =
    maskedThrowables[this] ?: MaskedThrowable(javaClass.name, message?.maskPersonalData()).also { maskedThrowable ->
        maskedThrowables[this] = maskedThrowable
        maskedThrowable.stackTrace = stackTrace
        cause?.let { maskedThrowable.initCause(it.toMaskedThrowable(maskedThrowables)) }
        suppressed.forEach { maskedThrowable.addSuppressed(it.toMaskedThrowable(maskedThrowables)) }
    }
