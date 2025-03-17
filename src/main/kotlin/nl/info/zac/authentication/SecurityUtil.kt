/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
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

        val FUNCTIONEEL_GEBRUIKER = LoggedInUser(
            "FG",
            "",
            "Functionele gebruiker",
            "Functionele gebruiker",
            null,
            setOf("functionele_gebruiker"),
            emptySet()
        )
    }

    /**
     * Produces an authenticated [LoggedInUser] for use in CDI Beans.
     * The authenticated [LoggedInUser] instance is retrieved from the current user session, where it is set via the
     * [UserPrincipalFilter]
     *
     * @return the currently logged-in user
     */
    @Produces
    fun getLoggedInUser() = getLoggedInUser(httpSession.get())
}

/**
 * If there is a logged-in user in the given [httpSession], return it.
 * Otherwise, if there is an HTTP Session but if it does not contain a logged-in user attribute, return `null`.
 * If the provided HTTP session is null, return `[FUNCTIONEEL_GEBRUIKER]`.
 */
fun getLoggedInUser(httpSession: HttpSession?): LoggedInUser? =
    if (httpSession != null) {
        httpSession.getAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE)?.let { it as LoggedInUser }
    } else {
        FUNCTIONEEL_GEBRUIKER // No session in async context!
    }

fun setLoggedInUser(httpSession: HttpSession, loggedInUser: LoggedInUser?) {
    httpSession.setAttribute(LOGGED_IN_USER_SESSION_ATTRIBUTE, loggedInUser)
}

fun setFunctioneelGebruiker(httpSession: HttpSession) {
    setLoggedInUser(httpSession, FUNCTIONEEL_GEBRUIKER)
}
