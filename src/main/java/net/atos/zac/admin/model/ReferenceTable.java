/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(schema = SCHEMA, name = "referentie_tabel")
@SequenceGenerator(schema = SCHEMA, name = "sq_referentie_tabel", sequenceName = "sq_referentie_tabel", allocationSize = 1)
public class ReferenceTable {

    public enum Systeem {
        ADVIES,
        AFZENDER,
        COMMUNICATIEKANAAL,
        DOMEIN,
        SERVER_ERROR_ERROR_PAGINA_TEKST
    }

    @Id
    @GeneratedValue(generator = "sq_referentie_tabel", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_referentie_tabel")
    private Long id;

    @NotBlank
    @Column(name = "code", nullable = false)
    private String code;

    @NotBlank
    @Column(name = "naam", nullable = false)
    private String naam;

    @Transient
    private Boolean systeem;

    @OneToMany(mappedBy = "tabel", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<ReferenceTableValue> waarden = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(final String naam) {
        this.naam = naam;
    }

    public boolean isSysteem() {
        if (systeem == null) {
            systeem = Arrays.stream(Systeem.values())
                    .anyMatch(value -> value.name().equals(code));
        }
        return systeem;
    }

    public List<ReferenceTableValue> getWaarden() {
        return waarden.stream()
                .sorted(Comparator.comparingInt(ReferenceTableValue::getVolgorde))
                .toList();
    }

    public void setWaarden(final List<ReferenceTableValue> waarden) {
        this.waarden.clear();
        waarden.forEach(this::addWaarde);
    }

    public void addWaarde(final ReferenceTableValue waarde) {
        waarde.setTabel(this);
        waarde.setVolgorde(waarden.size());
        waarden.add(waarde);
    }

    public void removeWaarde(final ReferenceTableValue waarde) {
        waarden.remove(waarde);
    }
}
