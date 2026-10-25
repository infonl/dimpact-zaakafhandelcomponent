/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering

import nl.info.zac.signalering.model.Signalering

internal class CheckedSignalering<T : Any>(
    val signalering: Signalering,
    val result: T? = null,
    val invalidReason: String? = null
)
