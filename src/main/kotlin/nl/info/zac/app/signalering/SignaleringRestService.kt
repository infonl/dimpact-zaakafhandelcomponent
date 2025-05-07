/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.signalering

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieobject
import net.atos.zac.app.shared.RESTResultaat
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import net.atos.zac.signalering.model.SignaleringInstellingen
import net.atos.zac.signalering.model.SignaleringInstellingenZoekParameters
import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.app.shared.RestPageParameters
import nl.info.zac.app.signalering.converter.RestSignaleringInstellingenConverter
import nl.info.zac.app.signalering.exception.SignaleringException
import nl.info.zac.app.signalering.model.RestSignaleringInstellingen
import nl.info.zac.app.signalering.model.RestSignaleringTaskSummary
import nl.info.zac.app.zaak.model.RestZaakOverzicht
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@Path("signaleringen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class SignaleringRestService @Inject constructor(
    private val signaleringService: SignaleringService,
    private val identityService: IdentityService,
    private val policyService: PolicyService,
    private val restSignaleringInstellingenConverter: RestSignaleringInstellingenConverter,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {

    private fun Instance<LoggedInUser>.getSignaleringInstellingenZoekParameters() =
        SignaleringInstellingenZoekParameters(get())

    @GET
    @Path("/latest")
    fun latestSignaleringOccurrence(): ZonedDateTime? =
        signaleringService.latestSignaleringOccurrence()

    /**
     * Lists zaken signaleringen for the given signaleringsType.
     */
    @PUT
    @Path("/zaken/{type}")
    fun listZakenSignaleringen(
        @PathParam("type") signaleringsType: SignaleringType.Type,
        pageParameters: RestPageParameters
    ): RESTResultaat<RestZaakOverzicht> =
        signaleringService.countZakenSignaleringen(signaleringsType).let { objectsCount ->
            objectsCount.maxPages(pageParameters.rows).let { maxPages ->
                if (pageParameters.page > maxPages) {
                    throw SignaleringException("Requested page ${pageParameters.page} must be <= $maxPages")
                }
            }
            RESTResultaat(
                signaleringService.listZakenSignaleringenPage(signaleringsType, pageParameters),
                objectsCount
            )
        }

    private fun Long.maxPages(pageSize: Int) = (this + pageSize - 1) / pageSize

    @GET
    @Path("/taken/{type}")
    fun listTakenSignaleringen(
        @PathParam("type") signaleringsType: SignaleringType.Type
    ): List<RestSignaleringTaskSummary> =
        signaleringService.listTakenSignaleringenPage(signaleringsType)

    @GET
    @Path("/informatieobjecten/{type}")
    fun listInformatieobjectenSignaleringen(
        @PathParam("type") signaleringsType: SignaleringType.Type
    ): List<RestEnkelvoudigInformatieobject> =
        signaleringService.listInformatieobjectenSignaleringen(signaleringsType)

    @GET
    @Path("/instellingen")
    fun listUserSignaleringInstellingen(): List<RestSignaleringInstellingen> =
        loggedInUserInstance.getSignaleringInstellingenZoekParameters()
            .let(signaleringService::listInstellingenInclusiefMogelijke)
            .let(restSignaleringInstellingenConverter::convert)

    @PUT
    @Path("/instellingen")
    fun updateUserSignaleringInstellingen(restInstellingen: RestSignaleringInstellingen) =
        loggedInUserInstance.get()
            .let { restSignaleringInstellingenConverter.convert(restInstellingen, it) }
            .let(signaleringService::createUpdateOrDeleteInstellingen)

    @GET
    @Path("group/{groupId}/instellingen")
    fun listGroupSignaleringInstellingen(
        @PathParam("groupId") groupId: String
    ): List<RestSignaleringInstellingen> {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return SignaleringInstellingenZoekParameters(identityService.readGroup(groupId))
            .let(signaleringService::listInstellingenInclusiefMogelijke)
            .let(restSignaleringInstellingenConverter::convert)
    }

    @PUT
    @Path("group/{groupId}/instellingen")
    fun updateGroupSignaleringInstellingen(
        @PathParam("groupId") groupId: String,
        restInstellingen: RestSignaleringInstellingen
    ): SignaleringInstellingen? {
        assertPolicy(policyService.readOverigeRechten().beheren)
        return identityService.readGroup(groupId)
            .let { restSignaleringInstellingenConverter.convert(restInstellingen, it) }
            .let(signaleringService::createUpdateOrDeleteInstellingen)
    }

    @GET
    @Path("/typen/dashboard")
    fun listDashboardSignaleringTypen(): List<SignaleringType.Type> =
        loggedInUserInstance.getSignaleringInstellingenZoekParameters()
            .dashboard()
            .let(signaleringService::listInstellingen)
            .map { it.type.type }
}
