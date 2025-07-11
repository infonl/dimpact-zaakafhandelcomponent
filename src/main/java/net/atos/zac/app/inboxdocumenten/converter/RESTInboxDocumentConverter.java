/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocument;
import net.atos.zac.documenten.model.InboxDocument;

public class RESTInboxDocumentConverter {

    public static RESTInboxDocument convert(final InboxDocument document, final UUID informatieobjectTypeUUID) {
        final RESTInboxDocument restDocument = new RESTInboxDocument();
        restDocument.id = document.getId();
        restDocument.enkelvoudiginformatieobjectUUID = document.getEnkelvoudiginformatieobjectUUID();
        restDocument.enkelvoudiginformatieobjectID = document.getEnkelvoudiginformatieobjectID();
        restDocument.informatieobjectTypeUUID = informatieobjectTypeUUID;
        restDocument.titel = document.getTitel();
        restDocument.creatiedatum = document.getCreatiedatum();
        restDocument.bestandsnaam = document.getBestandsnaam();
        return restDocument;
    }

    public static List<RESTInboxDocument> convert(
            final List<InboxDocument> documenten,
            final List<UUID> informatieobjectTypeUUIDs
    ) {
        List<RESTInboxDocument> list = new ArrayList<>();
        for (int index = 0; index < documenten.size(); index++) {
            list.add(convert(
                    documenten.get(index),
                    informatieobjectTypeUUIDs.get(index)
            ));
        }
        return list;
    }
}
