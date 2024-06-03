package net.atos.zac.smartdocuments.templates.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import net.atos.zac.util.FlywayIntegrator
import nl.lifely.zac.util.AllOpen
import java.time.ZonedDateTime

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "template_group")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_template_group",
    sequenceName = "sq_template_group",
    allocationSize = 1
)
@AllOpen
class TemplateGroup {
    @Id
    @GeneratedValue(generator = "sq_template_group", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_template_group")
    var id: Long? = null

    @Column(name = "smartdocuments_id", nullable = false)
    lateinit var smartDocumentsId: String

    @Column(name = "name", nullable = false)
    lateinit var name: String

    @Column(name = "creation_date", nullable = false)
    var creationDate: ZonedDateTime? = null

    @ManyToOne
    @JoinColumn(name = "parent_template_group_id")
    var parent: TemplateGroup? = null

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL])
    var children: MutableSet<TemplateGroup> = mutableSetOf()

    @OneToMany(mappedBy = "templateGroup")
    var templates: MutableSet<Template> = mutableSetOf()
}
