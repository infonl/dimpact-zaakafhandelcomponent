/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.document.detacheddocument.model;

import java.util.List;

import nl.info.zac.shared.model.Resultaat;

public class DetachedDocumentResult extends Resultaat<DetachedDocument> {

    private final List<String> ontkoppeldDoorFilter;

    public DetachedDocumentResult(final List<DetachedDocument> items, final long count, final List<String> ontkoppeldDoorFilter) {
        super(items, count);
        this.ontkoppeldDoorFilter = ontkoppeldDoorFilter;
    }

    public List<String> getOntkoppeldDoorFilter() {
        return ontkoppeldDoorFilter;
    }
}
