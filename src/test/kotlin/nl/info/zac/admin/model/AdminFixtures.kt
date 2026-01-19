/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import net.atos.zac.admin.model.HumanTaskReferentieTabel
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplate
import java.time.ZonedDateTime
import java.util.UUID

fun createBetrokkeneKoppelingen(
    id: Long? = 1234L,
    // Do not add default `= createZaakafhandelParameters()` as it will cause infinite loop
    zaaktypeConfiguration: ZaaktypeConfiguration? = null,
    brpKoppelen: Boolean = true,
    kvkKoppelen: Boolean = true
) = ZaaktypeBetrokkeneParameters().apply {
    this.id = id
    this.zaaktypeConfiguration = zaaktypeConfiguration
    this.brpKoppelen = brpKoppelen
    this.kvkKoppelen = kvkKoppelen
}

fun createZaaktypeBrpParameters(
    zoekWaarde: String = "",
    raadpleegWaarde: String = "",
    verwerkingregisterWaarde: String = ""
) = ZaaktypeBrpParameters().apply {
    this.zoekWaarde = zoekWaarde
    this.raadpleegWaarde = raadpleegWaarde
    this.verwerkingregisterWaarde = verwerkingregisterWaarde
}

@Suppress("LongParameterList")
fun createHumanTaskParameters(
    id: Long = 1234L,
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
    isActief: Boolean = true,
    formulierDefinitieID: String? = "fakeFormulierDefinitieID",
    planItemDefinitionID: String = "fakePlanItemDefinitionID",
    groupId: String = "fakeGroupId",
    leadTime: Int? = 1000000000,
    referenceTables: List<HumanTaskReferentieTabel>? = emptyList()
) = ZaaktypeCmmnHumantaskParameters().apply {
    this.id = id
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
    this.actief = isActief
    this.setFormulierDefinitieID(formulierDefinitieID)
    this.planItemDefinitionID = planItemDefinitionID
    this.groepID = groupId
    this.doorlooptijd = leadTime
    this.setReferentieTabellen((referenceTables ?: emptyList()).toMutableList())
}

fun createHumanTaskReferentieTabel(
    id: Long = 1234L,
    referenceTable: ReferenceTable = createReferenceTable(),
    zaaktypeCmmnHumantaskParameters: ZaaktypeCmmnHumantaskParameters = createHumanTaskParameters(),
    field: String = "fakeField",
) = HumanTaskReferentieTabel().apply {
    this.id = id
    this.tabel = referenceTable
    this.humantask = zaaktypeCmmnHumantaskParameters
    this.veld = field
}

fun createReferenceTable(
    id: Long = 1234L,
    code: String = "fakeCode",
    name: String = "fakeReferentieTabel",
    isSystemReferenceTable: Boolean = false,
    values: MutableList<ReferenceTableValue> = mutableListOf(createReferenceTableValue())
) = ReferenceTable().apply {
    this.id = id
    this.code = code
    this.name = name
    this.isSystemReferenceTable = isSystemReferenceTable
    this.values = values
}

fun createReferenceTableValue(
    id: Long = 1234L,
    name: String = "fakeReferentieTabelWaarde",
    sortOrder: Int = 1,
    isSystemValue: Boolean = false
) = ReferenceTableValue().apply {
    this.id = id
    this.name = name
    this.sortOrder = sortOrder
    this.isSystemValue = isSystemValue
}

@Suppress("LongParameterList")
fun createZaaktypeCmmnConfiguration(
    id: Long? = 1234L,
    creationDate: ZonedDateTime = ZonedDateTime.now(),
    domein: String? = "fakeDomein",
    zaaktypeUUID: UUID = UUID.randomUUID(),
    zaaktypeOmschrijving: String = "fakeZaaktypeOmschrijving",
    einddatumGeplandWaarschuwing: Int? = null,
    productaanvraagtype: String? = null,
    nietOntvankelijkResultaattype: UUID = UUID.randomUUID(),
    zaaktypeCmmnCompletionParameters: Set<ZaaktypeCmmnCompletionParameters>? = emptySet(),
    groupId: String? = null,
    caseDefinitionId: String = "fakeCaseDefinitionId",
    gebruikersnaamMedewerker: String? = null,
    zaaktypeBetrokkeneParameters: ZaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(),
    zaaktypeBrpParameters: ZaaktypeBrpParameters? = createZaaktypeBrpParameters(),
    zaaktypeCmmnEmailParameters: ZaaktypeCmmnEmailParameters = createAutomaticEmailConfirmation()
) =
    ZaaktypeCmmnConfiguration().apply {
        this.id = id
        this.creatiedatum = creationDate
        this.domein = domein
        this.zaaktypeUuid = zaaktypeUUID
        this.zaaktypeOmschrijving = zaaktypeOmschrijving
        this.einddatumGeplandWaarschuwing = einddatumGeplandWaarschuwing
        this.productaanvraagtype = productaanvraagtype
        this.nietOntvankelijkResultaattype = nietOntvankelijkResultaattype
        this.groepID = groupId
        this.caseDefinitionID = caseDefinitionId
        this.gebruikersnaamMedewerker = gebruikersnaamMedewerker
        setMailtemplateKoppelingen(
            setOf(
                createMailtemplateKoppelingen(
                    zaaktypeCmmnConfiguration = this,
                    mailTemplate = createMailTemplate()
                )
            )
        )
        setZaakAfzenders(setOf(createZaakAfzender(zaaktypeCmmnConfiguration = this)))
        setZaakbeeindigParameters(zaaktypeCmmnCompletionParameters)
        val parameters = this
        this.zaaktypeBetrokkeneParameters = zaaktypeBetrokkeneParameters.apply {
            this.zaaktypeConfiguration = parameters
        }
        this.zaaktypeBrpParameters = zaaktypeBrpParameters.apply {
            this?.zaaktypeConfiguration = parameters
        }
        this.zaaktypeCmmnEmailParameters = zaaktypeCmmnEmailParameters.apply {
            this.zaaktypeCmmnConfiguration = parameters
        }
    }

fun createMailtemplateKoppelingen(
    id: Long? = 1234L,
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
    mailTemplate: MailTemplate
) = ZaaktypeCmmnMailtemplateParameters().apply {
    this.id = id
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
    this.mailTemplate = mailTemplate
}

@Suppress("LongParameterList")
fun createAutomaticEmailConfirmation(
    id: Long? = 1234L,
    enabled: Boolean = true,
    templateName: String? = "fakeTemplateName",
    emailSender: String? = "sender@example.com",
    emailReply: String? = "reply@example.com",
    // Do not add default `= createZaakafhandelParameters()` as it will cause an infinite loop
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration? = null,
) = ZaaktypeCmmnEmailParameters().apply {
    this.id = id
    this.enabled = enabled
    this.templateName = templateName
    this.emailSender = emailSender
    this.emailReply = emailReply
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
}

fun createMailTemplate(
    mail: Mail = Mail.ZAAK_ALGEMEEN
) = MailTemplate().apply {
    this.id = 1234L
    mailTemplateNaam = "fakeName"
    onderwerp = "fakeOnderwerp"
    body = "fakeBody"
    this.mail = mail
}

fun createZaakAfzender(
    id: Long? = 1234L,
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration,
    defaultMail: Boolean = false,
    mail: String = "mail@example.com",
    replyTo: String = "replyTo@example.com",
) = ZaaktypeCmmnZaakafzenderParameters().apply {
    this.id = id
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
    this.defaultMail = defaultMail
    this.mail = mail
    this.replyTo = replyTo
}
