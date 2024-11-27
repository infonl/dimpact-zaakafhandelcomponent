/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin

import net.atos.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.zac.app.admin.model.RESTZaaktypeOverzicht
import net.atos.zac.app.admin.model.RestDocumentCreationParameters
import net.atos.zac.app.admin.model.RestReferenceTable
import net.atos.zac.app.admin.model.RestReferenceTableUpdate
import net.atos.zac.app.admin.model.RestReferenceTableValue
import net.atos.zac.app.admin.model.RestZaakafhandelParameters
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createRestReferenceTable(
    id: Long = 1L,
    code: String = "dummyCode",
    naam: String = "dummyName",
    systeem: Boolean = false,
    aantalWaarden: Int = 2,
    waarden: List<RestReferenceTableValue> = listOf(
        createRestReferenceTableValue(),
        createRestReferenceTableValue()
    )
) = RestReferenceTable(
    id = id,
    code = code,
    naam = naam,
    systeem = systeem,
    aantalWaarden = aantalWaarden,
    waarden = waarden
)

fun createRestReferenceTableUpdate(
    code: String? = null,
    naam: String = "dummyUpdatedName",
    waarden: List<RestReferenceTableValue> = listOf(
        createRestReferenceTableValue(),
        createRestReferenceTableValue()
    )
) = RestReferenceTableUpdate(
    naam = naam,
    code = code,
    waarden = waarden
)

fun createRestReferenceTableValue(
    id: Long = 1234L,
    name: String = "dummyWaarde1",
    isSystemValue: Boolean = false
) = RestReferenceTableValue(
    id = id,
    naam = name,
    isSystemValue = isSystemValue
)

fun createRestZaakAfhandelParameters(
    id: Long? = 1234L,
    domein: String = "dummyDomein",
    restZaaktypeOverzicht: RESTZaaktypeOverzicht = createRESTZaaktypeOverzicht(),
    productaanvraagtype: String? = null
) = RestZaakafhandelParameters(
    id = id,
    domein = domein,
    zaaktype = restZaaktypeOverzicht,
    productaanvraagtype = productaanvraagtype,
    documentCreation = RestDocumentCreationParameters()
)

@Suppress("LongParameterList")
fun createRESTZaaktypeOverzicht(
    uuid: UUID = UUID.randomUUID(),
    identificatie: String = "dummyIdentificatie",
    doel: String = "dummyDoel",
    omschrijving: String = "dummyOmschrijving",
    servicenorm: Boolean = false,
    versiedatum: LocalDate = LocalDate.now(),
    beginGeldigheid: LocalDate = LocalDate.now(),
    eindeGeldigheid: LocalDate = LocalDate.now().plusDays(1),
    vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum = VertrouwelijkheidaanduidingEnum.OPENBAAR,
    nuGeldig: Boolean = true
) = RESTZaaktypeOverzicht().apply {
    this.uuid = uuid
    this.identificatie = identificatie
    this.doel = doel
    this.omschrijving = omschrijving
    this.servicenorm = servicenorm
    this.versiedatum = versiedatum
    this.beginGeldigheid = beginGeldigheid
    this.eindeGeldigheid = eindeGeldigheid
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.nuGeldig = nuGeldig
}
