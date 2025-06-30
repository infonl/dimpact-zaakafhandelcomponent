/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.admin.model

import net.atos.zac.admin.model.BetrokkeneKoppelingen
import net.atos.zac.admin.model.BrpDoelbindingen
import net.atos.zac.admin.model.HumanTaskParameters
import net.atos.zac.admin.model.HumanTaskReferentieTabel
import net.atos.zac.admin.model.MailtemplateKoppeling
import net.atos.zac.admin.model.ZaakAfzender
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.admin.model.ZaakbeeindigParameter
import net.atos.zac.mailtemplates.model.Mail
import net.atos.zac.mailtemplates.model.MailTemplate
import java.time.ZonedDateTime
import java.util.UUID

fun createBetrokkeneKoppelingen(
    id: Long? = 1234L,
    // Do not add default `= createZaakafhandelParameters()` as it will cause infinite loop
    zaakafhandelParameters: ZaakafhandelParameters? = null,
    brpKoppelen: Boolean = true,
    kvkKoppelen: Boolean = true
) = BetrokkeneKoppelingen().apply {
    this.id = id
    this.zaakafhandelParameters = zaakafhandelParameters
    this.brpKoppelen = brpKoppelen
    this.kvkKoppelen = kvkKoppelen
}

fun createHumanTaskParameters(
    id: Long = 1234L,
    zaakafhandelParameters: ZaakafhandelParameters = createZaakafhandelParameters(),
    isActief: Boolean = true
) = HumanTaskParameters().apply {
    this.id = id
    this.zaakafhandelParameters = zaakafhandelParameters
    this.isActief = isActief
}

fun createHumanTaskReferentieTabel(
    id: Long = 1234L,
    referenceTable: ReferenceTable = createReferenceTable(),
    humanTaskParameters: HumanTaskParameters = createHumanTaskParameters(),
    field: String = "fakeField",
) = HumanTaskReferentieTabel().apply {
    this.id = id
    this.tabel = referenceTable
    this.humantask = humanTaskParameters
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
fun createZaakafhandelParameters(
    id: Long? = 1234L,
    creationDate: ZonedDateTime = ZonedDateTime.now(),
    domein: String? = "fakeDomein",
    zaaktypeUUID: UUID = UUID.randomUUID(),
    zaaktypeOmschrijving: String = "fakeZaaktypeOmschrijving",
    einddatumGeplandWaarschuwing: Int? = null,
    productaanvraagtype: String? = null,
    nietOntvankelijkResultaattype: UUID = UUID.randomUUID(),
    zaakbeeindigParameters: Set<ZaakbeeindigParameter>? = emptySet(),
    groupId: String? = "fakeGroupId",
    caseDefinitionId: String = "fakeCaseDefinitionId",
    betrokkeneKoppelingen: BetrokkeneKoppelingen = createBetrokkeneKoppelingen(),
    brpDoelbindingen: BrpDoelbindingen? = BrpDoelbindingen().apply {
        zoekWaarde = ""
        raadpleegWaarde = ""
    },
) =
    ZaakafhandelParameters().apply {
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
                    zaakafhandelParameters = this,
                    mailTemplate = createMailTemplate()
                )
            )
        )
        setZaakAfzenders(setOf(createZaakAfzender(zaakafhandelParameters = this)))
        setZaakbeeindigParameters(zaakbeeindigParameters)
        val parameters = this
        this.betrokkeneKoppelingen = betrokkeneKoppelingen.apply {
            this.zaakafhandelParameters = parameters
        }
        this.brpDoelbindingen = brpDoelbindingen.apply {
            this?.zaakafhandelParameters = parameters
        }
    }

fun createMailtemplateKoppelingen(
    id: Long? = 1234L,
    zaakafhandelParameters: ZaakafhandelParameters,
    mailTemplate: MailTemplate
) = MailtemplateKoppeling().apply {
    this.id = id
    this.zaakafhandelParameters = zaakafhandelParameters
    this.mailTemplate = mailTemplate
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
    zaakafhandelParameters: ZaakafhandelParameters,
    defaultMail: Boolean = false,
    mail: String? = "mail@example.com",
    replyTo: String? = "replyTo@example.com",
) = ZaakAfzender().apply {
    this.id = id
    this.zaakafhandelParameters = zaakafhandelParameters
    this.isDefault = defaultMail
    this.mail = mail
    this.replyTo = replyTo
}
