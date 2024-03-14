package net.atos.zac.mailtemplates;

import static net.atos.client.zgw.shared.util.URIUtil.parseUUIDFromResourceURI;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.DOCUMENT_LINK;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.DOCUMENT_TITEL;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.DOCUMENT_URL;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.GEMEENTE;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.TAAK_BEHANDELAAR_GROEP;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.TAAK_BEHANDELAAR_MEDEWERKER;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.TAAK_FATALEDATUM;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.TAAK_LINK;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.TAAK_URL;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_BEHANDELAAR_GROEP;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_BEHANDELAAR_MEDEWERKER;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_FATALEDATUM;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_INITIATOR;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_INITIATOR_ADRES;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_LINK;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_NUMMER;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_OMSCHRIJVING;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_REGISTRATIEDATUM;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_STARTDATUM;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_STATUS;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_STREEFDATUM;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_TOELICHTING;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_TYPE;
import static net.atos.zac.mailtemplates.model.MailTemplateVariabelen.ZAAK_URL;
import static net.atos.zac.util.StringUtil.joinNonBlankWith;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.TaskInfo;

import net.atos.client.brp.BRPClientService;
import net.atos.client.brp.model.generated.Adres;
import net.atos.client.brp.model.generated.Persoon;
import net.atos.client.brp.model.generated.VerblijfadresBinnenland;
import net.atos.client.brp.model.generated.VerblijfadresBuitenland;
import net.atos.client.brp.model.generated.VerblijfplaatsBuitenland;
import net.atos.client.kvk.KVKClientService;
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.shared.ZGWApiService;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.BetrokkeneType;
import net.atos.client.zgw.zrc.model.Rol;
import net.atos.client.zgw.zrc.model.RolMedewerker;
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid;
import net.atos.client.zgw.zrc.model.Status;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.configuratie.ConfiguratieService;
import net.atos.zac.flowable.TaakVariabelenService;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.identity.model.Group;
import net.atos.zac.identity.model.User;
import net.atos.zac.mailtemplates.model.MailLink;
import net.atos.zac.mailtemplates.model.MailTemplateVariabelen;
import net.atos.zac.util.DateTimeConverterUtil;

public class MailTemplateHelper {

