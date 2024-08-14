/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.identity

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.identity.converter.RestGroupConverter
import net.atos.zac.app.identity.converter.RestUserConverter
import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.app.identity.model.RestLoggedInUser
import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.identity.IdentityService
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@Singleton
@Path("identity")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class IdentityRestService @Inject constructor(
    private val groupConverter: RestGroupConverter,
    private val userConverter: RestUserConverter,
    private val identityService: IdentityService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    @GET
    @Path("groups")
    fun listGroups(): List<RestGroup> =
        identityService.listGroups().let {
            groupConverter.convertGroups(it)
        }

    @GET
    @Path("groups/{groupId}/users")
    fun listUsersInGroup(@PathParam("groupId") groupId: String): List<RestUser> =
        identityService.listUsersInGroup(groupId).let {
            userConverter.convertUsers(it)
        }

    @GET
    @Path("users")
    fun listUsers(): List<RestUser> =
        identityService.listUsers().let {
            return userConverter.convertUsers(it)
        }

    @GET
    @Path("loggedInUser")
    fun readLoggedInUser(): RestLoggedInUser =
        userConverter.convertLoggedInUser(loggedInUserInstance.get())
}
