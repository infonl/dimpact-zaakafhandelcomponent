/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

class FakeZaakAssigned(val zaakIdentificatie: String, val groupId: String) : LogEvent("Zaak assigned")
