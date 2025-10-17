/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.healthcheck.model

import java.time.ZonedDateTime

data class BuildInformation(
    val commit: String?,
    val buildId: String?,
    val buildDateTime: ZonedDateTime?,
    val versionNumber: String?
)
