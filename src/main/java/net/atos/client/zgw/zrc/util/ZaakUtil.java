/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.util;

import net.atos.client.zgw.zrc.model.generated.Zaak;

public class ZaakUtil {
  private ZaakUtil() {}

  public static boolean isOpgeschort(Zaak zaak) {
    return zaak.getOpschorting() != null && zaak.getOpschorting().getIndicatie();
  }

  public static boolean isOpen(Zaak zaak) {
    return zaak.getArchiefnominatie() == null;
  }

  public static boolean isVerlengd(Zaak zaak) {
    return zaak.getVerlenging() != null && zaak.getVerlenging().getDuur() != null;
  }
}
