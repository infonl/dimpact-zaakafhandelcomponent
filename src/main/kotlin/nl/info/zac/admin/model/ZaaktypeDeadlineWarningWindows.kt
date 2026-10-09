/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import java.util.UUID

data class ZaaktypeDeadlineWarningWindows(
    val zaaktypeUuid: UUID,
    val einddatumGeplandWaarschuwing: Int?,
    val uiterlijkeEinddatumAfdoeningWaarschuwing: Int?
)
