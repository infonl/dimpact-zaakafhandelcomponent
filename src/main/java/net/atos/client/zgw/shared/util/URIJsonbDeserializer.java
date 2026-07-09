/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.util;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import org.apache.commons.lang3.StringUtils;

public class URIJsonbDeserializer implements JsonbDeserializer<URI> {

    @Override
    public URI deserialize(
            final JsonParser parser,
            final DeserializationContext ctx,
            final Type rtType
    ) {
        try {
            final String uri = parser.getString();
            return StringUtils.isNotEmpty(uri) ? new URI(uri) : null;
        } catch (final IllegalStateException e) {
            // ignore this exception for now. workaround for the following error:
            // "RESTEASY008200: JSON Binding deserialization error: jakarta.json.bind.JsonbException:
            // Internal error: JsonParser#getString() is valid only for KEY_NAME, VALUE_STRING,
            // VALUE_NUMBER parser states. But current parser state is VALUE_NULL
            return null;
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
