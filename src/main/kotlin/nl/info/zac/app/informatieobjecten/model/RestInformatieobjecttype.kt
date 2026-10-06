/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.app.shared.toRestVertrouwelijkheidaanduiding
import java.util.UUID

data class RestInformatieobjecttype(
    var uuid: UUID,
    var omschrijving: String? = null,
    var vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding? = null,

    @get:JsonbProperty("isConcept")
    @set:JsonbProperty("isConcept")
    var isConcept: Boolean = false
)

fun InformatieObjectType.toRestInformatieobjecttype() = RestInformatieobjecttype(
    uuid = this.url.extractUuid(),
    isConcept = this.concept,
    omschrijving = this.omschrijving,
    vertrouwelijkheidaanduiding = this.vertrouwelijkheidaanduiding?.toRestVertrouwelijkheidaanduiding()
)
