/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

class CheckedSignaleringen<T : Any>(
    val valid: List<T>,
    val invalid: List<InvalidSignalering>
)
