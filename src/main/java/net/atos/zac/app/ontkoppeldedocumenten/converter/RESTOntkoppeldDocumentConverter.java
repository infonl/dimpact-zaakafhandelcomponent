/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.ontkoppeldedocumenten.converter;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

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
        return IntStream.range(0, documenten.size())
                .mapToObj(index -> convert(
                        documenten.get(index),
                        informatieobjectTypeUUIDs.get(index)
                ))
                .toList();
    }
}
