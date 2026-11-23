/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

abstract class FakeBaseEvent(val zaakIdentificatie: String, description: String) : LogEvent(description)
