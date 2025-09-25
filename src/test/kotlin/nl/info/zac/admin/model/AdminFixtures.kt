/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.admin.model

import net.atos.zac.admin.model.HumanTaskReferentieTabel
import net.atos.zac.admin.model.ZaaktypeCmmnBetrokkeneParameters
import net.atos.zac.admin.model.ZaaktypeCmmnBrpParameters
import net.atos.zac.admin.model.ZaaktypeCmmnCompletionParameters
import net.atos.zac.admin.model.ZaaktypeCmmnConfiguration
import net.atos.zac.admin.model.ZaaktypeCmmnEmailParameters
import net.atos.zac.admin.model.ZaaktypeCmmnHumantaskParameters
import net.atos.zac.admin.model.ZaaktypeCmmnMailtemplateParameters
import net.atos.zac.admin.model.ZaaktypeCmmnZaakafzenderParameters
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplate
import java.time.ZonedDateTime
import java.util.UUID

fun createBetrokkeneKoppelingen(
    id: Long? = 1234L,
    // Do not add default `= createZaakafhandelParameters()` as it will cause infinite loop
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration? = null,
    brpKoppelen: Boolean = true,
    kvkKoppelen: Boolean = true
) = ZaaktypeCmmnBetrokkeneParameters().apply {
    this.id = id
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
    this.brpKoppelen = brpKoppelen
    this.kvkKoppelen = kvkKoppelen
}

@Suppress("LongParameterList")
fun createHumanTaskParameters(
    id: Long = 1234L,
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
    isActief: Boolean = true,
    formulierDefinitieID: String? = "fakeFormulierDefinitieID",
    planItemDefinitionID: String? = "fakePlanItemDefinitionID",
    groupId: String? = "fakeGroupId",
    leadTime: Int? = 1000000000,
    referenceTables: List<HumanTaskReferentieTabel>? = emptyList()
) = ZaaktypeCmmnHumantaskParameters().apply {
    this.id = id
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
    this.isActief = isActief
    this.formulierDefinitieID = formulierDefinitieID
    this.planItemDefinitionID = planItemDefinitionID
    this.groepID = groupId
    this.doorlooptijd = leadTime
    referentieTabellen = referenceTables ?: emptyList()
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
    zaaktypeCmmnBetrokkeneParameters: ZaaktypeCmmnBetrokkeneParameters = createBetrokkeneKoppelingen(),
    zaaktypeCmmnBrpParameters: ZaaktypeCmmnBrpParameters? = ZaaktypeCmmnBrpParameters().apply {
        zoekWaarde = ""
        raadpleegWaarde = ""
    },
    zaaktypeCmmnEmailParameters: ZaaktypeCmmnEmailParameters = createAutomaticEmailConfirmation()
) =
    ZaaktypeCmmnConfiguration().apply {
        this.id = id
        this.creatiedatum = creationDate
        this.domein = domein
        this.zaakTypeUUID = zaaktypeUUID
        this.zaaktypeOmschrijving = zaaktypeOmschrijving
        this.einddatumGeplandWaarschuwing = einddatumGeplandWaarschuwing
        this.productaanvraagtype = productaanvraagtype
        this.nietOntvankelijkResultaattype = nietOntvankelijkResultaattype
        this.groepID = groupId
        this.caseDefinitionID = caseDefinitionId
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
        this.betrokkeneParameters = zaaktypeCmmnBetrokkeneParameters.apply {
            this.zaaktypeCmmnConfiguration = parameters
        }
        this.brpDoelbindingen = zaaktypeCmmnBrpParameters.apply {
            this?.zaaktypeCmmnConfiguration = parameters
        }
        this.automaticEmailConfirmation = zaaktypeCmmnEmailParameters.apply {
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
    emailSender: String? = "sender@info.nl",
    emailReply: String? = "reply@info.nl",
    // Do not add default `= createZaakafhandelParameters()` as it will cause infinite loop
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
    mail: String? = "mail@example.com",
    replyTo: String? = "replyTo@example.com",
) = ZaaktypeCmmnZaakafzenderParameters().apply {
    this.id = id
    this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
    this.isDefault = defaultMail
    this.mail = mail
    this.replyTo = replyTo
}
