/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.converter;

import static net.atos.client.zgw.util.UriUtilsKt.extractUuid;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;
import net.atos.zac.app.informatieobjecten.model.RestInformatieobjecttype;

public class RestInformatieobjecttypeConverter {

    @Inject
    private ZtcClientService ztcClientService;

    public static RestInformatieobjecttype convert(final InformatieObjectType type) {
        final RestInformatieobjecttype restType = new RestInformatieobjecttype();
        restType.uuid = extractUuid(type.getUrl());
        restType.concept = type.getConcept();
        restType.omschrijving = type.getOmschrijving();
        // we use the uppercase version of this enum in the ZAC backend API
        restType.vertrouwelijkheidaanduiding = type.getVertrouwelijkheidaanduiding().name();
        return restType;
    }

    public static List<RestInformatieobjecttype> convert(final List<InformatieObjectType> informatieobjecttypen) {
        return informatieobjecttypen.stream()
                .map(RestInformatieobjecttypeConverter::convert)
                .collect(Collectors.toList());
    }

    public List<RestInformatieobjecttype> convertFromUris(final List<URI> informatieobjecttypeUris) {
        return informatieobjecttypeUris.stream()
                .map(ztcClientService::readInformatieobjecttype)
                .map(RestInformatieobjecttypeConverter::convert)
                .collect(Collectors.toList());
    }
}
