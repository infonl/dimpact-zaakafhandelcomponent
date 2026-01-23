/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.converter;

import net.atos.zac.app.informatieobjecten.model.RESTOndertekening;
import nl.info.client.zgw.drc.model.generated.Ondertekening;

public class RestOndertekeningConverter {
    public static RESTOndertekening convert(final Ondertekening ondertekening) {
        final RESTOndertekening restOndertekening = new RESTOndertekening();
        restOndertekening.soort = ondertekening.getSoort().name().toLowerCase();
        restOndertekening.datum = ondertekening.getDatum();
        return restOndertekening;
    }
}
