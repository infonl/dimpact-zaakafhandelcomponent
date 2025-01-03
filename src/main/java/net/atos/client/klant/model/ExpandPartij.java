/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * klantinteracties
 * Description WIP.
 *
 * The version of the OpenAPI document: 0.0.3
 * Contact: standaarden.ondersteuning@vng.nl
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package net.atos.client.klant.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;


public class ExpandPartij {

    /**
     * Unieke (technische) identificatiecode van de partij.
     */
    @JsonbProperty("uuid")
    private UUID uuid;

    /**
     * De unieke URL van deze partij binnen deze API.
     */
    @JsonbProperty("url")
    private URI url;

    /**
     * Uniek identificerend nummer dat tijdens communicatie tussen mensen kan worden gebruikt om de specifieke partij aan te duiden.
     */
    @JsonbProperty("nummer")
    private String nummer;

    /**
     * Mededelingen, aantekeningen of bijzonderheden over de partij, bedoeld voor intern gebruik.
     */
    @JsonbProperty("interneNotitie")
    private String interneNotitie;

    /**
     * Betrokkene bij klantcontact die een partij was.
     */
    @JsonbProperty("betrokkenen")
    private List<BetrokkeneForeignKey> betrokkenen = new ArrayList<>();

    /**
     * De Categorie relaties van een partij: Let op: Dit attribuut is EXPERIMENTEEL.
     */
    @JsonbProperty("categorieRelaties")
    private List<CategorieRelatieForeignKey> categorieRelaties = new ArrayList<>();

    /**
     * Digitaal adresen dat een partij verstrekte voor gebruik bij toekomstig contact met de gemeente.
     */
    @JsonbProperty("digitaleAdressen")
    private List<DigitaalAdresForeignKey> digitaleAdressen;

    /**
     * Digitaal adres waarop een partij bij voorkeur door de gemeente benaderd wordt.
     */
    @JsonbProperty("voorkeursDigitaalAdres")
    private DigitaalAdresForeignKey voorkeursDigitaalAdres;

    /**
     * Partijen die een andere partijen vertegenwoordigden.
     */
    @JsonbProperty("vertegenwoordigden")
    private List<PartijForeignKey> vertegenwoordigden = new ArrayList<>();

    /**
     * Rekeningnummers van een partij
     */
    @JsonbProperty("rekeningnummers")
    private List<RekeningnummerForeignKey> rekeningnummers;

    /**
     * Rekeningsnummer die een partij bij voorkeur gebruikt.
     */
    @JsonbProperty("voorkeursRekeningnummer")
    private RekeningnummerForeignKey voorkeursRekeningnummer;

    /**
     * Partij-identificatoren die hoorde bij deze partij.
     */
    @JsonbProperty("partijIdentificatoren")
    private List<PartijIdentificatorForeignkey> partijIdentificatoren = new ArrayList<>();

    /**
     * Geeft aan van welke specifieke soort partij sprake is.
     */
    @JsonbProperty("soortPartij")
    private SoortPartijEnum soortPartij;

    /**
     * Geeft aan of de verstrekker van partijgegevens heeft aangegeven dat deze gegevens als geheim beschouwd moeten worden.
     */
    @JsonbProperty("indicatieGeheimhouding")
    private Boolean indicatieGeheimhouding;

    /**
     * Taal, in ISO 639-2/B formaat, waarin de partij bij voorkeur contact heeft met de gemeente. Voorbeeld: nld. Zie:
     * https://www.iso.org/standard/4767.html
     */
    @JsonbProperty("voorkeurstaal")
    private String voorkeurstaal;

    /**
     * Geeft aan of de contactgegevens van de partij nog gebruikt morgen worden om contact op te nemen. Gegevens van niet-actieve partijen
     * mogen hiervoor niet worden gebruikt.
     */
    @JsonbProperty("indicatieActief")
    private Boolean indicatieActief;

    /**
     * Adres waarop de partij door gemeente bezocht wil worden. Dit mag afwijken van voor de verstrekker eventueel in een basisregistratie
     * bekende adressen.
     */
    @JsonbProperty("bezoekadres")
    private PartijBezoekadres bezoekadres;

    /**
     * Adres waarop de partij post van de gemeente wil ontvangen. Dit mag afwijken van voor de verstrekker eventueel in een basisregistratie
     * bekende adressen.
     */
    @JsonbProperty("correspondentieadres")
    private PartijCorrespondentieadres correspondentieadres;

