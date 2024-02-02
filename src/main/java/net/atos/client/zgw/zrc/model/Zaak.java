/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;


import static net.atos.client.zgw.shared.util.DateTimeUtil.DATE_TIME_FORMAT;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbTransient;

import org.apache.commons.collections4.CollectionUtils;

import net.atos.client.zgw.drc.model.EnkelvoudigInformatieObject;
import net.atos.client.zgw.shared.model.Archiefnominatie;
import net.atos.client.zgw.zrc.model.generated.Opschorting;
import net.atos.client.zgw.zrc.model.generated.ZaakKenmerk;

/**
 * Zaak
 */
public class Zaak {

    public static final int OMSCHRIJVING_MAX_LENGTH = 80;

    public static final int TOELICHTING_MAX_LENGTH = 1000;

    /**
     * URL-referentie naar dit object. Dit is de unieke identificatie en locatie van dit object.
     */
    private URI url;

    /**
     * Unieke resource identifier (UUID4)
     */
    private UUID uuid;

    /**
     * De unieke identificatie van de ZAAK binnen de organisatie die verantwoordelijk is voor de behandeling van de ZAAK.
     * maxLength: 40
     */
    private String identificatie;

    /**
     * Het RSIN van de Niet-natuurlijk persoon zijnde de organisatie die de zaak heeft gecreeerd.
     * Dit moet een geldig RSIN zijn van 9 nummers en voldoen aan https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef
     */
    private String bronorganisatie;

    /**
     * Een korte omschrijving van de zaak.
     * maxLength: {@link Zaak#OMSCHRIJVING_MAX_LENGTH}
     */
    private String omschrijving;

    /**
     * Een toelichting op de zaak.
     * maxLength: {@link Zaak#TOELICHTING_MAX_LENGTH}
     */
    private String toelichting;

    /**
     * URL-referentie naar het ZAAKTYPE (in de Catalogi API) in de CATALOGUS waar deze voorkomt
     */
    private URI zaaktype;

    /**
     * De datum waarop de zaakbehandelende organisatie de ZAAK heeft geregistreerd.
     * Indien deze niet opgegeven wordt, wordt de datum van vandaag gebruikt.
     */
    private LocalDate registratiedatum;

    /**
     * Het RSIN van de Niet-natuurlijk persoon zijnde de organisatie die eindverantwoordelijk is voor de behandeling van de zaak.
     * Dit moet een geldig RSIN zijn van 9 nummers en voldoen aan https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef
     */
    private String verantwoordelijkeOrganisatie;

    /**
     * De datum waarop met de uitvoering van de zaak is gestart
     */
    private LocalDate startdatum;

    /**
     * De datum waarop de uitvoering van de zaak afgerond is.
     */
    private LocalDate einddatum;

    /**
     * De datum waarop volgens de planning verwacht wordt dat de zaak afgerond wordt.
     */
    private LocalDate einddatumGepland;

    /**
     * De laatste datum waarop volgens wet- en regelgeving de zaak afgerond dient te zijn.
     */
    private LocalDate uiterlijkeEinddatumAfdoening;

    /**
     * Datum waarop (het starten van) de zaak gepubliceerd is of wordt.
     */
    private LocalDate publicatiedatum;

    /**
     * Het medium waarlangs de aanleiding om een zaak te starten is ontvangen.
     * URL naar een communicatiekanaal in de VNG-Referentielijst van communicatiekanalen.
     */
    private URI communicatiekanaal;

    /**
     * De producten en/of diensten die door de zaak worden voortgebracht.
     * Dit zijn URLs naar de resources zoals die door de producten- en dienstencatalogus-API wordt ontsloten.
     * De producten/diensten moeten bij het zaaktype vermeld zijn.
     */
    private List<URI> productenOfDiensten;

    /**
     * Aanduiding van de mate waarin het zaakdossier van de ZAAK voor de openbaarheid bestemd is.
     * Optioneel - indien geen waarde gekozen wordt, dan wordt de waarde van het ZAAKTYPE overgenomen.
     * Dit betekent dat de API _altijd_ een waarde teruggeeft.
     */
    private EnkelvoudigInformatieObject.VertrouwelijkheidaanduidingEnum vertrouwelijkheidaanduiding;

