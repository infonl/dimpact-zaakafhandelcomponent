package net.atos.zac.mail.model

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.zrc.model.Zaak
import org.flowable.task.api.TaskInfo

class Bronnen private constructor(
    val zaak: Zaak?,
    val document: EnkelvoudigInformatieObject?,
    val taskInfo: TaskInfo?
) {
    class Builder {
        private var zaak: Zaak? = null

        private var document: EnkelvoudigInformatieObject? = null

        private var taskInfo: TaskInfo? = null

        fun add(zaak: Zaak?): Builder {
            this.zaak = zaak
            return this
        }

        fun add(document: EnkelvoudigInformatieObject?): Builder {
            this.document = document
            return this
        }

        fun add(taskInfo: TaskInfo?): Builder {
            this.taskInfo = taskInfo
            return this
        }

        fun build(): Bronnen {
            return Bronnen(zaak, document, taskInfo)
        }
    }

    companion object {
        @JvmStatic
        fun fromZaak(zaak: Zaak?): Bronnen {
            return Builder().add(zaak).build()
        }

        fun fromDocument(document: EnkelvoudigInformatieObject?): Bronnen {
            return Builder().add(document).build()
        }

        fun fromTaak(taskInfo: TaskInfo?): Bronnen {
            return Builder().add(taskInfo).build()
        }
    }
}
