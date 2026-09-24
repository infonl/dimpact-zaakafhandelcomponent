/*
* SPDX-FileCopyrightText: 2024 INFO.nl
* SPDX-License-Identifier: EUPL-1.2+
*/
package nl.info.zac.zaak

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.RolMedewerker
import nl.info.client.zgw.zrc.model.RolNatuurlijkPersoon
import nl.info.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import nl.info.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.exception.CaseOrProcessNotFoundException
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.pabc.PabcClientService
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.zaak.ZaakRestService.Companion.VESTIGING_IDENTIFICATIE_DELIMITER
import nl.info.zac.app.zaak.model.RestResultaattype
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.app.zaak.model.toRestResultaatTypes
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.identity.model.ZacApplicationRole
import nl.info.zac.identity.model.ZacApplicationRole.BEHANDELAAR
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException
import nl.info.zac.zaak.exception.BetrokkeneIsAlreadyAddedToZaakException
import nl.info.zac.zaak.model.Betrokkenen.BETROKKENEN_ENUMSET
import nl.info.zac.zaak.model.ZaakAssignment
import nl.info.zac.zaak.model.ZaakToewijzing
import java.net.URI
import java.util.Locale
import java.util.UUID
import java.util.logging.Level
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.concurrent.withLock

private val LOG = Logger.getLogger(ZaakService::class.java.name)

