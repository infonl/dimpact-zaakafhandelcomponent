/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * klantinteracties
 * **Warning: Difference between `PUT` and `PATCH`** Both `PUT` and `PATCH` methods can be used to update the fields in a resource, but
 * there is a key difference in how they handle required fields: * The `PUT` method requires you to specify **all mandatory fields** when
 * updating a resource. If any mandatory field is missing, the update will fail. Optional fields are left unchanged if they are not
 * specified. * The `PATCH` method, on the other hand, allows you to update only the fields you specify. Some mandatory fields can be left
 * out, and the resource will only be updated with the provided data, leaving other fields unchanged.
 *
 * The version of the OpenAPI document: 0.1.2 (1)
 * Contact: standaarden.ondersteuning@vng.nl
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package net.atos.client.klant.model;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;


/**
 * Set gegevensgroepdata from validated nested data. Usage: include the mixin on the ModelSerializer that has gegevensgroepen.
 */

public class PatchedOnderwerpobject {

    /**
     * Unieke (technische) identificatiecode van het onderwerpdeel.
     */
    @JsonbProperty("uuid")
    protected UUID uuid;

    /**
     * De unieke URL van dit klantcontact binnen deze API.
     */
    @JsonbProperty("url")
    protected URI url;

    /**
     * 'Klantcontact' ging over 'Onderwerpobject'
     */
    @JsonbProperty("klantcontact")
    protected KlantcontactForeignKey klantcontact;

    /**
     * 'Onderwerpobject' was 'Klantcontact'
     */
    @JsonbProperty("wasKlantcontact")
    protected KlantcontactForeignKey wasKlantcontact;

    /**
     * Gegevens die een onderwerpobject in een extern register uniek identificeren.
     */
    @JsonbProperty("onderwerpobjectidentificator")
    protected Onderwerpobjectidentificator onderwerpobjectidentificator;

    public PatchedOnderwerpobject() {
    }

    @JsonbCreator
    public PatchedOnderwerpobject(
            @JsonbProperty(value = "uuid", nillable = true) UUID uuid,
            @JsonbProperty(value = "url", nillable = true) URI url
    ) {
        this.uuid = uuid;
        this.url = url;
    }

    /**
     * Unieke (technische) identificatiecode van het onderwerpdeel.
     * 
     * @return uuid
     **/
    public UUID getUuid() {
        return uuid;
    }


    /**
     * De unieke URL van dit klantcontact binnen deze API.
     * 
     * @return url
     **/
    public URI getUrl() {
        return url;
    }


    /**
     * &#39;Klantcontact&#39; ging over &#39;Onderwerpobject&#39;
     * 
     * @return klantcontact
     **/
    public KlantcontactForeignKey getKlantcontact() {
        return klantcontact;
    }

    /**
     * Set klantcontact
     */
    public void setKlantcontact(KlantcontactForeignKey klantcontact) {
        this.klantcontact = klantcontact;
    }

    public PatchedOnderwerpobject klantcontact(KlantcontactForeignKey klantcontact) {
        this.klantcontact = klantcontact;
        return this;
    }

    /**
     * &#39;Onderwerpobject&#39; was &#39;Klantcontact&#39;
     * 
     * @return wasKlantcontact
     **/
    public KlantcontactForeignKey getWasKlantcontact() {
        return wasKlantcontact;
    }

    /**
     * Set wasKlantcontact
     */
    public void setWasKlantcontact(KlantcontactForeignKey wasKlantcontact) {
        this.wasKlantcontact = wasKlantcontact;
    }

    public PatchedOnderwerpobject wasKlantcontact(KlantcontactForeignKey wasKlantcontact) {
        this.wasKlantcontact = wasKlantcontact;
        return this;
    }

    /**
     * Gegevens die een onderwerpobject in een extern register uniek identificeren.
     * 
     * @return onderwerpobjectidentificator
     **/
    public Onderwerpobjectidentificator getOnderwerpobjectidentificator() {
        return onderwerpobjectidentificator;
    }

    /**
     * Set onderwerpobjectidentificator
     */
    public void setOnderwerpobjectidentificator(Onderwerpobjectidentificator onderwerpobjectidentificator) {
        this.onderwerpobjectidentificator = onderwerpobjectidentificator;
    }

    public PatchedOnderwerpobject onderwerpobjectidentificator(Onderwerpobjectidentificator onderwerpobjectidentificator) {
        this.onderwerpobjectidentificator = onderwerpobjectidentificator;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PatchedOnderwerpobject patchedOnderwerpobject = (PatchedOnderwerpobject) o;
        return Objects.equals(this.uuid, patchedOnderwerpobject.uuid) &&
               Objects.equals(this.url, patchedOnderwerpobject.url) &&
               Objects.equals(this.klantcontact, patchedOnderwerpobject.klantcontact) &&
               Objects.equals(this.wasKlantcontact, patchedOnderwerpobject.wasKlantcontact) &&
               Objects.equals(this.onderwerpobjectidentificator, patchedOnderwerpobject.onderwerpobjectidentificator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, url, klantcontact, wasKlantcontact, onderwerpobjectidentificator);
    }

    /**
     * Create a string representation of this pojo.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PatchedOnderwerpobject {\n");

        sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
        sb.append("    url: ").append(toIndentedString(url)).append("\n");
        sb.append("    klantcontact: ").append(toIndentedString(klantcontact)).append("\n");
        sb.append("    wasKlantcontact: ").append(toIndentedString(wasKlantcontact)).append("\n");
        sb.append("    onderwerpobjectidentificator: ").append(toIndentedString(onderwerpobjectidentificator)).append("\n");
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
