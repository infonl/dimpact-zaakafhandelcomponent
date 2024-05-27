/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.lifely.zac.util.AllOpen

class Deposit {
    @JsonbProperty("SmartDocument")
    lateinit var smartDocument: SmartDocument
}
