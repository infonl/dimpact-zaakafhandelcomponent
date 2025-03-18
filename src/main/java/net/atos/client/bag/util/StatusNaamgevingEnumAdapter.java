/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import nl.info.client.bag.model.generated.StatusNaamgeving;

public class StatusNaamgevingEnumAdapter implements JsonbAdapter<StatusNaamgeving, String> {

    @Override
    public String adaptToJson(final StatusNaamgeving statusNaamgeving) {
        return statusNaamgeving.toString();
    }

    @Override
    public StatusNaamgeving adaptFromJson(final String json) {
        return StatusNaamgeving.fromValue(json);
    }
}
