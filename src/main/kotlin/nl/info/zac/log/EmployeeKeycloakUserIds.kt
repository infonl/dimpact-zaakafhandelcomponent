/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * The Keycloak user IDs of employees by username, shared by all [ZacLogger]s of a ZAC instance.
 *
 * A found ID is kept for the lifetime of the instance; a lookup that finds nothing is not repeated for
 * [MISSED_LOOKUP_RETRY_DELAY], so that a Keycloak outage costs at most one lookup per employee per delay.
 */
internal object EmployeeKeycloakUserIds {
    private val MISSED_LOOKUP_RETRY_DELAY: Duration = Duration.ofMinutes(5)

    private val keycloakUserIds = ConcurrentHashMap<String, String>()
    private val missedLookups = ConcurrentHashMap<String, Instant>()
    private val lookupLocks = ConcurrentHashMap<String, Any>()
    private val lookupInProgress = ThreadLocal.withInitial { false }

    @Volatile
    private var employeeLookup: ((username: String) -> String?)? = null

    @Volatile
    var clock: Clock = Clock.systemUTC()

    fun register(username: String, keycloakUserId: String) {
        keycloakUserIds[username] = keycloakUserId
        missedLookups.remove(username)
    }

    fun useEmployeeLookup(lookup: (username: String) -> String?) {
        employeeLookup = lookup
    }

    fun resolve(employee: Employee): String =
        keycloakUserIds[employee.username] ?: lookUp(employee.username) ?: UNKNOWN_EMPLOYEE

    fun reset() {
        keycloakUserIds.clear()
        missedLookups.clear()
        lookupLocks.clear()
        employeeLookup = null
        clock = Clock.systemUTC()
    }

    // One lookup per employee at a time: a thread that resolves the same employee meanwhile waits for its result,
    // so that concurrent log records do not each call Keycloak, nor each wait for its timeout during an outage.
    private fun lookUp(username: String): String? =
        employeeLookup
            ?.takeUnless { lookupInProgress.get() || isRecentlyMissed(username) }
            ?.let { lookup ->
                synchronized(lookupLocks.computeIfAbsent(username) { Any() }) {
                    keycloakUserIds[username]
                        ?: if (isRecentlyMissed(username)) null else runLookup(username, lookup)
                }
            }

    // The lookup itself can log an event with an employee; that employee must not start a second lookup.
    // A lookup that fails never reaches the caller: the log record is then written with an unknown employee.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun runLookup(username: String, lookup: (username: String) -> String?): String? {
        lookupInProgress.set(true)
        val keycloakUserId = try {
            lookup(username)
        } catch (exception: Exception) {
            null
        } finally {
            lookupInProgress.set(false)
        }
        keycloakUserId?.let { register(username, it) } ?: missedLookups.put(username, clock.instant())
        return keycloakUserId
    }

    private fun isRecentlyMissed(username: String) =
        missedLookups[username]?.let { clock.instant() < it.plus(MISSED_LOOKUP_RETRY_DELAY) } ?: false
}
