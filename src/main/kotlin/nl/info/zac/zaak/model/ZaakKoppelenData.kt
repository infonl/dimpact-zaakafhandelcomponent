/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isDeelzaak
import nl.info.client.zgw.zrc.util.isHoofdzaak
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.zac.policy.output.ZaakRechten
import java.util.UUID

data class ZaakKoppelenData(
    val isOpen: Boolean,
    val isHoofdzaak: Boolean,
    val isDeelzaak: Boolean,
    val zaaktypeUUID: UUID,
    val lezen: Boolean,
    val koppelen: Boolean
)

fun Zaak.toKoppelData(rechten: ZaakRechten) = ZaakKoppelenData(
    isOpen = this.isOpen(),
    isHoofdzaak = this.isHoofdzaak(),
    isDeelzaak = this.isDeelzaak(),
    zaaktypeUUID = this.zaaktype.extractUuid(),
    lezen = rechten.lezen,
    koppelen = rechten.koppelen
)
