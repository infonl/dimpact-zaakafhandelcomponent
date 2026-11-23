/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.log

import nl.info.zac.log.LogEvent

class BrpProtocolleringHeaderNotConfigured(
    val header: String
) : LogEvent("BRP protocollering is enabled but a header is not configured; it is not included in the BRP protocollering headers")
