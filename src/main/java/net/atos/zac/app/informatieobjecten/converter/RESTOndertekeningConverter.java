/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.converter;

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectOndertekening;
import net.atos.zac.app.informatieobjecten.model.RESTOndertekening;

public class RESTOndertekeningConverter {

    public RESTOndertekening convert(final EnkelvoudigInformatieObjectOndertekening enkelvoudigInformatieObjectOndertekening) {
        final RESTOndertekening restOndertekening = new RESTOndertekening();
        restOndertekening.soort = enkelvoudigInformatieObjectOndertekening.getSoort().name().toLowerCase();
        restOndertekening.datum = enkelvoudigInformatieObjectOndertekening.getDatum();
        return restOndertekening;
    }
}
