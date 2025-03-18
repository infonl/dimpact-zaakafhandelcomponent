/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import nl.info.client.bag.model.generated.StatusPand;

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
