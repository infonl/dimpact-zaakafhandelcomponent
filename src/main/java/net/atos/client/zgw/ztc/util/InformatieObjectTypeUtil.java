/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.util;

import java.time.LocalDate;

import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;

public class InformatieObjectTypeUtil {
    private InformatieObjectTypeUtil() {}

    public static boolean isNuGeldig(InformatieObjectType informatieObjectType) {
        LocalDate eindeGeldigheid = informatieObjectType.getEindeGeldigheid();
        return informatieObjectType.getBeginGeldigheid().isBefore(LocalDate.now().plusDays(1))
                && (eindeGeldigheid == null || eindeGeldigheid.isAfter(LocalDate.now()));
    }
}
