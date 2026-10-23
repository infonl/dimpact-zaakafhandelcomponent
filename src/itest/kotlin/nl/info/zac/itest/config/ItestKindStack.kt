/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jacoco.core.tools.ExecDumpClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * The kind cluster with the stack of `scripts/kind` that the integration tests run against: ZAC from the Docker image
 * under test, the services that ZAC needs and the test doubles of the `itest` profile.
 *
 * The cluster is named after the `ZAC_ITEST_KIND_CLUSTER_NAME` environment variable (default `zac-itest`), so that the
 * integration tests never touch the cluster of `./start-kind.sh`. The host ports of the services are those of
 * `scripts/kind/ports.json`, moved by the `KIND_PORT_OFFSET` and `KIND_PORT_<NAME>` environment variables as
 * `scripts/kind/up.sh` does.
 */
object ItestKindStack {
    private const val CLUSTER_NAME_ENV_VAR = "ZAC_ITEST_KIND_CLUSTER_NAME"
    private const val DEFAULT_CLUSTER_NAME = "zac-itest"
    private const val PORT_OFFSET_ENV_VAR = "KIND_PORT_OFFSET"
    private const val PORT_ENV_VAR_PREFIX = "KIND_PORT_"
    private const val NAMESPACE = "zac"
    private const val ZAC_DEPLOYMENT = "zac"
    private const val ZAC_CONTAINER = "zaakafhandelcomponent"
    private const val DATABASE_DEPLOYMENT = "postgres"
    private const val JACOCO_PORT_NAME = "zac-jacoco"
    private const val JACOCO_DEFAULT_HOST_PORT = 6300
    private const val JACOCO_AGENT_CONFIG_MAP = "jacoco-agent"
    private const val JACOCO_AGENT_FILE_NAME = "org.jacoco.agent-runtime.jar"

    private val upScript = File("scripts/kind/up.sh")
    private val downScript = File("scripts/kind/down.sh")
    private val zacValuesFile = File("scripts/kind/values/zac-itest.yaml")
    private val jacocoAgentFile = File("build/jacoco/itest/jacoco-agent/$JACOCO_AGENT_FILE_NAME")
    private val jacocoAgentManifestFile = File("build/jacoco/itest/jacoco-agent-config-map.json")
    private val upTimeout = 40.minutes
    private val downTimeout = 5.minutes
    private val kubectlTimeout = 2.minutes
    private val terminalColorCode = Regex("\u001B\\[[0-9;]*m")

    /**
     * The environment variables that `scripts/kind/up.sh` gets from the environment of the integration tests, next to
     * the `KIND_*` variables. The other settings of a developer's shell must not change the stack under test.
     */
    private val environmentPassedOn = setOf(
        "PATH",
        "HOME",
        "USER",
        "LOGNAME",
        "TMPDIR",
        "LANG",
        "DOCKER_HOST",
        "DOCKER_CONFIG",
        "DOCKER_CONTEXT",
        "DOCKER_USE_ARM64_CONTAINERS",
        "KUBECONFIG"
    )

    private val logger = KotlinLogging.logger {}

    val clusterName: String = System.getenv(CLUSTER_NAME_ENV_VAR) ?: DEFAULT_CLUSTER_NAME

    private val kubeContext = "kind-$clusterName"

    /**
     * The host port of the service [portName] of `scripts/kind/ports.json`, which publishes it on [defaultHostPort]
     * unless an environment variable moves it.
     */
    fun hostPort(portName: String, defaultHostPort: Int) =
        System.getenv(PORT_ENV_VAR_PREFIX + portName.uppercase().replace('-', '_'))?.toInt()
            ?: (defaultHostPort + (System.getenv(PORT_OFFSET_ENV_VAR)?.toInt() ?: 0))

    /**
     * Starts the stack with ZAC from [zacDockerImage] in the local Docker daemon. A cluster that ZAC has not run in
     * yet, such as one that CI starts while it builds the ZAC image, is reused; any other cluster of the same name
     * holds the data of an earlier run and is deleted first.
     */
    fun start(zacDockerImage: String) {
        if (isZacInstalled()) {
            logger.info { "The kind cluster '$clusterName' holds the data of an earlier run; deleting it" }
            delete()
        }
        writeJacocoAgentManifest()
        runScript(
            script = upScript,
            timeout = upTimeout,
            scriptEnvironment = mapOf(
                "KIND_CLUSTER_NAME" to clusterName,
                "ZAC_IN_CLUSTER" to "local",
                "ZAC_DOCKER_IMAGE" to zacDockerImage,
                "KIND_PROFILES" to "itest",
                "KIND_ENV_FILE" to "",
                "KIND_ZAC_VALUES" to zacValuesFile.absolutePath,
                "KIND_EXTRA_MANIFESTS" to jacocoAgentManifestFile.absolutePath
            )
        )
    }

