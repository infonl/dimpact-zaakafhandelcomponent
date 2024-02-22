/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import net.atos.client.bag.model.StatusPand;

public class StatusPandEnumAdapter implements JsonbAdapter<StatusPand, String> {

    @Override
    public String adaptToJson(final StatusPand statusPand) {
        return statusPand.toString();
    }

    @Override
    public StatusPand adaptFromJson(final String json) {
        return StatusPand.fromValue(json);
    }
}
