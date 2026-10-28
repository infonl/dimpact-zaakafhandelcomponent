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
import nl.info.client.zgw.zrc.util.unmarkZaakspecifiekGeautoriseerd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.flowable.task.FlowableTaskService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.User
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.ZaakAutorisatieGegevens
import nl.info.zac.task.TaskHistoryService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.exception.GroupNotBehandelaarForZaaktypeException
import nl.info.zac.zaak.exception.MedewerkerAlreadyZaakspecifiekGeautoriseerdException
import nl.info.zac.zaak.exception.MedewerkerCannotBeDetachedException
import nl.info.zac.zaak.exception.MedewerkerNotZaakspecifiekGeautoriseerdException
import nl.info.zac.zaak.exception.ZaakNotZaakspecifiekGeautoriseerdException
import nl.info.zac.zaak.exception.ZaakWithoutBehandelaarCannotBeMarkedException
import nl.info.zac.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import nl.info.zac.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException
import nl.info.zac.zaak.exception.ZaakspecifiekeAutorisatieNotAllowedException
import nl.info.zac.zaak.exception.ZaaktypeNotZaakspecifiekAutoriseerbaarException
import nl.info.zac.zaak.model.ZaakToewijzing
import nl.info.zac.zaak.model.ZaakspecifiekGeautoriseerdeMedewerkerRol
import org.flowable.task.api.TaskInfo
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
        const val ROLTOELICHTING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER =
            "Zaakspecifiek geautoriseerde medewerker van de zaak"

        private val zaakAssignmentLocks = Array(64) { ReentrantLock() }
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
        requestedMarking != true || isAlreadyZaakspecifiekGeautoriseerd -> false
        else -> {
            if (!isZaakspecifiekAutoriseerbaar(zaakType)) throw ZaaktypeNotZaakspecifiekAutoriseerbaarException()
            if (currentAndRequestedBehandelaarIds.isEmpty()) throw ZaakWithoutBehandelaarCannotBeMarkedException()
            if (loggedInUser.username !in currentAndRequestedBehandelaarIds &&
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
     * @param task the taak whose taakhistorie records the added rol. 'null' for a caller that grants the rol
     * before the taak exists, and records it itself once the taak is created.
     * @return true when a rol was added
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaaktype does not define
     * the roltype
     */
    fun grantZaakspecifiekeAutorisatieToTaakbehandelaar(
        zaak: Zaak,
        medewerkerId: String,
        task: TaskInfo? = null
    ): Boolean {
        if (!isZaakspecifiekGeautoriseerd(zaak)) return false
        val isGranted = lockForZaak(zaak.uuid).withLock {
            val zaakToewijzing = readZaakToewijzing(zaak = zaak)
            val isGranted = zaakToewijzing.isZaakspecifiekGeautoriseerd &&
                zaakToewijzing.behandelaarRollen.none { it.identificatienummer == medewerkerId } &&
                grantZaakspecifiekeAutorisatie(
                    zaak = zaak,
                    medewerker = readMedewerkerIdentificatie(medewerkerId = medewerkerId, zaak = zaak),
                    reason = ROLTOELICHTING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER,
                    zaakspecifiekGeautoriseerdeMedewerkers = zaakToewijzing.zaakspecifiekGeautoriseerdeMedewerkers
                )
            if (isGranted) indexingService.addOrUpdateZaakWithTakenAndInformatieobjecten(zaak.uuid)
            isGranted
        }
        if (isGranted && task != null) {
            taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(task, medewerkerId)
        }
        return isGranted
    }

    /**
     * Lists the behandelaar and zaakspecifiek geautoriseerde medewerker rollen of the zaakspecifiek geautoriseerde
     * [zaak], sorted by name, with one row per medewerker. Medewerkers who have access to all zaakspecifiek
     * geautoriseerde zaken of the zaaktype through their application role are left out, since they hold no rol on
     * the zaak, and so are rollen without a medewerker identificatie, which only come from outside ZAC.
     */
    fun listGeautoriseerdeMedewerkerRollen(
        zaak: Zaak,
        zaakAutorisatieGegevens: ZaakAutorisatieGegevens
    ): List<ZaakspecifiekGeautoriseerdeMedewerkerRol> {
        assertZaakspecifiekGeautoriseerd(zaakAutorisatieGegevens.isZaakspecifiekGeautoriseerd)
        val zaakToewijzing = readZaakToewijzing(zaak = zaak, isZaakspecifiekGeautoriseerd = true)
        val nonDetachableMedewerkerIds = listNonDetachableMedewerkerIds(zaak, zaakToewijzing)
        val behandelaars = zaakToewijzing.behandelaarRollen.toZaakspecifiekGeautoriseerdeMedewerkerRollen { false }
        val geautoriseerdeMedewerkers = zaakToewijzing.zaakspecifiekGeautoriseerdeMedewerkers
            .toZaakspecifiekGeautoriseerdeMedewerkerRollen { medewerkerId -> medewerkerId !in nonDetachableMedewerkerIds }
        return (behandelaars + geautoriseerdeMedewerkers)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.rol.naam.orEmpty() })
    }

    /**
     * Takes away the individual access of [medewerkerId] to the zaakspecifiek geautoriseerde [zaak], by deleting
     * every zaakspecifiek geautoriseerde medewerker rol they hold on it. A concurrent addition can have left more
     * than one.
     *
     * @throws MedewerkerCannotBeDetachedException when the medewerker is the zaakbehandelaar or the behandelaar
     * of an open taak of the zaak
     * @throws MedewerkerNotZaakspecifiekGeautoriseerdException when the medewerker holds no such rol
     */
    fun removeZaakspecifiekGeautoriseerdeMedewerker(
        zaak: Zaak,
        zaakAutorisatieGegevens: ZaakAutorisatieGegevens,
        medewerkerId: String,
        reden: String
    ) {
        assertZaakspecifiekGeautoriseerd(zaakAutorisatieGegevens.isZaakspecifiekGeautoriseerd)
        lockForZaak(zaak.uuid).withLock {
            val zaakToewijzing = readZaakToewijzing(zaak = zaak, isZaakspecifiekGeautoriseerd = true)
            if (medewerkerId in listNonDetachableMedewerkerIds(zaak, zaakToewijzing)) {
                throw MedewerkerCannotBeDetachedException()
            }
            zaakToewijzing.zaakspecifiekGeautoriseerdeMedewerkers
                .filter { it.identificatienummer == medewerkerId }
                .ifEmpty { throw MedewerkerNotZaakspecifiekGeautoriseerdException() }
                .forEach { zrcClientService.deleteRol(it, reden) }
            indexingService.addOrUpdateZaakWithTakenAndInformatieobjecten(zaak.uuid)
            indexingService.commit()
        }
    }

    /**
     * Lists the members of [groepId] who can still be added to the zaakspecifiek geautoriseerde zaak, leaving out
     * everyone who already has access to it.
     */
    fun listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(
        zaakAutorisatieGegevens: ZaakAutorisatieGegevens,
        zaakType: ZaakType,
        groepId: String
    ): List<User> {
        assertZaakspecifiekGeautoriseerd(zaakAutorisatieGegevens.isZaakspecifiekGeautoriseerd)
        assertBehandelaarGroep(zaakType, groepId)
        val medewerkerIdsWithAccess = zaakAutorisatieGegevens.geautoriseerdeMedewerkers.toSet() +
            listZaakspecifiekGeautoriseerdeMedewerkerIdsForZaaktype(zaakType)
        return identityService.listUsersInGroup(groepId).filter { it.username !in medewerkerIdsWithAccess }
    }

    /**
     * Grants [medewerkerId], a member of the behandelaar group [groepId], individual access to the zaakspecifiek
     * geautoriseerde [zaak].
     *
     * @return the added medewerker
     * @throws MedewerkerAlreadyZaakspecifiekGeautoriseerdException when the medewerker already has access
     * @throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException when the zaaktype does not define
     * the roltype
     */
    fun addZaakspecifiekGeautoriseerdeMedewerker(
        zaak: Zaak,
        zaakType: ZaakType,
        zaakAutorisatieGegevens: ZaakAutorisatieGegevens,
        groepId: String,
        medewerkerId: String
    ): User {
        assertZaakspecifiekGeautoriseerd(zaakAutorisatieGegevens.isZaakspecifiekGeautoriseerd)
        assertBehandelaarGroep(zaakType, groepId)
        identityService.validateIfUserIsInGroup(medewerkerId, groepId)
        if (medewerkerId in listZaakspecifiekGeautoriseerdeMedewerkerIdsForZaaktype(zaakType)) {
            throw MedewerkerAlreadyZaakspecifiekGeautoriseerdException()
        }
        return lockForZaak(zaak.uuid).withLock {
            val zaakToewijzing = readZaakToewijzing(zaak = zaak)
            assertZaakspecifiekGeautoriseerd(zaakToewijzing.isZaakspecifiekGeautoriseerd)
            if (zaakToewijzing.isGeautoriseerdeMedewerker(medewerkerId)) {
                throw MedewerkerAlreadyZaakspecifiekGeautoriseerdException()
            }
            identityService.readUser(username = medewerkerId, logContext = "zaak '${zaak.identificatie}'").also {
                grantZaakspecifiekeAutorisatie(
                    zaak = zaak,
                    medewerker = it.toMedewerkerIdentificatie(),
                    reason = ROLTOELICHTING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER,
                    zaakspecifiekGeautoriseerdeMedewerkers = zaakToewijzing.zaakspecifiekGeautoriseerdeMedewerkers
                )
                indexingService.addOrUpdateZaakWithTakenAndInformatieobjecten(zaak.uuid)
            }
        }
    }

    fun markZaakspecifiekGeautoriseerd(zaak: Zaak) {
        zrcClientService.markZaakspecifiekGeautoriseerd(zaak, ztcClientService)
        flowableTaskService.listOpenTasksForZaak(zaak.uuid)
            .filter { it.assignee != null }
            .distinctBy { it.assignee }
            .forEach { grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak = zaak, medewerkerId = it.assignee, task = it) }
        indexingService.addOrUpdateZaakWithTakenAndInformatieobjecten(zaak.uuid)
    }

    /**
     * Removes zaakspecifieke autorisatie from a [zaak]. The rollen go before the marking, so that a failure halfway
     * leaves a zaak that is still marked, rather than an unmarked zaak whose rollen would grant access again once it
     * is marked anew.
     */
    fun removeZaakspecifiekeAutorisatie(zaak: Zaak, reason: String) {
        lockForZaak(zaak.uuid).withLock {
            zgwApiService.listZaakspecifiekGeautoriseerdeMedewerkerRolesForZaak(zaak)
                .forEach { zrcClientService.deleteRol(it, reason) }
            zrcClientService.unmarkZaakspecifiekGeautoriseerd(zaak.uuid, reason)
            indexingService.addOrUpdateZaakWithTakenAndInformatieobjecten(zaak.uuid)
            indexingService.commit()
        }
    }

    fun reindexZaakspecifiekeAutorisatieDependents(zaak: Zaak) {
        if (!isZaakspecifiekGeautoriseerd(zaak)) return
        indexingService.addOrUpdateTakenAndInformatieobjectenForZaak(zaak.uuid)
    }

    private fun readZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak: Zaak) =
        zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaak.zaaktype)
            ?: throw ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException(
                "Roltype '${ZgwApiService.ROLTYPE_OMSCHRIJVING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER}' not found " +
                    "for zaaktype '${zaak.zaaktype}' of zaak with UUID '${zaak.uuid}'"
            )

    private fun assertZaakspecifiekGeautoriseerd(isZaakspecifiekGeautoriseerd: Boolean) {
        if (!isZaakspecifiekGeautoriseerd) throw ZaakNotZaakspecifiekGeautoriseerdException()
    }

    private fun assertBehandelaarGroep(zaakType: ZaakType, groepId: String) {
        if (identityService.listActiveGroupsForBehandelaarRoleAndZaaktype(zaakType.omschrijving).none { it.name == groepId }) {
            throw GroupNotBehandelaarForZaaktypeException()
        }
    }

    private fun listZaakspecifiekGeautoriseerdeMedewerkerIdsForZaaktype(zaakType: ZaakType) =
        identityService.listUsernamesForApplicationRoleAndZaaktype(
            applicationRole = ROLE_NAME_ZAAKSPECIFIEK_GEAUTORISEERD,
            zaaktypeDescription = zaakType.omschrijving
        )

    private fun listNonDetachableMedewerkerIds(zaak: Zaak, zaakToewijzing: ZaakToewijzing) =
        zaakToewijzing.behandelaarRollen.mapNotNull { it.identificatienummer }.toSet() +
            flowableTaskService.listOpenTasksForZaak(zaak.uuid).mapNotNull { it.assignee }

    private fun readMedewerkerIdentificatie(medewerkerId: String, zaak: Zaak) =
        identityService.readUser(username = medewerkerId, logContext = "zaak '${zaak.identificatie}'")
            .toMedewerkerIdentificatie()

    private fun User.toMedewerkerIdentificatie() = MedewerkerIdentificatie().apply {
        identificatie = this@toMedewerkerIdentificatie.username
        voorletters = firstName
        achternaam = lastName
    }

    private fun List<RolMedewerker>.toZaakspecifiekGeautoriseerdeMedewerkerRollen(
        isDetachable: (medewerkerId: String) -> Boolean
    ) = mapNotNull { rol -> rol.identificatienummer?.let { medewerkerId -> medewerkerId to rol } }
        .distinctBy { (medewerkerId) -> medewerkerId }
        .map { (medewerkerId, rol) ->
            ZaakspecifiekGeautoriseerdeMedewerkerRol(
                medewerkerId = medewerkerId,
                rol = rol,
                isDetachable = isDetachable(medewerkerId)
            )
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
