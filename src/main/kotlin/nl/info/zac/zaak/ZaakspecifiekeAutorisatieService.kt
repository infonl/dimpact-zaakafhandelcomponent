/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.pabc.ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.RolMedewerker
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.client.zgw.zrc.util.isZaakspecifiekGeautoriseerd
import nl.info.client.zgw.zrc.util.markZaakspecifiekGeautoriseerd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.zaak.exception.GroupNotBehandelaarForZaaktypeException
import nl.info.zac.app.zaak.exception.MedewerkerAlreadyZaakspecifiekGeautoriseerdException
import nl.info.zac.app.zaak.exception.ZaakNotZaakspecifiekGeautoriseerdException
import nl.info.zac.app.zaak.exception.ZaakWithoutBehandelaarCannotBeMarkedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieCannotBeLiftedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekeAutorisatieNotAllowedException
import nl.info.zac.app.zaak.exception.ZaaktypeNotZaakspecifiekAutoriseerbaarException
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.User
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.ZaakAutorisatieGegevens
import nl.info.zac.task.TaskHistoryService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.model.ZaakToewijzing
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@ApplicationScoped
@AllOpen
@NoArgConstructor
@Suppress("TooManyFunctions", "LongParameterList")
class ZaakspecifiekeAutorisatieService @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zgwApiService: ZgwApiService,
    private val indexingService: IndexingService,
    private val identityService: IdentityService,
    private val flowableTaskService: FlowableTaskService,
    private val taskHistoryService: TaskHistoryService
) {
    companion object {
        private const val ROLTOELICHTING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER =
            "Zaakspecifiek geautoriseerde medewerker van de zaak"

        private val zaakAssignmentLocks = Array(64) { ReentrantLock() }

        fun zaakspecifiekGeautoriseerdeMedewerkerToelichting(zaak: Zaak) =
            "Zaakspecifiek geautoriseerd medewerker van zaak ${zaak.identificatie}"
    }

    /**
     * The lock that serialises every change to the behandelaar and zaakspecifiek geautoriseerde medewerker
     * rollen of a zaak, whether it comes from assigning the zaak or one of its taken.
     */
    fun lockForZaak(zaakUuid: UUID) =
        zaakAssignmentLocks[Math.floorMod(zaakUuid.hashCode(), zaakAssignmentLocks.size)]

    fun isZaakspecifiekGeautoriseerd(zaak: Zaak) = zrcClientService.isZaakspecifiekGeautoriseerd(zaak.uuid)

    /**
     * Reads which employee a zaak is assigned to and which employees are individually authorised for it.
     *
     * @param rollen pre-fetched rollen for [zaak], to avoid a redundant `listRollen` call when the caller
     * already fetched all rollen for the zaak. When 'null', the rollen are fetched here.
     * @param isZaakspecifiekGeautoriseerd likewise for the marking of [zaak], which a caller that already
     * checked it can pass in. When 'null', it is read here.
     */
    fun readZaakToewijzing(
        zaak: Zaak,
        rollen: List<Rol<*>>? = null,
        isZaakspecifiekGeautoriseerd: Boolean? = null
    ): ZaakToewijzing =
        (rollen ?: zrcClientService.listRollen(zaak)).let { zaakRollen ->
            ZaakToewijzing(
                groep = zgwApiService.findGroepForZaak(zaak, zaakRollen),
                behandelaarRollen = zgwApiService.listBehandelaarMedewerkerRolesForZaak(zaak, zaakRollen),
                zaakspecifiekGeautoriseerdeMedewerkers =
                zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak, zaakRollen),
                isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd ?: isZaakspecifiekGeautoriseerd(zaak)
            )
        }

    /**
     * A zaaktype can only be zaakspecifiek geautoriseerd when its catalogus defines both the eigenschap that
     * marks a zaak and the roltype that grants an individual medewerker access to a marked zaak.
     */
    fun isZaakspecifiekAutoriseerbaar(zaakType: ZaakType) =
        ztcClientService.findEigenschap(
            zaaktype = zaakType.url,
            eigenschap = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
        ) != null &&
            zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url) != null

    @Suppress("ThrowsCount")
    fun shouldMarkZaakspecifiekGeautoriseerd(
        zaakType: ZaakType,
        requestedMarking: Boolean?,
        isAlreadyZaakspecifiekGeautoriseerd: Boolean,
        currentAndRequestedBehandelaarIds: Set<String>,
        loggedInUser: LoggedInUser
    ): Boolean = when {
        requestedMarking == null -> false
        !requestedMarking -> {
            if (isAlreadyZaakspecifiekGeautoriseerd) throw ZaakspecifiekeAutorisatieCannotBeLiftedException()
            false
        }
        isAlreadyZaakspecifiekGeautoriseerd -> false
        else -> {
            if (!isZaakspecifiekAutoriseerbaar(zaakType)) throw ZaaktypeNotZaakspecifiekAutoriseerbaarException()
            if (currentAndRequestedBehandelaarIds.isEmpty()) throw ZaakWithoutBehandelaarCannotBeMarkedException()
            if (loggedInUser.id !in currentAndRequestedBehandelaarIds &&
                !loggedInUser.isZaakspecifiekGeautoriseerdFor(zaakType.omschrijving)
            ) {
                throw ZaakspecifiekeAutorisatieNotAllowedException()
            }
            true
        }
    }

    /**
     * A zaakspecifiek geautoriseerde zaak can be handed over to another behandelaar but can never be released:
     * without a behandelaar nobody would be able to pick the zaak up again.
     */
    fun assertBehandelaarMayChange(zaakToewijzing: ZaakToewijzing, requestedBehandelaarId: String?) {
        if (zaakToewijzing.isZaakspecifiekGeautoriseerd &&
            zaakToewijzing.behandelaarRollen.isNotEmpty() &&
            requestedBehandelaarId.isNullOrEmpty()
        ) {
            throw ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException()
        }
    }

    /**
     * Lets a caller refuse a handover of a zaakspecifiek geautoriseerde zaak before it writes anything else of
     * the zaak, since keeping the previous behandelaar authorised needs a roltype that zaaktypen configured before
     * this roltype existed may lack.
     *
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaaktype does not define
     * the roltype
     */
    fun assertBehandelaarCanBeHandedOver(
        zaak: Zaak,
        isZaakspecifiekGeautoriseerd: Boolean,
        currentBehandelaarId: String?,
        requestedBehandelaarId: String?
    ) {
        if (isZaakspecifiekGeautoriseerd && currentBehandelaarId != null && currentBehandelaarId != requestedBehandelaarId) {
            readZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak)
        }
    }

    /**
     * Grants [medewerker] individual access to [zaak] by adding a zaakspecifiek geautoriseerde medewerker rol.
     * Does nothing when the medewerker already holds one.
     *
     * @param zaakspecifiekGeautoriseerdeMedewerkers the rollen [zaak] already has, for a caller that read
     * them before. When 'null', they are read here.
     * @return true when a rol was added
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaaktype does not define
     * the roltype
     */
    fun grantZaakspecifiekeAutorisatie(
        zaak: Zaak,
        medewerker: MedewerkerIdentificatie,
        reason: String?,
        zaakspecifiekGeautoriseerdeMedewerkers: List<RolMedewerker>? = null
    ): Boolean {
        val medewerkerId = medewerker.identificatie ?: return false
        val roltype = readZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak)
        val isAlreadyGeautoriseerd = (
            zaakspecifiekGeautoriseerdeMedewerkers
                ?: zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak)
            ).any { it.identificatienummer == medewerkerId }
        if (!isAlreadyGeautoriseerd) {
            zrcClientService.createRol(
                RolMedewerker(
                    zaak = zaak.url,
                    roltype = roltype,
                    roltoelichting = ROLTOELICHTING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER,
                    medewerkerIdentificatie = MedewerkerIdentificatie().apply {
                        identificatie = medewerkerId
                        voorletters = medewerker.voorletters
                        voorvoegselAchternaam = medewerker.voorvoegselAchternaam
                        achternaam = medewerker.achternaam
                    }
                ),
                reason
            )
        }
        return !isAlreadyGeautoriseerd
    }

    /**
     * Grants a taakbehandelaar individual access to the zaakspecifiek geautoriseerde [zaak] of their taak. The
     * zaakbehandelaar and a medewerker who already holds a zaakspecifiek geautoriseerde medewerker rol get
     * nothing, since they already have access.
     *
     * @return true when a rol was added
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaaktype does not define
     * the roltype
     */
    fun grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak: Zaak, medewerkerId: String): Boolean {
        if (!isZaakspecifiekGeautoriseerd(zaak)) return false
        val isGranted = lockForZaak(zaak.uuid).withLock {
            val zaakToewijzing = readZaakToewijzing(zaak = zaak, isZaakspecifiekGeautoriseerd = true)
            zaakToewijzing.behandelaarRollen.none { it.identificatienummer == medewerkerId } &&
                grantZaakspecifiekeAutorisatie(
                    zaak = zaak,
                    medewerker = readMedewerkerIdentificatie(medewerkerId),
                    reason = zaakspecifiekGeautoriseerdeMedewerkerToelichting(zaak),
                    zaakspecifiekGeautoriseerdeMedewerkers = zaakToewijzing.zaakspecifiekGeautoriseerdeMedewerkers
                )
        }
        if (isGranted) {
            indexingService.addOrUpdateZaak(zaak.uuid, inclusiefTaken = false)
            reindexDependents(zaak)
        }
        return isGranted
    }

    /**
     * Lists the members of [groepId] who can still be added to the zaakspecifiek geautoriseerde [zaak], leaving out
     * everyone who already has access to it.
     */
    fun listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(zaak: Zaak, zaakType: ZaakType, groepId: String): List<User> {
        assertZaakspecifiekGeautoriseerd(zaak)
        assertBehandelaarGroep(zaakType, groepId)
        val medewerkerIdsWithAccess = readZaakToewijzing(zaak = zaak, isZaakspecifiekGeautoriseerd = true)
            .geautoriseerdeMedewerkerIds + listZaakspecifiekGeautoriseerdeMedewerkerIdsForZaaktype(zaakType)
        return identityService.listUsersInGroup(groepId).filter { it.id !in medewerkerIdsWithAccess }
    }

    /**
     * Grants [medewerkerId], a member of the behandelaar group [groepId], individual access to the zaakspecifiek
     * geautoriseerde [zaak].
     *
     * @throws MedewerkerAlreadyZaakspecifiekGeautoriseerdException when the medewerker already has access
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaaktype does not define
     * the roltype
     */
    fun addZaakspecifiekGeautoriseerdeMedewerker(zaak: Zaak, zaakType: ZaakType, groepId: String, medewerkerId: String) {
        assertZaakspecifiekGeautoriseerd(zaak)
        assertBehandelaarGroep(zaakType, groepId)
        identityService.validateIfUserIsInGroup(medewerkerId, groepId)
        if (medewerkerId in listZaakspecifiekGeautoriseerdeMedewerkerIdsForZaaktype(zaakType)) {
            throw MedewerkerAlreadyZaakspecifiekGeautoriseerdException()
        }
        lockForZaak(zaak.uuid).withLock {
            val zaakToewijzing = readZaakToewijzing(zaak = zaak, isZaakspecifiekGeautoriseerd = true)
            if (zaakToewijzing.isGeautoriseerdeMedewerker(medewerkerId)) {
                throw MedewerkerAlreadyZaakspecifiekGeautoriseerdException()
            }
            grantZaakspecifiekeAutorisatie(
                zaak = zaak,
                medewerker = readMedewerkerIdentificatie(medewerkerId),
                reason = zaakspecifiekGeautoriseerdeMedewerkerToelichting(zaak),
                zaakspecifiekGeautoriseerdeMedewerkers = zaakToewijzing.zaakspecifiekGeautoriseerdeMedewerkers
            )
        }
        indexingService.addOrUpdateZaak(zaak.uuid, inclusiefTaken = false)
        reindexDependents(zaak)
    }

    fun markZaakspecifiekGeautoriseerd(zaak: Zaak) {
        zrcClientService.markZaakspecifiekGeautoriseerd(zaak, ztcClientService)
        flowableTaskService.listOpenTasksForZaak(zaak.uuid)
            .filter { it.assignee != null }
            .distinctBy { it.assignee }
            .forEach {
                if (grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, it.assignee)) {
                    taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(it, zaak, it.assignee)
                }
            }
        indexingService.addOrUpdateZaak(zaak.uuid, inclusiefTaken = false)
        reindexDependents(zaak)
    }

    fun reindexZaakspecifiekeAutorisatieDependents(zaak: Zaak) {
        if (!isZaakspecifiekGeautoriseerd(zaak)) return
        reindexDependents(zaak)
    }

    private fun readZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak: Zaak) =
        zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak.zaaktype)
            ?: throw ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException(
                "Roltype '${ZgwApiService.ROLTYPE_OMSCHRIJVING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER}' not found " +
                    "for zaaktype '${zaak.zaaktype}' of zaak with UUID '${zaak.uuid}'"
            )

    private fun assertZaakspecifiekGeautoriseerd(zaak: Zaak) {
        if (!isZaakspecifiekGeautoriseerd(zaak)) throw ZaakNotZaakspecifiekGeautoriseerdException()
    }

    private fun assertBehandelaarGroep(zaakType: ZaakType, groepId: String) {
        if (identityService.listActiveGroupsForBehandelaarRoleAndZaaktype(zaakType.omschrijving).none { it.name == groepId }) {
            throw GroupNotBehandelaarForZaaktypeException()
        }
    }

    private fun listZaakspecifiekGeautoriseerdeMedewerkerIdsForZaaktype(zaakType: ZaakType) =
        identityService.listUserIdsForApplicationRoleAndZaaktype(
            applicationRole = ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD,
            zaaktypeDescription = zaakType.omschrijving
        )

    private fun readMedewerkerIdentificatie(medewerkerId: String) =
        identityService.readUser(medewerkerId).let { user ->
            MedewerkerIdentificatie().apply {
                identificatie = user.id
                voorletters = user.firstName
                achternaam = user.lastName
            }
        }

    private fun reindexDependents(zaak: Zaak) {
        indexingService.addOrUpdateTakenForZaak(zaak.uuid)
        indexingService.addOrUpdateInformatieobjectenForZaak(zaak.uuid)
    }

    private fun LoggedInUser.isZaakspecifiekGeautoriseerdFor(zaaktypeOmschrijving: String) =
        ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD in overallRoles ||
            ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD in applicationRolesPerZaaktype[zaaktypeOmschrijving].orEmpty()
}

