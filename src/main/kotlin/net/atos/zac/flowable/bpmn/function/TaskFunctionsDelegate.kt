/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn.function

import org.flowable.common.engine.api.delegate.FlowableMultiFunctionDelegate
import java.lang.reflect.Method
import kotlin.reflect.jvm.javaMethod

class TaskFunctionsDelegate : FlowableMultiFunctionDelegate {
    companion object {
        private const val PREFIX = "taken"

        // Functions here need to be package level as Flowable uses [Method:invoke] that checks `Modifier.isStatic(modifiers)`
        // @JVMStatic for companion object functions does *NOT* generate static modifier @ 2026-01-07, but only `public final`
        private val functions = mapOf(
            ::groep.name to ::groep.javaMethod,
            ::behandelaar.name to ::behandelaar.javaMethod
        )
    }

    override fun prefix(): String = PREFIX

    override fun localNames(): Collection<String?> =
        functions.keys.toList()

    override fun functionMethod(prefix: String?, localName: String?): Method? =
        functions[localName].takeIf { prefix == PREFIX }
}
