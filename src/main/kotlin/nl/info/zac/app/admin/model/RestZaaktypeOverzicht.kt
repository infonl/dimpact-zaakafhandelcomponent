/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.extensions.isServicenormAvailable
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

/**
 * Currently this class it used both for creating and updating as well as for reading
 * zaaktype-related data.
 * For this reason, all fields are currently nullable.
 * In future, we should consider splitting this class into separate classes, depending
 * on the CRUD operation.
 */
@NoArgConstructor
@AllOpen
data class RestZaaktypeOverzicht(
    var uuid: UUID?,
    var identificatie: String?,
    var doel: String?,
    var omschrijving: String?,
    var servicenorm: Boolean = false,
    var versiedatum: LocalDate?,
    var beginGeldigheid: LocalDate?,
    var eindeGeldigheid: LocalDate?,
    var vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum?,
    var nuGeldig: Boolean = false
)

fun ZaakType.toRestZaaktypeOverzicht() = RestZaaktypeOverzicht(
    uuid = this.getUrl().extractUuid(),
    identificatie = this.getIdentificatie(),
    doel = this.getDoel(),
    omschrijving = this.getOmschrijving(),
    servicenorm = this.isServicenormAvailable(),
    versiedatum = this.getVersiedatum(),
    nuGeldig = this.isNuGeldig(),
    beginGeldigheid = this.getBeginGeldigheid(),
    eindeGeldigheid = this.getEindeGeldigheid(),
    vertrouwelijkheidaanduiding = this.getVertrouwelijkheidaanduiding()
)
