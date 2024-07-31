/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.model;

import java.net.URI;

public class RestDocumentCreationResponse {

    public final URI redirectURL;

    public final String message;

    public RestDocumentCreationResponse(final URI redirectURL, final String message) {
        this.redirectURL = redirectURL;
        this.message = message;
    }
}
