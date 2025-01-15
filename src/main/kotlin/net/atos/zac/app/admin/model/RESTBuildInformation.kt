/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import java.time.LocalDateTime

data class RESTBuildInformation(
    val commit: String?,
    val buildId: String?,
    val buildDatumTijd: LocalDateTime?,
    val versienummer: String?
)
