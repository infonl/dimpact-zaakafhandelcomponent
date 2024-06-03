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
import java.time.ZonedDateTime

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "template")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_template",
    sequenceName = "sq_template",
    allocationSize = 1
)
open class Template {
    @Id
    @GeneratedValue(generator = "sq_template", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_template")
    private var id: Long = 0

    @Column(name = "smartdocuments_id", nullable = false)
    lateinit var smartDocumentsId: String

    @ManyToOne
    @JoinColumn(name = "id_template_group", nullable = false)
    lateinit var templateGroup: TemplateGroup

    @Column(name = "name", nullable = false)
    lateinit var name: String

    @Column(name = "creation_date", nullable = false)
    lateinit var creationDate: ZonedDateTime
}
