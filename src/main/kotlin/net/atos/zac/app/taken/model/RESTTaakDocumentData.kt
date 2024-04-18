/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken.model

import net.atos.zac.app.informatieobjecten.model.RESTInformatieobjecttype
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RESTTaakDocumentData(
    var bestandsnaam: String,
    var documentTitel: String,
    var documentType: RESTInformatieobjecttype
)
