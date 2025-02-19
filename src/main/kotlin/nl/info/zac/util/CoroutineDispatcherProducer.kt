/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Produces a CoroutineDispatcher which can be injected in classes that need to run coroutines.
 * Injecting this dispatcher in classes makes it possible to switch it to a test dispatcher in unit tests.
 */
@AllOpen
@ApplicationScoped
class CoroutineDispatcherProducer {

    @Produces
    fun provideDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