    fun delete() = runScript(
        script = downScript,
        timeout = downTimeout,
        scriptEnvironment = mapOf("KIND_CLUSTER_NAME" to clusterName, "DELETE_CLUSTER" to "true")
    )

    /**
     * Writes the coverage that the JaCoCo agent in ZAC recorded so far to [execFile].
     */
    fun dumpCoverage(execFile: File) {
        execFile.parentFile.mkdirs()
        ExecDumpClient()
            .dump("localhost", hostPort(JACOCO_PORT_NAME, JACOCO_DEFAULT_HOST_PORT))
            .save(execFile, false)
    }

    fun zacLogs() = kubectl("logs", "--namespace", NAMESPACE, "deployment/$ZAC_DEPLOYMENT", "--container", ZAC_CONTAINER)

    /**
     * Runs the SQL statement with `psql` on the ZAC database and returns its output.
     */
    fun queryZacDatabase(sql: String) = kubectl(
        "exec",
        "--namespace",
        NAMESPACE,
        "deployment/$DATABASE_DEPLOYMENT",
        "--",
        "psql",
        "--username=zac",
        "--dbname=zac",
        "--no-align",
        "--tuples-only",
        "--command=$sql"
    )

    fun pods(): JSONArray =
        JSONObject(kubectl("get", "pods", "--namespace", NAMESPACE, "--output=json")).getJSONArray("items")

    private fun kubectl(vararg arguments: String): String {
        val process = ProcessBuilder(listOf("kubectl", "--context", kubeContext) + arguments).start()
        val standardError = CompletableFuture.supplyAsync { process.errorStream.bufferedReader().readText() }
        val standardOutput = process.inputStream.bufferedReader().readText()
        check(process.waitFor(kubectlTimeout.inWholeSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            "kubectl ${arguments.joinToString(" ")} timed out"
        }
        check(process.exitValue() == 0) {
            "kubectl ${arguments.joinToString(" ")} failed with exit code ${process.exitValue()}: ${standardError.get()}"
        }
        return standardOutput
    }

    private fun isZacInstalled() =
        ProcessBuilder(
            "kubectl",
            "--context",
            kubeContext,
            "get",
            "deployment",
            ZAC_DEPLOYMENT,
            "--namespace",
            NAMESPACE,
            "--ignore-not-found",
            "--output=name"
        ).redirectError(ProcessBuilder.Redirect.DISCARD).start().let { process ->
            val standardOutput = process.inputStream.bufferedReader().readText()
            process.waitFor(kubectlTimeout.inWholeSeconds, TimeUnit.SECONDS) &&
                process.exitValue() == 0 &&
                standardOutput.isNotBlank()
        }

    private fun writeJacocoAgentManifest() {
        check(jacocoAgentFile.isFile) {
            "The JaCoCo agent '${jacocoAgentFile.path}' is missing; the Gradle task 'copyJacocoAgentForItest' copies it"
        }
        jacocoAgentManifestFile.parentFile.mkdirs()
        jacocoAgentManifestFile.writeText(
            JSONObject(
                mapOf(
                    "apiVersion" to "v1",
                    "kind" to "ConfigMap",
                    "metadata" to mapOf("name" to JACOCO_AGENT_CONFIG_MAP),
                    "binaryData" to mapOf(
                        JACOCO_AGENT_FILE_NAME to Base64.getEncoder().encodeToString(jacocoAgentFile.readBytes())
                    )
                )
            ).toString()
        )
    }

    private fun runScript(script: File, timeout: Duration, scriptEnvironment: Map<String, String>) {
        logger.info { "Running ${script.path} with $scriptEnvironment" }
        val process = ProcessBuilder(script.absolutePath)
            .redirectErrorStream(true)
            .apply {
                environment().run {
                    val passedOn = filterKeys { it in environmentPassedOn || it.startsWith("KIND_") }
                    clear()
                    putAll(passedOn + scriptEnvironment)
                }
            }
            .start()
        val output = CompletableFuture.runAsync {
            process.inputStream.bufferedReader().forEachLine { line ->
                logger.info { "${script.name}: ${line.replace(terminalColorCode, "")}" }
            }
        }
        if (!process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("${script.path} did not finish within $timeout")
        }
        output.join()
        check(process.exitValue() == 0) { "${script.path} failed with exit code ${process.exitValue()}" }
    }
}
