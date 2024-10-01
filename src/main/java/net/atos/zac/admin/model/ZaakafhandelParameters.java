/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;


@Entity
@Table(schema = SCHEMA, name = "zaakafhandelparameters")
@SequenceGenerator(schema = SCHEMA, name = "sq_zaakafhandelparameters", sequenceName = "sq_zaakafhandelparameters", allocationSize = 1)
public class ZaakafhandelParameters extends ZaakafhandelParametersBase {

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaakafhandelParameters", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<HumanTaskParameters> humanTaskParametersCollection;

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaakafhandelParameters", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<UserEventListenerParameters> userEventListenerParametersCollection;

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaakafhandelParameters", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<ZaakbeeindigParameter> zaakbeeindigParameters;

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaakafhandelParameters", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<MailtemplateKoppeling> mailtemplateKoppelingen;

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaakafhandelParameters", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<ZaakAfzender> zaakAfzenders;

    public Set<HumanTaskParameters> getHumanTaskParametersCollection() {
        return humanTaskParametersCollection != null ? humanTaskParametersCollection : Collections.emptySet();
    }

    public void setHumanTaskParametersCollection(final Collection<HumanTaskParameters> humanTaskParametersCollection) {
        if (this.humanTaskParametersCollection == null) {
            this.humanTaskParametersCollection = new HashSet<>();
        } else {
            this.humanTaskParametersCollection.clear();
        }
        humanTaskParametersCollection.forEach(this::addHumanTaskParameters);
    }

    public Set<MailtemplateKoppeling> getMailtemplateKoppelingen() {
        return mailtemplateKoppelingen != null ? mailtemplateKoppelingen : Collections.emptySet();
    }

    public void setMailtemplateKoppelingen(final Collection<MailtemplateKoppeling> mailtemplateKoppelingen) {
        if (this.mailtemplateKoppelingen == null) {
            this.mailtemplateKoppelingen = new HashSet<>();
        } else {
            this.mailtemplateKoppelingen.clear();
        }
        mailtemplateKoppelingen.forEach(this::addMailtemplateKoppeling);
    }

    public Set<ZaakbeeindigParameter> getZaakbeeindigParameters() {
        return zaakbeeindigParameters != null ? zaakbeeindigParameters : Collections.emptySet();
    }

    public void setZaakbeeindigParameters(final Collection<ZaakbeeindigParameter> zaakbeeindigParameters) {
        if (this.zaakbeeindigParameters == null) {
            this.zaakbeeindigParameters = new HashSet<>();
        } else {
            this.zaakbeeindigParameters.clear();
        }
        zaakbeeindigParameters.forEach(this::addZaakbeeindigParameter);
    }

    public Set<UserEventListenerParameters> getUserEventListenerParametersCollection() {
        return userEventListenerParametersCollection != null ? userEventListenerParametersCollection : Collections.emptySet();
    }

    public void setUserEventListenerParametersCollection(
            final Collection<UserEventListenerParameters> userEventListenerParametersCollection
    ) {
        if (this.userEventListenerParametersCollection == null) {
            this.userEventListenerParametersCollection = new HashSet<>();
        } else {
            this.userEventListenerParametersCollection.clear();
        }
        userEventListenerParametersCollection.forEach(this::addUserEventListenerParameters);
    }

    public Set<ZaakAfzender> getZaakAfzenders() {
        return zaakAfzenders != null ? zaakAfzenders : Collections.emptySet();
    }

    public void setZaakAfzenders(final Collection<ZaakAfzender> zaakAfzenders) {
        if (this.zaakAfzenders == null) {
            this.zaakAfzenders = new HashSet<>();
        } else {
            this.zaakAfzenders.clear();
        }
        zaakAfzenders.forEach(this::addZaakAfzender);
    }

    private void addMailtemplateKoppeling(final MailtemplateKoppeling mailtemplateKoppeling) {
        mailtemplateKoppeling.setZaakafhandelParameters(this);
        mailtemplateKoppelingen.add(mailtemplateKoppeling);
    }

    private void addHumanTaskParameters(final HumanTaskParameters humanTaskParameters) {
        humanTaskParameters.setZaakafhandelParameters(this);
        humanTaskParametersCollection.add(humanTaskParameters);
    }

    private void addZaakbeeindigParameter(final ZaakbeeindigParameter zaakbeeindigParameter) {
        zaakbeeindigParameter.setZaakafhandelParameters(this);
        zaakbeeindigParameters.add(zaakbeeindigParameter);
    }

    private void addUserEventListenerParameters(final UserEventListenerParameters userEventListenerParameters) {
        userEventListenerParameters.setZaakafhandelParameters(this);
        userEventListenerParametersCollection.add(userEventListenerParameters);
    }

    private void addZaakAfzender(final ZaakAfzender zaakAfzender) {
        zaakAfzender.setZaakafhandelParameters(this);
        zaakAfzenders.add(zaakAfzender);
    }

    public ZaakbeeindigParameter readZaakbeeindigParameter(final Long zaakbeeindigRedenId) {
        return getZaakbeeindigParameters().stream()
                .filter(zaakbeeindigParameter -> zaakbeeindigParameter.getZaakbeeindigReden().getId().equals(zaakbeeindigRedenId))
                .findAny().orElseThrow(() -> new RuntimeException(
                        String.format("No ZaakbeeindigParameter found for zaaktypeUUID: '%s' and zaakbeeindigRedenId: '%d'",
                                getZaakTypeUUID(),
                                zaakbeeindigRedenId)));
    }


    public UserEventListenerParameters readUserEventListenerParameters(final String planitemDefinitionID) {
        return getUserEventListenerParametersCollection().stream()
                .filter(userEventListenerParameters -> userEventListenerParameters.getPlanItemDefinitionID().equals(planitemDefinitionID))
                .findAny().orElseThrow(() -> new RuntimeException(
                        String.format("No UserEventListenerParameters found for zaaktypeUUID: '%s' and planitemDefinitionID: '%s'",
                                getZaakTypeUUID(),
                                planitemDefinitionID)));
    }

    public Optional<HumanTaskParameters> findHumanTaskParameter(final String planitemDefinitionID) {
        return getHumanTaskParametersCollection().stream()
                .filter(humanTaskParameter -> humanTaskParameter.getPlanItemDefinitionID().equals(planitemDefinitionID))
                .findAny();
    }
}
