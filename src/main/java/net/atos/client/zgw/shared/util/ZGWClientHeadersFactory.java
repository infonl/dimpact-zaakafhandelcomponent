/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nullable;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import net.atos.client.util.JWTTokenGenerator;
import nl.info.zac.authentication.LoggedInUser;

public class ZGWClientHeadersFactory implements ClientHeadersFactory {
    public static final String X_AUDIT_TOELICHTING_HEADER = "X-Audit-Toelichting";

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    @Inject
    @ConfigProperty(name = "ZGW_API_CLIENTID")
    private String clientId;

    @Inject
    @ConfigProperty(name = "ZGW_API_SECRET")
    private String secret;

    private static final Map<String, String> AUDIT_TOELICHTINGEN = new ConcurrentHashMap<>();

    @Override
    public MultivaluedMap<String, String> update(
            final MultivaluedMap<String, String> incomingHeaders,
            final MultivaluedMap<String, String> outgoingHeaders
    ) {
        final LoggedInUser loggedInUser = loggedInUserInstance.get();
        try {
            addAutorizationHeader(outgoingHeaders, loggedInUser);
            addXAuditToelichtingHeader(outgoingHeaders, loggedInUser);
            return outgoingHeaders;
        } finally {
            clearAuditToelichting(loggedInUser);
        }
    }

    public String generateJWTToken() {
        return JWTTokenGenerator.generate(clientId, secret, loggedInUserInstance.get());
    }

    public void setAuditToelichting(final String toelichting) {
        final LoggedInUser loggedInUser = loggedInUserInstance.get();
        if (loggedInUser != null) {
            AUDIT_TOELICHTINGEN.put(loggedInUser.getId(), toelichting);
        }
    }

    private void clearAuditToelichting(@Nullable final LoggedInUser loggedInUser) {
        if (loggedInUser != null) {
            AUDIT_TOELICHTINGEN.remove(loggedInUser.getId());
        }
    }

    private void addAutorizationHeader(
            final MultivaluedMap<String, String> outgoingHeaders,
            final LoggedInUser loggedInUser
    ) {
        outgoingHeaders.add(HttpHeaders.AUTHORIZATION, JWTTokenGenerator.generate(clientId, secret, loggedInUser));
    }

    private void addXAuditToelichtingHeader(
            final MultivaluedMap<String, String> outgoingHeaders,
            final @Nullable LoggedInUser loggedInUser
    ) {
        if (loggedInUser != null) {
            final String toelichting = AUDIT_TOELICHTINGEN.get(loggedInUser.getId());
            if (toelichting != null) {
                outgoingHeaders.add(X_AUDIT_TOELICHTING_HEADER, toelichting);
            }
        }
    }
}
