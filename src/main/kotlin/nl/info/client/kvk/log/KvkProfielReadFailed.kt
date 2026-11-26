/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.log

import nl.info.zac.log.LogEvent

data object KvkProfielReadFailed : LogEvent("Could not read the KVK profiel of the bedrijf, so its address is left out")
