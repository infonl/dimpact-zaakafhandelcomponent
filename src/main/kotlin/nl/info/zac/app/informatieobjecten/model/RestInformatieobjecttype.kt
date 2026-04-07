/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import java.util.UUID

data class RestInformatieobjecttype(
    var uuid: UUID,
    var omschrijving: String? = null,
    var vertrouwelijkheidaanduiding: String? = null,
    var concept: Boolean = false
)

fun InformatieObjectType.toRestInformatieobjecttype() = RestInformatieobjecttype(
    uuid = this.url.extractUuid(),
    concept = this.concept,
    omschrijving = this.omschrijving,
    // we use the uppercase version of this enum in the ZAC backend API
    vertrouwelijkheidaanduiding = this.vertrouwelijkheidaanduiding.name
)
