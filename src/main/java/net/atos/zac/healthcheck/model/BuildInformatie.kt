/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.healthcheck.model

import java.time.LocalDateTime

class BuildInformatie(
    @JvmField val commit: String?,
    @JvmField val buildId: String?,
    @JvmField val buildDatumTijd: LocalDateTime?,
    @JvmField val versienummer: String?
)
