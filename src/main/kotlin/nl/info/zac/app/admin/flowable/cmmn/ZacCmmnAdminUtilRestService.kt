/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.flowable.cmmn

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.CmmnTaskService
import org.flowable.engine.RuntimeService
import java.util.UUID
import java.util.logging.Logger

/**
 * Utility class for finding and fixing issues with missing ZAC metadata in zaken and tasks in Flowable.
 * Any issues are logged rather than returned in the response.
 * Meant to be used by developers / system admins only.
 * In future see if we can move this functionality somewhere else. It should not be in the ZAC API really.
 */
@Singleton
@Path("admin/cmmn")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class ZacCmmnAdminUtilRestService @Inject constructor(
    val cmmnRuntimeService: CmmnRuntimeService,
    val cmmnTaskService: CmmnTaskService,
    val runtimeService: RuntimeService,
    val policyService: PolicyService
) {
    companion object {
        private val LOG = Logger.getLogger(ZacCmmnAdminUtilRestService::class.java.getName())
    }

    @GET
    @Path("countmissingvariables")
    fun countMissingVariables(): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        countMissingVariable(ZaakVariabelenService.VAR_ZAAK_UUID)
        countMissingVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE)
        countMissingVariable(ZaakVariabelenService.VAR_ZAAKTYPE_UUUID)
        countMissingVariable(ZaakVariabelenService.VAR_ZAAKTYPE_OMSCHRIJVING)
        return Response.noContent().build()
    }

    @GET
    @Path("logzaaktypeuuid")
    fun logExistingZaaktypeUUID(): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        cmmnRuntimeService.createCaseInstanceQuery().variableExists(
            ZaakVariabelenService.VAR_ZAAKTYPE_UUUID
        ).list().forEach { caseInstance ->
            logVariable(
                caseInstanceId = caseInstance.id,
                variable = ZaakVariabelenService.VAR_ZAAKTYPE_UUUID
            )
        }
        return Response.noContent().build()
    }

    @GET
    @Path("fixmissingzaaktypeuuid/{zaaktypeuuid}")
    fun fixMissingZaaktypeUUID(@PathParam("zaaktypeuuid") zaaktypeUUIDString: String): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val zaaktypeUUID = UUID.fromString(zaaktypeUUIDString)
        cmmnRuntimeService.createCaseInstanceQuery().variableNotExists(
            ZaakVariabelenService.VAR_ZAAKTYPE_UUUID
        ).list().forEach { caseInstance ->
            fixVariable(
                caseInstanceId = caseInstance.id,
                variable = ZaakVariabelenService.VAR_ZAAKTYPE_UUUID,
                value = zaaktypeUUID
            )
        }
        return Response.noContent().build()
    }

    @GET
    @Path("fixexistingzaaktypeuuid/{zaaktypeuuid}")
    fun fixAllZaaktypeUUID(@PathParam("zaaktypeuuid") zaaktypeUUIDString: String): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val zaaktypeUUID = UUID.fromString(zaaktypeUUIDString)
        cmmnRuntimeService.createCaseInstanceQuery().variableExists(
            ZaakVariabelenService.VAR_ZAAKTYPE_UUUID
        ).list().forEach { caseInstance ->
            fixVariable(
                caseInstanceId = caseInstance.id,
                variable = ZaakVariabelenService.VAR_ZAAKTYPE_UUUID,
                value = zaaktypeUUID
            )
        }
        return Response.noContent().build()
    }

    @GET
    @Path("logtasksmissingscopeid")
    fun logTasksMissingScopeId(): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        val tasks = cmmnTaskService.createTaskQuery().list().filter { it.scopeId == null }
        LOG.info("Number of tasks missing scopeId : ${tasks.size}")
        tasks.forEach {
            LOG.info("${it.id} : name = '${it.name}', createTime = '${it.createTime}'")
        }
        return Response.noContent().build()
    }

    @GET
    @Path("completetasksmissingscopeid")
    fun completeTasksMissingScopeId(): Response {
        assertPolicy(policyService.readOverigeRechten().beheren)
        runtimeService.createActivityInstanceQuery().list()
            .forEach {
                runtimeService.deleteProcessInstance(
                    it.processInstanceId,
                    "none"
                )
            }
        return Response.noContent().build()
    }

    private fun countMissingVariable(variable: String) {
        val count = cmmnRuntimeService.createCaseInstanceQuery().variableNotExists(variable).count()
        LOG.info("Number of cases missing variable '$variable' = $count")
    }

    private fun logVariable(caseInstanceId: String, variable: String) {
        val value = cmmnRuntimeService.getVariable(caseInstanceId, variable)
        LOG.info("'$caseInstanceId' : '$variable' = '$value'")
    }

    private fun fixVariable(caseInstanceId: String, variable: String, value: Any) {
        LOG.info("'$caseInstanceId' : Set '$variable' to '$value'")
        cmmnRuntimeService.setVariable(caseInstanceId, variable, value)
    }
}
