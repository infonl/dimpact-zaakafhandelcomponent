/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.database.flyway.log

import nl.info.zac.log.LogEvent

class DatabaseMigrated(
    val previousVersion: String?,
    val previousDescription: String?,
    val currentVersion: String?
) : LogEvent("Successfully migrated the ZAC database")
