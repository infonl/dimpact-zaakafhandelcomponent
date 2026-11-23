/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.keycloak

import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import nl.info.client.keycloak.log.KeycloakAdminClientBuilding
import nl.info.zac.log.ZacLogger
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.JacksonProvider
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.admin.client.spi.ResteasyClientClassicProvider
import java.util.concurrent.TimeUnit

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
        private val LOG = ZacLogger.getLogger(KeycloakEmployeesAdminClientConfiguration::class)

        // Without a bound, a Keycloak that does not answer holds the business thread, and the employee lookup of
        // ZacLogger runs on that thread.
        private const val TIMEOUT_SECONDS = 2L

        // The priority with which the Keycloak admin client registers its own JSON provider
        private const val JACKSON_PROVIDER_PRIORITY = 100
    }

    @Produces
    @Named("keycloakZacRealmResource")
    fun build(): RealmResource {
        LOG.info(KeycloakAdminClientBuilding(url = keycloakUrl, realm = realmName, clientId = zacAdminClientId))
        return KeycloakBuilder.builder()
            .serverUrl(keycloakUrl)
            .realm(realmName)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId(zacAdminClientId)
            .clientSecret(zacAdminClientSecret)
            .resteasyClient(
                ResteasyClientClassicProvider.createClientBuilder()
                    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .register(JacksonProvider::class.java, JACKSON_PROVIDER_PRIORITY)
                    .build()
            )
            .build()
            .realm(realmName)
    }
}
