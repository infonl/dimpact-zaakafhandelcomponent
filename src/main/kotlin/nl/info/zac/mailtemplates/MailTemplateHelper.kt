/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeOmschrijving
import net.atos.zac.mailtemplates.model.MailTemplateVariabelen
import net.atos.zac.util.StringUtil
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.Persoon
import nl.info.client.brp.model.generated.VerblijfadresBinnenland
import nl.info.client.brp.model.generated.VerblijfadresBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsBuitenland
import nl.info.client.kvk.KvkClientService
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
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.logging.Logger
import java.util.regex.Pattern

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
    }

    fun resolveGemeenteVariable(text: String): String =
        replaceVariabele(
            targetString = text,
            mailTemplateVariable = MailTemplateVariabelen.GEMEENTE,
            value = configuratieService.readGemeenteNaam()
        )

    @Suppress("LongMethod")
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
        resolvedTekst = replaceVariabele<String>(
            resolvedTekst,
            MailTemplateVariabelen.ZAAK_STREEFDATUM,
            Optional.ofNullable<LocalDate>(
                zaak.getEinddatumGepland()
            )
                .map { it.format(DATE_FORMATTER) }
        )
        resolvedTekst = replaceVariabele(
            resolvedTekst, MailTemplateVariabelen.ZAAK_FATALEDATUM,
            zaak.getUiterlijkeEinddatumAfdoening().format(DATE_FORMATTER)
        )
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_STATUS.variabele)) {
            resolvedTekst = replaceVariabele<String>(
                resolvedTekst,
                MailTemplateVariabelen.ZAAK_STATUS,
                Optional.of(zaak.getStatus())
                    .map { zrcClientService.readStatus(it) }
                    .map { it.getStatustype() }
                    .map { ztcClientService.readStatustype(it) }
                    .map { it.getOmschrijving() }
            )
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_TYPE.variabele)) {
            resolvedTekst = replaceVariabele<String>(
                resolvedTekst, MailTemplateVariabelen.ZAAK_TYPE,
                Optional.of(zaak.getZaaktype())
                    .map { ztcClientService.readZaaktype(it) }
                    .map { it.getOmschrijving() }
            )
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_INITIATOR.variabele) ||
            resolvedTekst.contains(MailTemplateVariabelen.ZAAK_INITIATOR_ADRES.variabele)
        ) {
            resolvedTekst = replaceInitiatorVariabeles(
                resolvedTekst,
                zaak.getIdentificatie() + "@" + ACTION,
                Optional.ofNullable<Rol<*>>(zgwApiService.findInitiatorRoleForZaak(zaak))
            )
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_BEHANDELAAR_GROEP.variabele)) {
            val groupName = Optional.ofNullable<RolOrganisatorischeEenheid>(zgwApiService.findGroepForZaak(zaak))
                .map { it.getNaam() }
                .orElse(null)
            resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.ZAAK_BEHANDELAAR_GROEP, groupName)
        }
        if (resolvedTekst.contains(MailTemplateVariabelen.ZAAK_BEHANDELAAR_MEDEWERKER.variabele)) {
            val medewerkerName = Optional.ofNullable<RolMedewerker>(
                zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            )
                .map { it.getNaam() }
                .orElse(null)
            resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.ZAAK_BEHANDELAAR_MEDEWERKER, medewerkerName)
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
                resolvedTekst, MailTemplateVariabelen.TAAK_BEHANDELAAR_MEDEWERKER,
                Optional.of(taskInfo.assignee)
                    .map { identityService.readUser(it) }
                    .map { it.getFullName() }
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
            mailTemplateVariabele = MailTemplateVariabelen.DOCUMENT_LINK,
            htmlEscapedValue = link.toHtml()
        )
    }

    private fun createMailLinkFromZaak(zaak: Zaak): MailLink {
        val zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype())
        return MailLink(
            zaak.getIdentificatie(),
            configuratieService.zaakTonenUrl(zaak.getIdentificatie()),
            "de zaak",
            "(${zaaktype.getOmschrijving()}"
        )
    }

    private fun createMailLinkFromTask(taskInfo: TaskInfo): MailLink {
        val zaakIdentificatie = readZaakIdentificatie(taskInfo)
        val zaaktypeOmschrijving = readZaaktypeOmschrijving(taskInfo)
        return MailLink(
            taskInfo.name,
            configuratieService.taakTonenUrl(taskInfo.getId()),
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
    private fun replaceInitiatorVariabeles(
        resolvedTekst: String,
        auditEvent: String,
        initiatorRole: Optional<Rol<*>>
    ): String =
        if (initiatorRole.isPresent) {
            val identificatie = initiatorRole.get().getIdentificatienummer()
            when (val betrokkeneType = initiatorRole.get().betrokkeneType) {
                BetrokkeneTypeEnum.NATUURLIJK_PERSOON ->
                    brpClientService.retrievePersoon(identificatie, auditEvent)?.let {
                        replaceInitiatorVariabelenPersoon(
                            resolvedTekst,
                            it
                        )
                    } ?: StringUtils.EMPTY

                BetrokkeneTypeEnum.VESTIGING -> replaceInitiatorVariabelenResultaatItem(
                    resolvedTekst,
                    kvkClientService.findVestiging(identificatie)
                )

                BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON -> {
                    val resultaatItem = (initiatorRole.get().betrokkeneIdentificatie as NietNatuurlijkPersoonIdentificatie).let {
                        when {
                            it.innNnpId?.isNotBlank() == true ->
                                kvkClientService.findRechtspersoon(identificatie)
                            it.vestigingsNummer?.isNotBlank() == true ->
                                kvkClientService.findVestiging(identificatie)
                            else -> {
                                LOG.warning { "Unsupported niet-natuurlijk persoon identificatie: '$it'" }
                                Optional.empty()
                            }
                        }
                    }
                    replaceInitiatorVariabelenResultaatItem(
                        resolvedText = resolvedTekst,
                        initiatorResultaatItem = resultaatItem
                    )
                }

                else -> error("Unsupported betrokkene type '$betrokkeneType'")
            }
        } else {
            StringUtils.EMPTY
        }

    private fun replaceInitiatorVariabelenPersoon(
        resolvedTekst: String,
        initiator: Persoon
    ) = replaceInitiatorVariabeles(
        resolvedTekst,
        initiator.getNaam().getVolledigeNaam(),
        convertAdres(initiator)
    )

    private fun replaceInitiatorVariabelenResultaatItem(
        resolvedText: String,
        initiatorResultaatItem: Optional<ResultaatItem>
    ): String = initiatorResultaatItem.map {
        replaceInitiatorVariabeles(
            resolvedText,
            it.getNaam(),
            convertAdres(it)
        )
    }.orElseGet { replaceInitiatorVariabelenOnbekend(resolvedText) }

    private fun convertAdres(persoon: Persoon): String {
        val verblijfplaats = persoon.getVerblijfplaats()
        return when (verblijfplaats) {
            is Adres if verblijfplaats.verblijfadres != null ->
                convertAdres(verblijfplaats.verblijfadres)
            is VerblijfplaatsBuitenland if verblijfplaats.verblijfadres != null ->
                convertAdres(verblijfplaats.verblijfadres)
            else -> {
                LOG.info { "Unsupported persoon verblijfplaats type: '${verblijfplaats.javaClass.name}'" }
                StringUtils.EMPTY
            }
        }
    }

    private fun convertAdres(adres: VerblijfadresBinnenland) =
        "${StringUtils.defaultIfBlank(adres.getOfficieleStraatnaam(), StringUtils.EMPTY)} " +
            "${ObjectUtils.defaultIfNull(adres.getHuisnummer(), StringUtils.EMPTY)}" +
            "${StringUtils.defaultIfBlank(adres.getHuisletter(), StringUtils.EMPTY)}" +
            "${StringUtils.defaultIfBlank(adres.getHuisnummertoevoeging(), StringUtils.EMPTY)}, " +
            "${StringUtils.defaultIfBlank(adres.getPostcode(), StringUtils.EMPTY)} " +
            "${adres.getWoonplaats()}"

    private fun convertAdres(adres: VerblijfadresBuitenland) =
        StringUtil.joinNonBlankWith(", ", adres.getRegel1(), adres.getRegel2(), adres.getRegel3())

    private fun convertAdres(resultaatItem: ResultaatItem): String {
        val binnenlandsAdres = resultaatItem.getAdres().getBinnenlandsAdres()
        return "${binnenlandsAdres.getStraatnaam()} " +
            "${ObjectUtils.defaultIfNull(binnenlandsAdres.getHuisnummer(), StringUtils.EMPTY)}" +
            "${StringUtils.defaultIfBlank(binnenlandsAdres.getHuisletter(), StringUtils.EMPTY)}, " +
            "${StringUtils.defaultIfBlank(binnenlandsAdres.getPostcode(), StringUtils.EMPTY)} " +
            "${binnenlandsAdres.getPlaats()}"
    }

    private fun replaceInitiatorVariabelenOnbekend(resolvedTekst: String) =
        replaceInitiatorVariabeles(resolvedTekst, "Onbekend", "")

    private fun replaceInitiatorVariabeles(
        resolvedTekst: String,
        naam: String,
        adres: String
    ) = replaceVariabele(
        replaceVariabele(
            resolvedTekst,
            MailTemplateVariabelen.ZAAK_INITIATOR,
            naam
        ),
        MailTemplateVariabelen.ZAAK_INITIATOR_ADRES,
        adres
    )

    private fun <T> replaceVariabele(
        targetString: String,
        mailTemplateVariable: MailTemplateVariabelen,
        value: Optional<T>
    ) = replaceVariabele(targetString, mailTemplateVariable, value.map { it.toString() }.orElse(null))

    private fun replaceVariabele(
        targetString: String,
        mailTemplateVariable: MailTemplateVariabelen,
        value: String?
    ) = replaceVariabeleHtml(
        targetString,
        mailTemplateVariable,
        StringEscapeUtils.escapeHtml4(value)
    )

    /**
     * Make sure that the [htmlEscapedValue] parameter is HTML escaped to avoid injection vulnerabilities.
     */
    private fun replaceVariabeleHtml(
        targetString: String,
        mailTemplateVariabele: MailTemplateVariabelen,
        htmlEscapedValue: String?
    ): String {
        val replacement = htmlEscapedValue?.takeIf { it.isNotBlank() }
            ?: if (mailTemplateVariabele.isResolveVariabeleAlsLegeString) StringUtils.EMPTY else htmlEscapedValue
        return targetString.replace(mailTemplateVariabele.variabele, replacement ?: mailTemplateVariabele.variabele)
    }
}

fun stripParagraphTags(onderwerp: String): String =
    // Can't parse HTML with a regular expression, but in this case there will only be bare P-tags.
    Pattern.compile("</?p>", Pattern.CASE_INSENSITIVE).matcher(onderwerp).replaceAll(StringUtils.EMPTY)
