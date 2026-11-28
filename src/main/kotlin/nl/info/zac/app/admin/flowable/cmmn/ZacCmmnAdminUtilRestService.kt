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
import nl.info.zac.app.log.CaseVariableRead
import nl.info.zac.app.log.CaseVariableSet
import nl.info.zac.app.log.CasesMissingVariable
import nl.info.zac.app.log.TaskMissingScopeId
import nl.info.zac.app.log.TasksMissingScopeId
import nl.info.zac.flowable.ZaakVariabelenService
import nl.info.zac.log.ZacLogger
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.NoArgConstructor
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.CmmnTaskService
import org.flowable.engine.RuntimeService
import java.util.UUID

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
        private val LOG = ZacLogger.getLogger(ZacCmmnAdminUtilRestService::class)
    }

    @GET
    @Path("countmissingvariables")
    fun countMissingVariables(): Response {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        countMissingVariable(ZaakVariabelenService.VAR_ZAAK_UUID)
        countMissingVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE)
        countMissingVariable(ZaakVariabelenService.VAR_ZAAKTYPE_UUID)
        countMissingVariable(ZaakVariabelenService.VAR_ZAAKTYPE_OMSCHRIJVING)
        return Response.noContent().build()
    }

    @GET
    @Path("logzaaktypeuuid")
    fun logExistingZaaktypeUUID(): Response {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        cmmnRuntimeService.createCaseInstanceQuery().variableExists(
            ZaakVariabelenService.VAR_ZAAKTYPE_UUID
        ).list().forEach { caseInstance ->
            logVariable(
                caseInstanceId = caseInstance.id,
                variable = ZaakVariabelenService.VAR_ZAAKTYPE_UUID
            )
        }
        return Response.noContent().build()
    }

    @GET
    @Path("fixmissingzaaktypeuuid/{zaaktypeuuid}")
    fun fixMissingZaaktypeUUID(@PathParam("zaaktypeuuid") zaaktypeUUIDString: String): Response {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        val zaaktypeUUID = UUID.fromString(zaaktypeUUIDString)
        cmmnRuntimeService.createCaseInstanceQuery().variableNotExists(
            ZaakVariabelenService.VAR_ZAAKTYPE_UUID
        ).list().forEach { caseInstance ->
            fixVariable(
                caseInstanceId = caseInstance.id,
                variable = ZaakVariabelenService.VAR_ZAAKTYPE_UUID,
                value = zaaktypeUUID
            )
        }
        return Response.noContent().build()
    }

    @GET
    @Path("fixexistingzaaktypeuuid/{zaaktypeuuid}")
    fun fixAllZaaktypeUUID(@PathParam("zaaktypeuuid") zaaktypeUUIDString: String): Response {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        val zaaktypeUUID = UUID.fromString(zaaktypeUUIDString)
        cmmnRuntimeService.createCaseInstanceQuery().variableExists(
            ZaakVariabelenService.VAR_ZAAKTYPE_UUID
        ).list().forEach { caseInstance ->
            fixVariable(
                caseInstanceId = caseInstance.id,
                variable = ZaakVariabelenService.VAR_ZAAKTYPE_UUID,
                value = zaaktypeUUID
            )
        }
        return Response.noContent().build()
    }

    @GET
    @Path("logtasksmissingscopeid")
    fun logTasksMissingScopeId(): Response {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
        val tasks = cmmnTaskService.createTaskQuery().list().filter { it.scopeId == null }
        LOG.info(TasksMissingScopeId(taskCount = tasks.size))
        tasks.forEach {
            LOG.info(TaskMissingScopeId(taakId = it.id, taskName = it.name, createTime = it.createTime?.toString()))
        }
        return Response.noContent().build()
    }

    @GET
    @Path("completetasksmissingscopeid")
    fun completeTasksMissingScopeId(): Response {
        assertPolicy(policyService.readOverigeRechten().canBeheren)
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
        LOG.info(CasesMissingVariable(variable = variable, caseCount = count))
    }

    private fun logVariable(caseInstanceId: String, variable: String) {
        // Only whether the variable is present: its value can hold personal data
        val value = cmmnRuntimeService.getVariable(caseInstanceId, variable)
        LOG.info(CaseVariableRead(caseInstanceId = caseInstanceId, variable = variable, isPresent = value != null))
    }

    private fun fixVariable(caseInstanceId: String, variable: String, value: Any) {
        LOG.info(CaseVariableSet(caseInstanceId = caseInstanceId, variable = variable))
        cmmnRuntimeService.setVariable(caseInstanceId, variable, value)
    }
}
