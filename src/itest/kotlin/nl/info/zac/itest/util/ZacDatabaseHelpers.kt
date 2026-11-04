/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import nl.info.zac.itest.config.ItestKindStack

/**
 * Runs the SQL statement with `psql` on the ZAC database of the kind stack and returns its output lines.
 */
fun queryZacDatabase(sql: String): List<String> =
    ItestKindStack.queryZacDatabase(sql).lines().filter { it.isNotBlank() }
