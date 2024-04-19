/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RESTTaakVerdelenTaak(
    var taakId: String,

    var zaakUuid: UUID
)
