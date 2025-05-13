/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.util;

import static nl.info.zac.configuratie.ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND;
import static nl.info.zac.configuratie.ConfiguratieService.STATUSTYPE_OMSCHRIJVING_INTAKE;

import jakarta.annotation.Nullable;

import nl.info.client.zgw.ztc.model.generated.StatusType;

public class StatusTypeUtil {
    private StatusTypeUtil() {
    }

    public static boolean isHeropend(@Nullable StatusType statusType) {
        return statusType != null && STATUSTYPE_OMSCHRIJVING_HEROPEND.equals(statusType.getOmschrijving());
    }

    public static boolean isIntake(@Nullable final StatusType statustype) {
        return statustype != null && STATUSTYPE_OMSCHRIJVING_INTAKE.equals(statustype.getOmschrijving());
    }
}
