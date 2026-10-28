/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.task.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty

data class ValueChangeData @JsonbCreator constructor(
    @param:JsonbProperty("oldValue") val oldValue: String,
    @param:JsonbProperty("newValue") val newValue: String,
    @param:JsonbProperty("explanation") val explanation: String?
)
