/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.ontkoppeldedocumenten.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.atos.zac.app.shared.RESTResultaat;
import nl.info.zac.app.identity.model.RestUser;

public class RESTOntkoppeldDocumentResultaat extends RESTResultaat<RESTOntkoppeldDocument> {

    public List<RestUser> filterOntkoppeldDoor = new ArrayList<>();

    public RESTOntkoppeldDocumentResultaat(final Collection<RESTOntkoppeldDocument> resultaten, final long aantalTotaal) {
        super(resultaten, aantalTotaal);
    }
}
