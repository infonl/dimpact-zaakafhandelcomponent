/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

/**
 * A log record of ZAC: a fixed [description] and the typed fields of the subclass.
 *
 * Pass the description as a literal; never build it from values, because those belong in fields.
 */
abstract class LogEvent(val description: String)