/**
 * The single place where it is decided whether [zaak] is zaakspecifiek geautoriseerd and which medewerkers are
 * individually authorised for it, so that the rechten and the search index always agree. The medewerkers are only
 * read from the zaakregister when they are used.
 *
 * This is an extension function rather than a member, so that the derivation itself also runs in unit tests that
 * mock [ZaakspecifiekeAutorisatieService].
 */
fun ZaakspecifiekeAutorisatieService.readZaakAutorisatieGegevens(zrcClientService: ZrcClientService, zaak: Zaak) =
    readZaakAutorisatieGegevens(zrcClientService = zrcClientService, zaakUuid = zaak.uuid) { zaak }

/**
 * Variant of [readZaakAutorisatieGegevens] for callers that only know the UUID of the zaak; the zaak itself is only
 * read when the medewerkers are used.
 */
fun ZaakspecifiekeAutorisatieService.readZaakAutorisatieGegevens(
    zrcClientService: ZrcClientService,
    zaakUuid: UUID,
    zaakSupplier: () -> Zaak = { zrcClientService.readZaak(zaakUuid) }
) = zrcClientService.isZaakspecifiekGeautoriseerd(zaakUuid).let { isZaakspecifiekGeautoriseerd ->
    ZaakAutorisatieGegevens(isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd) {
        readZaakToewijzing(zaak = zaakSupplier(), isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd)
            .geautoriseerdeMedewerkerIds
            .toList()
    }
}