    /**
     * Indicatie of de, met behandeling van de zaak gemoeide, kosten betaald zijn door de desbetreffende betrokkene
     */
    private Betalingsindicatie betalingsindicatie;

    /**
     * Uitleg bij `betalingsindicatie`.
     */
    private String betalingsindicatieWeergave;

    /**
     * De datum waarop de meest recente betaling is verwerkt van kosten die gemoeid zijn met behandeling van de zaak.
     */
    @JsonbDateFormat(DATE_TIME_FORMAT)
    private ZonedDateTime laatsteBetaaldatum;

    private Geometry zaakgeometrie;

    private Verlenging verlenging;

    private Opschorting opschorting;

    /**
     * URL-referentie naar de categorie in de gehanteerde 'Selectielijst Archiefbescheiden' die, gezien het zaaktype en het resultaattype van de zaak,
     * bepalend is voor het archiefregime van de zaak.
     */
    private URI selectielijstklasse;

    /**
     * "URL-referentie naar de ZAAK, waarom verzocht is door de initiator daarvan,
     * die behandeld wordt in twee of meer separate ZAAKen waarvan de onderhavige ZAAK er 1 is."
     */
    private URI hoofdzaak;

    /**
     * URL-referenties naar deel ZAAKen.
     */
    private Set<URI> deelzaken;

    /**
     * Een lijst van relevante andere zaken.
     */
    private List<RelevanteZaak> relevanteAndereZaken;

    /**
     * Indien geen status bekend is, dan is de waarde 'null'
     */
    private URI status;

    /**
     * Lijst van kenmerken.
     * Merk op dat refereren naar gerelateerde objecten beter kan via `ZaakObject`.
     */
    private List<ZaakKenmerk> kenmerken;

    /**
     * Aanduiding of het zaakdossier blijvend bewaard of na een bepaalde termijn vernietigd moet worden
     */
    private Archiefnominatie archiefnominatie;

    /**
     * Aanduiding of het zaakdossier blijvend bewaard of na een bepaalde termijn vernietigd moet worden.
     */
    private Archiefstatus archiefstatus;

    /**
     * De datum waarop het gearchiveerde zaakdossier vernietigd moet worden dan wel overgebracht moet worden naar een archiefbewaarplaats.
     * Wordt automatisch berekend bij het aanmaken of wijzigen van een RESULTAAT aan deze ZAAK indien nog leeg.
     */
    private LocalDate archiefactiedatum;

    /**
     * URL-referentie naar het RESULTAAT.
     * Indien geen resultaat bekend is, dan is de waarde 'null'
     */
    private URI resultaat;

    public Zaak() {
    }

    /**
     * Constructor with required attributes for POST and PUT requests
     */
    public Zaak(final URI zaaktype, final LocalDate startdatum, final String bronorganisatie, final String verantwoordelijkeOrganisatie) {
        this.bronorganisatie = bronorganisatie;
        this.zaaktype = zaaktype;
        this.verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie;
        this.startdatum = startdatum;
    }

