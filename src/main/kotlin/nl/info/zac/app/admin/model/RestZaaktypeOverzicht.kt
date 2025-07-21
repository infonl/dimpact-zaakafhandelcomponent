/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.extensions.isServicenormBeschikbaar
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.ztc.model.generated.ZaakType
import java.time.LocalDate
import java.util.UUID

data class RestZaaktypeOverzicht(
    val uuid: UUID,
    /**
     * Note that the zaaktype identificatie field as per ZGW ZTC API is unique,
     * but (for some reason) unfortunately not required (i.e. nullable).
     */
    val identificatie: String?,
    val doel: String,
    val omschrijving: String,
    val servicenorm: Boolean,
    val versiedatum: LocalDate,
    val beginGeldigheid: LocalDate,
    /**
     * The zaaktype eindeGeldigheid field is nullable, where a null value
     * indicates that the zaaktype is currently valid.
     */
    var eindeGeldigheid: LocalDate?,
    var vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum,
    var nuGeldig: Boolean
)

fun ZaakType.toRestZaaktypeOverzicht() = RestZaaktypeOverzicht(
    uuid = this.getUrl().extractUuid(),
    identificatie = this.getIdentificatie(),
    doel = this.getDoel(),
    omschrijving = this.getOmschrijving(),
    servicenorm = this.isServicenormBeschikbaar(),
    versiedatum = this.getVersiedatum(),
    nuGeldig = this.isNuGeldig(),
    beginGeldigheid = this.getBeginGeldigheid(),
    eindeGeldigheid = this.getEindeGeldigheid(),
    vertrouwelijkheidaanduiding = this.getVertrouwelijkheidaanduiding()
)
