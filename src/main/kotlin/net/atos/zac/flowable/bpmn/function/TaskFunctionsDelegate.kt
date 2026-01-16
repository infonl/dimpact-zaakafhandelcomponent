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

        /**
         * The functions we register in Flowable are listed below.
         *
         * Flowable uses Java reflection - [Method:invoke] that checks `Modifier.isStatic(modifiers)` to invoke these methods, and we need
         * to ensure they are accessible. To do that, we use package level functions and *NOT* companion object functions marked with
         * @JVMStatic as they do *NOT* generate static modifier @ 2026-01-07 in the compiled bytecode, but only `public final` modifiers.
         */
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
