/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signalering

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.atos.zac.app.signalering.converter.RestSignaleringInstellingenConverter
import net.atos.zac.app.signalering.model.RestSignaleringInstellingen
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.identity.IdentityService
import net.atos.zac.signalering.SignaleringService
import net.atos.zac.signalering.model.SignaleringInstellingenZoekParameters
import net.atos.zac.signalering.model.SignaleringType
import nl.lifely.zac.util.NoArgConstructor
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
    private val restSignaleringInstellingenConverter: RestSignaleringInstellingenConverter,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {

    companion object {
        private const val TOTAL_COUNT_HEADER = "X-Total-Count"
    }

    private fun Instance<LoggedInUser>.getSignaleringInstellingenZoekParameters() =
        SignaleringInstellingenZoekParameters(get())

    @GET
    @Path("/latest")
    fun latestSignaleringOccurrence(): ZonedDateTime? =
        signaleringService.latestSignaleringOccurrence()

    /**
     * Starts listing zaken signaleringen for the given signaleringsType.
     * This can be a long-running operation, so it is run asynchronously.
     */
    @PUT
    @Path("/zaken/{type}")
    @Deprecated("To be removed when we switch to paging")
    fun startListingZakenSignaleringen(
        @PathParam("type") signaleringsType: SignaleringType.Type,
        screenEventResourceId: String
    ) {
        // User is not available in co-routines, so fetch it outside the co-routine scope
        loggedInUserInstance.get().let {
            CoroutineScope(Dispatchers.IO).launch {
                signaleringService.listZakenSignaleringen(it, signaleringsType, screenEventResourceId)
            }
        }
    }

    /**
     * Lists zaken signaleringen for the given signaleringsType.
     */
    @GET
    @Path("/zaken/{type}")
    fun listZakenSignaleringen(
        @PathParam("type") signaleringsType: SignaleringType.Type,
        @QueryParam("pageNumber") @DefaultValue(value = "0") pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue(value = "5") pageSize: Int
    ): Response =
        signaleringService.countZakenSignaleringen(signaleringsType).let { objectsCount ->
            if (pageNumber > objectsCount.maxPages(pageSize)) {
                Response.status(Response.Status.NOT_FOUND).build()
            }
            Response.ok()
                .header(TOTAL_COUNT_HEADER, objectsCount)
                .entity(signaleringService.listZakenSignaleringenPage(signaleringsType, pageNumber, pageSize))
                .build()
        }

    @GET
    @Path("/taken/{type}")
    @Deprecated("default page size is to be changed when we switch to paging")
    fun listTakenSignaleringen(
        @PathParam("type") signaleringsType: SignaleringType.Type,
        @QueryParam("pageNumber") @DefaultValue(value = "0") pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue(value = "1000") pageSize: Int
    ): Response =
        signaleringService.countTakenSignaleringen(signaleringsType).let { objectsCount ->
            if (pageNumber > objectsCount.maxPages(pageSize)) {
                Response.status(Response.Status.NOT_FOUND).build()
            }
            Response.ok()
                .header(TOTAL_COUNT_HEADER, objectsCount)
                .entity(signaleringService.listTakenSignaleringenPage(signaleringsType, pageNumber, pageSize))
                .build()
        }

    @GET
    @Path("/informatieobjecten/{type}")
    @Deprecated("default page size is to be changed when we switch to paging")
    fun listInformatieobjectenSignaleringen(
        @PathParam("type") signaleringsType: SignaleringType.Type,
        @QueryParam("pageNumber") @DefaultValue(value = "0") pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue(value = "1000") pageSize: Int
    ): Response =
        signaleringService.countInformatieobjectenSignaleringen(signaleringsType).let { objectsCount ->
            if (pageNumber > objectsCount.maxPages(pageSize)) {
                Response.status(Response.Status.NOT_FOUND).build()
            }
            Response.ok()
                .header(TOTAL_COUNT_HEADER, objectsCount)
                .entity(signaleringService.listInformatieobjectenSignaleringen(signaleringsType, pageNumber, pageSize))
                .build()
        }

    private fun Long.maxPages(pageSize: Int) = (this + pageSize - 1) / pageSize

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
    ): List<RestSignaleringInstellingen> =
        SignaleringInstellingenZoekParameters(identityService.readGroup(groupId))
            .let(signaleringService::listInstellingenInclusiefMogelijke)
            .let(restSignaleringInstellingenConverter::convert)

    @PUT
    @Path("group/{groupId}/instellingen")
    fun updateGroupSignaleringInstellingen(
        @PathParam("groupId") groupId: String,
        restInstellingen: RestSignaleringInstellingen
    ) = identityService.readGroup(groupId)
        .let { restSignaleringInstellingenConverter.convert(restInstellingen, it) }
        .let(signaleringService::createUpdateOrDeleteInstellingen)

    @GET
    @Path("/typen/dashboard")
    fun listDashboardSignaleringTypen(): List<SignaleringType.Type> =
        loggedInUserInstance.getSignaleringInstellingenZoekParameters()
            .dashboard()
            .let(signaleringService::listInstellingen)
            .map { it.type.type }
}
