/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.identity

import com.unboundid.ldap.sdk.Filter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.identity.exception.IdentityRuntimeException
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.identity.model.getFullName
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.keycloak.admin.client.Keycloak
import java.util.Hashtable
import java.util.logging.Logger
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.Attributes
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls

@AllOpen
@NoArgConstructor
@ApplicationScoped
@Suppress("TooManyFunctions")
class IdentityService @Inject constructor(
    @Named("keycloakZacAdminClient")
    private val keycloak: Keycloak,

    @ConfigProperty(name = "LDAP_DN")
    private val usersDN: String,

    @ConfigProperty(name = "LDAP_DN")
    private val groupsDN: String,

    @ConfigProperty(name = "LDAP_URL")
    private val ldapUrl: String,

    @ConfigProperty(name = "LDAP_USER")
    private val ldapUser: String,

    @ConfigProperty(name = "LDAP_PASSWORD")
    private val ldapPassword: String
) {
    companion object {
        private const val USER_ID_ATTRIBUTE = "cn"
        private const val USER_FIRST_NAME_ATTRIBUTE = "givenName"
        private const val USER_LAST_NAME_ATTRIBUTE = "sn"
        private const val USER_MAIL_ATTRIBUTE = "mail"
        private val USER_ATTRIBUTES = arrayOf(
            USER_ID_ATTRIBUTE,
            USER_FIRST_NAME_ATTRIBUTE,
            USER_LAST_NAME_ATTRIBUTE,
            USER_MAIL_ATTRIBUTE
        )
        private const val GROUP_ID_ATTRIBUTE = "cn"
        private const val GROUP_NAME_ATTRIBUTE = "description"
        private const val GROUP_MAIL_ATTRIBUTE = "email"
        private val GROUP_ATTRIBUTES = arrayOf(GROUP_ID_ATTRIBUTE, GROUP_NAME_ATTRIBUTE, GROUP_MAIL_ATTRIBUTE)
        private const val GROUP_MEMBER_ATTRIBUTE = "uniqueMember"
        private val GROUP_MEMBERSHIP_ATTRIBUTES = arrayOf(GROUP_MEMBER_ATTRIBUTE)
        private const val USER_OBJECT_CLASS = "inetOrgPerson"
        private const val GROUP_OBJECT_CLASS = "groupOfUniqueNames"
        private const val OBJECT_CLASS_ATTRIBUTE = "objectClass"
    }

    private val environment = mapOf(
        Context.INITIAL_CONTEXT_FACTORY to "com.sun.jndi.ldap.LdapCtxFactory",
        Context.PROVIDER_URL to ldapUrl,
        Context.SECURITY_AUTHENTICATION to "simple",
        Context.SECURITY_PRINCIPAL to ldapUser,
        Context.SECURITY_CREDENTIALS to ldapPassword
    )

    fun listUsers(): List<User> =
        search(
            root = usersDN,
            filter = Filter.createANDFilter(Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, USER_OBJECT_CLASS)),
            attributesToReturn = USER_ATTRIBUTES
        ).map { it.toUser() }
            .sortedBy { it.getFullName() }

    fun listGroups(): List<Group> =
        search(
            root = groupsDN,
            filter = Filter.createANDFilter(Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, GROUP_OBJECT_CLASS)),
            attributesToReturn = GROUP_ATTRIBUTES
        ).map { it.toGroup() }
            .sortedBy { it.name }

    fun readUser(userId: String): User =
        search(
            root = usersDN,
            filter = Filter.createANDFilter(
                Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, USER_OBJECT_CLASS),
                Filter.createEqualityFilter(USER_ID_ATTRIBUTE, userId)
            ),
            attributesToReturn = USER_ATTRIBUTES
        ).map { it.toUser() }
            .firstOrNull() ?: User(userId)

    fun readGroup(groupId: String): Group =
        search(
            root = groupsDN,
            filter = Filter.createANDFilter(
                Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, GROUP_OBJECT_CLASS),
                Filter.createEqualityFilter(GROUP_ID_ATTRIBUTE, groupId)
            ),
            attributesToReturn = GROUP_ATTRIBUTES
        ).map { it.toGroup() }
            .firstOrNull() ?: Group(groupId)

    fun listUsersInGroup(groupId: String): List<User> =
        search(
            root = groupsDN,
            filter = Filter.createANDFilter(
                Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, GROUP_OBJECT_CLASS),
                Filter.createEqualityFilter(GROUP_ID_ATTRIBUTE, groupId)
            ),
            attributesToReturn = GROUP_MEMBERSHIP_ATTRIBUTES
        ).map { this.convertToMembers(it) }
            .flatMap { this.readUsers(it) }

    private fun readUsers(userIds: Collection<String>): List<User> =
        search(
            root = usersDN,
            filter = Filter.createANDFilter(
                Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, USER_OBJECT_CLASS),
                Filter.createORFilter(
                    userIds.map { Filter.createEqualityFilter(USER_ID_ATTRIBUTE, it) }
                )
            ),
            attributesToReturn = USER_ATTRIBUTES
        ).map { it.toUser() }

    private fun convertToMembers(attributes: Attributes): List<String> =
        readAttributeToListOfStrings(attributes)
            .mapNotNull { StringUtils.substringBetween(it, "cn=", ",") }

    private fun search(
        root: String,
        filter: Filter,
        attributesToReturn: Array<String>
    ): List<Attributes> {
        val searchControls = SearchControls().apply {
            returningAttributes = attributesToReturn
            searchScope = SearchControls.ONELEVEL_SCOPE
        }
        try {
            val dirContext = InitialDirContext(Hashtable(environment))
            val namingEnumeration = dirContext.search(
                root,
                filter.toString(),
                searchControls
            )
            val attributesList = mutableListOf<Attributes>()
            while (namingEnumeration.hasMore()) {
                attributesList.add(namingEnumeration.next().attributes)
            }
            dirContext.close()
            return attributesList
        } catch (namingException: NamingException) {
            throw IdentityRuntimeException(
                "Failed to search for attributes in LDAP",
                namingException
            )
        }
    }

    private fun readAttributeToString(attributes: Attributes, attributeName: String): String? {
        val attribute = attributes[attributeName]
        return attribute?.get()?.toString()
    }

    private fun readAttributeToListOfStrings(attributes: Attributes): List<String> =
        attributes[GROUP_MEMBER_ATTRIBUTE]?.all?.toList()?.map {
            it.toString()
        } ?: emptyList()

    private fun Attributes.toUser(): User {
        val userID = readAttributeToString(this, USER_ID_ATTRIBUTE)
        require(userID != null) { "User ID is required" }
        return User(
            id = userID,
            firstName = readAttributeToString(this, USER_FIRST_NAME_ATTRIBUTE),
            lastName = readAttributeToString(this, USER_LAST_NAME_ATTRIBUTE),
            email = readAttributeToString(this, USER_MAIL_ATTRIBUTE)
        )
    }

    private fun Attributes.toGroup(): Group {
        val groupID = readAttributeToString(this, GROUP_ID_ATTRIBUTE)
        require(groupID != null) { "Group ID is required" }
        return Group(
            id = groupID,
            name = readAttributeToString(this, GROUP_NAME_ATTRIBUTE) ?: groupID,
            email = readAttributeToString(this, GROUP_MAIL_ATTRIBUTE)
        )
    }
}
