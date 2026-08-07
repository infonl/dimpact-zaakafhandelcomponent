/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.ws.rs.QueryParam
import net.atos.client.zgw.shared.model.AbstractListParameters
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.ArchiefstatusEnum
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import java.net.URI
import java.time.LocalDate

/**
 * Parameters to retrieve lists of zaken.
 */
class ZaakListParameters : AbstractListParameters() {
    /**
     * De unieke identificatie van de ZAAK binnen de organisatie die verantwoordelijk is voor de behandeling van de ZAAK.
     */
    @QueryParam("identificatie")
    var identificatie: String? = null

    /**
     * Het RSIN van de Niet-natuurlijk persoon zijnde de organisatie die de zaak heeft gecreeerd.
     * Dit moet een geldig RSIN zijn van 9 nummers en voldoen aan https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef
     */
    @QueryParam("bronorganisatie")
    var bronorganisatie: String? = null

    /**
     * URL-referentie naar het ZAAKTYPE (in de Catalogi API) in de CATALOGUS waar deze voorkomt
     */
    @QueryParam("zaaktype")
    var zaaktype: URI? = null

    /**
     * Aanduiding of het zaakdossier blijvend bewaard of na een bepaalde termijn vernietigd moet worden.
     */
    private var archiefnominatieValue: ArchiefnominatieEnum? = null

    private var archiefnominatieInValue: Set<ArchiefnominatieEnum>? = null

    /**
     * De datum waarop het gearchiveerde zaakdossier vernietigd moet worden dan wel overgebracht moet worden naar een archiefbewaarplaats.
     * Wordt automatisch berekend bij het aanmaken of wijzigen van een RESULTAAT aan deze ZAAK indien nog leeg.
     */
    @QueryParam("archiefactiedatum")
    var archiefactiedatum: LocalDate? = null

    @QueryParam("archiefactiedatum__lt")
    var archiefactiedatumLessThan: LocalDate? = null

    @QueryParam("archiefactiedatum__gt")
    var archiefactiedatumGreaterThan: LocalDate? = null

    /**
     * Aanduiding of het zaakdossier blijvend bewaard of na een bepaalde termijn vernietigd moet worden.
     */
    private var archiefstatusValue: ArchiefstatusEnum? = null

    private var archiefstatusInValue: Set<ArchiefstatusEnum>? = null

    /**
     * De datum waarop met de uitvoering van de zaak is gestart
     */
    @QueryParam("startdatum")
    var startdatum: LocalDate? = null

    @QueryParam("startdatum__gt")
    var startdatumGreaterThan: LocalDate? = null

    @QueryParam("startdatum__gte")
    var startdatumGreaterThanOrEqual: LocalDate? = null

    @QueryParam("startdatum__lt")
    var startdatumLessThan: LocalDate? = null

    @QueryParam("startdatum__lte")
    var startdatumLessThanOrEqual: LocalDate? = null

    /**
     * Type van de `betrokkene`
     */
    private var rolBetrokkeneTypeValue: BetrokkeneTypeEnum? = null

    /**
     * URL-referentie naar een betrokkene gerelateerd aan de ZAAK.
     */
    @QueryParam("rol__betrokkene")
    var rolBetrokkene: URI? = null

    /**
     * Algemeen gehanteerde benaming van de aard van de ROL, afgeleid uit het ROLTYPE.
     */
    private var rolOmschrijvingGeneriekValue: OmschrijvingGeneriekEnum? = null

    /**
     * Zaken met een vertrouwelijkheidaanduiding die beperkter is dan de aangegeven aanduiding worden uit de resultaten gefiltered.
     */
    private var maximaleVertrouwelijkheidaanduidingValue: VertrouwelijkheidaanduidingEnum? = null

    /**
     * Het burgerservicenummer, bedoeld in artikel 1.1 van de Wet algemene bepalingen burgerservicenummer.
     */
    @QueryParam("rol__betrokkeneIdentificatie__natuurlijkPersoon__inpBsn")
    var rolBetrokkeneIdentificatieNatuurlijkPersoonInpBsn: String? = null

    /**
     * Een korte unieke aanduiding van de MEDEWERKER.
     */
    @QueryParam("rol__betrokkeneIdentificatie__medewerker__identificatie")
    var rolBetrokkeneIdentificatieMedewerkerIdentificatie: String? = null

    /**
     * Een korte identificatie van de organisatorische eenheid.
     */
    @QueryParam("rol__betrokkeneIdentificatie__organisatorischeEenheid__identificatie")
    var rolBetrokkeneIdentificatieOrganisatorischeEenheidIdentificatie: String? = null

    /**
     * Which field to use when ordering the results,
     * [field] voor asc en -[field] voor desc
     */
    @QueryParam("ordering")
    var ordering: String? = null

    @get:QueryParam("archiefnominatie")
    val archiefnominatie: String?
        get() = archiefnominatieValue?.toString()

    fun setArchiefnominatie(archiefnominatie: ArchiefnominatieEnum) {
        this.archiefnominatieValue = archiefnominatie
    }

    @get:QueryParam("archiefnominatie__in")
    val archiefnominatieIn: String?
        get() = archiefnominatieInValue?.takeIf { it.isNotEmpty() }?.joinToString(",")

    fun setArchiefnominatieIn(archiefnominatieIn: Set<ArchiefnominatieEnum>?) {
        this.archiefnominatieInValue = archiefnominatieIn
    }

    @get:QueryParam("archiefstatus")
    val archiefstatus: String?
        get() = archiefstatusValue?.toString()

    fun setArchiefstatus(archiefstatus: ArchiefstatusEnum) {
        this.archiefstatusValue = archiefstatus
    }

    @get:QueryParam("archiefstatus__in")
    val archiefstatusIn: String?
        get() = archiefstatusInValue?.takeIf { it.isNotEmpty() }?.joinToString(",")

    fun setArchiefstatusIn(archiefstatusIn: Set<ArchiefstatusEnum>?) {
        this.archiefstatusInValue = archiefstatusIn
    }

    @get:QueryParam("rol__betrokkeneType")
    val rolBetrokkeneType: String?
        get() = rolBetrokkeneTypeValue?.toString()

    fun setRolBetrokkeneType(rolBetrokkeneType: BetrokkeneTypeEnum) {
        this.rolBetrokkeneTypeValue = rolBetrokkeneType
    }

    @get:QueryParam("rol__omschrijvingGeneriek")
    val rolOmschrijvingGeneriek: String?
        get() = rolOmschrijvingGeneriekValue?.name?.lowercase()

    fun setRolOmschrijvingGeneriek(rolOmschrijvingGeneriek: OmschrijvingGeneriekEnum) {
        this.rolOmschrijvingGeneriekValue = rolOmschrijvingGeneriek
    }

    @get:QueryParam("maximaleVertrouwelijkheidaanduiding")
    val maximaleVertrouwelijkheidaanduiding: String?
        get() = maximaleVertrouwelijkheidaanduidingValue?.toString()

    fun setMaximaleVertrouwelijkheidaanduiding(maximaleVertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum) {
        this.maximaleVertrouwelijkheidaanduidingValue = maximaleVertrouwelijkheidaanduiding
    }
}
