/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.servlet.http.HttpSession
import kotlinx.coroutines.asContextElement
import nl.info.zac.authentication.LoggedInUserProvider.Companion.FUNCTIONEEL_GEBRUIKER
import nl.info.zac.authentication.LoggedInUserProvider.Companion.LOGGED_IN_USER_SESSION_ATTRIBUTE
import java.io.Serial
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class LoggedInUserProvider @Inject constructor(
    @ActiveSession
    val httpSession: Instance<HttpSession>
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID = 654714651976511004L

        private val LOG = Logger.getLogger(LoggedInUserProvider::class.java.name)

        private const val MAX_LOGGED_FALLBACK_ORIGINS = 100
        private const val FALLBACK_ORIGIN_FRAMES = 3L
        private val ZAC_PACKAGES = listOf("nl.info.", "net.atos.")

        /** Origins already reported, so a recurring fallback is logged once instead of on every call. */
        internal val loggedFallbackOrigins: MutableSet<String> = ConcurrentHashMap.newKeySet()

        /**
         * Constant which indicates in which [HttpSession] attribute the current authenticated [LoggedInUser] can be found.
         */
        const val LOGGED_IN_USER_SESSION_ATTRIBUTE = "logged-in-user"

        /**
         * Internal-only 'system user' which is used for internal ZAC API request not originating from an actual user.
         * Requests to these internal API calls are typically initiated from external systems or cron jobs.
         */
        val FUNCTIONEEL_GEBRUIKER = LoggedInUser(
            "FG",
            "",
            "Functionele gebruiker",
            "Functionele gebruiker",
            null,
            emptySet(),
            emptySet()
        )

        /**
         * System user for a zaak created from a productaanvraag, so it can be told apart from other
         * system work in the zaakhistorie and be given its own roles. The source of the aanvraag is
         * not fixed, so it is not named after one: the zaak records it in its toelichting instead.
         */
        val PRODUCTAANVRAAG_GEBRUIKER = LoggedInUser(
            "PA",
            "",
            "Productaanvraag",
            "Productaanvraag",
            null,
            emptySet(),
            emptySet()
        )

        val systemUser: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

        /**
         * The user that started background work, for threads that have no HTTP session of their own.
         */
        val asyncContextUser: ThreadLocal<LoggedInUser?> = ThreadLocal.withInitial { null }
    }

    /**
     * Produces an authenticated [LoggedInUser] for use in CDI Beans.
     *
     * If [systemUser] is enabled (set to true) the [FUNCTIONEEL_GEBRUIKER] user is returned.
     *
     * If http session is available, the authenticated [LoggedInUser] instance is retrieved from the current user
     * session, where it is set via the [UserPrincipalFilter]
     *
     * [asyncContextUser] takes precedence over the session, so background work stays attributed to the user
     * that started it even when it runs inside a request whose session is not its own.
     *
     * Work that no user session owns should say so, through [runAsSystemUser] or [runAsLoggedInUser].
     * Where it does not, this falls back to the [FUNCTIONEEL_GEBRUIKER] and reports where that happened.
     *
     * @return the currently logged-in user, or [FUNCTIONEEL_GEBRUIKER] when no user is in scope
     */
    @Produces
    fun getLoggedInUser() =
        if (systemUser.get() ?: false) {
            FUNCTIONEEL_GEBRUIKER // explicitly requested
        } else {
            // an explicitly named user wins over the session, which for background work is not its own
            asyncContextUser.get()
                ?: httpSession.get()?.let { getLoggedInUser(it) }
                ?: fallBackToFunctioneelGebruiker()
        }

    private fun fallBackToFunctioneelGebruiker(): LoggedInUser {
        val origin = fallbackOrigin()
        if (loggedFallbackOrigins.add(origin) && loggedFallbackOrigins.size <= MAX_LOGGED_FALLBACK_ORIGINS) {
            LOG.warning { "No logged-in user in scope, using the functionele gebruiker. Called from: $origin" }
        }
        return FUNCTIONEEL_GEBRUIKER
    }

    /**
     * Names the ZAC code that asked for the user. The nearest frames are CDI machinery producing this
     * bean, which say nothing about the path that needs fixing.
     */
    private fun fallbackOrigin(): String =
        StackWalker.getInstance().walk { frames ->
            frames
                .filter { it.className != LoggedInUserProvider::class.java.name }
                .map { "${it.className}.${it.methodName}" }
                .filter { ZAC_PACKAGES.any(it::startsWith) }
                .limit(FALLBACK_ORIGIN_FRAMES)
                .toList()
        }.joinToString(" <- ").ifEmpty { "outside ZAC code" }
}

/**
 * If there is a logged-in user in the given [httpSession], return it.
 * Returns `null` if the session does not contain a logged-in user attribute or if the session has been
 * invalidated (e.g. the user logged out while a request was still in-flight).
 */
fun getLoggedInUser(httpSession: HttpSession) =
    try {
        httpSession.getAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE)?.let { it as LoggedInUser }
    } catch (_: IllegalStateException) {
        // Session was invalidated (user logged out) while the request was still in-flight; treat as no session.
        null
    }

fun setLoggedInUser(httpSession: HttpSession, loggedInUser: LoggedInUser) =
    httpSession.setAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE, loggedInUser)

fun setFunctioneelGebruiker(httpSession: HttpSession) =
    setLoggedInUser(httpSession, FUNCTIONEEL_GEBRUIKER)

/**
 * Carries [loggedInUser] into a coroutine, which has no HTTP session and may resume on another thread.
 */
fun loggedInUserContext(loggedInUser: LoggedInUser) =
    LoggedInUserProvider.asyncContextUser.asContextElement(loggedInUser)

/**
 * Runs [block] as [loggedInUser], for work that has no user session of its own.
 */
fun <T> runAsLoggedInUser(loggedInUser: LoggedInUser, block: () -> T): T {
    LoggedInUserProvider.asyncContextUser.set(loggedInUser)
    return try {
        block()
    } finally {
        LoggedInUserProvider.asyncContextUser.remove()
    }
}

/**
 * Runs [block] as the system user, for work that no user session owns.
 */
fun <T> runAsSystemUser(block: () -> T): T {
    val wasSystemUser = LoggedInUserProvider.systemUser.get()
    LoggedInUserProvider.systemUser.set(true)
    return try {
        block()
    } finally {
        if (wasSystemUser) LoggedInUserProvider.systemUser.set(true) else LoggedInUserProvider.systemUser.remove()
    }
}

/**
 * Carries the current system user state into a coroutine, which does not inherit the thread it was
 * started from.
 */
fun systemUserContext() =
    LoggedInUserProvider.systemUser.asContextElement(LoggedInUserProvider.systemUser.get())
