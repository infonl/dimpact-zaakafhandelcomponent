/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
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
    private const val FLYWAY_TIMEOUT_MINUTES = 6L
    private const val FLYWAY_MIGRATIONS_CONFIG_MAP = "zac-flyway-migrations"

    val isKind = (System.getProperty(PLATFORM_SYSTEM_PROPERTY) ?: System.getenv(PLATFORM_ENV_VAR)) == "kind"

    private val kubeContext = System.getenv(KUBE_CONTEXT_ENV_VAR) ?: "kind-zac-spike"

    const val ZAC_NAMESPACE = "zac"
    const val PODIUMD_NAMESPACE = "podiumd"

    fun hostPort(composeHostPort: Int) = if (isKind) composeHostPort + KIND_HOST_PORT_OFFSET else composeHostPort

    /**
     * Runs kubectl against the kind cluster and returns its standard output.
     */
    fun kubectl(vararg arguments: String, input: String? = null, timeoutMinutes: Long = KUBECTL_TIMEOUT_MINUTES): String {
        val process = ProcessBuilder(listOf("kubectl", "--context", kubeContext) + arguments)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        process.outputStream.use { standardInput -> input?.let { standardInput.write(it.toByteArray()) } }
        val standardOutput = process.inputStream.bufferedReader().readText()
        val standardError = process.errorStream.bufferedReader().readText()
        check(process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) { "kubectl ${arguments.toList()} timed out" }
        check(process.exitValue() == 0) { "kubectl ${arguments.toList()} failed: $standardOutput $standardError" }
        return standardOutput
    }

    private val flywayMigrationsConfigMap by lazy {
        val configMap = kubectl(
            "create", "configmap", FLYWAY_MIGRATIONS_CONFIG_MAP, "--namespace", PODIUMD_NAMESPACE,
            "--from-file=src/main/resources/schemas", "--dry-run=client", "--output=yaml"
        )
        kubectl("apply", "--server-side", "--force-conflicts", "--filename=-", input = configMap)
        FLYWAY_MIGRATIONS_CONFIG_MAP
    }

    /**
     * Runs Flyway in a pod next to the Postgres of the kind cluster and returns its output; the kind equivalent of
     * the Flyway container that shares the network of the Docker Compose database container.
     * The migrations come from a ConfigMap, copied to an emptyDir first, because Flyway would also find the
     * migrations in the hidden directories of a ConfigMap volume.
     */
    fun runFlyway(flywayVersion: String, flywayArguments: List<String>): String {
        val podName = "flyway-${UUID.randomUUID().toString().take(8)}"
        val pod = JSONObject().put(
            "spec",
            JSONObject()
                .put(
                    "containers",
                    JSONArray().put(
                        JSONObject()
                            .put("name", "flyway")
                            .put("image", "flyway/flyway:$flywayVersion")
                            .put("command", JSONArray(listOf("sh", "-c", "cp /migrations/* /flyway/sql/ && exec flyway \"$@\"", "flyway")))
                            .put("args", JSONArray(flywayArguments))
                            .put(
                                "volumeMounts",
                                JSONArray()
                                    .put(JSONObject().put("name", "migrations").put("mountPath", "/migrations"))
                                    .put(JSONObject().put("name", "sql").put("mountPath", "/flyway/sql"))
                            )
                    )
                )
                .put(
                    "volumes",
                    JSONArray()
                        .put(JSONObject().put("name", "migrations").put("configMap", JSONObject().put("name", flywayMigrationsConfigMap)))
                        .put(JSONObject().put("name", "sql").put("emptyDir", JSONObject()))
                )
        )
        return kubectl(
            "run", podName, "--namespace", PODIUMD_NAMESPACE, "--image=flyway/flyway:$flywayVersion", "--restart=Never",
            "--rm", "--attach", "--quiet", "--pod-running-timeout=5m", "--overrides=$pod",
            timeoutMinutes = FLYWAY_TIMEOUT_MINUTES
        )
    }

    fun queryZacDatabaseWithKubectl(sql: String) = kubectl(
        "exec", "--namespace", PODIUMD_NAMESPACE, "deploy/postgres", "--",
        "psql", "-U", "zac", "-d", "zac", "-At", "-c", sql
    )

    fun zacLogsWithKubectl() = kubectl("logs", "--namespace", ZAC_NAMESPACE, "deploy/zac", "-c", "zaakafhandelcomponent")
}
