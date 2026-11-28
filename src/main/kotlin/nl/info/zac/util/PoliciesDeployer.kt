/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import nl.info.client.opa.OpaAdminClient
import nl.info.zac.log.ZacLogger
import nl.info.zac.util.log.PolicyModuleDeploymentStarted
import org.eclipse.microprofile.rest.client.inject.RestClient

class PoliciesDeployer @Inject constructor(
    @RestClient private val opaAdminClient: OpaAdminClient
) {
    companion object {
        private const val POLICIES_FOLDER_NAME = "policies"
        private const val POLICIES_FILE_NAME = "policies"
        private const val POLICIES_FILE_EXTENSION = ".rego"
        private val LOG = ZacLogger.getLogger(PoliciesDeployer::class)
    }

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) =
        javaClass.classLoader.readNonBlankResourceLines("$POLICIES_FOLDER_NAME/$POLICIES_FILE_NAME")
            .forEach(::deployPolicy)

    private fun deployPolicy(policyFileName: String) {
        val policy = javaClass.classLoader.readResourceBytes("$POLICIES_FOLDER_NAME/$policyFileName").decodeToString()
        val moduleId = policyFileName.substringBefore(POLICIES_FILE_EXTENSION)
        LOG.info(PolicyModuleDeploymentStarted(moduleId = moduleId))
        opaAdminClient.policyUpdate(moduleId, policy)
    }
}
