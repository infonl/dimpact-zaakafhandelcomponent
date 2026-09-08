/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakEigenschap
import nl.info.client.zgw.ztc.ZtcClientService
import java.util.UUID

const val ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD = "ZAAK_GEAUTORISEERD"
private const val ZAAKEIGENSCHAP_WAARDE_GEAUTORISEERD = "true"

fun ZrcClientService.isZaakspecifiekGeautoriseerd(zaakUUID: UUID): Boolean =
    listZaakeigenschappen(zaakUUID).any {
        it.naam == ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD && it.waarde == ZAAKEIGENSCHAP_WAARDE_GEAUTORISEERD
    }

/**
 * Records [zaak] as zaakspecifiek geautoriseerd in the zaakregister, and does nothing when it already
 * carries the marking, so that marking a zaak twice never creates a duplicate zaakeigenschap.
 *
 * Throws [nl.info.client.zgw.ztc.exception.EigenschapNotFoundException] when the zaak's zaaktype does not
 * define the [ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD] eigenschap, i.e. when it is not zaakspecifiek
 * autoriseerbaar.
 */
fun ZrcClientService.markZaakspecifiekGeautoriseerd(zaak: Zaak, ztcClientService: ZtcClientService) {
    if (isZaakspecifiekGeautoriseerd(zaak.uuid)) return
    ztcClientService.readEigenschap(zaak.zaaktype, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD).let { eigenschap ->
        createEigenschap(
            zaak.uuid,
            ZaakEigenschap().apply {
                this.eigenschap = eigenschap.url
                this.zaak = zaak.url
                this.waarde = ZAAKEIGENSCHAP_WAARDE_GEAUTORISEERD
            }
        )
    }
}
