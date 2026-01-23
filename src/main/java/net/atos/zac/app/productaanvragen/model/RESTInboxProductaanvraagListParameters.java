/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.productaanvragen.model;

import net.atos.zac.app.shared.RESTListParameters;
import nl.info.zac.app.search.model.RestDatumRange;

public class RESTInboxProductaanvraagListParameters extends RESTListParameters {

    public String type;

    public RestDatumRange ontvangstdatum;

    public String initiatorID;
}
