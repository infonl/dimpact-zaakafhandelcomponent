/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.Rol
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeOmschrijving
import net.atos.zac.mailtemplates.model.MailTemplateVariabelen
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.model.generated.Persoon
import nl.info.client.brp.util.toAddressString
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.util.toAddressString
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.getFullName
import nl.info.zac.mailtemplates.model.MailLink
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.text.StringEscapeUtils
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.time.format.DateTimeFormatter
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrNull

@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions", "LongParameterList")
class MailTemplateHelper @Inject constructor(
    private val brpClientService: BrpClientService,
    private var configuratieService: ConfiguratieService,
    private val identityService: IdentityService,
    private val kvkClientService: KvkClientService,
    private val zgwApiService: ZGWApiService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService
) {
    companion object {
        private val LOG = Logger.getLogger(MailTemplateHelper::class.java.name)
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        private const val ACTION = "E-mail verzenden"
        private const val REPLACEMENT_FOR_UNKNOWN_NAME = "Onbekend"
    }

    fun resolveGemeenteVariable(text: String): String =
        replaceVariabele(
            targetString = text,
            mailTemplateVariable = MailTemplateVariabelen.GEMEENTE,
            value = configuratieService.readGemeenteNaam()
        )

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun resolveVariabelen(tekst: String, zaak: Zaak): String {
        var resolvedTekst = tekst
        resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.ZAAK_NUMMER, zaak.getIdentificatie())

        val link = createMailLinkFromZaak(zaak)
        resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.ZAAK_URL, link.url)
        resolvedTekst = replaceVariabeleHtml(resolvedTekst, MailTemplateVariabelen.ZAAK_LINK, link.toHtml())
        resolvedTekst = zaak.getOmschrijving()?.let {
            replaceVariabele(resolvedTekst, MailTemplateVariabelen.ZAAK_OMSCHRIJVING, it)
        } ?: resolvedTekst
        resolvedTekst = zaak.getToelichting()?.let {
            replaceVariabele(resolvedTekst, MailTemplateVariabelen.ZAAK_TOELICHTING, it)
        } ?: resolvedTekst
        resolvedTekst = replaceVariabele(
            resolvedTekst, MailTemplateVariabelen.ZAAK_REGISTRATIEDATUM,
            zaak.getRegistratiedatum().format(DATE_FORMATTER)
        )
        resolvedTekst = replaceVariabele(
            resolvedTekst, MailTemplateVariabelen.ZAAK_STARTDATUM,
            zaak.getStartdatum().format(DATE_FORMATTER)
        )
        resolvedTekst = replaceVariabele(
            targetString = resolvedTekst,
            mailTemplateVariable = MailTemplateVariabelen.ZAAK_STREEFDATUM,
            value = zaak.getEinddatumGepland()?.format(DATE_FORMATTER)
        )
        resolvedTekst = replaceVariabele(
            resolvedTekst, MailTemplateVariabelen.ZAAK_FATALEDATUM,
            zaak.getUiterlijkeEinddatumAfdoening()?.format(DATE_FORMATTER)
        )
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_STATUS.variabele)) {
            val statusOmschrijving = zaak.getStatus()
                .let(zrcClientService::readStatus)
                .getStatustype()
                .let(ztcClientService::readStatustype)
                .getOmschrijving()
            resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.ZAAK_STATUS, statusOmschrijving)
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_TYPE.variabele)) {
            val zaaktypeOmschrijving = ztcClientService.readZaaktype(zaak.getZaaktype()).getOmschrijving()
            resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.ZAAK_TYPE, zaaktypeOmschrijving)
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_INITIATOR.variabele) ||
            resolvedTekst.contains(MailTemplateVariabelen.ZAAK_INITIATOR_ADRES.variabele)
        ) {
            resolvedTekst = zgwApiService.findInitiatorRoleForZaak(zaak)?.let {
                replaceInitiatorVariables(
                    resolvedTekst = resolvedTekst,
                    auditEvent = zaak.getIdentificatie() + "@" + ACTION,
                    initiatorRole = it
                )
            } ?: ""
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_BEHANDELAAR_GROEP.variabele)) {
            val groupName = zgwApiService.findGroepForZaak(zaak)?.getNaam()
            resolvedTekst = replaceVariabele(
                targetString = resolvedTekst,
                mailTemplateVariable = MailTemplateVariabelen.ZAAK_BEHANDELAAR_GROEP,
                value = groupName
            )
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_BEHANDELAAR_MEDEWERKER.variabele)) {
            val medewerkerName = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)?.getNaam()
            resolvedTekst = replaceVariabele(
                targetString = resolvedTekst,
                mailTemplateVariable = MailTemplateVariabelen.ZAAK_BEHANDELAAR_MEDEWERKER,
                value = medewerkerName
            )
        }
        return resolvedTekst
    }

    fun resolveVariabelen(tekst: String, taskInfo: TaskInfo): String {
        var resolvedTekst = tekst
        val link = createMailLinkFromTask(taskInfo)
        resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.TAAK_URL, link.url)
        resolvedTekst = replaceVariabeleHtml(resolvedTekst, MailTemplateVariabelen.TAAK_LINK, link.toHtml())

        taskInfo.dueDate?.let {
            resolvedTekst = replaceVariabele(
                resolvedTekst,
                MailTemplateVariabelen.TAAK_FATALEDATUM,
                DateTimeConverterUtil.convertToLocalDate(it).format(DATE_FORMATTER)
            )
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.TAAK_BEHANDELAAR_GROEP.variabele)) {
            resolvedTekst = replaceVariabele(
                resolvedTekst,
                MailTemplateVariabelen.TAAK_BEHANDELAAR_GROEP,
                taskInfo.identityLinks
                    .filter { IdentityLinkType.CANDIDATE == it.type }
                    .map { it.groupId }
                    .map { identityService.readGroup(it) }
                    .map(Group::name)
                    .firstOrNull()
            )
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.TAAK_BEHANDELAAR_MEDEWERKER.variabele)) {
            resolvedTekst = replaceVariabele<String>(
                targetString = resolvedTekst, MailTemplateVariabelen.TAAK_BEHANDELAAR_MEDEWERKER,
                value = taskInfo.assignee.let { identityService.readUser(it).getFullName() }
            )
        }
        return resolvedTekst
    }

    fun resolveVariabelen(
        text: String,
        enkelvoudigInformatieObject: EnkelvoudigInformatieObject
    ): String {
        val link = createMailLinkFromDocument(enkelvoudigInformatieObject)
        return replaceVariabeleHtml(
            targetString = replaceVariabele(
                targetString = replaceVariabele(
                    targetString = text,
                    mailTemplateVariable = MailTemplateVariabelen.DOCUMENT_TITEL,
                    value = enkelvoudigInformatieObject.getTitel()
                ),
                mailTemplateVariable = MailTemplateVariabelen.DOCUMENT_URL,
                value = link.url
            ),
            mailTemplateVariable = MailTemplateVariabelen.DOCUMENT_LINK,
            htmlEscapedValue = link.toHtml()
        )
    }

    private fun createMailLinkFromZaak(zaak: Zaak): MailLink {
        val identificatie = zaak.getIdentificatie()
        val zaaktypeOmschrijving = ztcClientService.readZaaktype(zaak.getZaaktype()).getOmschrijving()
        return MailLink(
            identificatie,
            configuratieService.zaakTonenUrl(identificatie),
            "de zaak",
            "($zaaktypeOmschrijving)"
        )
    }

    private fun createMailLinkFromTask(taskInfo: TaskInfo): MailLink {
        val zaakIdentificatie = readZaakIdentificatie(taskInfo)
        val zaaktypeOmschrijving = readZaaktypeOmschrijving(taskInfo)
        return MailLink(
            taskInfo.name,
            configuratieService.taakTonenUrl(taskInfo.id),
            "de taak",
            "voor zaak $zaakIdentificatie ($zaaktypeOmschrijving)"
        )
    }

    private fun createMailLinkFromDocument(document: EnkelvoudigInformatieObject): MailLink =
        MailLink(
            document.getTitel(),
            configuratieService.informatieobjectTonenUrl(document.getUrl().extractUuid()),
            "het document",
            null
        )

    @Suppress("NestedBlockDepth")
    private fun replaceInitiatorVariables(
        resolvedTekst: String,
        auditEvent: String,
        initiatorRole: Rol<*>
    ): String {
        val identificatie = initiatorRole.getIdentificatienummer()
        return when (val betrokkeneType = initiatorRole.betrokkeneType) {
            BetrokkeneTypeEnum.NATUURLIJK_PERSOON ->
                brpClientService.retrievePersoon(identificatie, auditEvent)?.let {
                    replaceInitiatorVariablesPersoon(
                        resolvedTekst,
                        it
                    )
                } ?: ""

            BetrokkeneTypeEnum.VESTIGING -> replaceInitiatorVariablesResultaatItem(
                resolvedTekst,
                kvkClientService.findVestiging(identificatie).getOrNull()
            )

            BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON -> {
                val resultaatItem =
                    (initiatorRole.betrokkeneIdentificatie as NietNatuurlijkPersoonIdentificatie).let {
                        when {
                            it.innNnpId?.isNotBlank() == true ->
                                kvkClientService.findRechtspersoon(identificatie).getOrNull()

                            it.vestigingsNummer?.isNotBlank() == true ->
                                kvkClientService.findVestiging(identificatie).getOrNull()

                            else -> {
                                LOG.warning { "Unsupported niet-natuurlijk persoon identificatie: '$it'" }
                                null
                            }
                        }
                    }
                replaceInitiatorVariablesResultaatItem(
                    resolvedText = resolvedTekst,
                    initiatorResultaatItem = resultaatItem
                )
            }

            else -> error("Unsupported betrokkene type '$betrokkeneType'")
        }
    }

    private fun replaceInitiatorVariablesPersoon(
        resolvedTekst: String,
        initiatorPersoon: Persoon
    ): String {
        return replaceInitiatorVariables(
            resolvedText = resolvedTekst,
            name = initiatorPersoon.getNaam()?.getVolledigeNaam() ?: run {
                // In practise most likely never going to happen, but we log it anyway.
                // Note that we do not log the person's BSN because of data privacy reasons.
                LOG.warning(
                    "Initiator persoon with geboorte: '${initiatorPersoon.geboorte}' does not have a name. " +
                        "Using: '$REPLACEMENT_FOR_UNKNOWN_NAME' for full name."
                )
                REPLACEMENT_FOR_UNKNOWN_NAME
            },
            address = initiatorPersoon.toAddressString()
        )
    }

    private fun replaceInitiatorVariablesResultaatItem(
        resolvedText: String,
        initiatorResultaatItem: ResultaatItem?
    ): String = initiatorResultaatItem?.let {
        replaceInitiatorVariables(
            resolvedText = resolvedText,
            name = initiatorResultaatItem.getNaam(),
            address = it.toAddressString()
        )
    } ?: replaceInitiatorVariablesWithUnknownText(resolvedText)

    private fun replaceInitiatorVariablesWithUnknownText(resolvedTekst: String) =
        replaceInitiatorVariables(resolvedTekst, "Onbekend", "")

    private fun replaceInitiatorVariables(
        resolvedText: String,
        name: String,
        address: String
    ) = replaceVariabele(
        targetString = replaceVariabele(
            resolvedText,
            MailTemplateVariabelen.ZAAK_INITIATOR,
            name
        ),
        mailTemplateVariable = MailTemplateVariabelen.ZAAK_INITIATOR_ADRES,
        value = address
    )

    private fun <T> replaceVariabele(
        targetString: String,
        mailTemplateVariable: MailTemplateVariabelen,
        value: T
    ) = replaceVariabele(targetString, mailTemplateVariable, value.toString())

    private fun replaceVariabele(
        targetString: String,
        mailTemplateVariable: MailTemplateVariabelen,
        value: String?
    ) = replaceVariabeleHtml(
        targetString = targetString,
        mailTemplateVariable = mailTemplateVariable,
        htmlEscapedValue = StringEscapeUtils.escapeHtml4(value)
    )

    /**
     * Make sure that the [htmlEscapedValue] parameter is HTML escaped to avoid injection vulnerabilities.
     */
    private fun replaceVariabeleHtml(
        targetString: String,
        mailTemplateVariable: MailTemplateVariabelen,
        htmlEscapedValue: String?
    ): String {
        val replacement = if (htmlEscapedValue.isNullOrBlank() && mailTemplateVariable.isResolveVariabeleAlsLegeString) {
            ""
        } else {
            htmlEscapedValue
        }
        return targetString.replace(mailTemplateVariable.variabele, replacement ?: mailTemplateVariable.variabele)
    }
}
