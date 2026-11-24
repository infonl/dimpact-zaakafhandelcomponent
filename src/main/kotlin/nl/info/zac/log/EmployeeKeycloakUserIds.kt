/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * The Keycloak user IDs of employees by username, shared by all [ZacLogger]s of a ZAC instance.
 *
 * A found ID is kept for the lifetime of the instance. A lookup that finds nothing is not repeated for that employee
 * for [RETRY_DELAY]. When Keycloak cannot be reached, no employee is looked up for [RETRY_DELAY], so that an outage
 * does not make every log record wait for Keycloak.
 */
internal object EmployeeKeycloakUserIds {
    private val RETRY_DELAY: Duration = Duration.ofMinutes(1)
    private val LOG by lazy { ZacLogger.getLogger(EmployeeKeycloakUserIds::class) }

    private val keycloakUserIds = ConcurrentHashMap<String, String>()
    private val missedLookups = ConcurrentHashMap<String, Instant>()
    private val lookupLocks = ConcurrentHashMap<String, Any>()
    private val lookupInProgress = ThreadLocal.withInitial { false }
    private val keycloakUnreachableAt = AtomicReference<Instant?>(null)

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
        keycloakUnreachableAt.set(null)
        employeeLookup = null
        clock = Clock.systemUTC()
    }

    // One lookup per employee at a time: a thread that resolves the same employee meanwhile waits for its result,
    // so that concurrent log records do not each call Keycloak, nor each wait for its timeout during an outage.
    private fun lookUp(username: String): String? =
        employeeLookup
            ?.takeUnless { lookupInProgress.get() || isKeycloakRecentlyUnreachable() || isRecentlyMissed(username) }
            ?.let { lookup ->
                val lookupLock = lookupLocks.computeIfAbsent(username) { Any() }
                try {
                    synchronized(lookupLock) {
                        keycloakUserIds[username]
                            ?: if (isRecentlyMissed(username)) null else runLookup(username, lookup)
                    }
                } finally {
                    lookupLocks.remove(username, lookupLock)
                }
            }

    // The lookup itself can log an event with an employee; that employee must not start a second lookup.
    // A lookup that fails never reaches the caller: the log record is then written with an unknown employee.
    @Suppress("TooGenericExceptionCaught")
    private fun runLookup(username: String, lookup: (username: String) -> String?): String? {
        lookupInProgress.set(true)
        return try {
            lookup(username).also { keycloakUserId ->
                keycloakUserId?.let { register(username, it) } ?: registerMissedLookup(username)
            }
        } catch (exception: Exception) {
            registerKeycloakUnreachable(exception)
            null
        } finally {
            lookupInProgress.set(false)
        }
    }

    // Logs only the first failure of an outage, not one for every log record that is written meanwhile
    private fun registerKeycloakUnreachable(exception: Exception) {
        val previouslyUnreachableAt = keycloakUnreachableAt.getAndSet(clock.instant())
        if (previouslyUnreachableAt?.isWithinRetryDelay() != true) {
            LOG.warning(GenericEvent(type = GenericEvent.Type.KEYCLOAK_UNREACHABLE_FOR_EMPLOYEE_LOOKUP), exception)
        }
    }

    private fun isKeycloakRecentlyUnreachable() = keycloakUnreachableAt.get()?.isWithinRetryDelay() ?: false

    // Removes the expired misses first, so that usernames that Keycloak does not know do not pile up
    private fun registerMissedLookup(username: String) {
        missedLookups.entries.removeIf { (_, missedAt) -> !missedAt.isWithinRetryDelay() }
        missedLookups[username] = clock.instant()
    }

    private fun Instant.isWithinRetryDelay() = clock.instant() < plus(RETRY_DELAY)

    private fun isRecentlyMissed(username: String) = missedLookups[username]?.isWithinRetryDelay() ?: false
}
