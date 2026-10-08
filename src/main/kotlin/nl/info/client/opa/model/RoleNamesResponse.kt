/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.opa.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty

data class RoleNamesResponse @JsonbCreator constructor(
    @param:JsonbProperty("result") val result: Set<String>?
)
