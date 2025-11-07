/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

data class TestUser(
    val username: String,
    val password: String,
    val displayName: String,
    val email: String? = null
)
