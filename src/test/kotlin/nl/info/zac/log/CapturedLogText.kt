/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

/**
 * A log record as one text: the description of its event, followed by its event fields sorted by name,
 * as `<description> [name=value, ...]`. The fields that [ZacLogger] adds itself are left out.
 */
class CapturedLogText(val message: String, val thrown: Throwable?)
