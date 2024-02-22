/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import net.atos.client.bag.model.StatusWoonplaats;

public class StatusWoonplaatsEnumAdapter implements JsonbAdapter<StatusWoonplaats, String> {

    @Override
    public String adaptToJson(final StatusWoonplaats statusWoonplaats) {
        return statusWoonplaats.toString();
    }

    @Override
    public StatusWoonplaats adaptFromJson(final String json) {
        return StatusWoonplaats.fromValue(json);
    }
}