@AllOpen
@Suppress("TooManyFunctions", "LongParameterList")
class ZaakService @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zgwApiService: ZgwApiService,
    private var eventingService: EventingService,
    private var zaakVariabelenService: ZaakVariabelenService,
    private val identityService: IdentityService,
    private val indexingService: IndexingService,
    private val bpmnService: BpmnService,
    private val pabcClientService: PabcClientService,
    private val zaakspecifiekeAutorisatieService: ZaakspecifiekeAutorisatieService
) {
    companion object {
        private val zaakAssignmentLocks = Array(64) { ReentrantLock() }

        private fun lockForZaak(uuid: UUID) =
            zaakAssignmentLocks[Math.floorMod(uuid.hashCode(), zaakAssignmentLocks.size)]
    }

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
     * In case the provided user is not part of the group, all zaken will be skipped
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

        if (!isUserInGroup(user, group, zaakUUIDs)) {
            screenEventResourceId?.let {
                LOG.fine { "Sending 'ZAKEN_VERDELEN' skipped screen event with ID '$it'." }
                eventingService.send(ScreenEventType.ZAKEN_VERDELEN.skipped(it))
            }
            return
        }

        val numberOfAssignedZaken = zaakUUIDs
            .map(zrcClientService::readZaak)
            .count { assignZaakFromBatch(it, group, user, explanation) }

        LOG.fine { "Successfully assigned $numberOfAssignedZaken zaken." }

        // if a screen event resource ID was specified, send an 'updated zaken_verdelen' screen event
        // with the job UUID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            LOG.fine { "Sending 'ZAKEN_VERDELEN' updated screen event with ID '$it'." }
            eventingService.send(ScreenEventType.ZAKEN_VERDELEN.updated(it))
        }
    }

    /**
     * Assign a single zaak to a group and/or user.
     *
     * @param zaak The zaak to assign.
     * @param groupId The ID of the group to assign the zaak to. If null, the group of the zaak is left as it is.
     * @param userName The username of the user to assign the zaak to. If null, the user will be removed from the zaak.
     * @param reason The reason for the assignment.
     * @throws nl.info.zac.identity.exception.UserNotInGroupException when the user is not a member of the group,
     * before anything of the zaak is changed
     */
    fun assignZaak(zaak: Zaak, groupId: String?, userName: String?, reason: String?) =
        assignZaak(zaak = zaak, zaakAssignment = readZaakAssignment(groupId, userName), reason = reason)

    /**
     * Validates that the user is a member of the group and reads both, without changing any zaak.
     *
     * @param groupId The ID of the group. If null, the group of the zaak will be left as it is.
     * @param userName The username of the user. If null or empty, the user will be removed from the zaak.
     * @throws nl.info.zac.identity.exception.UserNotInGroupException when the user is not a member of the group
     */
    fun readZaakAssignment(groupId: String?, userName: String?): ZaakAssignment {
        val user = userName?.takeIf { it.isNotEmpty() }?.let { userNameToAssign ->
            groupId?.let { identityService.validateIfUserIsInGroup(userNameToAssign, it) }
            identityService.readUser(userNameToAssign)
        }
        return ZaakAssignment(group = groupId?.let(identityService::readGroup), user = user)
    }

    /**
     * Assign a single zaak to a validated [ZaakAssignment]. This is the only place where the groep and behandelaar
     * rollen of a zaak are written.
     *
     * When the zaak is zaakspecifiek geautoriseerd, the behandelaar that is replaced keeps access to the zaak
     * as a zaakspecifiek geautoriseerde medewerker.
     */
    fun assignZaak(zaak: Zaak, zaakAssignment: ZaakAssignment, reason: String?) {
        val (group, user) = zaakAssignment
        // lock for the given zaak so that it is impossible to assign the zaak to multiple users on quick subsequent calls
        lockForZaak(zaak.uuid).withLock {
            val zaakToewijzing = zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak)
            zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, user?.id)

            val isBehandelaarChanged = changeBehandelaar(zaak, zaakToewijzing, user, reason)
            val isGroupAssigned = group != null && assignGroup(zaak, zaakToewijzing, group, reason)

            changeZaakDataAssignment(zaak.uuid, group, user)

            if (isBehandelaarChanged || isGroupAssigned) {
                indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                if (isBehandelaarChanged) {
                    zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak)
                }
            }
        }
    }

    fun readZaakAndZaakTypeByZaakID(zaakID: String): Pair<Zaak, ZaakType> =
        zrcClientService.readZaakByID(zaakID).let { zaak ->
            zaak to readZaakTypeByZaak(zaak)
        }

    fun readZaakAndZaakTypeByZaakURI(zaakURI: URI): Pair<Zaak, ZaakType> =
        zrcClientService.readZaak(zaakURI).let { zaak ->
            zaak to readZaakTypeByZaak(zaak)
        }

    fun readZaakAndZaakTypeByZaakUUID(zaakUUID: UUID): Pair<Zaak, ZaakType> =
        zrcClientService.readZaak(zaakUUID).let { zaak ->
            zaak to readZaakTypeByZaak(zaak)
        }

    fun readZaakTypeByZaak(zaak: Zaak): ZaakType = ztcClientService.readZaaktype(zaak.zaaktype)

    fun readZaakTypeByUUID(zaakTypeUUID: UUID): ZaakType = ztcClientService.readZaaktype(zaakTypeUUID)

    fun bepaalRolGroep(group: Group, zaak: Zaak) =
        RolOrganisatorischeEenheid(
            zaak.url,
            zgwApiService.readBehandelaarRoltype(zaak.zaaktype),
            "Behandelend groep van de zaak",
            OrganisatorischeEenheidIdentificatie().apply {
                identificatie = group.name
                naam = group.description
            }
        )

    fun bepaalRolMedewerker(user: User, zaak: Zaak) =
        RolMedewerker(
            zaak.url,
            zgwApiService.readBehandelaarRoltype(zaak.zaaktype),
            "Behandelaar van de zaak",
            MedewerkerIdentificatie().apply {
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
            .forEach { releaseZaakFromBatch(it, explanation) }
        LOG.fine { "Successfully released  ${zaakUUIDs.size} zaken." }

        // if a screen event resource ID was specified, send a screen event
        // with the job UUID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            LOG.fine { "Sending 'ZAKEN_VRIJGEVEN' screen event with ID '$it'." }
            eventingService.send(ScreenEventType.ZAKEN_VRIJGEVEN.updated(it))
        }
    }

    fun setOntvangstbevestigingVerstuurdIfNotHeropend(zaak: Zaak) {
        val statusType = zaak.status?.let { statusUuid ->
            val status = zrcClientService.readStatus(statusUuid)
            ztcClientService.readStatustype(status.statustype)
        }
        if (!statusType.isHeropend()) {
            zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, true)
            eventingService.send(ScreenEventType.ZAAK.updated(zaak.uuid))
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
                    NatuurlijkPersoonIdentificatie().apply { inpBsn = identification }
                )

            IdentificatieType.VN -> {
                val (kvkNummer, vestigingsnummer) = identification.split(VESTIGING_IDENTIFICATIE_DELIMITER)
                RolNietNatuurlijkPersoon(
                    zaak.url,
                    roleType,
                    explanation,
                    NietNatuurlijkPersoonIdentificatie().apply {
                        this.kvkNummer = kvkNummer
                        this.vestigingsNummer = vestigingsnummer
                    }
                )
            }

            IdentificatieType.RSIN ->
                RolNietNatuurlijkPersoon(
                    zaak.url,
                    roleType,
                    explanation,
                    NietNatuurlijkPersoonIdentificatie().apply { this.kvkNummer = identification }
                )
        }
        zrcClientService.createRol(role, explanation)
    }

    private fun assignZaakFromBatch(zaak: Zaak, group: Group, user: User?, explanation: String?): Boolean {
        if (!isZaakOpen(zaak) ||
            !group.isAuthorisedForApplicationRoleAndZaaktype(
                // you are only allowed to assign zaken to 'behandelaren'
                zacApplicationRole = BEHANDELAAR,
                zaaktypeUuid = zaak.zaaktype.extractUuid()
            )
        ) {
            eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaak))
            return false
        }
        return try {
            assignZaak(zaak = zaak, zaakAssignment = ZaakAssignment(group = group, user = user), reason = explanation)
            true
        } catch (releaseException: ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException) {
            LOG.log(Level.FINE, releaseException) {
                "Zaak with UUID '${zaak.uuid}' is zaakspecifiek geautoriseerd and cannot be left without a " +
                    "behandelaar. Therefore it is skipped and not assigned."
            }
            eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaak))
            false
        } catch (roltypeNotFoundException: ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException) {
            LOG.log(Level.WARNING, roltypeNotFoundException) {
                "Zaak with UUID '${zaak.uuid}' is zaakspecifiek geautoriseerd but its zaaktype cannot keep the " +
                    "previous behandelaar authorised. Therefore it is skipped and not assigned."
            }
            eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaak))
            false
        }
    }

    private fun releaseZaakFromBatch(zaak: Zaak, explanation: String?) {
        if (!zaak.isOpen()) {
            LOG.fine("Zaak with UUID '${zaak.uuid} cannot be released. Therefore it is not released.")
            eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaak))
            return
        }
        try {
            assignZaak(zaak = zaak, zaakAssignment = ZaakAssignment(group = null, user = null), reason = explanation)
        } catch (releaseException: ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException) {
            LOG.log(Level.FINE, releaseException) {
                "Zaak with UUID '${zaak.uuid}' cannot be released. Therefore it is not released."
            }
            eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaak))
        }
    }

    private fun assignGroup(
        zaak: Zaak,
        zaakToewijzing: ZaakToewijzing,
        group: Group,
        reason: String?
    ) =
        if (zaakToewijzing.groepId != group.name) {
            // if the zaak is not already assigned to the requested group, assign it to this group
            zrcClientService.updateRol(zaak, bepaalRolGroep(group, zaak), reason)
            true
        } else {
            false
        }

    /**
     * Replaces the behandelaar rol(len) of the zaak with [user], or removes them when [user] is null.
     * A replaced behandelaar of a zaakspecifiek geautoriseerde zaak is granted an individual authorisation
     * first, so that they never lose access to the zaak.
     *
     * @return true when the behandelaar of the zaak changed
     */
    private fun changeBehandelaar(
        zaak: Zaak,
        zaakToewijzing: ZaakToewijzing,
        user: User?,
        reason: String?
    ): Boolean {
        val behandelaarRollen = zaakToewijzing.behandelaarRollen
        val isBehandelaarUnchanged = if (user == null) {
            behandelaarRollen.isEmpty()
        } else {
            behandelaarRollen.size == 1 && zaakToewijzing.behandelaarId == user.id
        }
        if (isBehandelaarUnchanged) return false

        if (zaakToewijzing.isZaakspecifiekGeautoriseerd) {
            behandelaarRollen
                .mapNotNull { it.betrokkeneIdentificatie }
                .distinctBy { it.identificatie }
                .forEach {
                    zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                        zaak = zaak,
                        medewerker = it,
                        reason = reason,
                        zaakspecifiekGeautoriseerdeMedewerkers = zaakToewijzing.zaakspecifiekGeautoriseerdeMedewerkers
                    )
                }
        }
        behandelaarRollen.forEach { zrcClientService.deleteRol(it, reason) }
        user?.let { zrcClientService.createRol(bepaalRolMedewerker(it, zaak), reason) }
        return true
    }

    private fun changeZaakDataAssignment(
        zaakUuid: UUID,
        group: Group?,
        user: User?
    ) {
        if (bpmnService.isZaakProcessDriven(zaakUuid)) {
            try {
                group?.let { zaakVariabelenService.setGroup(zaakUuid, it.name) }
                user?.let {
                    zaakVariabelenService.setUser(zaakUuid, it.id)
                } ?: zaakVariabelenService.removeUser(zaakUuid)
            } catch (exception: CaseOrProcessNotFoundException) {
                LOG.warning { exception.message }
            }
        }
    }

    fun listStatusTypes(zaaktypeUUID: UUID) =
        ztcClientService.readStatustypen(
            ztcClientService.readZaaktype(zaaktypeUUID).url
        ).toRestResultaatTypes()

    fun listResultTypes(zaaktypeUUID: UUID): List<RestResultaattype> {
        val zaaktype = ztcClientService.readZaaktype(zaaktypeUUID)
        val eigenschappen = ztcClientService.readEigenschappen(zaaktype.url)
        return ztcClientService.readResultaattypen(zaaktype.url).map { resultaattype ->
            resultaattype.toRestResultaatType().apply {
                val brondatumArchiefprocedure = resultaattype.brondatumArchiefprocedure
                if (brondatumArchiefprocedure?.afleidingswijze == AfleidingswijzeEnum.EIGENSCHAP) {
                    eigenschappen
                        .find { it.naam == brondatumArchiefprocedure.datumkenmerk }
                        ?.definitie
                        ?.takeIf { it.isNotBlank() }
                        ?.let { datumKenmerkOmschrijving = it }
                }
            }
        }
    }

    private fun isUserInGroup(
        user: User?,
        group: Group,
        zaakUUIDs: List<UUID>
    ) =
        user?.let {
            val inGroup = identityService.isUserInGroup(user.id, group.name)
            if (!inGroup) {
                LOG.warning(
                    "User '${user.displayName}' (id: {$user.id}) is not in the group '${group.description}'. " +
                        "Skipping all zaken."
                )
                zaakUUIDs
                    .map(zrcClientService::readZaak)
                    .forEach { eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(it)) }
            }
            inGroup
        } ?: true

    private fun isZaakOpen(zaak: Zaak) =
        zaak.let {
            if (!it.isOpen()) {
                LOG.fine("Zaak with UUID '${zaak.uuid} is not open. Therefore it is skipped and not assigned.")
            }
            it.isOpen()
        }

    /**
     * Checks if the group is authorised for the specified zaaktype and the specified ZAC application role.
     *
     * @param zaaktypeUuid The zaaktype UUID to check domain access for
     * @return true if the group is authorised for the specified zaaktype, false otherwise
     */
    private fun Group.isAuthorisedForApplicationRoleAndZaaktype(
        zacApplicationRole: ZacApplicationRole,
        zaaktypeUuid: UUID
    ): Boolean {
        val zaaktype = ztcClientService.readZaaktype(zaaktypeUuid)
        return pabcClientService.getGroupsByApplicationRoleAndZaaktype(
            applicationRole = zacApplicationRole.value,
            // we use the zaaktype description as the unique identifier for zaaktypes in ZAC
            zaaktypeDescription = zaaktype.omschrijving
        ).map { it.name }.contains(this.name)
    }
}