    @JsonbProperty("_expand")
    private ExpandPartijAllOfExpand expand;

    public ExpandPartij() {
    }

    @JsonbCreator
    public ExpandPartij(
            @JsonbProperty(value = "uuid") UUID uuid,
            @JsonbProperty(value = "url") URI url,
            @JsonbProperty(value = "betrokkenen") List<BetrokkeneForeignKey> betrokkenen,
            @JsonbProperty(value = "categorieRelaties") List<CategorieRelatieForeignKey> categorieRelaties,
            @JsonbProperty(value = "vertegenwoordigden") List<PartijForeignKey> vertegenwoordigden,
            @JsonbProperty(value = "partijIdentificatoren") List<PartijIdentificatorForeignkey> partijIdentificatoren
    ) {
        this.uuid = uuid;
        this.url = url;
        this.betrokkenen = betrokkenen;
        this.categorieRelaties = categorieRelaties;
        this.vertegenwoordigden = vertegenwoordigden;
        this.partijIdentificatoren = partijIdentificatoren;
    }

    /**
     * Unieke (technische) identificatiecode van de partij.
     * 
     * @return uuid
     **/
    public UUID getUuid() {
        return uuid;
    }


    /**
     * De unieke URL van deze partij binnen deze API.
     * 
     * @return url
     **/
    public URI getUrl() {
        return url;
    }


    /**
     * Uniek identificerend nummer dat tijdens communicatie tussen mensen kan worden gebruikt om de specifieke partij aan te duiden.
     * 
     * @return nummer
     **/
    public String getNummer() {
        return nummer;
    }

    /**
     * Set nummer
     */
    public void setNummer(String nummer) {
        this.nummer = nummer;
    }

    public ExpandPartij nummer(String nummer) {
        this.nummer = nummer;
        return this;
    }

    /**
     * Mededelingen, aantekeningen of bijzonderheden over de partij, bedoeld voor intern gebruik.
     * 
     * @return interneNotitie
     **/
    public String getInterneNotitie() {
        return interneNotitie;
    }

    /**
     * Set interneNotitie
     */
    public void setInterneNotitie(String interneNotitie) {
        this.interneNotitie = interneNotitie;
    }

    public ExpandPartij interneNotitie(String interneNotitie) {
        this.interneNotitie = interneNotitie;
        return this;
    }

    /**
     * Betrokkene bij klantcontact die een partij was.
     * 
     * @return betrokkenen
     **/
    public List<BetrokkeneForeignKey> getBetrokkenen() {
        return betrokkenen;
    }


    /**
     * De Categorie relaties van een partij: Let op: Dit attribuut is EXPERIMENTEEL.
     * 
     * @return categorieRelaties
     **/
    public List<CategorieRelatieForeignKey> getCategorieRelaties() {
        return categorieRelaties;
    }


    /**
     * Digitaal adresen dat een partij verstrekte voor gebruik bij toekomstig contact met de gemeente.
     * 
     * @return digitaleAdressen
     **/
    public List<DigitaalAdresForeignKey> getDigitaleAdressen() {
        return digitaleAdressen;
    }

    /**
     * Set digitaleAdressen
     */
    public void setDigitaleAdressen(List<DigitaalAdresForeignKey> digitaleAdressen) {
        this.digitaleAdressen = digitaleAdressen;
    }

    public ExpandPartij digitaleAdressen(List<DigitaalAdresForeignKey> digitaleAdressen) {
        this.digitaleAdressen = digitaleAdressen;
        return this;
    }

    public ExpandPartij addDigitaleAdressenItem(DigitaalAdresForeignKey digitaleAdressenItem) {
        if (this.digitaleAdressen == null) {
            this.digitaleAdressen = new ArrayList<>();
        }
        this.digitaleAdressen.add(digitaleAdressenItem);
        return this;
    }

    /**
     * Digitaal adres waarop een partij bij voorkeur door de gemeente benaderd wordt.
     * 
     * @return voorkeursDigitaalAdres
     **/
    public DigitaalAdresForeignKey getVoorkeursDigitaalAdres() {
        return voorkeursDigitaalAdres;
    }

