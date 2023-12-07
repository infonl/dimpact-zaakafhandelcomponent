/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaken.model;

import java.util.List;

import jakarta.validation.Valid;

import net.atos.zac.app.bag.model.RESTBAGObject;
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag;

public class RESTZaakAanmaakGegevens {

    @Valid
    public RESTZaak zaak;

    public RESTInboxProductaanvraag inboxProductaanvraag;

    public List<RESTBAGObject> bagObjecten;
}
