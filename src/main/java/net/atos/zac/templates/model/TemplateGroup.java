package net.atos.zac.templates.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import java.time.ZonedDateTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(schema = SCHEMA, name = "template_group")
@SequenceGenerator(schema = SCHEMA, name = "sq_template_group", sequenceName = "sq_template_group", allocationSize = 1)
public class TemplateGroup {

    @Id
    @GeneratedValue(generator = "sq_template_group", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_template_group")
    private Long id;

    @NotBlank
    @Column(name = "smartdocuments_id", nullable = false)
    private String smartdocumentsId;

    @NotBlank
    @Column(name = "naam", nullable = false)
    private String naam;

    @Column(name = "creatiedatum", nullable = false)
    private ZonedDateTime creatiedatum;

    @OneToMany(mappedBy = "templateGroup")
    private Set<Template> templates;
}
