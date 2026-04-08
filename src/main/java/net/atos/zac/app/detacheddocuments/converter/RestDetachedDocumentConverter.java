/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.detacheddocuments.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import net.atos.zac.app.detacheddocuments.model.RestDetachedDocument;
import net.atos.zac.document.detacheddocument.model.DetachedDocument;
import nl.info.zac.app.identity.converter.RestUserConverter;
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService;
import nl.info.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock;

public class RestDetachedDocumentConverter {

    private RestUserConverter userConverter;
    private EnkelvoudigInformatieObjectLockService lockService;

    @SuppressWarnings("unused")
    public RestDetachedDocumentConverter() {
        // Default constructor for CDI
    }

    @Inject
    public RestDetachedDocumentConverter(
            final RestUserConverter userConverter,
            final EnkelvoudigInformatieObjectLockService lockService
    ) {
        this.userConverter = userConverter;
        this.lockService = lockService;
    }

    public RestDetachedDocument convert(final DetachedDocument document, final UUID informatieobjectTypeUUID) {
        final RestDetachedDocument restDocument = new RestDetachedDocument();
        restDocument.id = document.getId();
        restDocument.documentUUID = document.documentUUID;
        restDocument.documentID = document.documentID;
        restDocument.informatieobjectTypeUUID = informatieobjectTypeUUID;
        restDocument.titel = document.titel;
        restDocument.zaakID = document.zaakID;
        restDocument.creatiedatum = document.creatiedatum;
        restDocument.bestandsnaam = document.getBestandsnaam();
        restDocument.ontkoppeldDoor = userConverter.convertUserId(document.ontkoppeldDoor);
        restDocument.ontkoppeldOp = document.ontkoppeldOp;
        restDocument.reden = document.getReden();
        final EnkelvoudigInformatieObjectLock lock = lockService.findLock(document.documentUUID);
        restDocument.isVergrendeld = lock != null && lock.getLock() != null;
        return restDocument;
    }

    public List<RestDetachedDocument> convert(
            final List<DetachedDocument> documenten,
            final List<UUID> informatieobjectTypeUUIDs
    ) {
        List<RestDetachedDocument> list = new ArrayList<>();
        for (int index = 0; index < documenten.size(); index++) {
            list.add(convert(
                    documenten.get(index),
                    informatieobjectTypeUUIDs.get(index)
            ));
        }
        return list;
    }
}
