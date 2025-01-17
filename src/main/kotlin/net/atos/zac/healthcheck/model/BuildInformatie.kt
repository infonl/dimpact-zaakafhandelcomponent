/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.healthcheck.model

import java.time.ZonedDateTime

data class BuildInformatie(
    val commit: String?,
    val buildId: String?,
    val buildDatumTijd: ZonedDateTime?,
    val versienummer: String?
)
