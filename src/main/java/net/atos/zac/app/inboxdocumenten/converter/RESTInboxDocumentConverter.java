/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten.converter;

import static nl.info.client.zgw.util.UriUtilsKt.extractUuid;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import net.atos.client.zgw.drc.DrcClientService;
import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocument;
import net.atos.zac.documenten.model.InboxDocument;

public class RESTInboxDocumentConverter {

    @Inject
    private DrcClientService drcClientService;

    public RESTInboxDocument convert(final InboxDocument document) {
        final RESTInboxDocument restDocument = new RESTInboxDocument();
        restDocument.id = document.getId();
        restDocument.enkelvoudiginformatieobjectUUID = document.getEnkelvoudiginformatieobjectUUID();
        restDocument.enkelvoudiginformatieobjectID = document.getEnkelvoudiginformatieobjectID();
        restDocument.informatieobjectTypeUUID = extractUuid(
                drcClientService
                        .readEnkelvoudigInformatieobject(restDocument.enkelvoudiginformatieobjectUUID)
                        .getInformatieobjecttype()
        );
        restDocument.titel = document.getTitel();
        restDocument.creatiedatum = document.getCreatiedatum().toLocalDate();
        restDocument.bestandsnaam = document.getBestandsnaam();
        return restDocument;
    }

    public List<RESTInboxDocument> convert(final List<InboxDocument> documenten) {
        return documenten.stream().map(this::convert).collect(Collectors.toList());
    }
}
