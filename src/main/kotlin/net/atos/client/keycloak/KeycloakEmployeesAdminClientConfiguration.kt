package net.atos.client.keycloak

import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import net.atos.zac.identity.IdentityService
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
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

    @ConfigProperty(name = "KEYCLOAK_ADMIN_CLIENT_ID")
    private val clientId: String,

    @ConfigProperty(name = "KEYCLOAK_ADMIN_CLIENT_SECRET")
    private val clientSecret: String,

    @ConfigProperty(name = "AUTH_REALM")
    private val realmName: String,
){
    companion object {
        private val LOG = Logger.getLogger(IdentityService::class.java.name)
    }

    @Produces
    @Named("keycloakZacRealmResource")
    fun build(): RealmResource {
        LOG.info(
            """
                Building Keycloak admin client using: url: '$keycloakUrl', realm: '$realmName', 
                clientid: '$clientId', clientsecret: '*******'
            """.trimIndent()
        )
        return KeycloakBuilder.builder()
            .serverUrl(keycloakUrl)
            .realm(realmName)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .build()
            .realm(realmName)
    }
}
