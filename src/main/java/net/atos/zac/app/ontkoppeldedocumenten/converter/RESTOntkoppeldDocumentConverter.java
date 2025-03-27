/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.ontkoppeldedocumenten.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import net.atos.zac.app.identity.converter.RestUserConverter;
import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocument;
import net.atos.zac.documenten.model.OntkoppeldDocument;
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService;
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock;

public class RESTOntkoppeldDocumentConverter {

    @Inject
    private RestUserConverter userConverter;

    @Inject
    private EnkelvoudigInformatieObjectLockService lockService;

    public RESTOntkoppeldDocument convert(final OntkoppeldDocument document, final UUID informatieobjectTypeUUID) {
        final RESTOntkoppeldDocument restDocument = new RESTOntkoppeldDocument();
        restDocument.id = document.getId();
        restDocument.documentUUID = document.getDocumentUUID();
        restDocument.documentID = document.getDocumentID();
        restDocument.informatieobjectTypeUUID = informatieobjectTypeUUID;
        restDocument.titel = document.getTitel();
        restDocument.zaakID = document.getZaakID();
        restDocument.creatiedatum = document.getCreatiedatum().toLocalDate();
        restDocument.bestandsnaam = document.getBestandsnaam();
        restDocument.ontkoppeldDoor = userConverter.convertUserId(document.getOntkoppeldDoor());
        restDocument.ontkoppeldOp = document.getOntkoppeldOp();
        restDocument.reden = document.getReden();
        final EnkelvoudigInformatieObjectLock lock = lockService.findLock(document.getDocumentUUID());
        restDocument.isVergrendeld = lock != null && lock.getLock() != null;
        return restDocument;
    }

    public List<RESTOntkoppeldDocument> convert(
            final List<OntkoppeldDocument> documenten,
            final List<UUID> informatieobjectTypeUUIDs
    ) {
        List<RESTOntkoppeldDocument> list = new ArrayList<>();
        for (int index = 0; index < documenten.size(); index++) {
            list.add(convert(
                    documenten.get(index),
                    informatieobjectTypeUUIDs.get(index)
            ));
        }
        return list;
    }
}