    public static final Pattern PTAGS = Pattern.compile("</?p>", Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Inject
    private ConfiguratieService configuratieService;

    @Inject
    private ZRCClientService zrcClientService;

    @Inject
    private ZTCClientService ztcClientService;

    @Inject
    private BRPClientService brpClientService;

    @Inject
    private KVKClientService kvkClientService;

    @Inject
    private ZGWApiService zgwApiService;

    @Inject
    private IdentityService identityService;

    @Inject
    private TaakVariabelenService taakVariabelenService;

    public static String stripParagraphTags(final String onderwerp) {
        // Can't parse HTML with a regular expression, but in this case there will only be bare P-tags.
        return PTAGS.matcher(onderwerp).replaceAll(StringUtils.EMPTY);
    }

    public String resolveVariabelen(final String tekst) {
        String resolvedTekst = tekst;
        resolvedTekst = replaceVariabele(resolvedTekst, GEMEENTE, configuratieService.readGemeenteNaam());
        return resolvedTekst;
    }

    public String resolveVariabelen(final String tekst, final Zaak zaak) {
        String resolvedTekst = tekst;
        if (zaak != null) {
            resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_NUMMER, zaak.getIdentificatie());

            final MailLink link = getLink(zaak);
            resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_URL, link.url);
            resolvedTekst = replaceVariabeleHtml(resolvedTekst, ZAAK_LINK, link.toHtml());

            resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_OMSCHRIJVING, zaak.getOmschrijving());
            resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_TOELICHTING, zaak.getToelichting());
            resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_REGISTRATIEDATUM,
                    zaak.getRegistratiedatum().format(DATE_FORMATTER));
            resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_STARTDATUM,
                    zaak.getStartdatum().format(DATE_FORMATTER));
            resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_STREEFDATUM,
                    Optional.ofNullable(zaak.getEinddatumGepland())
                            .map(datum -> datum.format(DATE_FORMATTER)));
            resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_FATALEDATUM,
                    zaak.getUiterlijkeEinddatumAfdoening().format(DATE_FORMATTER));

            if (resolvedTekst.contains(ZAAK_STATUS.getVariabele())) {
                resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_STATUS,
                        Optional.of(zaak.getStatus())
                                .map(zrcClientService::readStatus)
                                .map(Status::getStatustype)
                                .map(ztcClientService::readStatustype)
                                .map(StatusType::getOmschrijving)
                );
            }

            if (resolvedTekst.contains(ZAAK_TYPE.getVariabele())) {
                resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_TYPE,
                        Optional.of(zaak.getZaaktype())
                                .map(ztcClientService::readZaaktype)
                                .map(ZaakType::getOmschrijving));
            }

            if (resolvedTekst.contains(ZAAK_INITIATOR.getVariabele()) ||
                resolvedTekst.contains(ZAAK_INITIATOR_ADRES.getVariabele())) {
                resolvedTekst = replaceInitiatorVariabelen(resolvedTekst,
                        zgwApiService.findInitiatorForZaak(zaak));
            }

            if (resolvedTekst.contains(ZAAK_BEHANDELAAR_GROEP.getVariabele())) {
                resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_BEHANDELAAR_GROEP,
                        zgwApiService.findGroepForZaak(zaak)
                                .map(RolOrganisatorischeEenheid::getNaam));
            }

            if (resolvedTekst.contains(ZAAK_BEHANDELAAR_MEDEWERKER.getVariabele())) {
                resolvedTekst = replaceVariabele(resolvedTekst, ZAAK_BEHANDELAAR_MEDEWERKER,
                        zgwApiService.findBehandelaarForZaak(zaak)
                                .map(RolMedewerker::getNaam));
            }
        }
        return resolvedTekst;
    }

    public String resolveVariabelen(final String tekst, final TaskInfo taskInfo) {
        String resolvedTekst = tekst;
        if (taskInfo != null) {
            final MailLink link = getLink(taskInfo);
            resolvedTekst = replaceVariabele(resolvedTekst, TAAK_URL, link.url);
            resolvedTekst = replaceVariabeleHtml(resolvedTekst, TAAK_LINK, link.toHtml());

            resolvedTekst = replaceVariabele(resolvedTekst, TAAK_FATALEDATUM,
                    DateTimeConverterUtil.convertToLocalDate(taskInfo.getDueDate())
                            .format(DATE_FORMATTER));

            if (resolvedTekst.contains(TAAK_BEHANDELAAR_GROEP.getVariabele())) {
                resolvedTekst = replaceVariabele(resolvedTekst, TAAK_BEHANDELAAR_GROEP,
                        taskInfo.getIdentityLinks().stream()
                                .filter(identityLinkInfo -> IdentityLinkType.CANDIDATE.equals(
                                        identityLinkInfo.getType()))
                                .findAny()
                                .map(IdentityLinkInfo::getGroupId)
                                .map(identityService::readGroup)
                                .map(Group::getName));
            }

            if (resolvedTekst.contains(TAAK_BEHANDELAAR_MEDEWERKER.getVariabele())) {
                resolvedTekst = replaceVariabele(resolvedTekst, TAAK_BEHANDELAAR_MEDEWERKER,
                        Optional.of(taskInfo.getAssignee())
                                .map(identityService::readUser)
                                .map(User::getFullName));
            }
        }
        return resolvedTekst;
    }

    public String resolveVariabelen(
            final String tekst,
            final EnkelvoudigInformatieObject document
    ) {
        String resolvedTekst = tekst;
        if (document != null) {
            resolvedTekst = replaceVariabele(resolvedTekst, DOCUMENT_TITEL, document.getTitel());

            final MailLink link = getLink(document);
            resolvedTekst = replaceVariabele(resolvedTekst, DOCUMENT_URL, link.url);
            resolvedTekst = replaceVariabeleHtml(resolvedTekst, DOCUMENT_LINK, link.toHtml());
        }
        return resolvedTekst;
    }

    private MailLink getLink(final Zaak zaak) {
        final ZaakType zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype());
        return new MailLink(zaak.getIdentificatie(),
                configuratieService.zaakTonenUrl(zaak.getIdentificatie()),
                "de zaak", "(%s)".formatted(zaaktype.getOmschrijving()));
    }

    private MailLink getLink(final TaskInfo taskInfo) {
        final String zaakIdentificatie = taakVariabelenService.readZaakIdentificatie(taskInfo);
        final String zaaktypeOmschrijving = taakVariabelenService.readZaaktypeOmschrijving(taskInfo);
        return new MailLink(taskInfo.getName(),
                configuratieService.taakTonenUrl(taskInfo.getId()),
                "de taak", "voor zaak %s (%s)".formatted(zaakIdentificatie, zaaktypeOmschrijving));
    }

    private MailLink getLink(final EnkelvoudigInformatieObject document) {
        return new MailLink(
                document.getTitel(),
                configuratieService.informatieobjectTonenUrl(parseUUIDFromResourceURI(document.getUrl())),
                "het document",
                null
        );
    }

    private String replaceInitiatorVariabelen(final String resolvedTekst, final Optional<Rol<?>> initiator) {
        if (initiator.isPresent()) {
            final String identificatie = initiator.get().getIdentificatienummer();
            final BetrokkeneType betrokkene = initiator.get().getBetrokkeneType();
            return switch (betrokkene) {
                case NATUURLIJK_PERSOON -> replaceInitiatorVariabelenPersoon(
                        resolvedTekst,
                        brpClientService.findPersoon(identificatie));
                case VESTIGING -> replaceInitiatorVariabelenResultaatItem(
                        resolvedTekst,
                        kvkClientService.findVestiging(identificatie));
                case NIET_NATUURLIJK_PERSOON -> replaceInitiatorVariabelenResultaatItem(
                        resolvedTekst,
                        kvkClientService.findRechtspersoon(identificatie));
                default -> throw new IllegalStateException(String.format("unexpected betrokkenetype %s", betrokkene));
            };
        }
        return replaceInitiatorVariabelen(resolvedTekst, null, null);
    }

    private static String replaceInitiatorVariabelenPersoon(
            final String resolvedTekst,
            final Optional<Persoon> initiator
    ) {
        return initiator
                .map(persoon -> replaceInitiatorVariabelen(resolvedTekst, persoon.getNaam().getVolledigeNaam(),
                        convertAdres(persoon)))
                .orElseGet(() -> replaceInitiatorVariabelenOnbekend(resolvedTekst));
    }

    private static String replaceInitiatorVariabelenResultaatItem(
            final String resolvedTekst,
            final Optional<ResultaatItem> initiator
    ) {
        return initiator
                .map(item -> replaceInitiatorVariabelen(resolvedTekst, item.getHandelsnaam(), convertAdres(item)))
                .orElseGet(() -> replaceInitiatorVariabelenOnbekend(resolvedTekst));
    }

    private static String convertAdres(final Persoon persoon) {
        return switch (persoon.getVerblijfplaats()) {
            case Adres adres when adres.getVerblijfadres() != null -> convertAdres(adres.getVerblijfadres());
            case VerblijfplaatsBuitenland verblijfplaatsBuitenland when verblijfplaatsBuitenland.getVerblijfadres() != null ->
                    convertAdres(verblijfplaatsBuitenland.getVerblijfadres());
            default -> EMPTY;
        };
    }

    private static String convertAdres(final VerblijfadresBinnenland adres) {
        return "%s %s%s%s, %s %s".formatted(
                defaultIfBlank(adres.getOfficieleStraatnaam(), EMPTY),
                defaultIfNull(adres.getHuisnummer(), EMPTY),
                defaultIfBlank(adres.getHuisletter(), EMPTY),
                defaultIfBlank(adres.getHuisnummertoevoeging(), EMPTY),
                defaultIfBlank(adres.getPostcode(), EMPTY),
                adres.getWoonplaats());
    }

    private static String convertAdres(final VerblijfadresBuitenland adres) {
        return joinNonBlankWith(", ", adres.getRegel1(), adres.getRegel2(), adres.getRegel3());
    }

    private static String convertAdres(final ResultaatItem adres) {
        return "%s %s%s, %s %s".formatted(
                adres.getStraatnaam(),
                defaultIfNull(adres.getHuisnummer(), EMPTY),
                defaultIfBlank(adres.getHuisnummerToevoeging(), EMPTY),
                defaultIfBlank(adres.getPostcode(), EMPTY),
                adres.getPlaats());
    }

    private static String replaceInitiatorVariabelenOnbekend(final String resolvedTekst) {
        return replaceInitiatorVariabelen(resolvedTekst, "Onbekend", null);
    }

    private static String replaceInitiatorVariabelen(
            final String resolvedTekst,
            final String naam,
            final String adres
    ) {
        return replaceVariabele(replaceVariabele(resolvedTekst, ZAAK_INITIATOR, naam), ZAAK_INITIATOR_ADRES, adres);
    }

    private static <T> String replaceVariabele(
            final String target,
            final MailTemplateVariabelen variabele,
            final Optional<T> waarde
    ) {
        return replaceVariabele(target, variabele, waarde.map(T::toString).orElse(null));
    }

    private static String replaceVariabele(
            final String target,
            final MailTemplateVariabelen variabele,
            final String waarde
    ) {
        return replaceVariabeleHtml(target, variabele, StringEscapeUtils.escapeHtml4(waarde));
    }

    // Make sure that what is passed in the html argument is FULLY encoded HTML (no injection vulnerabilities please!)
    private static String replaceVariabeleHtml(
            final String target,
            final MailTemplateVariabelen variabele,
            final String html
    ) {
        return StringUtils.replace(target, variabele.getVariabele(),
                variabele.isResolveVariabeleAlsLegeString() ?
                        defaultIfBlank(html, EMPTY) : html);
    }
}
