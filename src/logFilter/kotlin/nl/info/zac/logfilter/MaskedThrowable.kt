/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.logfilter

import java.util.Collections
import java.util.IdentityHashMap

/**
 * A copy of a throwable with a masked message, which keeps the stack trace of the original.
 *
 * The message of the copy is the masked `toString()` of the original, because a text formatter logs the
 * `toString()` of a throwable, which some throwables override to show data that is not in their message.
 * The structured log formatters of WildFly log the class of a throwable as its type, and the default `toString()`
 * starts with the name of the class, so that name stays visible in the JSON logging.
 */
class MaskedThrowable(maskedDescription: String) : Throwable(maskedDescription) {
    override fun toString() = message.orEmpty()

    override fun fillInStackTrace() = this
}

fun Throwable.maskPersonalData(): Throwable =
    if (this is MaskedThrowable || !containsPersonalData()) this else toMaskedThrowable(IdentityHashMap())

private fun Throwable.containsPersonalData(
    checkedThrowables: MutableSet<Throwable> = Collections.newSetFromMap(IdentityHashMap())
): Boolean =
    checkedThrowables.add(this) && (
        listOfNotNull(message, localizedMessage, toString()).any { it.maskPersonalData() != it } ||
            cause?.containsPersonalData(checkedThrowables) == true ||
            suppressed.any { it.containsPersonalData(checkedThrowables) }
        )

private fun Throwable.toMaskedThrowable(maskedThrowables: IdentityHashMap<Throwable, MaskedThrowable>): MaskedThrowable =
    maskedThrowables[this] ?: MaskedThrowable(toString().maskPersonalData()).also { maskedThrowable ->
        maskedThrowables[this] = maskedThrowable
        maskedThrowable.stackTrace = stackTrace
        cause?.let { maskedThrowable.initCause(it.toMaskedThrowable(maskedThrowables)) }
        suppressed.forEach { maskedThrowable.addSuppressed(it.toMaskedThrowable(maskedThrowables)) }
    }
