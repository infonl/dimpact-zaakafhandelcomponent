/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates.log

import nl.info.zac.log.LogEvent

data object InitiatorKvkAddressReadFailed : LogEvent("Could not read the KVK address of the initiator, so the mail is sent without it")
