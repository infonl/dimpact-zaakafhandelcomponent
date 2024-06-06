package net.atos.zac.smartdocuments.templates.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import net.atos.zac.util.FlywayIntegrator
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import nl.lifely.zac.util.AllOpen
import java.time.ZonedDateTime

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "template")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_template",
    sequenceName = "sq_template",
    allocationSize = 1
)
@AllOpen
class SmartDocumentsTemplate {
    @Id
    @GeneratedValue(generator = "sq_template", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_template")
    var id: Long = 0

    @Column(name = "smartdocuments_id", nullable = false)
    lateinit var smartDocumentsId: String

    @ManyToOne
    @JoinColumn(name = "template_group_id", nullable = false)
    lateinit var templateGroup: SmartDocumentsTemplateGroup

    @ManyToOne
    @JoinColumn(name = "zaakafhandelparameters_id", nullable = false)
    lateinit var zaakafhandelParameters: ZaakafhandelParameters

    @Column(name = "name", nullable = false)
    lateinit var name: String

    @Column(name = "creation_date", nullable = false)
    lateinit var creationDate: ZonedDateTime
}
