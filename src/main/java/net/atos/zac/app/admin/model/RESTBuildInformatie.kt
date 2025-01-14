/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import net.atos.zac.healthcheck.model.BuildInformatie
import java.time.LocalDateTime

class RESTBuildInformatie(buildInformatie: BuildInformatie) {
    val commit: String?

    val buildId: String?

    val buildDatumTijd: LocalDateTime?

    val versienummer: String?

    init {
        this.commit = buildInformatie.commit
        this.buildId = buildInformatie.buildId
        this.buildDatumTijd = buildInformatie.buildDatumTijd
        this.versienummer = buildInformatie.versienummer
    }
}
