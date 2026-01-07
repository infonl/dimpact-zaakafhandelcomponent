/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
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

import nl.info.client.zgw.util.ZgwJwtTokenUtilsKt;
import nl.info.zac.authentication.LoggedInUser;


public class ZgwClientHeadersFactory implements ClientHeadersFactory {
    public static final String X_AUDIT_TOELICHTING_HEADER = "X-Audit-Toelichting";

    private Instance<LoggedInUser> loggedInUserInstance;
    private String clientId;
    private String secret;

    /**
     * No-arg constructor for CDI.
     */
    public ZgwClientHeadersFactory() {
    }

    @Inject
    public ZgwClientHeadersFactory(
            final Instance<LoggedInUser> loggedInUserInstance,

            @ConfigProperty(name = "ZGW_API_CLIENTID")
            final String clientId,

            @ConfigProperty(name = "ZGW_API_SECRET")
            final String secret
    ) {
        this.loggedInUserInstance = loggedInUserInstance;
        this.clientId = clientId;
        this.secret = secret;
    }

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
        outgoingHeaders.add(HttpHeaders.AUTHORIZATION, ZgwJwtTokenUtilsKt.generateZgwJwtToken(clientId, secret, loggedInUser));
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
