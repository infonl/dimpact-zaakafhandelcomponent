/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant.log

import nl.info.zac.log.LogEvent
import nl.info.zac.log.Redacted

class VestigingPartijWithoutKvkIdentificator(
    val vestigingsnummer: String?,
    // the KVK number of an eenmanszaak identifies a person
    @Redacted val kvkNummer: String?
) : LogEvent("The related partij of the vestiging partij does not have the required partij identificator with the KVK number")