    /**
     * Constructor with required attributes for POST and PUT requests but without startdatum which will be set to today.
     */
    public Zaak(final URI zaaktype, final String bronorganisatie, final String verantwoordelijkeOrganisatie) {
        this.bronorganisatie = bronorganisatie;
        this.zaaktype = zaaktype;
        this.verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie;
        this.startdatum = LocalDate.now();
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(final URI url) {
        this.url = url;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(final UUID uuid) {
        this.uuid = uuid;
    }

    public String getIdentificatie() {
        return identificatie;
    }

    public void setIdentificatie(final String identificatie) {
        this.identificatie = identificatie;
    }

    public String getBronorganisatie() {
        return bronorganisatie;
    }

    public void setBronorganisatie(final String bronorganisatie) {
        this.bronorganisatie = bronorganisatie;
    }

    public URI getZaaktype() {
        return zaaktype;
    }

    public void setZaaktype(final URI zaaktype) {
        this.zaaktype = zaaktype;
    }

    public String getVerantwoordelijkeOrganisatie() {
        return verantwoordelijkeOrganisatie;
    }

    public void setVerantwoordelijkeOrganisatie(final String verantwoordelijkeOrganisatie) {
        this.verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie;
    }

    public LocalDate getStartdatum() {
        return startdatum;
    }

    public void setStartdatum(final LocalDate startdatum) {
        this.startdatum = startdatum;
    }

    public String getOmschrijving() {
        return omschrijving;
    }

    public void setOmschrijving(final String omschrijving) {
        this.omschrijving = omschrijving;
    }

    public String getToelichting() {
        return toelichting;
    }

    public void setToelichting(final String toelichting) {
        this.toelichting = toelichting;
    }

    public Geometry getZaakgeometrie() {
        return zaakgeometrie;
    }

    public void setZaakgeometrie(final Geometry zaakgeometrie) {
        this.zaakgeometrie = zaakgeometrie;
    }

    public List<ZaakKenmerk> getKenmerken() {
        return kenmerken;
    }

    public void setKenmerken(final List<ZaakKenmerk> kenmerken) {
        this.kenmerken = kenmerken;
    }

    public LocalDate getRegistratiedatum() {
        return registratiedatum;
    }

    public void setRegistratiedatum(final LocalDate registratiedatum) {
        this.registratiedatum = registratiedatum;
    }

    public LocalDate getEinddatum() {
        return einddatum;
    }

    public void setEinddatum(final LocalDate einddatum) {
        this.einddatum = einddatum;
    }

    public LocalDate getEinddatumGepland() {
        return einddatumGepland;
    }

    public void setEinddatumGepland(final LocalDate einddatumGepland) {
        this.einddatumGepland = einddatumGepland;
    }

    public LocalDate getUiterlijkeEinddatumAfdoening() {
        return uiterlijkeEinddatumAfdoening;
    }

    public void setUiterlijkeEinddatumAfdoening(final LocalDate uiterlijkeEinddatumAfdoening) {
        this.uiterlijkeEinddatumAfdoening = uiterlijkeEinddatumAfdoening;
    }

    public LocalDate getPublicatiedatum() {
        return publicatiedatum;
    }

    public void setPublicatiedatum(final LocalDate publicatiedatum) {
        this.publicatiedatum = publicatiedatum;
    }

    public URI getCommunicatiekanaal() {
        return communicatiekanaal;
    }

    public void setCommunicatiekanaal(final URI communicatiekanaal) {
        this.communicatiekanaal = communicatiekanaal;
    }

    public List<URI> getProductenOfDiensten() {
        return productenOfDiensten;
    }

    public void setProductenOfDiensten(final List<URI> productenOfDiensten) {
        this.productenOfDiensten = productenOfDiensten;
    }

    public EnkelvoudigInformatieObject.VertrouwelijkheidaanduidingEnum getVertrouwelijkheidaanduiding() {
        return vertrouwelijkheidaanduiding;
    }

    public void setVertrouwelijkheidaanduiding(final EnkelvoudigInformatieObject.VertrouwelijkheidaanduidingEnum vertrouwelijkheidaanduiding) {
        this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding;
    }

    public Betalingsindicatie getBetalingsindicatie() {
        return betalingsindicatie;
    }

    public void setBetalingsindicatie(final Betalingsindicatie betalingsindicatie) {
        this.betalingsindicatie = betalingsindicatie;
    }

    public String getBetalingsindicatieWeergave() {
        return betalingsindicatieWeergave;
    }

    public void setBetalingsindicatieWeergave(final String betalingsindicatieWeergave) {
        this.betalingsindicatieWeergave = betalingsindicatieWeergave;
    }

    public ZonedDateTime getLaatsteBetaaldatum() {
        return laatsteBetaaldatum;
    }

    public void setLaatsteBetaaldatum(final ZonedDateTime laatsteBetaaldatum) {
        this.laatsteBetaaldatum = laatsteBetaaldatum;
    }

    public Verlenging getVerlenging() {
        return verlenging;
    }

    public void setVerlenging(final Verlenging verlenging) {
        this.verlenging = verlenging;
    }

    public Opschorting getOpschorting() {
        return opschorting;
    }

    public void setOpschorting(final Opschorting opschorting) {
        this.opschorting = opschorting;
    }

    public URI getSelectielijstklasse() {
        return selectielijstklasse;
    }

    public void setSelectielijstklasse(final URI selectielijstklasse) {
        this.selectielijstklasse = selectielijstklasse;
    }

    public URI getHoofdzaak() {
        return hoofdzaak;
    }

    public void setHoofdzaak(final URI hoofdzaak) {
        this.hoofdzaak = hoofdzaak;
    }

    public Set<URI> getDeelzaken() {
        return deelzaken;
    }

    public void setDeelzaken(final Set<URI> deelzaken) {
        this.deelzaken = deelzaken;
    }

    public List<RelevanteZaak> getRelevanteAndereZaken() {
        return relevanteAndereZaken;
    }

    public void setRelevanteAndereZaken(final List<RelevanteZaak> relevanteAndereZaken) {
        this.relevanteAndereZaken = relevanteAndereZaken;
    }

    public URI getStatus() {
        return status;
    }

    public void setStatus(final URI status) {
        this.status = status;
    }

    public Archiefnominatie getArchiefnominatie() {
        return archiefnominatie;
    }

    public void setArchiefnominatie(final Archiefnominatie archiefnominatie) {
        this.archiefnominatie = archiefnominatie;
    }

    public Archiefstatus getArchiefstatus() {
        return archiefstatus;
    }

    public void setArchiefstatus(final Archiefstatus archiefstatus) {
        this.archiefstatus = archiefstatus;
    }

    public LocalDate getArchiefactiedatum() {
        return archiefactiedatum;
    }

    public void setArchiefactiedatum(final LocalDate archiefactiedatum) {
        this.archiefactiedatum = archiefactiedatum;
    }

    public URI getResultaat() {
        return resultaat;
    }

    public void setResultaat(final URI resultaat) {
        this.resultaat = resultaat;
    }

    @JsonbTransient
    public boolean isOpen() {
        return archiefnominatie == null;
    }

    @JsonbTransient
    public boolean isOpgeschort() {
        return opschorting != null && opschorting.getIndicatie();
    }

    @JsonbTransient
    public boolean isVerlengd() {
        return verlenging != null && verlenging.getDuur() != null;
    }

    @JsonbTransient
    // Niet hernoemen naar isHoofdzaak omdat anders het 'hoofdzaak' attribuut niet meer wordt omgezet naar JSON
    public boolean is_Hoofdzaak() {
        return CollectionUtils.isNotEmpty(deelzaken);
    }

    @JsonbTransient
    public boolean isDeelzaak() {
        return hoofdzaak != null;
    }

    @Override
    public String toString() {
        return "Zaak{" +
                "url=" + url +
                ", uuid=" + uuid +
                ", identificatie='" + identificatie + '\'' +
                ", bronorganisatie='" + bronorganisatie + '\'' +
                ", omschrijving='" + omschrijving + '\'' +
                ", toelichting='" + toelichting + '\'' +
                ", zaaktype=" + zaaktype +
                ", registratiedatum=" + registratiedatum +
                ", verantwoordelijkeOrganisatie='" + verantwoordelijkeOrganisatie + '\'' +
                ", startdatum=" + startdatum +
                ", einddatum=" + einddatum +
                ", einddatumGepland=" + einddatumGepland +
                ", uiterlijkeEinddatumAfdoening=" + uiterlijkeEinddatumAfdoening +
                ", publicatiedatum=" + publicatiedatum +
                ", communicatiekanaal=" + communicatiekanaal +
                ", productenOfDiensten=" + productenOfDiensten +
                ", vertrouwelijkheidaanduiding=" + vertrouwelijkheidaanduiding +
                ", betalingsindicatie=" + betalingsindicatie +
                ", betalingsindicatieWeergave='" + betalingsindicatieWeergave + '\'' +
                ", laatsteBetaaldatum=" + laatsteBetaaldatum +
                ", zaakgeometrie=" + zaakgeometrie +
                ", verlenging=" + verlenging +
                ", opschorting=" + opschorting +
                ", selectielijstklasse=" + selectielijstklasse +
                ", hoofdzaak=" + hoofdzaak +
                ", deelzaken=" + deelzaken +
                ", relevanteAndereZaken=" + relevanteAndereZaken +
                ", status=" + status +
                ", kenmerken=" + kenmerken +
                ", archiefnominatie=" + archiefnominatie +
                ", archiefstatus=" + archiefstatus +
                ", archiefactiedatum=" + archiefactiedatum +
                ", resultaat=" + resultaat +
                '}';
    }
}
