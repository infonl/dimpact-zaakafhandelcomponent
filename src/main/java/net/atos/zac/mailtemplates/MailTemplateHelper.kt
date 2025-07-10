/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mailtemplates

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeOmschrijving
import net.atos.zac.mailtemplates.model.MailLink
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
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.getFullName
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.regex.Pattern

val PTAGS = Pattern.compile("</?p>", Pattern.CASE_INSENSITIVE)

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private const val ACTION = "E-mail verzenden"

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
    fun resolveVariabelen(tekst: String): String {
        var resolvedTekst = tekst
        resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.GEMEENTE, configuratieService!!.readGemeenteNaam())
        return resolvedTekst
    }

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
            resolvedTekst = replaceInitiatorVariabelen(
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

        if (taskInfo.dueDate != null) {
            resolvedTekst = replaceVariabele(
                resolvedTekst,
                MailTemplateVariabelen.TAAK_FATALEDATUM,
                DateTimeConverterUtil.convertToLocalDate(taskInfo.getDueDate()).format(DATE_FORMATTER)
            )
        }

        if (resolvedTekst.contains(MailTemplateVariabelen.TAAK_BEHANDELAAR_GROEP.variabele)) {
            resolvedTekst = replaceVariabele<String?>(
                resolvedTekst, MailTemplateVariabelen.TAAK_BEHANDELAAR_GROEP,
                taskInfo.identityLinks.stream()
                    .filter { identityLinkInfo: IdentityLinkInfo? -> IdentityLinkType.CANDIDATE == identityLinkInfo!!.getType() }
                    .findAny()
                    .map { it.groupId }
                    .map { identityService.readGroup(it) }
                    .map(Group::name)
            )
        }

        if (resolvedTekst.contains(MailTemplateVariabelen.TAAK_BEHANDELAAR_MEDEWERKER.variabele)) {
            resolvedTekst = replaceVariabele<String>(
                resolvedTekst, MailTemplateVariabelen.TAAK_BEHANDELAAR_MEDEWERKER,
                Optional.of<String>(taskInfo.assignee)
                    .map { identityService.readUser(it) }
                    .map { it.getFullName() }
            )
        }
        return resolvedTekst
    }

    fun resolveVariabelen(
        tekst: String,
        document: EnkelvoudigInformatieObject
    ): String {
        var resolvedTekst = tekst
        resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.DOCUMENT_TITEL, document.getTitel())

        val link = createMailLinkFromDocument(document)
        resolvedTekst = replaceVariabele(resolvedTekst, MailTemplateVariabelen.DOCUMENT_URL, link.url)
        resolvedTekst = replaceVariabeleHtml(resolvedTekst, MailTemplateVariabelen.DOCUMENT_LINK, link.toHtml())
        return resolvedTekst
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

    private fun createMailLinkFromDocument(document: EnkelvoudigInformatieObject): MailLink {
        return MailLink(
            document.getTitel(),
            configuratieService.informatieobjectTonenUrl(document.getUrl().extractUuid()),
            "het document",
            null
        )
    }

    private fun replaceInitiatorVariabelen(resolvedTekst: String, auditEvent: String, initiator: Optional<Rol<*>>): String {
        return if (initiator.isPresent) {
            val identificatie = initiator.get().getIdentificatienummer()
            val betrokkene = initiator.get().betrokkeneType
            when (betrokkene) {
                BetrokkeneTypeEnum.NATUURLIJK_PERSOON ->
                    brpClientService.retrievePersoon(identificatie, auditEvent)?.let {
                        replaceInitiatorVariabelenPersoon(
                            resolvedTekst,
                            it
                        )
                    } ?: ""

                BetrokkeneTypeEnum.VESTIGING -> replaceInitiatorVariabelenResultaatItem(
                    resolvedTekst,
                    kvkClientService.findVestiging(identificatie)
                )

                BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON -> replaceInitiatorVariabelenResultaatItem(
                    resolvedTekst,
                    kvkClientService.findRechtspersoon(identificatie)
                )

                else -> error("unexpected betrokkenetype $betrokkene")
            }
        } else {
            StringUtils.EMPTY
        }
    }
}

fun stripParagraphTags(onderwerp: String): String {
    // Can't parse HTML with a regular expression, but in this case there will only be bare P-tags.
    return PTAGS.matcher(onderwerp).replaceAll(StringUtils.EMPTY)
}

private fun replaceInitiatorVariabelenPersoon(
    resolvedTekst: String,
    initiator: Persoon
) = replaceInitiatorVariabelen(
    resolvedTekst,
    initiator.getNaam().getVolledigeNaam(),
    convertAdres(initiator)
)

private fun replaceInitiatorVariabelenResultaatItem(
    resolvedTekst: String,
    initiator: Optional<ResultaatItem>
): String = initiator.map {
    replaceInitiatorVariabelen(
        resolvedTekst,
        it.getNaam(),
        convertAdres(it)
    )
}
    .orElseGet { replaceInitiatorVariabelenOnbekend(resolvedTekst) }

private fun convertAdres(persoon: Persoon): String {
    val verblijfplaats = persoon.getVerblijfplaats()
    return when (verblijfplaats) {
        is Adres if verblijfplaats.verblijfadres != null ->
            convertAdres(verblijfplaats.verblijfadres)
        is VerblijfplaatsBuitenland if verblijfplaats.verblijfadres != null ->
            convertAdres(verblijfplaats.verblijfadres)
        else -> StringUtils.EMPTY
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
    replaceInitiatorVariabelen(resolvedTekst, "Onbekend", "")

private fun replaceInitiatorVariabelen(
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
    target: String,
    variabele: MailTemplateVariabelen,
    waarde: Optional<T>
) = replaceVariabele(target, variabele, waarde.map { it.toString() }.orElse(null))

private fun replaceVariabele(
    target: String,
    variabele: MailTemplateVariabelen,
    waarde: String?
) = replaceVariabeleHtml(target, variabele, StringEscapeUtils.escapeHtml4(waarde))

// Make sure that what is passed in the HTML argument is FULLY encoded HTML (no injection vulnerabilities)
private fun replaceVariabeleHtml(
    target: String,
    variabele: MailTemplateVariabelen,
    html: String?
) = StringUtils.replace(
    target,
    variabele.variabele,
    if (variabele.isResolveVariabeleAlsLegeString) {
        StringUtils.defaultIfBlank(
            html,
            StringUtils.EMPTY
        )
    } else {
        html
    }
)