    /**
     * Set voorkeursDigitaalAdres
     */
    public void setVoorkeursDigitaalAdres(DigitaalAdresForeignKey voorkeursDigitaalAdres) {
        this.voorkeursDigitaalAdres = voorkeursDigitaalAdres;
    }

    public ExpandPartij voorkeursDigitaalAdres(DigitaalAdresForeignKey voorkeursDigitaalAdres) {
        this.voorkeursDigitaalAdres = voorkeursDigitaalAdres;
        return this;
    }

    /**
     * Partijen die een andere partijen vertegenwoordigden.
     * 
     * @return vertegenwoordigden
     **/
    public List<PartijForeignKey> getVertegenwoordigden() {
        return vertegenwoordigden;
    }


    /**
     * Rekeningnummers van een partij
     * 
     * @return rekeningnummers
     **/
    public List<RekeningnummerForeignKey> getRekeningnummers() {
        return rekeningnummers;
    }

    /**
     * Set rekeningnummers
     */
    public void setRekeningnummers(List<RekeningnummerForeignKey> rekeningnummers) {
        this.rekeningnummers = rekeningnummers;
    }

    public ExpandPartij rekeningnummers(List<RekeningnummerForeignKey> rekeningnummers) {
        this.rekeningnummers = rekeningnummers;
        return this;
    }

    public ExpandPartij addRekeningnummersItem(RekeningnummerForeignKey rekeningnummersItem) {
        if (this.rekeningnummers == null) {
            this.rekeningnummers = new ArrayList<>();
        }
        this.rekeningnummers.add(rekeningnummersItem);
        return this;
    }

    /**
     * Rekeningsnummer die een partij bij voorkeur gebruikt.
     * 
     * @return voorkeursRekeningnummer
     **/
    public RekeningnummerForeignKey getVoorkeursRekeningnummer() {
        return voorkeursRekeningnummer;
    }

    /**
     * Set voorkeursRekeningnummer
     */
    public void setVoorkeursRekeningnummer(RekeningnummerForeignKey voorkeursRekeningnummer) {
        this.voorkeursRekeningnummer = voorkeursRekeningnummer;
    }

    public ExpandPartij voorkeursRekeningnummer(RekeningnummerForeignKey voorkeursRekeningnummer) {
        this.voorkeursRekeningnummer = voorkeursRekeningnummer;
        return this;
    }

    /**
     * Partij-identificatoren die hoorde bij deze partij.
     * 
     * @return partijIdentificatoren
     **/
    public List<PartijIdentificatorForeignkey> getPartijIdentificatoren() {
        return partijIdentificatoren;
    }


    /**
     * Geeft aan van welke specifieke soort partij sprake is.
     * 
     * @return soortPartij
     **/
    public SoortPartijEnum getSoortPartij() {
        return soortPartij;
    }

    /**
     * Set soortPartij
     */
    public void setSoortPartij(SoortPartijEnum soortPartij) {
        this.soortPartij = soortPartij;
    }

    public ExpandPartij soortPartij(SoortPartijEnum soortPartij) {
        this.soortPartij = soortPartij;
        return this;
    }

    /**
     * Geeft aan of de verstrekker van partijgegevens heeft aangegeven dat deze gegevens als geheim beschouwd moeten worden.
     * 
     * @return indicatieGeheimhouding
     **/
    public Boolean getIndicatieGeheimhouding() {
        return indicatieGeheimhouding;
    }

    /**
     * Set indicatieGeheimhouding
     */
    public void setIndicatieGeheimhouding(Boolean indicatieGeheimhouding) {
        this.indicatieGeheimhouding = indicatieGeheimhouding;
    }

    public ExpandPartij indicatieGeheimhouding(Boolean indicatieGeheimhouding) {
        this.indicatieGeheimhouding = indicatieGeheimhouding;
        return this;
    }

    /**
     * Taal, in ISO 639-2/B formaat, waarin de partij bij voorkeur contact heeft met de gemeente. Voorbeeld: nld. Zie:
     * https://www.iso.org/standard/4767.html
     * 
     * @return voorkeurstaal
     **/
    public String getVoorkeurstaal() {
        return voorkeurstaal;
    }

    /**
     * Set voorkeurstaal
     */
    public void setVoorkeurstaal(String voorkeurstaal) {
        this.voorkeurstaal = voorkeurstaal;
    }

    public ExpandPartij voorkeurstaal(String voorkeurstaal) {
        this.voorkeurstaal = voorkeurstaal;
        return this;
    }

