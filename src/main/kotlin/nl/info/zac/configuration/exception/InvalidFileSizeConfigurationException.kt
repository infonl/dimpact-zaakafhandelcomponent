/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration.exception

/**
 * ZAC deliberately fails to start rather than accepting uploads that are certain to run out of memory.
 */
class InvalidFileSizeConfigurationException(message: String) : RuntimeException(message)
