/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.log

import nl.info.zac.log.LogEvent

data object KvkSearchFailed : LogEvent("Failed to search for company information using the KVK API")
