package net.atos.zac.mail.model;

import org.flowable.task.api.TaskInfo;

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.zrc.model.Zaak;

public class Bronnen {

    public final Zaak zaak;

    public final EnkelvoudigInformatieObject document;

    public final TaskInfo taskInfo;

    private Bronnen(final Zaak zaak, final EnkelvoudigInformatieObject document,
            final TaskInfo taskInfo) {
        this.zaak = zaak;
        this.document = document;
        this.taskInfo = taskInfo;
    }

    public static Bronnen fromZaak(final Zaak zaak) {
        return new Builder().add(zaak).build();
    }

    public static Bronnen fromDocument(final EnkelvoudigInformatieObject document) {
        return new Builder().add(document).build();
    }

    public static Bronnen fromTaak(final TaskInfo taskInfo) {
        return new Builder().add(taskInfo).build();
    }

    public static class Builder {
        private Zaak zaak;

        private EnkelvoudigInformatieObject document;

        private TaskInfo taskInfo;

        public Builder add(final Zaak zaak) {
            this.zaak = zaak;
            return this;
        }

        public Builder add(final EnkelvoudigInformatieObject document) {
            this.document = document;
            return this;
        }

        public Builder add(final TaskInfo taskInfo) {
            this.taskInfo = taskInfo;
            return this;
        }

        public Bronnen build() {
            return new Bronnen(zaak, document, taskInfo);
        }
    }
}
