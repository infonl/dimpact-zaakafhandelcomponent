/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model.zaakobjecten;

import java.net.URI;

import jakarta.ws.rs.QueryParam;

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum;


public class ZaakobjectListParameters {

    /**
     * URL-referentie naar de ZAAK
     */
    @QueryParam("zaak")
    private URI zaak;

    /**
     * URL-referentie naar de resource die het OBJECT beschrijft
     */
    @QueryParam("object")
    private URI object;

    /**
     * Beschrijft het type OBJECT gerelateerd aan de ZAAK. Als er geen passend type is, dan moet het type worden opgegeven onder
     * `objectTypeOverige`
     */
    @QueryParam("objectType")
    private String objectType;

    /**
     * Een pagina binnen de gepagineerde set resultaten
     */
    @QueryParam("page")
    private Integer page;

    public URI getZaak() {
        return zaak;
    }

    public void setZaak(final URI zaak) {
        this.zaak = zaak;
    }

    public URI getObject() {
        return object;
    }

    public void setObject(final URI object) {
        this.object = object;
    }

    public ObjectTypeEnum getObjectType() {
        return ObjectTypeEnum.fromValue(objectType);
    }

    public void setObjectType(final ObjectTypeEnum objectType) {
        this.objectType = objectType.toString();
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(final Integer page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "ZaakobjectListParameters{" +
               "zaak=" + zaak +
               ", object=" + object +
               ", objectType='" + objectType + '\'' +
               ", page=" + page +
               '}';
    }
}
