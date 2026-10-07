/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.converter;

import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;

import net.atos.zac.app.bag.model.RESTAdresseerbaarObject;
import nl.info.client.bag.model.generated.AdresseerbaarObjectIOHal;
import nl.info.client.bag.model.generated.Gebruiksdoel;
import nl.info.client.bag.model.generated.Indicatie;
import nl.info.client.bag.model.generated.Ligplaats;
import nl.info.client.bag.model.generated.Standplaats;
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject;
import nl.info.client.bag.model.generated.Verblijfsobject;

public class RestAdresseerbaarObjectConverter {
    public static RESTAdresseerbaarObject convertToREST(final AdresseerbaarObjectIOHal adresseerbaarObjectIOHal) {
        if (adresseerbaarObjectIOHal == null) {
            return null;
        }

        if (adresseerbaarObjectIOHal.getLigplaats() != null) {
            return convertToREST(adresseerbaarObjectIOHal.getLigplaats().getLigplaats());
        } else if (adresseerbaarObjectIOHal.getStandplaats() != null) {
            return convertToREST(adresseerbaarObjectIOHal.getStandplaats().getStandplaats());
        } else if (adresseerbaarObjectIOHal.getVerblijfsobject() != null) {
            return convertToREST(adresseerbaarObjectIOHal.getVerblijfsobject().getVerblijfsobject());
        } else {
            throw new IllegalStateException("adresseerbaarObject is leeg");
        }
    }

    public static RESTAdresseerbaarObject convertToREST(final Ligplaats ligplaats) {
        final RESTAdresseerbaarObject restAdresseerbaarObject = new RESTAdresseerbaarObject();
        restAdresseerbaarObject.typeAdresseerbaarObject = TypeAdresseerbaarObject.LIGPLAATS;
        restAdresseerbaarObject.identificatie = ligplaats.getIdentificatie();
        restAdresseerbaarObject.status = ligplaats.getStatus().toString();
        restAdresseerbaarObject.geconstateerd = Indicatie.J.equals(ligplaats.getGeconstateerd());
        restAdresseerbaarObject.geometry = RestBagConverter.convertVlak(ligplaats.getGeometrie());
        return restAdresseerbaarObject;
    }

    public static RESTAdresseerbaarObject convertToREST(final Standplaats standplaats) {
        final RESTAdresseerbaarObject restAdresseerbaarObject = new RESTAdresseerbaarObject();
        restAdresseerbaarObject.typeAdresseerbaarObject = TypeAdresseerbaarObject.STANDPLAATS;
        restAdresseerbaarObject.identificatie = standplaats.getIdentificatie();
        restAdresseerbaarObject.status = standplaats.getStatus().toString();
        restAdresseerbaarObject.geconstateerd = Indicatie.J.equals(standplaats.getGeconstateerd());
        restAdresseerbaarObject.geometry = RestBagConverter.convertVlak(standplaats.getGeometrie());
        return restAdresseerbaarObject;
    }

    public static RESTAdresseerbaarObject convertToREST(final Verblijfsobject verblijfsobject) {
        final RESTAdresseerbaarObject restAdresseerbaarObject = new RESTAdresseerbaarObject();
        restAdresseerbaarObject.typeAdresseerbaarObject = TypeAdresseerbaarObject.VERBLIJFSOBJECT;
        restAdresseerbaarObject.identificatie = verblijfsobject.getIdentificatie();
        restAdresseerbaarObject.status = verblijfsobject.getStatus().toString();
        restAdresseerbaarObject.geconstateerd = Indicatie.J.equals(verblijfsobject.getGeconstateerd());
        restAdresseerbaarObject.vboDoel = ListUtils.emptyIfNull(
                verblijfsobject.getGebruiksdoelen()).stream().map(Gebruiksdoel::toString).collect(Collectors.joining(", "));
        restAdresseerbaarObject.vboOppervlakte = verblijfsobject.getOppervlakte() != null ? verblijfsobject.getOppervlakte() : 0;
        restAdresseerbaarObject.geometry = RestBagConverter.convertPuntOrVlak(verblijfsobject.getGeometrie());
        return restAdresseerbaarObject;
    }
}
