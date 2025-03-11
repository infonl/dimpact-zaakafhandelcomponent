/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.ontkoppeldedocumenten.model;

import net.atos.zac.app.identity.model.RestUser;
import net.atos.zac.app.shared.RESTListParameters;
import nl.info.zac.app.search.model.RestDatumRange;

public class RESTOntkoppeldDocumentListParameters extends RESTListParameters {
    public String titel;

    public String reden;

    public RestDatumRange creatiedatum;

    public RestUser ontkoppeldDoor;

    public RestDatumRange ontkoppeldOp;

    public String zaakID;
}
