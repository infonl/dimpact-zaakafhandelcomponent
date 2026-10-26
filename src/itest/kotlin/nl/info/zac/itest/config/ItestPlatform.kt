/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import java.util.concurrent.TimeUnit

/**
 * SPIKE: the platform that runs the services under test.
 * `compose` (the default) starts the Docker Compose stack through Testcontainers.
 * `kind` expects a running kind cluster, brought up by `hack/kind-spike/up.sh`, and reaches it on the
 * Docker Compose host ports + 20000 (see `hack/kind-spike/kind-config.yaml`).
 */
object ItestPlatform {
    private const val PLATFORM_SYSTEM_PROPERTY = "zac.itest.platform"
    private const val PLATFORM_ENV_VAR = "ZAC_ITEST_PLATFORM"
    private const val KUBE_CONTEXT_ENV_VAR = "ZAC_ITEST_KUBE_CONTEXT"
    private const val KIND_HOST_PORT_OFFSET = 20000
    private const val KUBECTL_TIMEOUT_MINUTES = 2L

    private const val NAMESPACE = "zac"

    val isKind = (System.getProperty(PLATFORM_SYSTEM_PROPERTY) ?: System.getenv(PLATFORM_ENV_VAR)) == "kind"

    private val kubeContext = System.getenv(KUBE_CONTEXT_ENV_VAR) ?: "kind-zac-spike"

    fun hostPort(composeHostPort: Int) = if (isKind) composeHostPort + KIND_HOST_PORT_OFFSET else composeHostPort

    /**
     * Runs kubectl against the kind cluster and returns its standard output.
     */
    fun kubectl(vararg arguments: String): String {
        val process = ProcessBuilder(listOf("kubectl", "--context", kubeContext) + arguments)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        val standardOutput = process.inputStream.bufferedReader().readText()
        val standardError = process.errorStream.bufferedReader().readText()
        check(process.waitFor(KUBECTL_TIMEOUT_MINUTES, TimeUnit.MINUTES)) { "kubectl ${arguments.toList()} timed out" }
        check(process.exitValue() == 0) { "kubectl ${arguments.toList()} failed: $standardOutput $standardError" }
        return standardOutput
    }

    fun queryZacDatabaseWithKubectl(sql: String) = kubectl(
        "exec", "--namespace", NAMESPACE, "deploy/postgres", "--",
        "psql", "-U", "zac", "-d", "zac", "-At", "-c", sql
    )

    fun zacLogsWithKubectl() = kubectl("logs", "--namespace", NAMESPACE, "deploy/zac", "-c", "zaakafhandelcomponent")
}
