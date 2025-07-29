/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.servlet.http.HttpSession
import nl.info.zac.authentication.SecurityUtil.Companion.FUNCTIONEEL_GEBRUIKER
import nl.info.zac.authentication.SecurityUtil.Companion.LOGGED_IN_USER_SESSION_ATTRIBUTE
import java.io.Serial
import java.io.Serializable

class SecurityUtil @Inject constructor(
    @ActiveSession
    val httpSession: Instance<HttpSession>
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID = 654714651976511004L

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

        val systemUser: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    }

    /**
     * Produces an authenticated [LoggedInUser] for use in CDI Beans.
     *
     * If [systemUser] is enabled (set to true) the [FUNCTIONEEL_GEBRUIKER] user is returned.
     *
     * If http session is available, the authenticated [LoggedInUser] instance is retrieved from the current user
     * session, where it is set via the [UserPrincipalFilter]. If no session is available, null is returned
     *
     * @return the currently logged-in user, null or [FUNCTIONEEL_GEBRUIKER]
     */
    @Produces
    fun getLoggedInUser() =
        if (systemUser.get() == true) {
            FUNCTIONEEL_GEBRUIKER
        } else {
            httpSession.get()?.let {
                getLoggedInUser(it)
            }
        }
}

/**
 * If there is a logged-in user in the given [httpSession], return it.
 * Otherwise, if there is an HTTP Session but if it does not contain a logged-in user attribute, return `null`.
 */
fun getLoggedInUser(httpSession: HttpSession) =
    httpSession.getAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE)?.let { it as LoggedInUser }

fun setLoggedInUser(httpSession: HttpSession, loggedInUser: LoggedInUser) {
    httpSession.setAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE, loggedInUser)
}

fun setFunctioneelGebruiker(httpSession: HttpSession) {
    setLoggedInUser(httpSession, FUNCTIONEEL_GEBRUIKER)
}
