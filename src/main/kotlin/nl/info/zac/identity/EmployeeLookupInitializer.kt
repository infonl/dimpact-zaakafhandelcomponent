/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import nl.info.zac.log.ZacLogger
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * Gives [ZacLogger] the lookup of the Keycloak user ID of an employee when the application starts.
 *
 * [ZacLogger] is created outside CDI and must not depend on [IdentityService], which logs through [ZacLogger] itself.
 */
@ApplicationScoped
@AllOpen
@NoArgConstructor
class EmployeeLookupInitializer @Inject constructor(
    private val identityService: IdentityService
) {
    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) =
        ZacLogger.useEmployeeLookup(identityService::findKeycloakUserId)
}
