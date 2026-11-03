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
 * The structured log formatters of WildFly log the class of a throwable as its type, so the name of the original
 * class is part of the message of the copy, where it stays visible in the JSON logging.
 */
class MaskedThrowable(
    originalClassName: String,
    maskedMessage: String?
) : Throwable(maskedMessage?.let { "$originalClassName: $it" } ?: originalClassName) {
    override fun toString() = message.orEmpty()

    override fun fillInStackTrace() = this
}

fun Throwable.maskPersonalData(): Throwable =
    if (this is MaskedThrowable || !containsPersonalData()) this else toMaskedThrowable(IdentityHashMap())

private fun Throwable.containsPersonalData(
    checkedThrowables: MutableSet<Throwable> = Collections.newSetFromMap(IdentityHashMap())
): Boolean =
    checkedThrowables.add(this) && (
        listOfNotNull(message, localizedMessage).any { it.maskPersonalData() != it } ||
            cause?.containsPersonalData(checkedThrowables) == true ||
            suppressed.any { it.containsPersonalData(checkedThrowables) }
        )

private fun Throwable.toMaskedThrowable(maskedThrowables: IdentityHashMap<Throwable, MaskedThrowable>): MaskedThrowable =
    maskedThrowables[this] ?: MaskedThrowable(javaClass.name, localizedMessage?.maskPersonalData()).also { maskedThrowable ->
        maskedThrowables[this] = maskedThrowable
        maskedThrowable.stackTrace = stackTrace
        cause?.let { maskedThrowable.initCause(it.toMaskedThrowable(maskedThrowables)) }
        suppressed.forEach { maskedThrowable.addSuppressed(it.toMaskedThrowable(maskedThrowables)) }
    }
