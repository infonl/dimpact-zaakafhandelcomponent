/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.communicatiekanalen.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.net.URI

@NoArgConstructor
@AllOpen
data class RESTCommunicatiekanaal(
    var url: URI,
    var naam: String,
    var omschrijving: String
)
