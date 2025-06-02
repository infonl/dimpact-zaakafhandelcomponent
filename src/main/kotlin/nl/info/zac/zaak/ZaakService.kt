/*
* SPDX-FileCopyrightText: 2024 INFO.nl
* SPDX-License-Identifier: EUPL-1.2+
*/
package nl.info.zac.zaak

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.NatuurlijkPersoon
import net.atos.client.zgw.zrc.model.NietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.OrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolVestiging
import net.atos.client.zgw.zrc.model.Vestiging
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.util.AllOpen
import nl.info.zac.zaak.exception.BetrokkeneIsAlreadyAddedToZaakException
import nl.info.zac.zaak.exception.CaseHasLockedInformationObjectsException
import nl.info.zac.zaak.exception.CaseHasOpenSubcasesException
import nl.info.zac.zaak.model.Betrokkenen.BETROKKENEN_ENUMSET
import java.lang.Boolean
import java.util.Locale
import java.util.UUID
import java.util.logging.Logger

private val LOG = Logger.getLogger(ZaakService::class.java.name)

@AllOpen
@Suppress("TooManyFunctions")
class ZaakService @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private var eventingService: EventingService,
    private var zaakVariabelenService: ZaakVariabelenService,
    private val lockService: EnkelvoudigInformatieObjectLockService
) {
    fun addBetrokkeneToZaak(
        roleTypeUUID: UUID,
        identificationType: IdentificatieType,
        identification: String,
        zaak: Zaak,
        explanation: String
    ) {
        val roleType = ztcClientService.readRoltype(roleTypeUUID)
        if (listBetrokkenenforZaak(zaak).any {
                it.identificatienummer == identification && it.roltype == roleType.url
            }
        ) {
            throw BetrokkeneIsAlreadyAddedToZaakException(
                "Betrokkene with type '$identificationType' and identification '$identification' " +
                    "was already added to the zaak with UUID '${zaak.uuid}'. Ignoring."
            )
        }
        addRoleToZaak(
            roleType = roleType,
            identificationType = identificationType,
            identification = identification,
            zaak = zaak,
            explanation = explanation
        )
    }

    fun addInitiatorToZaak(
        identificationType: IdentificatieType,
        identification: String,
        zaak: Zaak,
        explanation: String
    ) {
        addRoleToZaak(
            roleType = ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR),
            identificationType = identificationType,
            identification = identification,
            zaak = zaak,
            explanation = explanation
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

    fun listBetrokkenenforZaak(zaak: Zaak): List<Rol<*>> =
        zrcClientService.listRollen(zaak)
            // filter out the roles that are not 'betrokkenen'
            .filter {
                BETROKKENEN_ENUMSET.contains(
                    OmschrijvingGeneriekEnum.valueOf(it.omschrijvingGeneriek.uppercase(Locale.getDefault()))
                )
            }

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
                    LOG.fine("Zaak with UUID '${it.uuid} is not open. Therefore it is not released.")
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(it))
                }
                it.isOpen
            }
            .forEach { zrcClientService.deleteRol(it, BetrokkeneType.MEDEWERKER, explanation) }
        LOG.fine { "Successfully released  ${zaakUUIDs.size} zaken." }

        // if a screen event resource ID was specified, send a screen event
        // with the job UUID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            LOG.fine { "Sending 'ZAKEN_VRIJGEVEN' screen event with ID '$it'." }
            eventingService.send(ScreenEventType.ZAKEN_VRIJGEVEN.updated(it))
        }
    }

    fun checkZaakAfsluitbaar(zaak: Zaak) {
        if (zrcClientService.heeftOpenDeelzaken(zaak)) {
            throw CaseHasOpenSubcasesException("Case ${zaak.uuid} has open subcases")
        }
        if (lockService.hasLockedInformatieobjecten(zaak)) {
            throw CaseHasLockedInformationObjectsException("Case ${zaak.uuid} has locked information objects")
        }
    }

    fun setOntvangstbevestigingVerstuurdIfNotHeropend(zaak: Zaak) {
        val statusType = zaak.status?.let { statusUuid ->
            val status = zrcClientService.readStatus(statusUuid)
            ztcClientService.readStatustype(status.statustype)
        }
        if (!statusType.isHeropend()) {
            zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, Boolean.TRUE)
        }
    }

    private fun addRoleToZaak(
        roleType: RolType,
        identificationType: IdentificatieType,
        identification: String,
        zaak: Zaak,
        explanation: String
    ) {
        val role = when (identificationType) {
            IdentificatieType.BSN ->
                RolNatuurlijkPersoon(
                    zaak.url,
                    roleType,
                    explanation,
                    NatuurlijkPersoon(identification)
                )

            IdentificatieType.VN ->
                RolVestiging(
                    zaak.url,
                    roleType,
                    explanation,
                    Vestiging(identification)
                )

            IdentificatieType.RSIN ->
                RolNietNatuurlijkPersoon(
                    zaak.url,
                    roleType,
                    explanation,
                    NietNatuurlijkPersoon(identification)
                )
        }
        zrcClientService.createRol(role, explanation)
    }
}
