/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.zac.app.identity.model.RestGroup
import nl.info.zac.app.identity.model.RestLoggedInUser
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.identity.model.toRestGroups
import nl.info.zac.app.identity.model.toRestLoggedInUser
import nl.info.zac.app.identity.model.toRestUsers
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@Singleton
@Path("identity")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class IdentityRestService @Inject constructor(
    private val identityService: IdentityService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    @GET
    @Path("groups")
    fun listGroups(): List<RestGroup> = identityService.listGroups().toRestGroups()

    /**
     * Returns the list of groups that are authorised for the `behandelaar` application role for the given zaaktype.
     *
     * Once the PABC feature flag has been removed, this should be refactored to take the zaaktype 'omschrijving' field
     * instead of the zaaktype UUID.
     * This is because in the PABC group authorization is done on zaaktype and not on a specific zaaktype 'version'.
     * That will be a good time to refactor the URI of this endpoint as well, to reflect that it concerns behandelaar groups only.
     * Or possibly add a second parameter to specify the requested application role.
     */
    @GET
    @Path("groups/behandelaar/zaaktype/{zaaktypeUuid}")
    fun listBehandelaarGroupsForZaaktype(@PathParam("zaaktypeUuid") zaaktypeUuid: UUID): List<RestGroup> =
        identityService.listGroupsForBehandelaarRoleAndZaaktypeUuid(zaaktypeUuid).toRestGroups()

    @GET
    @Path("groups/{groupId}/users")
    fun listUsersInGroup(@PathParam("groupId") groupId: String): List<RestUser> =
        identityService.listUsersInGroup(groupId).toRestUsers()

    @GET
    @Path("users")
    fun listUsers(): List<RestUser> = identityService.listUsers().toRestUsers()

    @GET
    @Path("loggedInUser")
    fun readLoggedInUser(): RestLoggedInUser = loggedInUserInstance.get().toRestLoggedInUser()
}
