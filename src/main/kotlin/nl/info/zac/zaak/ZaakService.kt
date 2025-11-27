/*
* SPDX-FileCopyrightText: 2024 INFO.nl
* SPDX-License-Identifier: EUPL-1.2+
*/
package nl.info.zac.zaak

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakEigenschap
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.ztc.model.generated.BrondatumArchiefprocedure
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.zaak.ZaakRestService.Companion.VESTIGING_IDENTIFICATIE_DELIMITER
import nl.info.zac.app.zaak.model.toRestResultaatTypes
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.identity.model.ZacApplicationRole
import nl.info.zac.identity.model.getFullName
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.zaak.exception.BetrokkeneIsAlreadyAddedToZaakException
import nl.info.zac.zaak.exception.CaseHasLockedInformationObjectsException
import nl.info.zac.zaak.model.Betrokkenen.BETROKKENEN_ENUMSET
import java.lang.Boolean
import java.net.URI
import java.util.Locale
import java.util.UUID
import java.util.logging.Logger
import kotlin.String
import kotlin.Suppress

private val LOG = Logger.getLogger(ZaakService::class.java.name)

@AllOpen
@Suppress("TooManyFunctions", "LongParameterList")
class ZaakService @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zgwApiService: ZGWApiService,
    private var eventingService: EventingService,
    private var zaakVariabelenService: ZaakVariabelenService,
    private val lockService: EnkelvoudigInformatieObjectLockService,
    private val identityService: IdentityService,
    private val indexingService: IndexingService,
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService,
    private val bpmnService: BpmnService,
    private val configuratieService: ConfiguratieService
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

        val zakenAssignedList = zaakUUIDs
            .map(zrcClientService::readZaak)
            .filter { isZaakOpen(it) && group.hasDomainAccess(it) }
            .onEach { zaak ->
                zrcClientService.updateRol(zaak, bepaalRolGroep(group, zaak), explanation)
                user?.let {
                    zrcClientService.updateRol(zaak, bepaalRolMedewerker(it, zaak), explanation)
                } ?: zrcClientService.deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, explanation)
            }
            .map { it.uuid }

        LOG.fine { "Successfully assigned ${zakenAssignedList.size} zaken." }

        // if a screen event resource ID was specified, send an 'updated zaken_verdelen' screen event
        // with the job UUID so that it can be picked up by a client
        // that has created a websocket subscription to this event
        screenEventResourceId?.let {
            LOG.fine { "Sending 'ZAKEN_VERDELEN' updated screen event with ID '$it'." }
            eventingService.send(ScreenEventType.ZAKEN_VERDELEN.updated(it))
        }
    }

    fun assignZaak(zaak: Zaak, groupId: String, user: String?, reason: String?) {
        user?.let {
            identityService.validateIfUserIsInGroup(it, groupId)
        }

        val behandelaar = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            ?.betrokkeneIdentificatie?.identificatie
        val (user, userUpdated) = assignUser(zaak, user, reason, behandelaar)
        val (group, groupUpdated) = assignGroup(zaak, groupId, reason)
        changeZaakDataAssignment(zaak.uuid, group, user)
        if (userUpdated || groupUpdated) {
            indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
        }
    }

    private fun assignUser(
        zaak: Zaak,
        userName: String?,
        reason: String?,
        behandelaar: String?
    ): Pair<User?, kotlin.Boolean> {
        var user: User? = null
        var userUpdated = false
        userName?.takeIf { it.isNotEmpty() }?.let {
            user = identityService.readUser(it)
            if (behandelaar != userName) {
                zrcClientService.updateRol(
                    zaak,
                    bepaalRolMedewerker(user, zaak),
                    reason
                )
                userUpdated = true
            }
        } ?: zrcClientService.deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, reason).also {
            userUpdated = true
        }
        return Pair(user, userUpdated)
    }

    private fun assignGroup(
        zaak: Zaak,
        groupId: String,
        reason: String?
    ): Pair<Group, kotlin.Boolean> {
        var groupUpdated = false
        val group = identityService.readGroup(groupId)
        zgwApiService.findGroepForZaak(zaak)?.betrokkeneIdentificatie?.identificatie.let { currentGroupId ->
            // if the zaak is not already assigned to the requested group, assign it to this group
            if (currentGroupId == null || currentGroupId != groupId) {
                val role = bepaalRolGroep(group, zaak)
                zrcClientService.updateRol(zaak, role, reason)
                groupUpdated = true
            }
        }
        return Pair(group, groupUpdated)
    }

    private fun changeZaakDataAssignment(
        zaakUuid: UUID,
        group: Group,
        user: User?
    ) {
        if (bpmnService.isZaakProcessDriven(zaakUuid)) {
            zaakVariabelenService.setGroup(zaakUuid, group.name)
            user?.let {
                zaakVariabelenService.setUser(zaakUuid, it.getFullName())
            } ?: zaakVariabelenService.removeUser(zaakUuid)
        }
    }

    fun assignZaakToLoggedInUser(zaak: Zaak, groupId: String, userId: String, reason: String?) {
        identityService.validateIfUserIsInGroup(userId, groupId)

        val user = assignLoggedInUserToZaak(zaak, userId, reason)
        val (group) = assignGroup(
            zaak = zaak,
            groupId = groupId,
            reason = reason
        )
        changeZaakDataAssignment(zaak.uuid, group, user)
        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
    }

    private fun assignLoggedInUserToZaak(zaak: Zaak, userId: String, reason: String?) =
        identityService.readUser(userId).also {
            zrcClientService.updateRol(
                zaak = zaak,
                rol = bepaalRolMedewerker(it, zaak),
                toelichting = reason
            )
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

    private fun isUserInGroup(
        user: User?,
        group: Group,
        zaakUUIDs: List<UUID>
    ) =
        user?.let {
            val inGroup = identityService.isUserInGroup(user.id, group.id)
            if (!inGroup) {
                LOG.warning(
                    "User '${user.displayName}' (id: {$user.id}) is not in the group '${group.name}'. " +
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
                eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaak))
            }
            it.isOpen()
        }

    /**
     * Checks if the group is authorised for the domain associated with the specified zaak, through the
     * zaakafhandelparameters of the zaaktype.
     * This function currently only works for the old IAM architecture.
     * In the new IAM architecture, zaaktype authorisation for groups is not yet supported.
     * This first needs to be implemented by the PABC.
     *
     * Domain access is granted to a:
     * - zaaktype without domain
     * - zaaktype with domain/role DOMEIN_ELK_ZAAKTYPE
     * - group with domain/role DOMEIN_ELK_ZAAKTYPE has access to all domains
     * - group with one (or more) specific domains only access to zaaktype with this certain (or more) domain
     *
     * @param zaak The zaak to check domain access for
     * @return true if the group has access to the zaak's domain, false otherwise
     */
    private fun Group.hasDomainAccess(zaak: Zaak) =
        if (configuratieService.featureFlagPabcIntegration()) {
            // In the new IAM architecture, zaaktype authorisation for groups is not yet supported.
            // This first needs to be implemented by the PABC.
            true
        } else {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid()).let { params ->
                val hasAccess = params.domein == ZacApplicationRole.DOMEIN_ELK_ZAAKTYPE.value ||
                    this.zacClientRoles.contains(ZacApplicationRole.DOMEIN_ELK_ZAAKTYPE.value) ||
                    params.domein?.let {
                        this.zacClientRoles.contains(it)
                    } ?: false
                if (!hasAccess) {
                    LOG.fine(
                        "Zaak with UUID '${zaak.uuid}' is skipped and not assigned. Group '${this.name}' " +
                            "with roles '${this.zacClientRoles}' has no access to domain '${params.domein}'"
                    )
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaak))
                }
                hasAccess
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
            OrganisatorischeEenheidIdentificatie().apply {
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
            .filter {
                if (!it.isOpen()) {
                    LOG.fine("Zaak with UUID '${it.uuid} is not open. Therefore it is not released.")
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(it))
                }
                it.isOpen()
            }
            .forEach { zrcClientService.deleteRol(it, BetrokkeneTypeEnum.MEDEWERKER, explanation) }
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

    fun processBrondatumProcedure(zaak: Zaak, resultaatTypeUUID: UUID, brondatumArchiefprocedure: BrondatumArchiefprocedure) {
        val resultaattype = ztcClientService.readResultaattype(resultaatTypeUUID)

        when (resultaattype.brondatumArchiefprocedure.afleidingswijze) {
            AfleidingswijzeEnum.EIGENSCHAP -> {
                if (brondatumArchiefprocedure.datumkenmerk.isNullOrBlank()) {
                    throw InputValidationFailedException(
                        errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
                        message = """
                    'brondatumEigenschap' moet gevuld zijn bij het afhandelen van een zaak met een resultaattype dat
                    een 'brondatumArchiefprocedure' heeft met 'afleidingswijze' 'EIGENSCHAP'.
                        """.trimIndent()
                    )
                }
                this.upsertEigenschapToZaak(
                    resultaattype.brondatumArchiefprocedure.datumkenmerk,
                    brondatumArchiefprocedure.datumkenmerk,
                    zaak
                )
            }
            else -> null
        }
    }

    private fun upsertEigenschapToZaak(eigenschap: String, waarde: String, zaak: Zaak) {
        zrcClientService.listZaakeigenschappen(zaak.uuid).firstOrNull { it.naam == eigenschap }?.let {
            zrcClientService.updateZaakeigenschap(
                zaak.uuid, it.uuid,
                it.apply {
                    this.waarde = waarde
                }
            )
        } ?: run {
            ztcClientService.readEigenschap(zaak.zaaktype, eigenschap).let {
                val zaakEigenschap = ZaakEigenschap().apply {
                    this.eigenschap = it.url
                    this.zaak = zaak.url
                    this.waarde = waarde
                }
                zrcClientService.createEigenschap(zaak.uuid, zaakEigenschap)
            }
        }
    }

    fun listStatusTypes(zaaktypeUUID: UUID) =
        ztcClientService.readStatustypen(
            ztcClientService.readZaaktype(zaaktypeUUID).url
        ).toRestResultaatTypes()

    fun listResultTypes(zaaktypeUUID: UUID) =
        ztcClientService.readResultaattypen(
            ztcClientService.readZaaktype(zaaktypeUUID).url
        ).toRestResultaatTypes()
}
