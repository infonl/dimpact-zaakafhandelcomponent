/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaken.converter;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;

import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.client.zgw.ztc.model.generated.BesluitType;
import net.atos.zac.app.zaken.model.RESTBesluittype;
import net.atos.zac.util.UriUtil;

public class RESTBesluittypeConverter {

    @Inject
    private ZTCClientService ztcClientService;

    public RESTBesluittype convertToRESTBesluittype(final BesluitType besluittype) {
        final RESTBesluittype restBesluittype = new RESTBesluittype();
        restBesluittype.id = UriUtil.uuidFromURI(besluittype.getUrl());
        restBesluittype.naam = besluittype.getOmschrijving();
        restBesluittype.toelichting = besluittype.getToelichting();
        restBesluittype.informatieobjecttypen = besluittype.getInformatieobjecttypen().stream().toList();
        return restBesluittype;
    }

    public RESTBesluittype convertToRESTBesluittype(final URI besluittypeURI) {
        return convertToRESTBesluittype(ztcClientService.readBesluittype(besluittypeURI));
    }

    public List<RESTBesluittype> convertToRESTBesluittypes(final List<BesluitType> besluittypes) {
        return besluittypes.stream()
                .map(this::convertToRESTBesluittype)
                .toList();
    }

}
