/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.task.model

import net.atos.zac.app.informatieobjecten.model.RestInformatieobjecttype
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestTaskDocumentData(
    var bestandsnaam: String,
    var documentTitel: String,
    var documentType: RestInformatieobjecttype
)
