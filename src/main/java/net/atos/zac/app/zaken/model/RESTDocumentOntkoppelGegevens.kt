/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.*

@NoArgConstructor
@AllOpen
data class RESTDocumentOntkoppelGegevens(
    var zaakUUID: UUID? = null,

    var documentUUID: UUID? = null,

    var reden: String? = null
)
