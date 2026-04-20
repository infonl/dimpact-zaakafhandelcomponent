/*
 * SPDX-FileCopyrightText: 2024 INFO.nl, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.util

import jakarta.json.bind.adapter.JsonbAdapter
import nl.info.zac.productaanvraag.model.generated.Betaling

/**
 * JSON adapter for the [Betaling.Status] enum that matches on the enum's value instead of the enum's name.
 */
class BetalingStatusEnumJsonAdapter : JsonbAdapter<Betaling.Status, String> {
    override fun adaptToJson(betalingStatus: Betaling.Status): String = betalingStatus.toString()
    override fun adaptFromJson(value: String): Betaling.Status = Betaling.Status.fromValue(value)
}
