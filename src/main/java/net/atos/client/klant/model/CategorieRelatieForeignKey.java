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
import java.time.LocalDate;
import java.util.UUID;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Let op: Dit attribuut is EXPERIMENTEEL.
 */

public class CategorieRelatieForeignKey {

    /**
     * Unieke (technische) identificatiecode van de Categorie Relatie.
     */
    @JsonbProperty("uuid")
    private UUID uuid;

    /**
     * De unieke URL van deze categorie binnen deze API.
     */
    @JsonbProperty("url")
    private URI url;

    /**
     * De naam van de gelinkte categorie.
     */
    @JsonbProperty("categorieNaam")
    private String categorieNaam;

    /**
     * Aanduiding van datum volgens de NEN-ISO 8601:2019-standaard. Een datum wordt genoteerd van het meest naar het minst significante
     * onderdeel. Een voorbeeld: 2022-02-21
     */
    @JsonbProperty("beginDatum")
    private LocalDate beginDatum;

    /**
     * Aanduiding van datum volgens de NEN-ISO 8601:2019-standaard. Een datum wordt genoteerd van het meest naar het minst significante
     * onderdeel. Een voorbeeld: 2022-02-21
     */
    @JsonbProperty("eindDatum")
    private LocalDate eindDatum;

    public CategorieRelatieForeignKey() {
    }

    @JsonbCreator
    public CategorieRelatieForeignKey(
            @JsonbProperty(value = "url") URI url,
            @JsonbProperty(value = "categorieNaam") String categorieNaam
    ) {
        this.url = url;
        this.categorieNaam = categorieNaam;
    }

    /**
     * Unieke (technische) identificatiecode van de Categorie Relatie.
     * 
     * @return uuid
     **/
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Set uuid
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public CategorieRelatieForeignKey uuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    /**
     * De unieke URL van deze categorie binnen deze API.
     * 
     * @return url
     **/
    public URI getUrl() {
        return url;
    }


    /**
     * De naam van de gelinkte categorie.
     * 
     * @return categorieNaam
     **/
    public String getCategorieNaam() {
        return categorieNaam;
    }


    /**
     * Aanduiding van datum volgens de NEN-ISO 8601:2019-standaard. Een datum wordt genoteerd van het meest naar het minst significante
     * onderdeel. Een voorbeeld: 2022-02-21
     * 
     * @return beginDatum
     **/
    public LocalDate getBeginDatum() {
        return beginDatum;
    }

    /**
     * Set beginDatum
     */
    public void setBeginDatum(LocalDate beginDatum) {
        this.beginDatum = beginDatum;
    }

    public CategorieRelatieForeignKey beginDatum(LocalDate beginDatum) {
        this.beginDatum = beginDatum;
        return this;
    }

    /**
     * Aanduiding van datum volgens de NEN-ISO 8601:2019-standaard. Een datum wordt genoteerd van het meest naar het minst significante
     * onderdeel. Een voorbeeld: 2022-02-21
     * 
     * @return eindDatum
     **/
    public LocalDate getEindDatum() {
        return eindDatum;
    }

    /**
     * Set eindDatum
     */
    public void setEindDatum(LocalDate eindDatum) {
        this.eindDatum = eindDatum;
    }

    public CategorieRelatieForeignKey eindDatum(LocalDate eindDatum) {
        this.eindDatum = eindDatum;
        return this;
    }


    /**
     * Create a string representation of this pojo.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CategorieRelatieForeignKey {\n");

        sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
        sb.append("    url: ").append(toIndentedString(url)).append("\n");
        sb.append("    categorieNaam: ").append(toIndentedString(categorieNaam)).append("\n");
        sb.append("    beginDatum: ").append(toIndentedString(beginDatum)).append("\n");
        sb.append("    eindDatum: ").append(toIndentedString(eindDatum)).append("\n");
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
