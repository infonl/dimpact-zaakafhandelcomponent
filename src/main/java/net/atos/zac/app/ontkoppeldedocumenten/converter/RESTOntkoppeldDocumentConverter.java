/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.ontkoppeldedocumenten.converter;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import net.atos.zac.app.identity.converter.RESTUserConverter;
import net.atos.zac.app.ontkoppeldedocumenten.model.RESTOntkoppeldDocument;
import net.atos.zac.documenten.model.OntkoppeldDocument;

public class RESTOntkoppeldDocumentConverter {

    @Inject private RESTUserConverter userConverter;

    public RESTOntkoppeldDocument convert(final OntkoppeldDocument document) {
        final RESTOntkoppeldDocument restDocument = new RESTOntkoppeldDocument();
        restDocument.id = document.getId();
        restDocument.documentUUID = document.getDocumentUUID();
        restDocument.documentID = document.getDocumentID();
        restDocument.titel = document.getTitel();
        restDocument.zaakID = document.getZaakID();
        restDocument.creatiedatum = document.getCreatiedatum().toLocalDate();
        restDocument.bestandsnaam = document.getBestandsnaam();
        restDocument.ontkoppeldDoor = userConverter.convertUserId(document.getOntkoppeldDoor());
        restDocument.ontkoppeldOp = document.getOntkoppeldOp();
        restDocument.reden = document.getReden();
        return restDocument;
    }

    public List<RESTOntkoppeldDocument> convert(final List<OntkoppeldDocument> documenten) {
        return documenten.stream().map(this::convert).collect(Collectors.toList());
    }
}
