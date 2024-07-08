/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.audit.model;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import net.atos.client.zgw.drc.model.generated.StatusEnum;
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum;
import net.atos.client.zgw.shared.util.HistorieUtil;

public class RESTHistorieRegel {

    public final String attribuutLabel;

    public final String oudeWaarde;

    public final String nieuweWaarde;

    public ZonedDateTime datumTijd;

    public String door;

    public String applicatie;

    public String toelichting;

    public RESTHistorieActie actie;

    public RESTHistorieRegel(final String attribuutLabel, final String oudeWaarde, final String nieuweWaarde) {
        this.attribuutLabel = attribuutLabel;
        this.oudeWaarde = oudeWaarde;
        this.nieuweWaarde = nieuweWaarde;
    }
}
