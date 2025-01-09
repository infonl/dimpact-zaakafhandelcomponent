/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@NoArgConstructor
@AllOpen
data class RESTZaakOpschorting(
    var vanafDatumTijd: ZonedDateTime? = null,

    var duurDagen: Int = 0
)
