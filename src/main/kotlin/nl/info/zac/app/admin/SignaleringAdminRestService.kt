/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.servlet.http.HttpSession
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.event.EventingService
import net.atos.zac.util.event.JobEvent
import net.atos.zac.util.event.JobId
import nl.info.zac.app.admin.model.RESTDeletedSignaleringenResponse
import nl.info.zac.authentication.ActiveSession
import nl.info.zac.authentication.InternalEndpoint
import nl.info.zac.authentication.setFunctioneelGebruiker
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty

/**
 * Internal REST service to send signaleringen and delete old signaleringen.
 * Not intended to be called by the ZAC frontend but rather by a system cron job or similar.
 */
@Path("internal/signaleringen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
@InternalEndpoint
class SignaleringAdminRestService @Inject constructor(
    private val signaleringService: SignaleringService,
    private val eventingService: EventingService,
    @ActiveSession
    private val httpSession: Instance<HttpSession>,
    @ConfigProperty(name = "SIGNALERINGEN_DELETE_OLDER_THAN_DAYS")
    private val deleteOlderThanDays: Long

) {
    @GET
    @Path("send-signaleringen")
    fun sendSignaleringen(): String {
        setFunctioneelGebruiker(httpSession.get())
        eventingService.send(JobEvent(JobId.SIGNALERINGEN_JOB))
        return "Started sending signaleringen using job: '${JobId.SIGNALERINGEN_JOB.getName()}'"
    }

    @DELETE
    @Path("delete-old")
    fun deleteOldSignaleringen(): RESTDeletedSignaleringenResponse {
        setFunctioneelGebruiker(httpSession.get())
        signaleringService.deleteOldSignaleringen(deleteOlderThanDays).let {
            return RESTDeletedSignaleringenResponse(it)
        }
    }
}
