/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.keycloak

import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource
import java.util.logging.Logger

@AllOpen
@NoArgConstructor
@Singleton
class KeycloakEmployeesAdminClientConfiguration @Inject constructor(
    @ConfigProperty(name = "AUTH_SERVER")
    private val keycloakUrl: String,

    @ConfigProperty(name = "AUTH_REALM")
    private val realmName: String,

    @ConfigProperty(name = "KEYCLOAK_ADMIN_CLIENT_ID")
    private val zacAdminClientId: String,

    @ConfigProperty(name = "KEYCLOAK_ADMIN_CLIENT_SECRET")
    private val zacAdminClientSecret: String
) {
    companion object {
        private val LOG = Logger.getLogger(KeycloakEmployeesAdminClientConfiguration::class.java.name)
    }

    @Produces
    @Named("keycloakZacRealmResource")
    fun build(): RealmResource {
        LOG.info(
            "Building Keycloak admin client using: url: '$keycloakUrl', realm: '$realmName', " +
                "client id: '$zacAdminClientId', client secret: '*******'"
        )
        return KeycloakBuilder.builder()
            .serverUrl(keycloakUrl)
            .realm(realmName)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId(zacAdminClientId)
            .clientSecret(zacAdminClientSecret)
            .build()
            .realm(realmName)
    }
}
