/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.app.shared.toRestVertrouwelijkheidaanduiding
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

/**
 * The settings of a zaaktype that the Zaak aanmaken form applies once the user chose that zaaktype.
 */
@NoArgConstructor
@AllOpen
data class RestZaaktypeAanmaakInstellingen(
    var uuid: UUID,
    var omschrijving: String,
    var vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding? = null,
    var defaultGroepId: String? = null,
    var defaultBehandelaarId: String? = null,

    @get:JsonbProperty("isKvkKoppelenEnabled")
    @set:JsonbProperty("isKvkKoppelenEnabled")
    var isKvkKoppelenEnabled: Boolean = false,

    @get:JsonbProperty("isBrpKoppelenEnabled")
    @set:JsonbProperty("isBrpKoppelenEnabled")
    var isBrpKoppelenEnabled: Boolean = false
)

fun ZaakType.toRestZaaktypeAanmaakInstellingen(zaaktypeConfiguration: ZaaktypeConfiguration) =
    zaaktypeConfiguration.getBetrokkeneParameters().let { betrokkeneParameters ->
        RestZaaktypeAanmaakInstellingen(
            uuid = url.extractUuid(),
            omschrijving = omschrijving,
            vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding?.toRestVertrouwelijkheidaanduiding(),
            defaultGroepId = zaaktypeConfiguration.groepID,
            defaultBehandelaarId = zaaktypeConfiguration.defaultBehandelaarId,
            isKvkKoppelenEnabled = betrokkeneParameters.isKvkKoppelenEnabled == true,
            isBrpKoppelenEnabled = betrokkeneParameters.isBrpKoppelenEnabled == true
        )
    }
