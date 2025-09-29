/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model;

import static nl.info.zac.database.flyway.FlywayIntegrator.SCHEMA;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption;

@Entity
@Table(schema = SCHEMA, name = "zaaktype_cmmn_configuration")
@SequenceGenerator(schema = SCHEMA, name = "sq_zaaktype_cmmn_configuration", sequenceName = "sq_zaaktype_cmmn_configuration", allocationSize = 1)
public class ZaaktypeCmmnConfiguration {

    /** Naam van property: {@link ZaaktypeCmmnConfiguration#zaakTypeUUID} */
    public static final String ZAAKTYPE_UUID = "zaakTypeUUID";

    /** Naam van property: {@link ZaaktypeCmmnConfiguration#zaaktypeOmschrijving} */
    public static final String ZAAKTYPE_OMSCHRIJVING = "zaaktypeOmschrijving";

    /** Naam van property: {@link ZaaktypeCmmnConfiguration#creatiedatum} */
    public static final String CREATIEDATUM = "creatiedatum";

    /** Naam van property: {@link ZaaktypeCmmnConfiguration#productaanvraagtype} */
    public static final String PRODUCTAANVRAAGTYYPE = "productaanvraagtype";

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_configuration", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @NotNull @Column(name = "zaaktype_uuid", nullable = false)
    private UUID zaakTypeUUID;

    @NotBlank @Column(name = "zaaktype_omschrijving", nullable = false)
    private String zaaktypeOmschrijving;

    /**
     * This field is nullable because when a new zaaktype is published,
     * ZAC creates an initial 'inactive' zaaktypeCmmnConfiguration record without a value.
     * For 'active' zaaktypeCmmnConfiguration, however, this field becomes mandatory is never null.
     */
    @Column(name = "id_case_definition")
    private String caseDefinitionID;

    /**
     * This field is nullable because when a new zaaktype is published,
     * ZAC creates an initial 'inactive' zaaktypeCmmnConfiguration record without a value.
     * For 'active' zaaktypeCmmnConfiguration, however, this field becomes mandatory is never null.
     */
    @Column(name = "groep_id")
    private String groepID;

    @Column(name = "gebruikersnaam_behandelaar")
    private String gebruikersnaamMedewerker;

    @Column(name = "eindatum_gepland_waarschuwing")
    private Integer einddatumGeplandWaarschuwing;

    @Column(name = "uiterlijke_einddatum_afdoening_waarschuwing")
    private Integer uiterlijkeEinddatumAfdoeningWaarschuwing;

    @Column(name = "niet_ontvankelijk_resultaattype_uuid")
    private UUID nietOntvankelijkResultaattype;

    @Column(name = "creatiedatum", nullable = false)
    private ZonedDateTime creatiedatum;

    /**
     * This field has a sensible default value because it is non-nullable.
     */
    @Column(name = "intake_mail", nullable = false)
    private String intakeMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT.name();

    /**
     * This field has a sensible default value because it is non-nullable.
     */
    @Column(name = "afronden_mail", nullable = false)
    private String afrondenMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT.name();

    @Column(name = "productaanvraagtype")
    private String productaanvraagtype;

    @Column(name = "domein")
    private String domein;

    @Column(name = "smartdocuments_ingeschakeld")
    private boolean smartDocumentsIngeschakeld;

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaaktypeCmmnConfiguration", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<ZaaktypeCmmnHumantaskParameters> zaaktypeCmmnHumantaskParametersCollection;

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaaktypeCmmnConfiguration", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<ZaaktypeCmmnUsereventlistenerParameters> zaaktypeCmmnUsereventlistenerParametersCollection;

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaaktypeCmmnConfiguration", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<ZaaktypeCmmnCompletionParameters> zaaktypeCmmnCompletionParameters;

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaaktypeCmmnConfiguration", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<ZaaktypeCmmnMailtemplateParameters> zaaktypeCmmnMailtemplateKoppelingen;

