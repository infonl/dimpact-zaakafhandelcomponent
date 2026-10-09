/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

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
    this.isBrpKoppelenEnabled = brpKoppelen
    this.isKvkKoppelenEnabled = kvkKoppelen
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

fun createZaaktypeCmmnHumantaskParameters(
    planItemDefinitionId: String = "AANVULLENDE_INFORMATIE",
    id: Long = 1L,
    actief: Boolean = true,
    groepId: String = "fakeGroepId",
    doorlooptijd: Int = 5
) = ZaaktypeCmmnHumantaskParameters().apply {
    this.planItemDefinitionID = planItemDefinitionId
    this.id = id
    this.isActief = actief
    this.groepID = groepId
    this.doorlooptijd = doorlooptijd
    this.setReferentieTabellen(mutableListOf())
}

@Suppress("LongParameterList")
fun createHumanTaskParameters(
    id: Long = 1234L,
    zaaktypeCmmnExtension: ZaaktypeCmmnExtension = createZaaktypeCmmnConfiguration().getOrCreateCmmnExtension(),
    isActief: Boolean = true,
    formulierDefinitieID: String? = "fakeFormulierDefinitieID",
    planItemDefinitionID: String = "fakePlanItemDefinitionID",
    groupId: String = "fakeGroupId",
    leadTime: Int? = 1000000000,
    referenceTables: List<HumanTaskReferentieTabel>? = emptyList()
) = ZaaktypeCmmnHumantaskParameters().apply {
    this.id = id
    this.zaaktypeCmmnExtension = zaaktypeCmmnExtension
    this.isActief = isActief
    this.setFormulierDefinitieID(formulierDefinitieID)
    this.planItemDefinitionID = planItemDefinitionID
    this.groepID = groupId
    this.doorlooptijd = leadTime
    this.setReferentieTabellen(referenceTables.orEmpty().toMutableList())
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
    id: Long? = 1234L,
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
    id: Long? = 1234L,
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
    zaaktypeUUID: UUID = UUID.randomUUID(),
    zaaktypeOmschrijving: String = "fakeZaaktypeOmschrijving",
    einddatumGeplandWaarschuwing: Int? = null,
    productaanvraagtype: String? = null,
    nietOntvankelijkResultaattypeOmschrijving: String? = "fakeNietOntvankelijkResultaattype",
    zaaktypeCompletionParameters: Set<ZaaktypeCompletionParameters> = emptySet(),
    groupId: String? = null,
    caseDefinitionId: String = "fakeCaseDefinitionId",
    defaultBehandelaarId: String? = null,
    smartDocumentsEnabled: Boolean = false,
    zaaktypeBetrokkeneParameters: ZaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(),
    zaaktypeBrpParameters: ZaaktypeBrpParameters? = createZaaktypeBrpParameters(),
    zaaktypeEmailParameters: ZaaktypeEmailParameters = createAutomaticEmailConfirmation()
) =
    ZaaktypeConfiguration().apply {
        this.id = id
        this.creatiedatum = creationDate
        this.zaaktypeUuid = zaaktypeUUID
        this.zaaktypeOmschrijving = zaaktypeOmschrijving
        this.einddatumGeplandWaarschuwing = einddatumGeplandWaarschuwing
        this.productaanvraagtype = productaanvraagtype
        this.nietOntvankelijkResultaattypeOmschrijving = nietOntvankelijkResultaattypeOmschrijving
        this.groepID = groupId
        bindTo(ProcessEngine.CMMN, caseDefinitionId)
        getOrCreateCmmnExtension()
        this.defaultBehandelaarId = defaultBehandelaarId
        this.isSmartDocumentsEnabled = smartDocumentsEnabled
        setMailtemplateKoppelingen(
            setOf(
                createMailtemplateKoppelingen(
                    zaaktypeConfiguration = this,
                    mailTemplate = createMailTemplate()
                )
            )
        )
        setZaakAfzenders(setOf(createZaakAfzender(zaaktypeConfiguration = this)))
        setZaakbeeindigParameters(zaaktypeCompletionParameters)
        val parameters = this
        this.zaaktypeBetrokkeneParameters = zaaktypeBetrokkeneParameters.apply {
            this.zaaktypeConfiguration = parameters
        }
        this.zaaktypeBrpParameters = zaaktypeBrpParameters.apply {
            this?.zaaktypeConfiguration = parameters
        }
        this.zaaktypeEmailParameters = zaaktypeEmailParameters.apply {
            this.zaaktypeConfiguration = parameters
        }
    }

