/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util;

import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.net.URI;
import java.util.UUID;

/**
 *
 */
public final class UriUtil {

    private UriUtil() {
    }

    public static UUID uuidFromURI(final URI uri) {
        return uuidFromURI(uri.getPath());
    }

    public static Long longFromURI(final URI uri) {
        return longFromURI(uri.getPath());
    }

    public static UUID uuidFromURI(final String uri) {
        return UUID.fromString(extractLastPathParameter(uri));
    }

    public static Long longFromURI(final String uri) {
        return Long.parseLong(extractLastPathParameter(uri));
    }

    public static boolean isEqual(final URI a, final URI b) {
        return (a != null && b != null) ?
               extractLastPathParameter(a.getPath()).equals(extractLastPathParameter(b.getPath())) :
                a == null && b == null;
    }

    private static String extractLastPathParameter(final String path) {
        return contains(path, "/") ? substringAfterLast(path, "/") : path;
    }
}
