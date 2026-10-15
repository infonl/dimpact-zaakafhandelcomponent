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
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.logging.Logger

class PoliciesDeployer @Inject constructor(
    @RestClient private val opaAdminClient: OpaAdminClient
) {
    companion object {
        private const val POLICIES_FOLDER_NAME = "policies"
        private const val POLICIES_FILE_NAME = "policies"
        private const val POLICIES_FILE_EXTENSION = ".rego"
        private val LOG = Logger.getLogger(PoliciesDeployer::class.java.name)
    }

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) =
        readPoliciesResource(POLICIES_FILE_NAME).bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }.forEach(::deployPolicy)
        }

    private fun deployPolicy(policyFileName: String) {
        val policy = readPoliciesResource(policyFileName).use { String(it.readAllBytes(), Charsets.UTF_8) }
        val moduleId = policyFileName.substringBefore(POLICIES_FILE_EXTENSION)
        LOG.info { "Deploying policy module: $moduleId" }
        opaAdminClient.policyUpdate(moduleId, policy)
    }

    private fun readPoliciesResource(fileName: String) =
        javaClass.classLoader.getResourceAsStream("$POLICIES_FOLDER_NAME/$fileName")
            ?: error("Resource '$POLICIES_FOLDER_NAME/$fileName' not found")
}
