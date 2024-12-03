/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.util;

import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.net.URI;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

/**
 * URI utility functions.
 */
public final class URIUtil {

    public static boolean equals(final URI resourceURI1, final URI resourceURI2) {
        return StringUtils.equals(parseUUIDAsStringFromResourceURI(resourceURI1), parseUUIDAsStringFromResourceURI(resourceURI2));
    }

    public static UUID parseUUIDFromResourceURI(final URI resourceURI) {
        return UUID.fromString(parseUUIDAsStringFromResourceURI(resourceURI));
    }

    private static String parseUUIDAsStringFromResourceURI(final URI uri) {
        final String uriString = uri.toString();
        return contains(uriString, "/") ? substringAfterLast(uriString, "/") : uriString;
    }

    private URIUtil() {
    }
}
