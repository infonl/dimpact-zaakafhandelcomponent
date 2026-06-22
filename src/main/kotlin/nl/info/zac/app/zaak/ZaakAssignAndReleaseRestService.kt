/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.model.RESTZakenVerdeelGegevens
import nl.info.zac.app.zaak.model.RESTZakenVrijgevenGegevens
import nl.info.zac.app.zaak.model.RestZaak
import nl.info.zac.app.zaak.model.RestZaakAssignmentData
import nl.info.zac.app.zaak.model.RestZaakAssignmentToLoggedInUserData
import nl.info.zac.app.zaak.model.RestZaakOverzicht
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService

@Path("zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@Suppress("LongParameterList")
@NoArgConstructor
@AllOpen
class ZaakAssignAndReleaseRestService @Inject constructor(
    /**
     * Declare a Kotlin coroutine dispatcher here so that it can be overridden in unit tests with a test dispatcher
     * while in normal operation it will be injected using [nl.info.zac.util.CoroutineDispatcherProducer].
     */
    private val dispatcher: CoroutineDispatcher,
    private val identityService: IdentityService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val restZaakConverter: RestZaakConverter,
    private val restZaakOverzichtConverter: RestZaakOverzichtConverter,
    private val zaakService: ZaakService
) {
    /**
     * Assign one or multiple zaken in a batch operation.
     * This can be a long-running operation, so it is run asynchronously.
     */
    @PUT
    @Path("lijst/verdelen")
    fun assignFromList(@Valid restZakenVerdeelGegevens: RESTZakenVerdeelGegevens) {
        // Only the 'zaken taken verdelen' permission is currently required to assign tasks from the list.
        // Checking the user's authorization for each task's zaaktype could improve this in the future.
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation, so run it asynchronously
        CoroutineScope(dispatcher).launch {
            zaakService.assignZaken(
                zaakUUIDs = restZakenVerdeelGegevens.uuids,
                explanation = restZakenVerdeelGegevens.reden,
                group = restZakenVerdeelGegevens.groepId.let {
                    identityService.readGroup(
                        restZakenVerdeelGegevens.groepId
                    )
                },
                user = restZakenVerdeelGegevens.behandelaarGebruikersnaam?.let {
                    identityService.readUser(it)
                },
                screenEventResourceId = restZakenVerdeelGegevens.screenEventResourceId
            )
        }
    }

    @PATCH
    @Path("toekennen")
    fun assignZaak(@Valid restZaakAssignmentData: RestZaakAssignmentData): RestZaak {
        val loggedInUser = loggedInUserInstance.get()
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentData.zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType, loggedInUser)
        assertPolicy(zaakRechten.toekennen)
        zaakService.assignZaak(
            zaak,
            restZaakAssignmentData.groupId,
            restZaakAssignmentData.assigneeUserName,
            restZaakAssignmentData.reason
        )
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)
    }

    @PUT
    @Path("toekennen/mij")
    fun assignZaakToLoggedInUser(
        @Valid restZaakAssignmentToLoggedInUserData: RestZaakAssignmentToLoggedInUserData
    ): RestZaak {
        val loggedInUser = loggedInUserInstance.get()
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentToLoggedInUserData.zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType, loggedInUser)
        assertPolicy(zaakRechten.toekennen)
        zaakService.assignZaak(
            zaak,
            restZaakAssignmentToLoggedInUserData.groupId,
            loggedInUser.id,
            restZaakAssignmentToLoggedInUserData.reason
        )
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser)
    }

    @PUT
    @Path("lijst/toekennen/mij")
    fun assignZaakToLoggedInUserFromList(
        @Valid restZaakAssignmentToLoggedInUserData: RestZaakAssignmentToLoggedInUserData
    ): RestZaakOverzicht {
        val loggedInUser = loggedInUserInstance.get()
        // Checking the user's authorization for the zaak's zaaktype could improve this in the future.
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentToLoggedInUserData.zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType, loggedInUser)
        assertPolicy(zaak.isOpen() && zaakRechten.toekennen)

        zaakService.assignZaak(
            zaak = zaak,
            groupId = restZaakAssignmentToLoggedInUserData.groupId,
            userName = loggedInUser.id,
            reason = restZaakAssignmentToLoggedInUserData.reason
        )

        return restZaakOverzichtConverter.convert(zaak, loggedInUser)
    }

    /**
     * Release one or multiple zaken in a batch operation.
     * This can be a long-running operation, so it is run asynchronously.
     */
    @PUT
    @Path("lijst/vrijgeven")
    fun releaseZakenFromList(@Valid restZakenVrijgevenGegevens: RESTZakenVrijgevenGegevens) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation, so run it asynchronously
        CoroutineScope(dispatcher).launch {
            zaakService.releaseZaken(
                zaakUUIDs = restZakenVrijgevenGegevens.uuids,
                explanation = restZakenVrijgevenGegevens.reden,
                screenEventResourceId = restZakenVrijgevenGegevens.screenEventResourceId
            )
        }
    }
}
