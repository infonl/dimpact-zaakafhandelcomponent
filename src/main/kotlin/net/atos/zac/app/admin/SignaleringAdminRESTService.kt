package net.atos.zac.app.admin

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.servlet.http.HttpSession
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.admin.model.RESTDeletedSignaleringenResponse
import net.atos.zac.authentication.ActiveSession
import net.atos.zac.authentication.SecurityUtil
import net.atos.zac.event.EventingService
import net.atos.zac.signalering.SignaleringService
import net.atos.zac.util.event.JobEvent
import net.atos.zac.util.event.JobId
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import kotlin.let

@Path("admin/signaleringen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class SignaleringAdminRESTService @Inject constructor(
    private val signaleringService: SignaleringService,
    private val eventingService: EventingService,
    @ActiveSession
    private val httpSession: Instance<HttpSession>,
    @ConfigProperty(name = "SIGNALERINGEN_DELETE_OLDER_THAN_DAYS")
    private val deleteOlderThanDays: Long

) {
    @GET
    @Path("send-signaleringen")
    fun zaakSignaleringenVerzenden(): String {
        SecurityUtil.setFunctioneelGebruiker(httpSession.get())
        eventingService.send(JobEvent(JobId.SIGNALERINGEN_JOB))
        return "Started sending signaleringen using job: '${JobId.SIGNALERINGEN_JOB.getName()}'"
    }

    @DELETE
    @Path("delete-old")
    fun deleteOldSignaleringen(): RESTDeletedSignaleringenResponse {
        SecurityUtil.setFunctioneelGebruiker(httpSession.get())
        signaleringService.deleteOldSignaleringen(deleteOlderThanDays).let {
            return RESTDeletedSignaleringenResponse(it)
        }
    }
}
