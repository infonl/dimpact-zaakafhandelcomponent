/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model;

import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

@MappedSuperclass
public class ZaakafhandelParametersBase {

    /** Naam van property: {@link ZaakafhandelParametersBase#zaakTypeUUID} */
    public static final String ZAAKTYPE_UUID = "zaakTypeUUID";

    /** Naam van property: {@link ZaakafhandelParametersBase#zaaktypeOmschrijving} */
    public static final String ZAAKTYPE_OMSCHRIJVING = "zaaktypeOmschrijving";

    /** Naam van property: {@link ZaakafhandelParametersBase#creatiedatum} */
    public static final String CREATIEDATUM = "creatiedatum";

    /** Naam van property: {@link ZaakafhandelParametersBase#productaanvraagtype} */
    public static final String PRODUCTAANVRAAGTYYPE = "productaanvraagtype";

    @Id
    @GeneratedValue(generator = "sq_zaakafhandelparameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_zaakafhandelparameters")
    private Long id;

    @NotNull @Column(name = "uuid_zaaktype", nullable = false)
    private UUID zaakTypeUUID;

    @NotBlank
    @Column(name = "zaaktype_omschrijving", nullable = false)
    private String zaaktypeOmschrijving;

    @Column(name = "id_case_definition", nullable = false)
    private String caseDefinitionID;

    @Column(name = "id_groep", nullable = false)
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

    @Column(name = "intake_mail", nullable = false)
    private String intakeMail;

    @Column(name = "afronden_mail", nullable = false)
    private String afrondenMail;

    @Column(name = "productaanvraagtype")
    private String productaanvraagtype;

    @Column(name = "domein")
    private String domein;

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
}
