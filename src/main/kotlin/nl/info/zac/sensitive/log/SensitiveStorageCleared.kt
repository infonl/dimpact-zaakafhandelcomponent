/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.sensitive.log

import nl.info.zac.log.LogEvent

data object SensitiveStorageCleared : LogEvent("Sensitive storage cleared")
