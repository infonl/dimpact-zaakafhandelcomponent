/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.converter;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import net.atos.client.zgw.shared.util.URIUtil;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;
import net.atos.zac.app.informatieobjecten.model.RESTInformatieobjecttype;

public class RESTInformatieobjecttypeConverter {

    @Inject
    private ZtcClientService ztcClientService;

    public RESTInformatieobjecttype convert(final InformatieObjectType type) {
        final RESTInformatieobjecttype restType = new RESTInformatieobjecttype();
        restType.uuid = URIUtil.parseUUIDFromResourceURI(type.getUrl());
        restType.concept = type.getConcept();
        restType.omschrijving = type.getOmschrijving();
        restType.vertrouwelijkheidaanduiding = type.getVertrouwelijkheidaanduiding().value();
        return restType;
    }

    public List<RESTInformatieobjecttype> convert(final Set<URI> informatieobjecttypen) {
        return informatieobjecttypen.stream().map(ztcClientService::readInformatieobjecttype).map(this::convert).collect(Collectors
                .toList());
    }

    public List<RESTInformatieobjecttype> convert(final List<InformatieObjectType> informatieobjecttypen) {
        return informatieobjecttypen.stream().map(this::convert).collect(Collectors.toList());
    }
}
