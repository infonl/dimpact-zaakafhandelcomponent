/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import nl.info.client.zgw.zrc.ZrcClientService
import java.util.UUID

const val ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD = "ZAAK_GEAUTORISEERD"
private const val ZAAKEIGENSCHAP_WAARDE_GEAUTORISEERD = "true"

fun ZrcClientService.isZaakspecifiekGeautoriseerd(zaakUUID: UUID): Boolean =
    listZaakeigenschappen(zaakUUID).any {
        it.naam == ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD && it.waarde == ZAAKEIGENSCHAP_WAARDE_GEAUTORISEERD
    }
