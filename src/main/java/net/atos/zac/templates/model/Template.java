package net.atos.zac.templates.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(schema = SCHEMA, name = "template")
@SequenceGenerator(schema = SCHEMA, name = "sq_template", sequenceName = "sq_template", allocationSize = 1)
public class Template {

    @Id
    @GeneratedValue(generator = "sq_template", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_template")
    private Long id;

    @NotBlank
    @Column(name = "smartdocuments_id", nullable = false)
    private String smartdocumentsId;

    @ManyToOne
    @JoinColumn(name = "id_template_group", nullable = false)
    private TemplateGroup templateGroup;

    @NotBlank
    @Column(name = "naam", nullable = false)
    private String naam;

    @Column(name = "creatiedatum", nullable = false)
    private ZonedDateTime creatiedatum;

    public @NotBlank String getSmartdocumentsId() {
        return smartdocumentsId;
    }

    public void setSmartdocumentsId(@NotBlank String smartdocumentsId) {
        this.smartdocumentsId = smartdocumentsId;
    }

    public TemplateGroup getTemplateGroup() {
        return templateGroup;
    }

    public void setTemplateGroup(TemplateGroup templateGroup) {
        this.templateGroup = templateGroup;
    }

    public @NotBlank String getNaam() {
        return naam;
    }

    public void setNaam(@NotBlank String naam) {
        this.naam = naam;
    }

    public ZonedDateTime getCreatiedatum() {
        return creatiedatum;
    }

    public void setCreatiedatum(ZonedDateTime creatiedatum) {
        this.creatiedatum = creatiedatum;
    }
}
