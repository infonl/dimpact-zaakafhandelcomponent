package net.atos.zac.admin.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@EntityListeners(ZaakafhandelParametersSummary.PreventAnyUpdate.class)
@Table(schema = SCHEMA, name = "zaakafhandelparameters")
@SequenceGenerator(schema = SCHEMA, name = "sq_zaakafhandelparameters", sequenceName = "sq_zaakafhandelparameters", allocationSize = 1)
public class ZaakafhandelParametersSummary extends ZaakafhandelParametersBase {

    public static class PreventAnyUpdate {
        @PrePersist
        void onPrePersist(Object o) {
            throw new IllegalStateException("Attempt to persist an entity of type " + (o == null ? "null" : o.getClass()));
        }

        @PreUpdate
        void onPreUpdate(Object o) {
            throw new IllegalStateException("Attempt to update an entity of type " + (o == null ? "null" : o.getClass()));
        }

        @PreRemove
        void onPreRemove(Object o) {
            throw new IllegalStateException("Attempt to remove an entity of type " + (o == null ? "null" : o.getClass()));
        }
    }

}
