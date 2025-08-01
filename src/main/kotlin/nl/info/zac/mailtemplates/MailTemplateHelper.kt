/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.Rol
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeOmschrijving
import net.atos.zac.mailtemplates.model.MailTemplateVariables
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
        replaceVariable(
            targetString = text,
            mailTemplateVariable = MailTemplateVariables.GEMEENTE,
            value = configuratieService.readGemeenteNaam()
        )

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun resolveZaakVariables(tekst: String, zaak: Zaak): String {
        var resolvedTekst = tekst
        resolvedTekst = replaceVariable(resolvedTekst, MailTemplateVariables.ZAAK_NUMMER, zaak.getIdentificatie())

        val link = createMailLinkFromZaak(zaak)
        resolvedTekst = replaceVariable(resolvedTekst, MailTemplateVariables.ZAAK_URL, link.url)
        resolvedTekst = replaceVariableHtml(resolvedTekst, MailTemplateVariables.ZAAK_LINK, link.toHtml())
        resolvedTekst = zaak.getOmschrijving()?.let {
            replaceVariable(resolvedTekst, MailTemplateVariables.ZAAK_OMSCHRIJVING, it)
        } ?: resolvedTekst
        resolvedTekst = zaak.getToelichting()?.let {
            replaceVariable(resolvedTekst, MailTemplateVariables.ZAAK_TOELICHTING, it)
        } ?: resolvedTekst
        resolvedTekst = replaceVariable(
            resolvedTekst, MailTemplateVariables.ZAAK_REGISTRATIEDATUM,
            zaak.getRegistratiedatum().format(DATE_FORMATTER)
        )
        resolvedTekst = replaceVariable(
            resolvedTekst, MailTemplateVariables.ZAAK_STARTDATUM,
            zaak.getStartdatum().format(DATE_FORMATTER)
        )
        resolvedTekst = replaceVariable(
            targetString = resolvedTekst,
            mailTemplateVariable = MailTemplateVariables.ZAAK_STREEFDATUM,
            value = zaak.getEinddatumGepland()?.format(DATE_FORMATTER)
        )
        resolvedTekst = replaceVariable(
            resolvedTekst, MailTemplateVariables.ZAAK_FATALEDATUM,
            zaak.getUiterlijkeEinddatumAfdoening()?.format(DATE_FORMATTER)
        )
        if (resolvedTekst.contains(MailTemplateVariables.ZAAK_STATUS.variable)) {
            val statusOmschrijving = zaak.getStatus()
                .let(zrcClientService::readStatus)
                .getStatustype()
                .let(ztcClientService::readStatustype)
                .getOmschrijving()
            resolvedTekst = replaceVariable(resolvedTekst, MailTemplateVariables.ZAAK_STATUS, statusOmschrijving)
        }
        if (resolvedTekst.contains(MailTemplateVariables.ZAAK_TYPE.variable)) {
            val zaaktypeOmschrijving = ztcClientService.readZaaktype(zaak.getZaaktype()).getOmschrijving()
            resolvedTekst = replaceVariable(resolvedTekst, MailTemplateVariables.ZAAK_TYPE, zaaktypeOmschrijving)
        }
        if (MailTemplateVariables.ZAAK_INITIATOR.variable in resolvedTekst ||
            MailTemplateVariables.ZAAK_INITIATOR_ADRES.variable in resolvedTekst
        ) {
            resolvedTekst = zgwApiService.findInitiatorRoleForZaak(zaak)?.let { initiatorRole ->
                replaceInitiatorVariables(
                    resolvedText = resolvedTekst,
                    auditEvent = "${zaak.getIdentificatie()}@$ACTION",
                    initiatorRole = initiatorRole
                )
            } ?: replaceInitiatorVariablesWithUnknownText(resolvedTekst)
        }
        if (resolvedTekst.contains(MailTemplateVariables.ZAAK_BEHANDELAAR_GROEP.variable)) {
            val groupName = zgwApiService.findGroepForZaak(zaak)?.getNaam()
            resolvedTekst = replaceVariable(
                targetString = resolvedTekst,
                mailTemplateVariable = MailTemplateVariables.ZAAK_BEHANDELAAR_GROEP,
                value = groupName
            )
        }
        if (resolvedTekst.contains(MailTemplateVariables.ZAAK_BEHANDELAAR_MEDEWERKER.variable)) {
            val medewerkerName = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)?.getNaam()
            resolvedTekst = replaceVariable(
                targetString = resolvedTekst,
                mailTemplateVariable = MailTemplateVariables.ZAAK_BEHANDELAAR_MEDEWERKER,
                value = medewerkerName
            )
        }
        return resolvedTekst
    }

    fun resolveTaskVariables(tekst: String, taskInfo: TaskInfo): String {
        var resolvedTekst = tekst
        val link = createMailLinkFromTask(taskInfo)
        resolvedTekst = replaceVariable(resolvedTekst, MailTemplateVariables.TAAK_URL, link.url)
        resolvedTekst = replaceVariableHtml(resolvedTekst, MailTemplateVariables.TAAK_LINK, link.toHtml())

        taskInfo.dueDate?.let {
            resolvedTekst = replaceVariable(
                resolvedTekst,
                MailTemplateVariables.TAAK_FATALEDATUM,
                DateTimeConverterUtil.convertToLocalDate(it).format(DATE_FORMATTER)
            )
        }
        if (resolvedTekst.contains(MailTemplateVariables.TAAK_BEHANDELAAR_GROEP.variable)) {
            resolvedTekst = replaceVariable(
                resolvedTekst,
                MailTemplateVariables.TAAK_BEHANDELAAR_GROEP,
                taskInfo.identityLinks
                    .filter { IdentityLinkType.CANDIDATE == it.type }
                    .map { it.groupId }
                    .map { identityService.readGroup(it) }
                    .map(Group::name)
                    .firstOrNull()
            )
        }
        if (resolvedTekst.contains(MailTemplateVariables.TAAK_BEHANDELAAR_MEDEWERKER.variable)) {
            resolvedTekst = replaceVariable(
                targetString = resolvedTekst, MailTemplateVariables.TAAK_BEHANDELAAR_MEDEWERKER,
                value = taskInfo.assignee?.let { identityService.readUser(it).getFullName() }
            )
        }
        return resolvedTekst
    }

    fun resolveEnkelvoudigInformatieObjectVariables(
        text: String,
        enkelvoudigInformatieObject: EnkelvoudigInformatieObject
    ): String {
        val link = createMailLinkFromDocument(enkelvoudigInformatieObject)
        return replaceVariableHtml(
            targetString = replaceVariable(
                targetString = replaceVariable(
                    targetString = text,
                    mailTemplateVariable = MailTemplateVariables.DOCUMENT_TITEL,
                    value = enkelvoudigInformatieObject.getTitel()
                ),
                mailTemplateVariable = MailTemplateVariables.DOCUMENT_URL,
                value = link.url
            ),
            mailTemplateVariable = MailTemplateVariables.DOCUMENT_LINK,
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
        resolvedText: String,
        auditEvent: String,
        initiatorRole: Rol<*>
    ): String {
        val identificatie = initiatorRole.getIdentificatienummer()
        return when (val betrokkeneType = initiatorRole.betrokkeneType) {
            BetrokkeneTypeEnum.NATUURLIJK_PERSOON ->
                brpClientService.retrievePersoon(identificatie, auditEvent)?.let {
                    replaceInitiatorVariablesPersoon(
                        resolvedText,
                        it
                    )
                } ?: ""

            BetrokkeneTypeEnum.VESTIGING -> replaceInitiatorVariablesResultaatItem(
                resolvedText,
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
                    resolvedText = resolvedText,
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
    ) = replaceVariable(
        targetString = replaceVariable(
            resolvedText,
            MailTemplateVariables.ZAAK_INITIATOR,
            name
        ),
        mailTemplateVariable = MailTemplateVariables.ZAAK_INITIATOR_ADRES,
        value = address
    )

    private fun <T> replaceVariable(
        targetString: String,
        mailTemplateVariable: MailTemplateVariables,
        value: T
    ) = replaceVariable(targetString, mailTemplateVariable, value.toString())

    private fun replaceVariable(
        targetString: String,
        mailTemplateVariable: MailTemplateVariables,
        value: String?
    ) = replaceVariableHtml(
        targetString = targetString,
        mailTemplateVariable = mailTemplateVariable,
        htmlEscapedValue = StringEscapeUtils.escapeHtml4(value)
    )

    /**
     * Make sure that the [htmlEscapedValue] parameter is HTML escaped to avoid injection vulnerabilities.
     */
    private fun replaceVariableHtml(
        targetString: String,
        mailTemplateVariable: MailTemplateVariables,
        htmlEscapedValue: String?
    ): String {
        val replacement = if (htmlEscapedValue.isNullOrBlank() && mailTemplateVariable.isResolveVariableAsEmptyString) {
            ""
        } else {
            htmlEscapedValue
        }
        return targetString.replace(mailTemplateVariable.variable, replacement ?: mailTemplateVariable.variable)
    }
}
