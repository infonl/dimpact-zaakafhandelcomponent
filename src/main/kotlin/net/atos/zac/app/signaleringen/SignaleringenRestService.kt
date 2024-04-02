/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signaleringen

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
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.RESTEnkelvoudigInformatieobject
import net.atos.zac.app.signaleringen.converter.RESTSignaleringInstellingenConverter
import net.atos.zac.app.signaleringen.model.RESTSignaleringInstellingen
import net.atos.zac.app.taken.converter.RESTTaakConverter
import net.atos.zac.app.taken.model.RESTTaak
import net.atos.zac.app.zaken.converter.RESTZaakOverzichtConverter
import net.atos.zac.app.zaken.model.RESTZaakOverzicht
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.flowable.TakenService
import net.atos.zac.identity.IdentityService
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.signalering.model.SignaleringInstellingenZoekParameters
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringZoekParameters
import nl.lifely.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@Path("signaleringen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class SignaleringenRestService @Inject constructor(
    private val signaleringenService: SignaleringenService,
    private val zrcClientService: ZRCClientService,
    private val takenService: TakenService,
    private val drcClientService: DRCClientService,
    private val identityService: IdentityService,
    private val restZaakOverzichtConverter: RESTZaakOverzichtConverter,
    private val restTaakConverter: RESTTaakConverter,
    private val restInformatieobjectConverter: RESTInformatieobjectConverter,
    private val restSignaleringInstellingenConverter: RESTSignaleringInstellingenConverter,
    private val loggedInUserInstance: Instance<LoggedInUser>,
) {
    private fun Instance<LoggedInUser>.getSignaleringZoekParameters() =
        SignaleringZoekParameters(get())
    private fun Instance<LoggedInUser>.getSignaleringInstellingenZoekParameters() =
        SignaleringInstellingenZoekParameters(get())

    @GET
    @Path("/latest")
    fun latestSignaleringen(): ZonedDateTime? =
        loggedInUserInstance.getSignaleringZoekParameters()
            .let { signaleringenService.latestSignalering(it) }

    @GET
    @Path("/zaken/{type}")
    fun listZakenSignaleringen(
        @PathParam("type") signaleringsType: SignaleringType.Type
    ): List<RESTZaakOverzicht> =
        loggedInUserInstance.getSignaleringZoekParameters()
            .types(signaleringsType)
            .subjecttype(SignaleringSubject.ZAAK)
            .let { signaleringenService.listSignaleringen(it) }
            .stream()
            .map { zrcClientService.readZaak(UUID.fromString(it.subject)) }
            .map { restZaakOverzichtConverter.convert(it) }
            .toList()

    @GET
    @Path("/taken/{type}")
    fun listTakenSignaleringen(@PathParam("type") signaleringsType: SignaleringType.Type): List<RESTTaak> =
        loggedInUserInstance.getSignaleringZoekParameters()
            .types(signaleringsType)
            .subjecttype(SignaleringSubject.TAAK)
            .let { signaleringenService.listSignaleringen(it) }
            .stream()
            .map { takenService.readTask(it.subject) }
            .map { restTaakConverter.convert(it) }
            .toList()

    @GET
    @Path("/informatieobjecten/{type}")
    fun listInformatieobjectenSignaleringen(
        @PathParam("type") signaleringsType: SignaleringType.Type
    ): List<RESTEnkelvoudigInformatieobject> =
        loggedInUserInstance.getSignaleringZoekParameters()
            .types(signaleringsType)
            .subjecttype(SignaleringSubject.DOCUMENT)
            .let { signaleringenService.listSignaleringen(it) }
            .stream()
            .map { drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(it.subject)) }
            .map { restInformatieobjectConverter.convertToREST(it) }
            .toList()

    @GET
    @Path("/instellingen")
    fun listUserSignaleringInstellingen(): List<RESTSignaleringInstellingen> =
        loggedInUserInstance.getSignaleringInstellingenZoekParameters()
            .let { signaleringenService.listInstellingenInclusiefMogelijke(it) }
            .let { restSignaleringInstellingenConverter.convert(it) }

    @PUT
    @Path("/instellingen")
    fun updateUserSignaleringInstellingen(restInstellingen: RESTSignaleringInstellingen) =
        loggedInUserInstance.get()
            .let { restSignaleringInstellingenConverter.convert(restInstellingen, it) }
            .let { signaleringenService.createUpdateOrDeleteInstellingen(it) }

    @GET
    @Path("group/{groupId}/instellingen")
    fun listGroupSignaleringInstellingen(
        @PathParam("groupId") groupId: String
    ): List<RESTSignaleringInstellingen> =
        SignaleringInstellingenZoekParameters(identityService.readGroup(groupId))
            .let { signaleringenService.listInstellingenInclusiefMogelijke(it) }
            .let { restSignaleringInstellingenConverter.convert(it) }

    @PUT
    @Path("group/{groupId}/instellingen")
    fun updateGroupSignaleringInstellingen(
        @PathParam("groupId") groupId: String,
        restInstellingen: RESTSignaleringInstellingen
    ) =
        identityService.readGroup(groupId)
            .let { restSignaleringInstellingenConverter.convert(restInstellingen, it) }
            .let { signaleringenService.createUpdateOrDeleteInstellingen(it) }

    @GET
    @Path("/typen/dashboard")
    fun listDashboardSignaleringTypen(): List<SignaleringType.Type> =
        loggedInUserInstance.getSignaleringInstellingenZoekParameters()
            .dashboard()
            .let { signaleringenService.listInstellingen(it) }
            .stream()
            .map { it.type.type }
            .toList()
}
