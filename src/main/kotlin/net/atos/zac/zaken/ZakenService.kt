/*
* SPDX-FileCopyrightText: 2024 Lifely
* SPDX-License-Identifier: EUPL-1.2+
*/
package net.atos.zac.zaken

import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.OrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.event.EventingService
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import nl.lifely.zac.util.AllOpen
import java.util.UUID
import java.util.logging.Logger

@AllOpen
class ZakenService @Inject constructor(
    private val zrcClientService: ZRCClientService,
    private val ztcClientService: ZTCClientService,
    private val indexeerService: IndexeerService,
    private var eventingService: EventingService,
    private val configuratieService: ConfiguratieService
) {
    companion object {
        private val LOG = Logger.getLogger(ZakenService::class.java.name)
    }

    /**
     * Assigns a list of zaken to a group and/or user and updates the search index on the fly.
     * This can be a long-running operation.
     */
    @WithSpan
    @Suppress("LongParameterList")
    fun assignZakenAsync(
        zaakUUIDs: List<UUID>,
        group: Group,
        user: User? = null,
        explanation: String? = null,
        screenEventResourceId: String? = null,
    ) {
        LOG.fine { "Started asynchronous job with ID: $screenEventResourceId to assign ${zaakUUIDs.size} zaken." }
        val zakenAssignedList = mutableListOf<UUID>()
        zaakUUIDs
            .map { zrcClientService.readZaak(it) }
            .map { zaak ->
                group.let {
                    zrcClientService.updateRol(
                        zaak,
                        bepaalRolGroep(it, zaak),
                        explanation
                    )
                }
                user?.let {
                    zrcClientService.updateRol(
                        zaak,
                        bepaalRolMedewerker(it, zaak),
                        explanation
                    )
                }
                // unless OpenNotificaties is disabled, we don't need to do any indexing here,
                // because we will do it based on notifications from OpenZaak
                if (configuratieService.notificationsDisabled) {
                    indexeerService.indexeerDirect(
                        zaak.uuid.toString(),
                        ZoekObjectType.ZAAK,
                        false
                    )
                    eventingService.send(ScreenEventType.ZAAK.updated(zaak.uuid))
                }
                zakenAssignedList.add(zaak.uuid)
            }
        if (configuratieService.notificationsDisabled) {
            indexeerService.commit()
        }
        LOG.fine {
            "Asynchronous assign zaken job with job ID '$screenEventResourceId' finished. " +
                "Successfully assigned ${zakenAssignedList.size} zaken."
        }
        // if a screen event resource ID was specified, send an 'updated zaken_verdelen' screen event
        // with the job UUID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            eventingService.send(ScreenEventType.ZAKEN_VERDELEN.updated(it))
        }
    }

    fun bepaalRolGroep(group: Group, zaak: Zaak) =
        RolOrganisatorischeEenheid(
            zaak.url,
            ztcClientService.readRoltype(
                RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaak.zaaktype
            ),
            "Behandelend groep van de zaak",
            OrganisatorischeEenheid().apply {
                identificatie = group.id
                naam = group.name
            }
        )

    fun bepaalRolMedewerker(user: User, zaak: Zaak) =
        RolMedewerker(
            zaak.url,
            ztcClientService.readRoltype(
                RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaak.zaaktype
            ),
            "Behandelaar van de zaak",
            Medewerker().apply {
                identificatie = user.id
                voorletters = user.firstName
                achternaam = user.lastName
            }
        )

    /**
     * Releases a list of zaken from a user and updates the search index on the fly.
     * This can be a long-running operation.
     */
    @WithSpan
    fun releaseZaken(
        zaakUUIDs: List<UUID>,
        explanation: String? = null,
        screenEventResourceId: String? = null
    ) {
        LOG.fine {
            "Started asynchronous job with ID: $screenEventResourceId to release ${zaakUUIDs.size} zaken"
        }
        zaakUUIDs
            .map { zrcClientService.readZaak(it) }
            .forEach {
                zrcClientService.deleteRol(it, BetrokkeneType.MEDEWERKER, explanation)
                // unless OpenNotificaties is disabled, we don't need to do any indexing here,
                // because we will do it based on notifications from OpenZaak
                if (configuratieService.notificationsDisabled) {
                    indexeerService.indexeerDirect(
                        it.uuid.toString(),
                        ZoekObjectType.ZAAK,
                        false
                    )
                    eventingService.send(ScreenEventType.ZAAK.updated(it.uuid))
                }
            }
        if (configuratieService.notificationsDisabled) {
            indexeerService.commit()
        }
        LOG.fine {
            "Asynchronous release zaken job with job ID '$screenEventResourceId' finished. " +
                "Successfully released ${zaakUUIDs.size} zaken."
        }
        // if a screen event resource ID was specified, send an 'updated zaken_verdelen' screen event
        // with the job UUID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            eventingService.send(ScreenEventType.ZAKEN_VRIJGEVEN.updated(it))
        }
    }
}
