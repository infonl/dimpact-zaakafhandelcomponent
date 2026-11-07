/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import nl.info.client.bag.model.generated.Gebruiksdoel;

public class GebruiksdoelEnumAdapter implements JsonbAdapter<Gebruiksdoel, String> {

    @Override
    public String adaptToJson(final Gebruiksdoel gebruiksdoel) {
        return gebruiksdoel.toString();
    }

    @Override
    public Gebruiksdoel adaptFromJson(final String json) {
        return Gebruiksdoel.fromValue(json);
    }
}
