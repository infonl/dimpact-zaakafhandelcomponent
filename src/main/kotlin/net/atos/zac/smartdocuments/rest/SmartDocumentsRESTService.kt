package net.atos.zac.smartdocuments.rest

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.SecurityUtil
import net.atos.zac.policy.PolicyService
import net.atos.zac.smartdocuments.SmartDocumentsService
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toModel
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@Singleton
@Path("smartdocuments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
class SmartDocumentsRESTService @Inject constructor(
    private val smartDocumentsService: SmartDocumentsService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {

    @GET
    @Path("templates")
    fun listTemplates(): Set<RESTSmartDocumentsTemplateGroup> {
        PolicyService.assertPolicy(loggedInUserInstance.get().equals(SecurityUtil.FUNCTIONEEL_GEBRUIKER))
        return smartDocumentsService.listTemplates()
    }

    @POST
    @Path("templates")
    fun storeTemplatesMapping(restTemplateGroups: Set<RESTSmartDocumentsTemplateGroup>) {
        val modelTemplateGroups = restTemplateGroups.toModel()
        smartDocumentsService.storeTemplatesMapping(modelTemplateGroups)
    }
}
