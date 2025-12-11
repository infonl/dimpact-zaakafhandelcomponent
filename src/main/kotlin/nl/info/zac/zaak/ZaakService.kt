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
import nl.info.client.pabc.PabcClientService
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
import nl.info.zac.identity.model.ZacApplicationRole.BEHANDELAAR
import nl.info.zac.identity.model.ZacApplicationRole.DOMEIN_ELK_ZAAKTYPE
import nl.info.zac.identity.model.getFullName
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.zaak.exception.BetrokkeneIsAlreadyAddedToZaakException
import nl.info.zac.zaak.exception.CaseHasLockedInformationObjectsException
import nl.info.zac.zaak.model.Betrokkenen.BETROKKENEN_ENUMSET
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
    private val configuratieService: ConfiguratieService,
    private val pabcClientService: PabcClientService
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

        val (zakenAssignedList, zakenToSkip) = zaakUUIDs
            .map(zrcClientService::readZaak)
            .partition {
                isZaakOpen(it) && group.isAuthorisedForApplicationRoleAndZaaktype(
                    // you are only allowed to assign zaken to 'behandelaren'
                    zacApplicationRole = BEHANDELAAR,
                    zaaktypeUuid = it.zaaktype.extractUuid()
                )
            }
        zakenToSkip
            .onEach { eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(it)) }
        zakenAssignedList
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

    /**
     * Assign a single zaak to a group and/or user.
     *
     * @param zaak The zaak to assign.
     * @param groupId The ID of the group to assign the zaak to.
     * @param userName The username of the user to assign the zaak to. If null, the user will be removed from the zaak.
     * @param reason The reason for the assignment.
     */
    fun assignZaak(zaak: Zaak, groupId: String, userName: String?, reason: String?) {
        userName?.let {
            identityService.validateIfUserIsInGroup(it, groupId)
        }

        var userAssigned = false
        val user: User? = userName?.takeIf { it.isNotEmpty() }?.let {
            identityService.readUser(userName).let {
                userAssigned = assignUser(zaak, it, reason)
                it
            }
        }

        // No user should be assigned - delete the role
        var userDeleted = false
        if (user == null) {
            zrcClientService.deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, reason)
            userDeleted = true
        }

        val group = identityService.readGroup(groupId)
        val groupAssigned = assignGroup(zaak, group, reason)

        changeZaakDataAssignment(zaak.uuid, group, user)

        if (userAssigned || userDeleted || groupAssigned) {
            indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
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

    fun checkZaakHasLockedInformationObjects(zaak: Zaak) {
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
            zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, true)
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

    private fun assignGroup(
        zaak: Zaak,
        group: Group,
        reason: String?
    ): Boolean =
        zgwApiService.findGroepForZaak(zaak)?.betrokkeneIdentificatie?.identificatie.let { currentGroupId ->
            if (currentGroupId == null || currentGroupId != group.id) {
                // if the zaak is not already assigned to the requested group, assign it to this group
                zrcClientService.updateRol(zaak, bepaalRolGroep(group, zaak), reason)
                true
            } else {
                false
            }
        }

    private fun assignUser(
        zaak: Zaak,
        user: User,
        reason: String?,
    ): Boolean =
        zgwApiService.findBehandelaarMedewerkerRoleForZaak(
            zaak
        )?.betrokkeneIdentificatie?.identificatie.let { behandelaar ->
            if (behandelaar != user.id) {
                zrcClientService.updateRol(zaak, bepaalRolMedewerker(user, zaak), reason)
                true
            } else {
                false
            }
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
            }
            it.isOpen()
        }

    /**
     * New IAM architecture (PABC integration): checks if the group is authorised for the specified zaaktype
     * and the specified ZAC application role.
     * Old IAM architecture: checks if the group is authorised for the specified zaaktype, using the domain associated
     * with the specified zaak, through the zaakafhandelparameters of the zaaktype.
     *
     * Domain access is granted to a:
     * - zaaktype without domain
     * - zaaktype with domain/role DOMEIN_ELK_ZAAKTYPE
     * - group with domain/role DOMEIN_ELK_ZAAKTYPE has access to all domains
     * - group with one (or more) specific domains only access to zaaktype with this certain (or more) domain
     *
     * @param zaaktypeUuid The zaaktype UUID to check domain access for
     * @return true if the group is authorised for the specified zaaktype, false otherwise
     */
    private fun Group.isAuthorisedForApplicationRoleAndZaaktype(
        zacApplicationRole: ZacApplicationRole,
        zaaktypeUuid: UUID
    ) =
        if (configuratieService.featureFlagPabcIntegration()) {
            val zaaktype = ztcClientService.readZaaktype(zaaktypeUuid)
            pabcClientService.getGroupsByApplicationRoleAndZaaktype(
                applicationRole = zacApplicationRole.value,
                // we use the generic zaaktype description as the unique identifier for zaaktypes in ZAC
                zaaktypeDescription = zaaktype.omschrijvingGeneriek
            ).map { it.name }.contains(this.id)
        } else {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid).let { params ->
                val hasAccess = params.domein == DOMEIN_ELK_ZAAKTYPE.value ||
                    this.zacClientRoles.contains(DOMEIN_ELK_ZAAKTYPE.value) ||
                    params.domein?.let {
                        this.zacClientRoles.contains(it)
                    } ?: false
                if (!hasAccess) {
                    LOG.fine(
                        "Zaaktype with UUID '$zaaktypeUuid' is skipped and not assigned. Group '${this.name}' " +
                            "with roles '${this.zacClientRoles}' has no access to domain '${params.domein}'"
                    )
                }
                hasAccess
            }
        }
}