    @OneToOne(mappedBy = "zaaktypeCmmnConfiguration", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private ZaaktypeCmmnEmailParameters zaaktypeCmmnEmailParameters;

    @OneToOne(mappedBy = "zaaktypeCmmnConfiguration", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private ZaaktypeCmmnBetrokkeneParameters zaaktypeCmmnBetrokkeneParameters;

    @OneToOne(mappedBy = "zaaktypeCmmnConfiguration", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private ZaaktypeCmmnBrpParameters zaaktypeCmmnBrpParameters;

    // The set is necessary for Hibernate when you have more than one eager collection on an entity.
    @OneToMany(mappedBy = "zaaktypeCmmnConfiguration", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<ZaaktypeCmmnZaakafzenderParameters> zaaktypeCmmnZaakafzenderParameters;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public UUID getZaakTypeUUID() {
        return zaakTypeUUID;
    }

    public void setZaakTypeUUID(final UUID zaakTypeUUID) {
        this.zaakTypeUUID = zaakTypeUUID;
    }

    public String getCaseDefinitionID() {
        return caseDefinitionID;
    }

    public void setCaseDefinitionID(final String caseDefinitionID) {
        this.caseDefinitionID = caseDefinitionID;
    }

    public String getGroepID() {
        return groepID;
    }

    public void setGroepID(final String groepID) {
        this.groepID = groepID;
    }

    public String getGebruikersnaamMedewerker() {
        return gebruikersnaamMedewerker;
    }

    public void setGebruikersnaamMedewerker(final String gebruikersnaamMedewerker) {
        this.gebruikersnaamMedewerker = gebruikersnaamMedewerker;
    }

    public Integer getEinddatumGeplandWaarschuwing() {
        return einddatumGeplandWaarschuwing;
    }

    public void setEinddatumGeplandWaarschuwing(final Integer streefdatumWaarschuwing) {
        this.einddatumGeplandWaarschuwing = streefdatumWaarschuwing;
    }

    public Integer getUiterlijkeEinddatumAfdoeningWaarschuwing() {
        return uiterlijkeEinddatumAfdoeningWaarschuwing;
    }

    public void setUiterlijkeEinddatumAfdoeningWaarschuwing(final Integer fataledatumWaarschuwing) {
        this.uiterlijkeEinddatumAfdoeningWaarschuwing = fataledatumWaarschuwing;
    }

    public UUID getNietOntvankelijkResultaattype() {
        return nietOntvankelijkResultaattype;
    }

    public void setNietOntvankelijkResultaattype(final UUID nietOntvankelijkResultaattype) {
        this.nietOntvankelijkResultaattype = nietOntvankelijkResultaattype;
    }

    public Set<ZaaktypeCmmnHumantaskParameters> getHumanTaskParametersCollection() {
        return zaaktypeCmmnHumantaskParametersCollection != null ? zaaktypeCmmnHumantaskParametersCollection : Collections.emptySet();
    }

    public void setHumanTaskParametersCollection(
            final Collection<ZaaktypeCmmnHumantaskParameters> desiredZaaktypeCmmnHumantaskParametersCollection
    ) {
        if (zaaktypeCmmnHumantaskParametersCollection == null) {
            zaaktypeCmmnHumantaskParametersCollection = new HashSet<>();
        }
        desiredZaaktypeCmmnHumantaskParametersCollection.forEach(this::setHumanTaskParameters);
        zaaktypeCmmnHumantaskParametersCollection.removeIf(
                humanTaskParameters -> isElementNotInCollection(desiredZaaktypeCmmnHumantaskParametersCollection, humanTaskParameters)
        );
    }

    public Set<ZaaktypeCmmnMailtemplateParameters> getMailtemplateKoppelingen() {
        return zaaktypeCmmnMailtemplateKoppelingen != null ? zaaktypeCmmnMailtemplateKoppelingen : Collections.emptySet();
    }

    public void setMailtemplateKoppelingen(
            final Collection<ZaaktypeCmmnMailtemplateParameters> desiredZaaktypeCmmnMailtemplateKoppelingen
    ) {
        if (zaaktypeCmmnMailtemplateKoppelingen == null) {
            zaaktypeCmmnMailtemplateKoppelingen = new HashSet<>();
        }
        desiredZaaktypeCmmnMailtemplateKoppelingen.forEach(this::setMailtemplateKoppeling);
        zaaktypeCmmnMailtemplateKoppelingen.removeIf(
                zaaktypeCmmnMailtemplateKoppelingen -> isElementNotInCollection(desiredZaaktypeCmmnMailtemplateKoppelingen,
                        zaaktypeCmmnMailtemplateKoppelingen)
        );
    }

    public ZaaktypeCmmnEmailParameters getAutomaticEmailConfirmation() {
        return zaaktypeCmmnEmailParameters;
    }

    public void setAutomaticEmailConfirmation(final ZaaktypeCmmnEmailParameters zaaktypeCmmnEmailParameters) {
        this.zaaktypeCmmnEmailParameters = zaaktypeCmmnEmailParameters;
    }

    public Set<ZaaktypeCmmnCompletionParameters> getZaakbeeindigParameters() {
        return zaaktypeCmmnCompletionParameters != null ? zaaktypeCmmnCompletionParameters : Collections.emptySet();
    }

    public void setZaakbeeindigParameters(final Collection<ZaaktypeCmmnCompletionParameters> desiredZaaktypeCmmnCompletionParameters) {
        if (zaaktypeCmmnCompletionParameters == null) {
            zaaktypeCmmnCompletionParameters = new HashSet<>();
        }
        desiredZaaktypeCmmnCompletionParameters.forEach(this::setZaakbeeindigParameter);
        zaaktypeCmmnCompletionParameters.removeIf(
                zaaktypeCmmnCompletionParameter -> isElementNotInCollection(desiredZaaktypeCmmnCompletionParameters,
                        zaaktypeCmmnCompletionParameter)
        );
    }

    public Set<ZaaktypeCmmnUsereventlistenerParameters> getUserEventListenerParametersCollection() {
        return zaaktypeCmmnUsereventlistenerParametersCollection != null ? zaaktypeCmmnUsereventlistenerParametersCollection : Collections
                .emptySet();
    }

    public void setUserEventListenerParametersCollection(
            final Collection<ZaaktypeCmmnUsereventlistenerParameters> desiredZaaktypeCmmnUsereventlistenerParametersCollection
    ) {
        if (zaaktypeCmmnUsereventlistenerParametersCollection == null) {
            zaaktypeCmmnUsereventlistenerParametersCollection = new HashSet<>();
        }
        desiredZaaktypeCmmnUsereventlistenerParametersCollection.forEach(this::setUserEventListenerParameters);
        zaaktypeCmmnUsereventlistenerParametersCollection.removeIf(
                userEventListenerParam -> isElementNotInCollection(desiredZaaktypeCmmnUsereventlistenerParametersCollection,
                        userEventListenerParam)
        );
    }

    public Set<ZaaktypeCmmnZaakafzenderParameters> getZaakAfzenders() {
        return zaaktypeCmmnZaakafzenderParameters != null ? zaaktypeCmmnZaakafzenderParameters : Collections.emptySet();
    }

    public void setZaakAfzenders(final Collection<ZaaktypeCmmnZaakafzenderParameters> desiredZaaktypeCmmnZaakafzenderParameters) {
        if (zaaktypeCmmnZaakafzenderParameters == null) {
            zaaktypeCmmnZaakafzenderParameters = new HashSet<>();
        }
        desiredZaaktypeCmmnZaakafzenderParameters.forEach(this::setZaakAfzender);
        zaaktypeCmmnZaakafzenderParameters.removeIf(
                zaakAfzender -> isElementNotInCollection(desiredZaaktypeCmmnZaakafzenderParameters, zaakAfzender)
        );
    }

    private void setMailtemplateKoppeling(final ZaaktypeCmmnMailtemplateParameters zaaktypeCmmnMailtemplateParameters) {
        zaaktypeCmmnMailtemplateParameters.setZaaktypeCmmnConfiguration(this);
        setComponent(zaaktypeCmmnMailtemplateKoppelingen, zaaktypeCmmnMailtemplateParameters);
    }

    private void setHumanTaskParameters(final ZaaktypeCmmnHumantaskParameters zaaktypeCmmnHumantaskParameters) {
        zaaktypeCmmnHumantaskParameters.setZaaktypeCmmnConfiguration(this);
        setComponent(zaaktypeCmmnHumantaskParametersCollection, zaaktypeCmmnHumantaskParameters);
    }

    private void setZaakbeeindigParameter(final ZaaktypeCmmnCompletionParameters zaaktypeCmmnCompletionParameter) {
        zaaktypeCmmnCompletionParameter.setZaaktypeCmmnConfiguration(this);
        setComponent(zaaktypeCmmnCompletionParameters, zaaktypeCmmnCompletionParameter);
    }

    private void setUserEventListenerParameters(final ZaaktypeCmmnUsereventlistenerParameters zaaktypeCmmnUsereventlistenerParameter) {
        zaaktypeCmmnUsereventlistenerParameter.setZaaktypeCmmnConfiguration(this);
        setComponent(zaaktypeCmmnUsereventlistenerParametersCollection, zaaktypeCmmnUsereventlistenerParameter);
    }

    private void setZaakAfzender(final ZaaktypeCmmnZaakafzenderParameters zaaktypeCmmnZaakafzenderParameters) {
        zaaktypeCmmnZaakafzenderParameters.setZaaktypeCmmnConfiguration(this);
        setComponent(this.zaaktypeCmmnZaakafzenderParameters, zaaktypeCmmnZaakafzenderParameters);
    }

    private <T extends UserModifiable<T>> void setComponent(Collection<T> targetCollection, T candidate) {
        var existingElement = elementToChange(targetCollection, candidate);
        if (existingElement != null) {
            existingElement.applyChanges(candidate);
        } else {
            if (isElementNotInCollection(targetCollection, candidate)) {
                targetCollection.add(candidate.resetId());
            }
        }
    }

    /**
     * This method replaces the Hibernate's PersistentSet#contains that does not use overridden <code>equals</code>
     * and <code>hashCode</code>.
     *
     * @param targetCollection Collection that should be checked for the existence of the candidate element.
     * @param candidate        Candidate element to be added to the collection.
     * @return <code>true</code> if the element is not in the collection, <code>false</code> otherwise.
     *
     * @see <a href=https://hibernate.atlassian.net/browse/HHH-3799>Hibernate issue</a>
     *
     */
    private <T> boolean isElementNotInCollection(Collection<T> targetCollection, T candidate) {
        return targetCollection.stream().noneMatch(targetElement -> targetElement.equals(candidate));
    }

    private <T extends UserModifiable<T>> T elementToChange(Collection<T> persistentCollection, T changeCandidate) {
        return persistentCollection.stream()
                .filter(targetElement -> targetElement.isModifiedFrom(changeCandidate))
                .findFirst()
                .orElse(null);
    }

    public String getZaaktypeOmschrijving() {
        return zaaktypeOmschrijving;
    }

    public void setZaaktypeOmschrijving(final String zaaktypeOmschrijving) {
        this.zaaktypeOmschrijving = zaaktypeOmschrijving;
    }

    public ZonedDateTime getCreatiedatum() {
        return creatiedatum;
    }

    public void setCreatiedatum(final ZonedDateTime creatiedatum) {
        this.creatiedatum = creatiedatum;
    }

    public String getIntakeMail() {
        return intakeMail;
    }

    public void setIntakeMail(final String intakeMail) {
        this.intakeMail = intakeMail;
    }

    public String getAfrondenMail() {
        return afrondenMail;
    }

    public void setAfrondenMail(final String afrondenMail) {
        this.afrondenMail = afrondenMail;
    }

    public String getProductaanvraagtype() {
        return productaanvraagtype;
    }

    public void setProductaanvraagtype(final String productaanvraagtype) {
        this.productaanvraagtype = productaanvraagtype;
    }

    public String getDomein() {
        return domein;
    }

    public void setDomein(final String domein) {
        this.domein = domein;
    }

    public boolean isSmartDocumentsIngeschakeld() {
        return smartDocumentsIngeschakeld;
    }

    public void setSmartDocumentsIngeschakeld(boolean smartDocumentsIngeschakeld) {
        this.smartDocumentsIngeschakeld = smartDocumentsIngeschakeld;
    }

    public ZaaktypeCmmnBetrokkeneParameters getBetrokkeneParameters() {
        return zaaktypeCmmnBetrokkeneParameters != null ? zaaktypeCmmnBetrokkeneParameters : new ZaaktypeCmmnBetrokkeneParameters();
    }

    public void setBetrokkeneParameters(ZaaktypeCmmnBetrokkeneParameters zaaktypeCmmnBetrokkeneParameters) {
        this.zaaktypeCmmnBetrokkeneParameters = zaaktypeCmmnBetrokkeneParameters;
    }

    public ZaaktypeCmmnBrpParameters getBrpDoelbindingen() {
        return zaaktypeCmmnBrpParameters != null ? zaaktypeCmmnBrpParameters : new ZaaktypeCmmnBrpParameters();
    }

    public void setBrpDoelbindingen(ZaaktypeCmmnBrpParameters zaaktypeCmmnBrpParameters) {
        this.zaaktypeCmmnBrpParameters = zaaktypeCmmnBrpParameters;
    }

    /**
     * Geeft aan dat er voldoende gegevens zijn ingevuld om een zaak te starten
     *
     * @return true indien er een zaak kan worden gestart
     */
    public boolean isValide() {
        return StringUtils.isNotBlank(groepID) &&
               StringUtils.isNotBlank(caseDefinitionID) &&
               nietOntvankelijkResultaattype != null;
    }


    public ZaaktypeCmmnCompletionParameters readZaakbeeindigParameter(final Long zaakbeeindigRedenId) {
        return getZaakbeeindigParameters().stream()
                .filter(zaaktypeCmmnCompletionParameters -> zaaktypeCmmnCompletionParameters.getZaakbeeindigReden().getId().equals(
                        zaakbeeindigRedenId))
                .findAny().orElseThrow(() -> new RuntimeException(
                        String.format("No ZaakbeeindigParameter found for zaaktypeUUID: '%s' and zaakbeeindigRedenId: '%d'", zaakTypeUUID,
                                zaakbeeindigRedenId)));
    }


    public ZaaktypeCmmnUsereventlistenerParameters readUserEventListenerParameters(final String planitemDefinitionID) {
        return getUserEventListenerParametersCollection().stream()
                .filter(zaaktypeCmmnUsereventlistenerParameters -> zaaktypeCmmnUsereventlistenerParameters.getPlanItemDefinitionID().equals(
                        planitemDefinitionID))
                .findAny().orElseThrow(() -> new RuntimeException(
                        String.format("No UserEventListenerParameters found for zaaktypeUUID: '%s' and planitemDefinitionID: '%s'",
                                zaakTypeUUID,
                                planitemDefinitionID)));
    }

    public Optional<ZaaktypeCmmnHumantaskParameters> findHumanTaskParameter(final String planitemDefinitionID) {
        return getHumanTaskParametersCollection().stream()
                .filter(humanTaskParameter -> humanTaskParameter.getPlanItemDefinitionID().equals(planitemDefinitionID))
                .findAny();
    }
}
