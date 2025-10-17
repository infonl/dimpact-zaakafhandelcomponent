/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import jakarta.ws.rs.NameBinding

/**
 * Custom annotation to mark internal-only endpoints.
 * These endpoints are not intended to be called by the ZAC frontend but rather using system integration.
 * They are secured using API key authentication using the [ZacApiKeyAuthFilter].
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@NameBinding
annotation class InternalEndpoint
