/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.app.planitems.model.PlanItemType
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.app.zaak.model.RestResultaattype
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplateVariables
import java.time.LocalDate
import java.util.UUID

fun createRestFormioFormulierContent(
    filename: String = "testForm.json",
    content: String = """{ "fakeKey": "fakeValue" }"""
) = BpmnProcessDefinitionTaskFormContent(
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
    name = naam,
    isSystemTable = systeem,
    valuesCount = aantalWaarden,
    values = waarden
)

fun createRestReferenceTableUpdate(
    code: String? = null,
    naam: String = "fakeUpdatedName",
    waarden: List<RestReferenceTableValue> = listOf(
        createRestReferenceTableValue(),
        createRestReferenceTableValue()
    )
) = RestReferenceTableUpdate(
    name = naam,
    code = code,
    values = waarden
)

fun createRestReferenceTableValue(
    id: Long = 1234L,
    name: String = "fakeWaarde1",
    isSystemValue: Boolean = false
) = RestReferenceTableValue(
    id = id,
    name = name,
    isSystemValue = isSystemValue
)

@Suppress("LongParameterList")
fun createRestZaaktypeConfiguration(
    id: Long? = 1234L,
    restZaaktypeOverzicht: RestZaaktypeOverzicht = createRestZaaktypeOverzicht(),
    productaanvraagtype: String? = null,
    defaultGroupId: String? = "fakeGroupId",
    defaultBehandelaarId: String? = null,
    restBetrokkeneKoppelingen: RestBetrokkeneKoppelingen = RestBetrokkeneKoppelingen(),
    restBrpDoelbindingen: RestBrpDoelbindingen = RestBrpDoelbindingen(),
    restProcessDefinition: RestProcessDefinition? = createRestProcessDefinition()
) = RestZaaktypeConfiguration(
    id = id,
    zaaktype = restZaaktypeOverzicht,
    processDefinition = restProcessDefinition,
    productaanvraagtype = productaanvraagtype,
    smartDocuments = RestSmartDocuments(
        isEnabledGlobally = true,
        isEnabledForZaaktype = false
    ),
    defaultBehandelaarId = defaultBehandelaarId,
    defaultGroepId = defaultGroupId,
    betrokkeneKoppelingen = restBetrokkeneKoppelingen,
    brpDoelbindingen = restBrpDoelbindingen
)

fun createRestProcessDefinition(
    processEngine: ProcessEngine = ProcessEngine.CMMN,
    key: String = "fakeProcessDefinitionKey",
    naam: String? = "fakeProcessDefinitionName"
) = RestProcessDefinition(processEngine = processEngine, key = key, naam = naam)

fun createRestBetrokkeneKoppelingen(
    brpKoppelen: Boolean = false,
    kvkKoppelen: Boolean = false
) = RestBetrokkeneKoppelingen(isBrpKoppelenEnabled = brpKoppelen, isKvkKoppelenEnabled = kvkKoppelen)

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
    vertrouwelijkheidaanduiding: RestVertrouwelijkheidaanduiding = RestVertrouwelijkheidaanduiding.OPENBAAR,
    nuGeldig: Boolean = true
) = RestZaaktypeOverzicht(
    uuid = uuid,
    identificatie = identificatie,
    doel = doel,
    omschrijving = omschrijving,
    hasServicenorm = servicenorm,
    versiedatum = versiedatum,
    beginGeldigheid = beginGeldigheid,
    eindeGeldigheid = eindeGeldigheid,
    vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding,
    isNuGeldig = nuGeldig
)

fun createRestZaakbeeindigReden(
    id: String = "fakeZaakbeeindigRedenId",
    name: String = "fakeZaakbeeindigRedenName"
) = RestZaakbeeindigReden(
    id = id,
    naam = name
)

@Suppress("LongParameterList")
fun createRestResultaattype(
    id: UUID = UUID.randomUUID(),
    name: String = "fakeRestResultaattype",
    genericName: String = "fakeGenericName",
    vervaldatumBesluitVerplicht: Boolean = false,
    datumKenmerkVerplicht: Boolean = false,
    besluitVerplicht: Boolean = false
) = RestResultaattype(
    id = id,
    naam = name,
    naamGeneriek = genericName,
    isVervaldatumBesluitVerplicht = vervaldatumBesluitVerplicht,
    isDatumKenmerkVerplicht = datumKenmerkVerplicht,
    isBesluitVerplicht = besluitVerplicht
)

fun createRestZaakbeeindigParameter(
    id: Long = 1L,
    zaakbeeindigReden: RestZaakbeeindigReden = createRestZaakbeeindigReden(),
    resultaattype: RestResultaattype = createRestResultaattype()
) = RestZaakbeeindigParameter(
    id = id,
    zaakbeeindigReden = zaakbeeindigReden,
    resultaattype = resultaattype
)

fun createRestPlanItemDefinition(
    id: String = "fakeId",
    naam: String = "fakePlanItemNaam",
    type: PlanItemType = PlanItemType.HUMAN_TASK
) = RestPlanItemDefinition(
    id = id,
    naam = naam,
    type = type
)

@Suppress("LongParameterList")
fun createRestMailTemplate(
    id: Long = 1234L,
    mailTemplateName: String = "fakeTemplateName",
    subject: String = "fakeSubject",
    body: String = "fakeBody",
    mail: Mail = Mail.ZAAK_ALGEMEEN,
    mailTemplateVariables: Set<MailTemplateVariables> = emptySet(),
    defaultTemplate: Boolean = false
) = RestMailtemplate(
    id = id,
    mailTemplateNaam = mailTemplateName,
    onderwerp = subject,
    body = body,
    mail = mail,
    variabelen = mailTemplateVariables,
    isDefaultMailtemplate = defaultTemplate
)
