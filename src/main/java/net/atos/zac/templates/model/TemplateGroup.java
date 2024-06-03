package net.atos.zac.templates.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne
    @JoinColumn(name = "parent_template_group_id")
    private TemplateGroup parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private Set<TemplateGroup> children = new HashSet<>();

    @OneToMany(mappedBy = "templateGroup")
    private Set<Template> templates = new HashSet<>();

    public @NotBlank String getSmartdocumentsId() {
        return smartdocumentsId;
    }

    public void setSmartdocumentsId(@NotBlank String smartdocumentsId) {
        this.smartdocumentsId = smartdocumentsId;
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

    public TemplateGroup getParent() {
        return parent;
    }

    public void setParent(TemplateGroup parent) {
        this.parent = parent;
    }

    public Set<TemplateGroup> getChildren() {
        return children;
    }

    public void setChildren(Set<TemplateGroup> children) {
        this.children = children;
    }

    public Set<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(Set<Template> templates) {
        this.templates = templates;
    }
}