@Suppress("LongParameterList")
fun createZaaktypeBpmnConfiguration(
    id: Long? = 1234L,
    creationDate: ZonedDateTime = ZonedDateTime.now(),
    zaaktypeUUID: UUID = UUID.randomUUID(),
    zaaktypeOmschrijving: String = "fakeZaaktypeOmschrijving",
    productaanvraagtype: String? = null,
    nietOntvankelijkResultaattypeOmschrijving: String? = "fakeNietOntvankelijkResultaattype",
    zaaktypeCompletionParameters: Set<ZaaktypeCompletionParameters> = emptySet(),
    groupId: String? = null,
    defaultBehandelaarId: String? = null,
    smartDocumentsEnabled: Boolean = false,
    zaaktypeBetrokkeneParameters: ZaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(),
    zaaktypeBrpParameters: ZaaktypeBrpParameters? = createZaaktypeBrpParameters(),
    bpmnProcessDefinitionKey: String = "fakeBpmnProcessDefinitionKey",
) =
    ZaaktypeConfiguration().apply {
        this.id = id
        this.creatiedatum = creationDate
        this.zaaktypeUuid = zaaktypeUUID
        this.zaaktypeOmschrijving = zaaktypeOmschrijving
        this.productaanvraagtype = productaanvraagtype
        this.nietOntvankelijkResultaattypeOmschrijving = nietOntvankelijkResultaattypeOmschrijving
        this.groepID = groupId
        this.defaultBehandelaarId = defaultBehandelaarId
        this.isSmartDocumentsEnabled = smartDocumentsEnabled
        setZaakbeeindigParameters(zaaktypeCompletionParameters)
        val parameters = this
        this.zaaktypeBetrokkeneParameters = zaaktypeBetrokkeneParameters.apply {
            this.zaaktypeConfiguration = parameters
        }
        this.zaaktypeBrpParameters = zaaktypeBrpParameters.apply {
            this?.zaaktypeConfiguration = parameters
        }
        bindTo(ProcessEngine.BPMN, bpmnProcessDefinitionKey)
    }

data class ZaaktypeConfigurationUnderTest(
    val configurationType: ProcessEngine,
    val create: (nietOntvankelijkResultaattypeOmschrijving: String?) -> ZaaktypeConfiguration
)

/**
 * One factory per configuration type, so that a test of behaviour that both engines share runs for each of them.
 */
fun createZaaktypeConfigurationsUnderTest() = listOf(
    ZaaktypeConfigurationUnderTest(ProcessEngine.CMMN) {
        createZaaktypeCmmnConfiguration(nietOntvankelijkResultaattypeOmschrijving = it)
    },
    ZaaktypeConfigurationUnderTest(ProcessEngine.BPMN) {
        createZaaktypeBpmnConfiguration(nietOntvankelijkResultaattypeOmschrijving = it)
    }
)

fun createZaaktypeCompletionParameters(
    id: Long? = 1234L,
    zaakbeeindigReden: ZaakbeeindigReden = createZaakbeeindigReden(),
    resultaattypeOmschrijving: String = "fakeResultaattypeOmschrijving"
) = ZaaktypeCompletionParameters().apply {
    this.id = id
    this.zaakbeeindigReden = zaakbeeindigReden
    this.resultaattypeOmschrijving = resultaattypeOmschrijving
}

fun createMailtemplateKoppelingen(
    id: Long? = 1234L,
    zaaktypeConfiguration: ZaaktypeConfiguration,
    mailTemplate: MailTemplate
) = ZaaktypeMailtemplateParameters().apply {
    this.id = id
    this.zaaktypeConfiguration = zaaktypeConfiguration
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
    zaaktypeConfiguration: ZaaktypeConfiguration? = null,
) = ZaaktypeEmailParameters().apply {
    this.id = id
    this.isEnabled = enabled
    this.templateName = templateName
    this.emailSender = emailSender
    this.emailReply = emailReply
    zaaktypeConfiguration?.let { this.zaaktypeConfiguration = it }
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
    zaaktypeConfiguration: ZaaktypeConfiguration,
    defaultMail: Boolean = false,
    mail: String = "mail@example.com",
    replyTo: String = "replyTo@example.com",
) = ZaaktypeZaakafzenderParameters().apply {
    this.id = id
    this.zaaktypeConfiguration = zaaktypeConfiguration
    this.isDefaultMail = defaultMail
    this.mail = mail
    this.replyTo = replyTo
}

fun createZaakbeeindigReden(
    id: Long? = 1234L,
    name: String = "fakeName"
) = ZaakbeeindigReden().apply {
    this.id = id
    this.naam = name
}
