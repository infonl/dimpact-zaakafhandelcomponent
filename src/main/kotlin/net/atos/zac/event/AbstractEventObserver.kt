/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.event

/**
 * Generic code for beans that listen to AbstractEvents.
 */
@Suppress("AbstractClassCanBeInterface")
abstract class AbstractEventObserver<EVENT : AbstractEvent<*, *>> {
    abstract fun onFire(event: EVENT)
}