    /**
     * Geeft aan of de contactgegevens van de partij nog gebruikt morgen worden om contact op te nemen. Gegevens van niet-actieve partijen
     * mogen hiervoor niet worden gebruikt.
     * 
     * @return indicatieActief
     **/
    public Boolean getIndicatieActief() {
        return indicatieActief;
    }

    /**
     * Set indicatieActief
     */
    public void setIndicatieActief(Boolean indicatieActief) {
        this.indicatieActief = indicatieActief;
    }

    public ExpandPartij indicatieActief(Boolean indicatieActief) {
        this.indicatieActief = indicatieActief;
        return this;
    }

    /**
     * Adres waarop de partij door gemeente bezocht wil worden. Dit mag afwijken van voor de verstrekker eventueel in een basisregistratie
     * bekende adressen.
     * 
     * @return bezoekadres
     **/
    public PartijBezoekadres getBezoekadres() {
        return bezoekadres;
    }

    /**
     * Set bezoekadres
     */
    public void setBezoekadres(PartijBezoekadres bezoekadres) {
        this.bezoekadres = bezoekadres;
    }

    public ExpandPartij bezoekadres(PartijBezoekadres bezoekadres) {
        this.bezoekadres = bezoekadres;
        return this;
    }

    /**
     * Adres waarop de partij post van de gemeente wil ontvangen. Dit mag afwijken van voor de verstrekker eventueel in een basisregistratie
     * bekende adressen.
     * 
     * @return correspondentieadres
     **/
    public PartijCorrespondentieadres getCorrespondentieadres() {
        return correspondentieadres;
    }

    /**
     * Set correspondentieadres
     */
    public void setCorrespondentieadres(PartijCorrespondentieadres correspondentieadres) {
        this.correspondentieadres = correspondentieadres;
    }

    public ExpandPartij correspondentieadres(PartijCorrespondentieadres correspondentieadres) {
        this.correspondentieadres = correspondentieadres;
        return this;
    }

    /**
     * Get expand
     * 
     * @return expand
     **/
    public ExpandPartijAllOfExpand getExpand() {
        return expand;
    }

    /**
     * Set expand
     */
    public void setExpand(ExpandPartijAllOfExpand expand) {
        this.expand = expand;
    }

    public ExpandPartij expand(ExpandPartijAllOfExpand expand) {
        this.expand = expand;
        return this;
    }


    /**
     * Create a string representation of this pojo.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExpandPartij {\n");

        sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
        sb.append("    url: ").append(toIndentedString(url)).append("\n");
        sb.append("    nummer: ").append(toIndentedString(nummer)).append("\n");
        sb.append("    interneNotitie: ").append(toIndentedString(interneNotitie)).append("\n");
        sb.append("    betrokkenen: ").append(toIndentedString(betrokkenen)).append("\n");
        sb.append("    categorieRelaties: ").append(toIndentedString(categorieRelaties)).append("\n");
        sb.append("    digitaleAdressen: ").append(toIndentedString(digitaleAdressen)).append("\n");
        sb.append("    voorkeursDigitaalAdres: ").append(toIndentedString(voorkeursDigitaalAdres)).append("\n");
        sb.append("    vertegenwoordigden: ").append(toIndentedString(vertegenwoordigden)).append("\n");
        sb.append("    rekeningnummers: ").append(toIndentedString(rekeningnummers)).append("\n");
        sb.append("    voorkeursRekeningnummer: ").append(toIndentedString(voorkeursRekeningnummer)).append("\n");
        sb.append("    partijIdentificatoren: ").append(toIndentedString(partijIdentificatoren)).append("\n");
        sb.append("    soortPartij: ").append(toIndentedString(soortPartij)).append("\n");
        sb.append("    indicatieGeheimhouding: ").append(toIndentedString(indicatieGeheimhouding)).append("\n");
        sb.append("    voorkeurstaal: ").append(toIndentedString(voorkeurstaal)).append("\n");
        sb.append("    indicatieActief: ").append(toIndentedString(indicatieActief)).append("\n");
        sb.append("    bezoekadres: ").append(toIndentedString(bezoekadres)).append("\n");
        sb.append("    correspondentieadres: ").append(toIndentedString(correspondentieadres)).append("\n");
        sb.append("    expand: ").append(toIndentedString(expand)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private static String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
