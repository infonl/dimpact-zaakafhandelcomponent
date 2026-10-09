/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import nl.info.zac.itest.config.ItestConfiguration.ZAC_DATABASE_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.dockerComposeContainer

/**
 * Runs the SQL statement with `psql` on the ZAC database of the Docker Compose stack and returns its output lines.
 */
fun queryZacDatabase(sql: String): List<String> {
    val execResult = dockerComposeContainer
        .getContainerByServiceName(ZAC_DATABASE_CONTAINER_SERVICE_NAME)
        .get()
        .execInContainer("psql", "-U", "zac", "-d", "zac", "-At", "-c", sql)
    check(execResult.exitCode == 0) { "psql failed: ${execResult.stderr}" }
    return execResult.stdout.lines().filter { it.isNotBlank() }
}
