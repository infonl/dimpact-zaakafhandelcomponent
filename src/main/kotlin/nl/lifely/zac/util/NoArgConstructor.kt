/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.util

/**
 * Annotation to add a no-arg constructor to a Kotlin data class so that for example
 * JAX-RS is able to instantiate the class.
 */
annotation class NoArgConstructor
