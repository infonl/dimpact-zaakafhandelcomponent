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

import java.util.Objects;

import jakarta.json.bind.annotation.JsonbProperty;


public class MaakKlantcontact {

    @JsonbProperty("klantcontact")
    protected Klantcontact klantcontact;

    @JsonbProperty("betrokkene")
    protected BetrokkeneKlantcontactReadOnly betrokkene;

    @JsonbProperty("onderwerpobject")
    protected OnderwerpobjectKlantcontactReadOnly onderwerpobject;

    /**
     * Get klantcontact
     * 
     * @return klantcontact
     **/
    public Klantcontact getKlantcontact() {
        return klantcontact;
    }

    /**
     * Set klantcontact
     */
    public void setKlantcontact(Klantcontact klantcontact) {
        this.klantcontact = klantcontact;
    }

    public MaakKlantcontact klantcontact(Klantcontact klantcontact) {
        this.klantcontact = klantcontact;
        return this;
    }

    /**
     * Get betrokkene
     * 
     * @return betrokkene
     **/
    public BetrokkeneKlantcontactReadOnly getBetrokkene() {
        return betrokkene;
    }

    /**
     * Set betrokkene
     */
    public void setBetrokkene(BetrokkeneKlantcontactReadOnly betrokkene) {
        this.betrokkene = betrokkene;
    }

    public MaakKlantcontact betrokkene(BetrokkeneKlantcontactReadOnly betrokkene) {
        this.betrokkene = betrokkene;
        return this;
    }

    /**
     * Get onderwerpobject
     * 
     * @return onderwerpobject
     **/
    public OnderwerpobjectKlantcontactReadOnly getOnderwerpobject() {
        return onderwerpobject;
    }

    /**
     * Set onderwerpobject
     */
    public void setOnderwerpobject(OnderwerpobjectKlantcontactReadOnly onderwerpobject) {
        this.onderwerpobject = onderwerpobject;
    }

    public MaakKlantcontact onderwerpobject(OnderwerpobjectKlantcontactReadOnly onderwerpobject) {
        this.onderwerpobject = onderwerpobject;
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
        MaakKlantcontact maakKlantcontact = (MaakKlantcontact) o;
        return Objects.equals(this.klantcontact, maakKlantcontact.klantcontact) &&
               Objects.equals(this.betrokkene, maakKlantcontact.betrokkene) &&
               Objects.equals(this.onderwerpobject, maakKlantcontact.onderwerpobject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(klantcontact, betrokkene, onderwerpobject);
    }

    /**
     * Create a string representation of this pojo.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MaakKlantcontact {\n");

        sb.append("    klantcontact: ").append(toIndentedString(klantcontact)).append("\n");
        sb.append("    betrokkene: ").append(toIndentedString(betrokkene)).append("\n");
        sb.append("    onderwerpobject: ").append(toIndentedString(onderwerpobject)).append("\n");
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
