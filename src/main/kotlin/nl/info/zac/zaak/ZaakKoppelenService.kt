/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.GerelateerdeZakenZaakPatch
import nl.info.client.zgw.zrc.model.NillableHoofdzaakZaakPatch
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.event.EventingService
import nl.info.zac.search.IndexingService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.websocket.event.ScreenEventType
import java.util.UUID

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ZaakKoppelenService @Inject constructor(
    private val eventingService: EventingService,
    private val indexingService: IndexingService,
    private val zrcClientService: ZrcClientService
) {
    fun addGerelateerdeZaak(zaak: Zaak, gerelateerdeZaak: Zaak, explanation: String?) {
        zrcClientService.patchZaak(
            zaakUUID = zaak.uuid,
            zaak = GerelateerdeZakenZaakPatch(
                gerelateerdeZaken = zaak.gerelateerdeZaken.orEmpty().let { gerelateerdeZaken ->
                    if (gerelateerdeZaken.any { it.url == gerelateerdeZaak.url }) {
                        gerelateerdeZaken
                    } else {
                        gerelateerdeZaken + GerelateerdeZaak().apply { url = gerelateerdeZaak.url }
                    }
                }
            ),
            explanation = explanation
        )
    }

    fun linkHoofdzaakAndDeelzaak(hoofdzaak: Zaak, deelzaak: Zaak) {
        zrcClientService.patchZaak(
            zaakUUID = deelzaak.uuid,
            zaak = NillableHoofdzaakZaakPatch(hoofdzaak = hoofdzaak.url)
        )
        updateHoofdzaak(hoofdzaak.uuid)
    }

    /**
     * Open Zaak only sends a notification for the deelzaak when the hoofdzaak of a deelzaak changes,
     * so the hoofdzaak is reindexed and its screen event is sent here.
     */
    fun updateHoofdzaak(hoofdzaakUuid: UUID) {
        indexingService.addOrUpdateZaakOrThrow(hoofdzaakUuid, false)
        eventingService.send(ScreenEventType.ZAAK.updated(hoofdzaakUuid))
    }
}
