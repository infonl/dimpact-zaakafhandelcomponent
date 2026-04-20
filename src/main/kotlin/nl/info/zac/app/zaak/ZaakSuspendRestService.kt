/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.model.RESTZaakOpschorting
import nl.info.zac.app.zaak.model.RestZaak
import nl.info.zac.app.zaak.model.RestZaakResumeData
import nl.info.zac.app.zaak.model.RestZaakSuspendData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import java.util.UUID

// note: do not use 'zaken/zaak' as path, as it will conflict with existing endpoints in other REST services
@Path("zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class ZaakSuspendRestService @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val restZaakConverter: RestZaakConverter,
    private val suspensionZaakHelper: SuspensionZaakHelper,
    private val zaakService: ZaakService,
    private val zaakVariabelenService: ZaakVariabelenService
) {
    @PATCH
    @Path("zaak/{uuid}/suspend")
    fun suspendZaak(
        @PathParam("uuid") zaakUUID: UUID,
        suspendData: RestZaakSuspendData
    ): RestZaak {
        val loggedInUser = loggedInUserInstance.get()
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType, loggedInUser)
        val suspendedZaak = suspensionZaakHelper.suspendZaak(
            zaak = zaak,
            numberOfDays = suspendData.numberOfDays,
            suspensionReason = suspendData.reason
        )
        return restZaakConverter.toRestZaak(suspendedZaak, zaakType, zaakRechten, loggedInUser)
    }

    @PATCH
    @Path("zaak/{uuid}/resume")
    fun resumeZaak(
        @PathParam("uuid") zaakUUID: UUID,
        resumeData: RestZaakResumeData
    ): RestZaak {
        val loggedInUser = loggedInUserInstance.get()
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType, loggedInUser)
        val resumedZaak = suspensionZaakHelper.resumeZaak(zaak, resumeData.reason)
        return restZaakConverter.toRestZaak(resumedZaak, zaakType, zaakRechten, loggedInUser)
    }

    @GET
    @Path("zaak/{uuid}/opschorting")
    fun readOpschortingZaak(@PathParam("uuid") zaakUUID: UUID): RESTZaakOpschorting {
        val loggedInUser = loggedInUserInstance.get()
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType, loggedInUser)
        assertPolicy(zaakRechten.lezen)
        return RESTZaakOpschorting().apply {
            vanafDatumTijd = zaakVariabelenService.findDatumtijdOpgeschort(zaakUUID)
            duurDagen = zaakVariabelenService.findVerwachteDagenOpgeschort(zaakUUID) ?: 0
        }
    }
}
