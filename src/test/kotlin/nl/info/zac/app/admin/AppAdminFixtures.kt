/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import net.atos.zac.app.admin.model.RESTZaakbeeindigParameter
import net.atos.zac.app.admin.model.RESTZaakbeeindigReden
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.app.admin.model.RestBetrokkeneKoppelingen
import nl.info.zac.app.admin.model.RestBrpDoelbindingen
import nl.info.zac.app.admin.model.RestFormioFormulierContent
import nl.info.zac.app.admin.model.RestReferenceTable
import nl.info.zac.app.admin.model.RestReferenceTableUpdate
import nl.info.zac.app.admin.model.RestReferenceTableValue
import nl.info.zac.app.admin.model.RestSmartDocuments
import nl.info.zac.app.admin.model.RestZaakafhandelParameters
import nl.info.zac.app.admin.model.RestZaaktypeOverzicht
import nl.info.zac.app.zaak.model.RestResultaattype
import java.time.LocalDate
import java.util.UUID

fun createRestFormioFormulierContent(
    filename: String = "testForm.json",
    content: String = """{ "fakeKey": "fakeValue" }"""
) = RestFormioFormulierContent(
    filename = filename,
    content = content
)

@Suppress("LongParameterList")
fun createRestReferenceTable(
    id: Long = 1L,
    code: String = "fakeCode",
    naam: String = "fakeName",
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
    naam: String = "fakeUpdatedName",
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
    name: String = "fakeWaarde1",
    isSystemValue: Boolean = false
) = RestReferenceTableValue(
    id = id,
    naam = name,
    isSystemValue = isSystemValue
)

@Suppress("LongParameterList")
fun createRestZaakAfhandelParameters(
    id: Long? = 1234L,
    domein: String = "fakeDomein",
    restZaaktypeOverzicht: RestZaaktypeOverzicht = createRestZaaktypeOverzicht(),
    productaanvraagtype: String? = null,
    defaultGroupId: String? = null,
    defaultBehandelaarId: String? = null,
    restBetrokkeneKoppelingen: RestBetrokkeneKoppelingen = RestBetrokkeneKoppelingen(),
    restBrpDoelbindingen: RestBrpDoelbindingen = RestBrpDoelbindingen()
) = RestZaakafhandelParameters(
    id = id,
    domein = domein,
    zaaktype = restZaaktypeOverzicht,
    productaanvraagtype = productaanvraagtype,
    smartDocuments = RestSmartDocuments(
        enabledGlobally = true,
        enabledForZaaktype = false
    ),
    defaultBehandelaarId = defaultBehandelaarId,
    defaultGroepId = defaultGroupId,
    betrokkeneKoppelingen = restBetrokkeneKoppelingen,
    brpDoelbindingen = restBrpDoelbindingen
)

fun createRestBetrokkeneKoppelingen(
    brpKoppelen: Boolean = false,
    kvkKoppelen: Boolean = false
) = RestBetrokkeneKoppelingen(brpKoppelen = brpKoppelen, kvkKoppelen = kvkKoppelen)

@Suppress("LongParameterList")
fun createRestZaaktypeOverzicht(
    uuid: UUID = UUID.randomUUID(),
    identificatie: String = "fakeIdentificatie",
    doel: String = "fakeDoel",
    omschrijving: String = "fakeOmschrijving",
    servicenorm: Boolean = false,
    versiedatum: LocalDate = LocalDate.now(),
    beginGeldigheid: LocalDate = LocalDate.now(),
    eindeGeldigheid: LocalDate = LocalDate.now().plusDays(1),
    vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum = VertrouwelijkheidaanduidingEnum.OPENBAAR,
    nuGeldig: Boolean = true
) = RestZaaktypeOverzicht(
    uuid = uuid,
    identificatie = identificatie,
    doel = doel,
    omschrijving = omschrijving,
    servicenorm = servicenorm,
    versiedatum = versiedatum,
    beginGeldigheid = beginGeldigheid,
    eindeGeldigheid = eindeGeldigheid,
    vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding,
    nuGeldig = nuGeldig
)

fun createRestZaakbeeindigReden(
    id: String = "fakeZaakbeeindigRedenId",
    name: String = "fakeZaakbeeindigRedenName"
) = RESTZaakbeeindigReden().apply {
    this.id = id
    this.naam = name
}

fun createRestZaakbeeindigParameter(
    id: Long = 1L,
    zaakbeeindigReden: RESTZaakbeeindigReden = createRestZaakbeeindigReden(),
    resultaattype: RestResultaattype
) = RESTZaakbeeindigParameter().apply {
    this.id = id
    this.zaakbeeindigReden = zaakbeeindigReden
    this.resultaattype = resultaattype
}
