/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.util;

import static net.atos.zac.configuratie.ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND;
import static net.atos.zac.configuratie.ConfiguratieService.STATUSTYPE_OMSCHRIJVING_INTAKE;

import nl.info.client.zgw.ztc.model.generated.StatusType;

public class StatusTypeUtil {
    private StatusTypeUtil() {
    }

    public static boolean isHeropend(StatusType statusType) {
        return statusType != null && STATUSTYPE_OMSCHRIJVING_HEROPEND.equals(statusType.getOmschrijving());
    }

    public static boolean isIntake(final StatusType statustype) {
        return statustype != null && STATUSTYPE_OMSCHRIJVING_INTAKE.equals(statustype.getOmschrijving());
    }
}
