/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mail.log

import nl.info.zac.log.LogEvent

data object SmtpAuthenticationDisabled : LogEvent("SMTP authentication disabled")
