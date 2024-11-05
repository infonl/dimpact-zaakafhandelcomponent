/*
* SPDX-FileCopyrightText: 2024 Lifely
* SPDX-License-Identifier: EUPL-1.2+
*/
package net.atos.zac.zaak

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.NatuurlijkPersoon
import net.atos.client.zgw.zrc.model.NietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.OrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolVestiging
import net.atos.client.zgw.zrc.model.Vestiging
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.zac.event.EventingService
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.websocket.event.ScreenEventType
import nl.lifely.zac.util.AllOpen
import java.util.UUID
import java.util.logging.Logger

private val LOG = Logger.getLogger(ZaakService::class.java.name)

@AllOpen
class ZaakService @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private var eventingService: EventingService,
) {
    fun addBetrokkenNatuurlijkPersoon(
        roltype: RolType,
        bsn: String,
        zaak: Zaak,
        toelichting: String
    ) {
        zrcClientService.createRol(
            RolNatuurlijkPersoon(
                zaak.url,
                roltype,
                toelichting,
                NatuurlijkPersoon(bsn)
            ),
            toelichting
        )
    }

    fun addBetrokkenVestiging(
        roltype: RolType,
        vestigingsnummer: String,
        zaak: Zaak,
        toelichting: String
    ) {
        zrcClientService.createRol(
            RolVestiging(
                zaak.url,
                roltype,
                toelichting,
                Vestiging(vestigingsnummer)
            ),
            toelichting
        )
    }

    fun addBetrokkenNietNatuurlijkPersoon(
        roltype: RolType,
        rsin: String,
        zaak: Zaak,
        toelichting: String
    ) {
        zrcClientService.createRol(
            RolNietNatuurlijkPersoon(
                zaak.url,
                roltype,
                toelichting,
                NietNatuurlijkPersoon(rsin)
            ),
            toelichting
        )
    }

    /**
     * Assigns a list of zaken to a group and/or user and updates the search index on the fly.
     * This can be a long-running operation.
     *
     * Zaken that are not open will be skipped.
     */
    @WithSpan
    @Suppress("LongParameterList")
    fun assignZaken(
        @SpanAttribute("zaakUUIDs") zaakUUIDs: List<UUID>,
        group: Group,
        user: User? = null,
        explanation: String? = null,
        screenEventResourceId: String? = null,
    ) {
        LOG.fine {
            "Started to assign ${zaakUUIDs.size} zaken with screen event resource ID: '$screenEventResourceId'."
        }
        val zakenAssignedList = mutableListOf<UUID>()
        zaakUUIDs
            .map(zrcClientService::readZaak)
            .filter {
                if (!it.isOpen) {
                    LOG.fine("Zaak with UUID '${it.uuid} is not open. Therefore it is skipped and not assigned.")
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(it))
                }
                it.isOpen
            }
            .map { zaak ->
                zrcClientService.updateRol(
                    zaak,
                    bepaalRolGroep(group, zaak),
                    explanation
                )
                user?.let {
                    zrcClientService.updateRol(
                        zaak,
                        bepaalRolMedewerker(it, zaak),
                        explanation
                    )
                } ?: zrcClientService.deleteRol(zaak, BetrokkeneType.MEDEWERKER, explanation)
                zakenAssignedList.add(zaak.uuid)
            }
        LOG.fine { "Successfully assigned ${zakenAssignedList.size} zaken." }
        // if a screen event resource ID was specified, send an 'updated zaken_verdelen' screen event
        // with the job UUID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            LOG.fine { "Sending 'ZAKEN_VERDELEN' screen event with ID '$it'." }
            eventingService.send(ScreenEventType.ZAKEN_VERDELEN.updated(it))
        }
    }

    fun bepaalRolGroep(group: Group, zaak: Zaak) =
        RolOrganisatorischeEenheid(
            zaak.url,
            ztcClientService.readRoltype(
                zaak.zaaktype,
                OmschrijvingGeneriekEnum.BEHANDELAAR
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
                zaak.zaaktype,
                OmschrijvingGeneriekEnum.BEHANDELAAR
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
     *
     * Zaken that are not open will be skipped.
     */
    @WithSpan
    fun releaseZaken(
        @SpanAttribute("zaakUUIDs") zaakUUIDs: List<UUID>,
        explanation: String? = null,
        screenEventResourceId: String? = null
    ) {
        LOG.fine {
            "Started to release ${zaakUUIDs.size} zaken with screen event resource ID: '$screenEventResourceId'."
        }
        zaakUUIDs
            .map(zrcClientService::readZaak)
            .filter {
                if (!it.isOpen) {
                    LOG.fine(
                        "Zaak with UUID '${it.uuid} is not open. Therefore it is not released."
                    )
                }
                it.isOpen
            }
            .forEach { zrcClientService.deleteRol(it, BetrokkeneType.MEDEWERKER, explanation) }
        LOG.fine { "Successfully released  ${zaakUUIDs.size} zaken." }

        // if a screen event resource ID was specified, send an 'updated zaken_verdelen' screen event
        // with the job UUID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            LOG.fine { "Sending 'ZAKEN_VRIJGEVEN' screen event with ID '$it'." }
            eventingService.send(ScreenEventType.ZAKEN_VRIJGEVEN.updated(it))
        }
    }
}
