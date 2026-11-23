/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

class FakeDerivedEvent(
    zaakIdentificatie: String,
    val objectTypes: List<Outcome>,
    val count: Int
) : FakeBaseEvent(zaakIdentificatie, "Derived event")
