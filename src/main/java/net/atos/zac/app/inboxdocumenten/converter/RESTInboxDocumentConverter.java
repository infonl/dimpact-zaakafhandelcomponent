/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten.converter;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocument;
import net.atos.zac.documenten.model.InboxDocument;

public class RESTInboxDocumentConverter {

    public RESTInboxDocument convert(final InboxDocument document, final UUID informatieobjectTypeUUID) {
        final RESTInboxDocument restDocument = new RESTInboxDocument();
        restDocument.id = document.getId();
        restDocument.enkelvoudiginformatieobjectUUID = document.getEnkelvoudiginformatieobjectUUID();
        restDocument.enkelvoudiginformatieobjectID = document.getEnkelvoudiginformatieobjectID();
        restDocument.informatieobjectTypeUUID = informatieobjectTypeUUID;
        restDocument.titel = document.getTitel();
        restDocument.creatiedatum = document.getCreatiedatum().toLocalDate();
        restDocument.bestandsnaam = document.getBestandsnaam();
        return restDocument;
    }

    public List<RESTInboxDocument> convert(
            final List<InboxDocument> documenten,
            final List<UUID> informatieobjectTypeUUIDs
    ) {
        return IntStream.range(0, documenten.size())
                .mapToObj(index -> convert(
                        documenten.get(index),
                        informatieobjectTypeUUIDs.get(index)
                ))
                .toList();
    }
}
