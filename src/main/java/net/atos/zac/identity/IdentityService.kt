/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.identity

import com.unboundid.ldap.sdk.Filter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.identity.model.getFullNameResolved
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Hashtable
import java.util.LinkedList
import java.util.Objects
import java.util.stream.Stream
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.Attributes
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls

@AllOpen
@NoArgConstructor
@ApplicationScoped
@Suppress("TooManyFunctions")
class IdentityService @Inject constructor(
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

    fun listUsers(): List<User> {
        return search(
            usersDN,
            Filter.createANDFilter(Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, USER_OBJECT_CLASS)),
            USER_ATTRIBUTES
        ).stream()
            .map { attributes: Attributes -> this.convertToUser(attributes) }
            .sorted(Comparator.comparing { it.getFullNameResolved() })
            .toList()
    }

    fun listGroups(): List<Group> {
        return search(
            groupsDN,
            Filter.createANDFilter(Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, GROUP_OBJECT_CLASS)),
            GROUP_ATTRIBUTES
        ).stream()
            .map { attributes: Attributes -> this.convertToGroup(attributes) }
            .sorted(Comparator.comparing { it.name!! })
            .toList()
    }

    fun readUser(userId: String): User {
        return search(
            usersDN,
            Filter.createANDFilter(
                Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, USER_OBJECT_CLASS),
                Filter.createEqualityFilter(USER_ID_ATTRIBUTE, userId)
            ),
            USER_ATTRIBUTES
        ).stream()
            .findAny()
            .map { attributes: Attributes -> this.convertToUser(attributes) }
            .orElseGet { User(userId) }
    }

    fun readGroup(groupId: String): Group =
        search(
            groupsDN,
            Filter.createANDFilter(
                Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, GROUP_OBJECT_CLASS),
                Filter.createEqualityFilter(GROUP_ID_ATTRIBUTE, groupId)
            ),
            GROUP_ATTRIBUTES
        ).stream()
            .findAny()
            .map { attributes: Attributes -> this.convertToGroup(attributes) }
            .orElseGet { Group(groupId) }

    fun listUsersInGroup(groupId: String): List<User> {
        return search(
            groupsDN,
            Filter.createANDFilter(
                Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, GROUP_OBJECT_CLASS),
                Filter.createEqualityFilter(GROUP_ID_ATTRIBUTE, groupId)
            ),
            GROUP_MEMBERSHIP_ATTRIBUTES
        ).stream()
            .map { this.convertToMembers(it) }
            .flatMap { this.readUsers(it) }
            .sorted(Comparator.comparing { it.getFullNameResolved() })
            .toList()
    }

    private fun readUsers(userIds: Collection<String?>): Stream<User> {
        return search(
            usersDN,
            Filter.createANDFilter(
                Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, USER_OBJECT_CLASS),
                Filter.createORFilter(
                    userIds.stream()
                        .map { userId: String? -> Filter.createEqualityFilter(USER_ID_ATTRIBUTE, userId) }
                        .toList()
                )
            ),
            USER_ATTRIBUTES
        ).stream().map { attributes: Attributes -> this.convertToUser(attributes) }
    }

    private fun convertToUser(attributes: Attributes): User {
        val userID = readAttributeToString(attributes, USER_ID_ATTRIBUTE)
        require(userID != null) { "User ID is required" }
        return User(
            id = userID,
            firstName = readAttributeToString(attributes, USER_FIRST_NAME_ATTRIBUTE),
            lastName = readAttributeToString(attributes, USER_LAST_NAME_ATTRIBUTE),
            email = readAttributeToString(attributes, USER_MAIL_ATTRIBUTE)
        )
    }

    private fun convertToGroup(attributes: Attributes): Group {
        val groupID = readAttributeToString(attributes, GROUP_ID_ATTRIBUTE)
        require(groupID != null) { "Group ID is required" }
        return Group(
            id = groupID,
            name = readAttributeToString(attributes, GROUP_NAME_ATTRIBUTE),
            email = readAttributeToString(attributes, GROUP_MAIL_ATTRIBUTE)
        )
    }

    private fun convertToMembers(attributes: Attributes): List<String?> {
        return readAttributeToListOfStrings(attributes).stream()
            .map { StringUtils.substringBetween(it, "cn=", ",") }
            .filter { Objects.nonNull(it) }
            .toList()
    }

    private fun search(
        root: String?,
        filter: Filter,
        attributesToReturn: Array<String>
    ): List<Attributes> {
        val searchControls = SearchControls()
        searchControls.returningAttributes = attributesToReturn
        searchControls.searchScope = SearchControls.ONELEVEL_SCOPE
        try {
            val dirContext: DirContext = InitialDirContext(Hashtable(environment))
            val namingEnumeration = dirContext.search(
                root,
                filter.toString(),
                searchControls
            )
            val attributesList: MutableList<Attributes> = LinkedList()
            while (namingEnumeration.hasMore()) {
                attributesList.add(namingEnumeration.next().attributes)
            }
            dirContext.close()
            return attributesList
        } catch (e: NamingException) {
            throw RuntimeException(e)
        }
    }

    private fun readAttributeToString(attributes: Attributes, attributeName: String): String? {
        try {
            val attribute = attributes[attributeName]
            return attribute?.get()?.toString()
        } catch (e: NamingException) {
            throw RuntimeException(e)
        }
    }

    private fun readAttributeToListOfStrings(attributes: Attributes): List<String> {
        try {
            val strings: MutableList<String> = LinkedList()
            val attribute = attributes[GROUP_MEMBER_ATTRIBUTE]
            if (attribute != null) {
                val enumeration = attribute.all
                while (enumeration.hasMore()) {
                    strings.add(enumeration.next().toString())
                }
            }
            return strings
        } catch (e: NamingException) {
            throw RuntimeException(e)
        }
    }
}
