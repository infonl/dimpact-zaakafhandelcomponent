package net.atos.zac.flowable.bpmn.function

import org.flowable.common.engine.api.delegate.FlowableMultiFunctionDelegate
import java.lang.reflect.Method
import kotlin.reflect.jvm.javaMethod

class TasksFunctionDelegate: FlowableMultiFunctionDelegate {
    companion object {
        private const val PREFIX = "taken"
    }

    override fun prefix(): String = PREFIX

    override fun localNames(): Collection<String?> = listOf(::behandelaar.name)

    override fun functionMethod(prefix: String?, localName: String?): Method? {
        if (prefix != PREFIX) return null

        return when(localName) {
            ::behandelaar.name -> ::behandelaar.javaMethod
            else -> null
        }
    }
}


