/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreation.model;

import java.net.URI;

public class DocumentCreationResponse {

    private final URI redirectUrl;

    private final String message;

    public DocumentCreationResponse(final URI redirectUrl) {
        this.redirectUrl = redirectUrl;
        message = null;
    }

    public DocumentCreationResponse(final String message) {
        this.message = message;
        redirectUrl = null;
    }

    public URI getRedirectUrl() {
        return redirectUrl;
    }

    public String getMessage() {
        return message;
    }
}
