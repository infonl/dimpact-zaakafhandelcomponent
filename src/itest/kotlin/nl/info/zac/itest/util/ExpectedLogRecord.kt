/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import org.json.JSONObject

/**
 * A ZAC log record, written by the JSON formatter of WildFly, that a test expects: [description] names it in the
 * failure message, and [matches] checks the `message` and the `mdc` fields of one record.
 */
class ExpectedLogRecord(val description: String, val matches: (JSONObject) -> Boolean)
