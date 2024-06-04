/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signaleringen

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.servlet.http.HttpSession
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.authentication.ActiveSession
import net.atos.zac.authentication.SecurityUtil
import net.atos.zac.signalering.SignaleringenService
import nl.lifely.zac.util.NoArgConstructor

@Path("admin/signaleringen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
class SignaleringenAdminRestService @Inject constructor(
    private val signaleringenService: SignaleringenService,
    @ActiveSession
    private val httpSession: Instance<HttpSession>
) {

    @DELETE
    @Path("/delete-old")
    fun deleteOldSignaleringen() {
        SecurityUtil.setFunctioneelGebruiker(httpSession.get())
        signaleringenService.deleteOldSignaleringen()
    }
}
