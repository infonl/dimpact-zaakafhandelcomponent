/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import net.atos.zac.signalering.model.Signalering

class InvalidSignalering(
    val signalering: Signalering,
    val reason: String
)
