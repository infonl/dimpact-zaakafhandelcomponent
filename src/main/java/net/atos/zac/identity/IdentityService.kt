/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.identity;

import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import java.util.Collection;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.unboundid.ldap.sdk.Filter;

import net.atos.zac.identity.model.Group;
import net.atos.zac.identity.model.User;

@ApplicationScoped
public class IdentityService {

    private static final String USER_ID_ATTRIBUTE = "cn";

    private static final String USER_FIRST_NAME_ATTRIBUTE = "givenName";

    private static final String USER_LAST_NAME_ATTRIBUTE = "sn";

    private static final String USER_MAIL_ATTRIBUTE = "mail";

    private static final String[] USER_ATTRIBUTES = {USER_ID_ATTRIBUTE, USER_FIRST_NAME_ATTRIBUTE, USER_LAST_NAME_ATTRIBUTE,
                                                     USER_MAIL_ATTRIBUTE};

    private static final String GROUP_ID_ATTRIBUTE = "cn";

    private static final String GROUP_NAME_ATTRIBUTE = "description";

    private static final String GROUP_MAIL_ATTRIBUTE = "email";

    private static final String[] GROUP_ATTRIBUTES = {GROUP_ID_ATTRIBUTE, GROUP_NAME_ATTRIBUTE, GROUP_MAIL_ATTRIBUTE};

    private static final String GROUP_MEMBER_ATTRIBUTE = "uniqueMember";

    private static final String[] GROUP_MEMBERSHIP_ATTRIBUTES = {GROUP_MEMBER_ATTRIBUTE};

    private static final String USER_OBJECT_CLASS = "inetOrgPerson";

    private static final String GROUP_OBJECT_CLASS = "groupOfUniqueNames";

    private static final String OBJECT_CLASS_ATTRIBUTE = "objectClass";

    private final Map<String, String> environment;

    @Inject
    @ConfigProperty(name = "LDAP_DN")
    private String usersDN;

    @Inject
    @ConfigProperty(name = "LDAP_DN")
    private String groupsDN;

    public IdentityService() {
        environment = Map.of(
                Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory",
                Context.PROVIDER_URL, getConfig().getValue("LDAP_URL", String.class),
                Context.SECURITY_AUTHENTICATION, "simple",
                Context.SECURITY_PRINCIPAL, getConfig().getValue("LDAP_USER", String.class),
                Context.SECURITY_CREDENTIALS, getConfig().getValue("LDAP_PASSWORD", String.class)
        );
    }

    public List<User> listUsers() {
        return search(
                usersDN,
                Filter.createANDFilter(Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, USER_OBJECT_CLASS)),
                USER_ATTRIBUTES
        ).stream()
                .map(this::convertToUser)
                .sorted(Comparator.comparing(User::getFullName))
                .toList();
    }

    public List<Group> listGroups() {
        return search(
                groupsDN,
                Filter.createANDFilter(Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, GROUP_OBJECT_CLASS)),
                GROUP_ATTRIBUTES
        ).stream()
                .map(this::convertToGroup)
                .sorted(Comparator.comparing(Group::getName))
                .toList();
    }

    public User readUser(final String userId) {
        return search(
                usersDN,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, USER_OBJECT_CLASS),
                        Filter.createEqualityFilter(USER_ID_ATTRIBUTE, userId)
                ),
                USER_ATTRIBUTES
        ).stream()
                .findAny()
                .map(this::convertToUser)
                .orElseGet(() -> new User(userId));
    }

    public Group readGroup(final String groupId) {
        return search(
                groupsDN,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, GROUP_OBJECT_CLASS),
                        Filter.createEqualityFilter(GROUP_ID_ATTRIBUTE, groupId)
                ),
                GROUP_ATTRIBUTES
        ).stream()
                .findAny()
                .map(this::convertToGroup)
                .orElseGet(() -> new Group(groupId));

    }

    public List<User> listUsersInGroup(final String groupId) {
        return search(
                groupsDN,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, GROUP_OBJECT_CLASS),
                        Filter.createEqualityFilter(GROUP_ID_ATTRIBUTE, groupId)
                ),
                GROUP_MEMBERSHIP_ATTRIBUTES
        ).stream()
                .map(this::convertToMembers)
                .flatMap(this::readUsers)
                .sorted(Comparator.comparing(User::getFullName))
                .toList();
    }

    private Stream<User> readUsers(final Collection<String> userIds) {
        return search(
                usersDN,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(OBJECT_CLASS_ATTRIBUTE, USER_OBJECT_CLASS),
                        Filter.createORFilter(
                                userIds.stream()
                                        .map(userId -> Filter.createEqualityFilter(USER_ID_ATTRIBUTE, userId))
                                        .toList()
                        )
                ),
                USER_ATTRIBUTES
        ).stream().map(this::convertToUser);
    }

    private User convertToUser(final Attributes attributes) {
        return new User(readAttributeToString(attributes, USER_ID_ATTRIBUTE),
                readAttributeToString(attributes, USER_FIRST_NAME_ATTRIBUTE),
                readAttributeToString(attributes, USER_LAST_NAME_ATTRIBUTE),
                readAttributeToString(attributes, USER_MAIL_ATTRIBUTE));
    }

    private Group convertToGroup(final Attributes attributes) {
        return new Group(readAttributeToString(attributes, GROUP_ID_ATTRIBUTE),
                readAttributeToString(attributes, GROUP_NAME_ATTRIBUTE),
                readAttributeToString(attributes, GROUP_MAIL_ATTRIBUTE));
    }

    private List<String> convertToMembers(final Attributes attributes) {
        return readAttributeToListOfStrings(attributes).stream()
                .map(member -> substringBetween(member, "cn=", ","))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Attributes> search(
            final String root,
            final Filter filter,
            final String[] attributesToReturn
    ) {
        final SearchControls searchControls = new SearchControls();
        searchControls.setReturningAttributes(attributesToReturn);
        searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        try {
            final DirContext dirContext = new InitialDirContext(new Hashtable<>(environment));
            final NamingEnumeration<SearchResult> namingEnumeration = dirContext.search(
                    root,
                    filter.toString(),
                    searchControls
            );
            final List<Attributes> attributesList = new LinkedList<>();
            while (namingEnumeration.hasMore()) {
                attributesList.add(namingEnumeration.next().getAttributes());
            }
            dirContext.close();
            return attributesList;
        } catch (final NamingException e) {
            throw new RuntimeException(e);
        }
    }

    private String readAttributeToString(final Attributes attributes, final String attributeName) {
        try {
            final Attribute attribute = attributes.get(attributeName);
            return attribute != null ? attribute.get().toString() : null;
        } catch (final NamingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> readAttributeToListOfStrings(final Attributes attributes) {
        try {
            final List<String> strings = new LinkedList<>();
            final Attribute attribute = attributes.get(IdentityService.GROUP_MEMBER_ATTRIBUTE);
            if (attribute != null) {
                final NamingEnumeration<?> enumeration = attribute.getAll();
                while (enumeration.hasMore()) {
                    strings.add(enumeration.next().toString());
                }
            }
            return strings;
        } catch (final NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
