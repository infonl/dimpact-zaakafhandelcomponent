/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaken.model;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import net.atos.zac.app.identity.model.RESTGroup;
import net.atos.zac.app.identity.model.RESTUser;
import net.atos.zac.app.klanten.model.klant.IdentificatieType;
import net.atos.zac.app.policy.model.RESTZaakRechten;
import net.atos.zac.zoeken.model.ZaakIndicatie;


public class RESTZaak {

    public UUID uuid;

    public String identificatie;

    @NotNull()
    public String omschrijving;

    public String toelichting;

    @NotNull() @Valid
    public RESTZaaktype zaaktype;

    public RESTZaakStatus status;

    public RESTZaakResultaat resultaat;

    public List<RESTBesluit> besluiten;

    public String bronorganisatie;

    public String verantwoordelijkeOrganisatie;

    public LocalDate registratiedatum;

    @NotNull()
    public LocalDate startdatum;

    public LocalDate einddatumGepland;

    public LocalDate einddatum;

    public LocalDate uiterlijkeEinddatumAfdoening;

    public LocalDate publicatiedatum;


    public LocalDate archiefActiedatum;

    public String archiefNominatie;

    @NotNull()
    public RESTCommunicatiekanaal communicatiekanaal;

    @NotNull()
    public String vertrouwelijkheidaanduiding;

    public RESTGeometry zaakgeometrie;

    public boolean isOpgeschort;

    public String redenOpschorting;

    public boolean isVerlengd;

    public String redenVerlenging;

    public String duurVerlenging;

    @Valid
    @Nullable
    public RESTGroup groep;

    public RESTUser behandelaar;

    public List<RESTGerelateerdeZaak> gerelateerdeZaken;

    public List<RESTZaakKenmerk> kenmerken;

    public List<RESTZaakEigenschap> eigenschappen;

    public Map<String, Object> zaakdata;

    public IdentificatieType initiatorIdentificatieType;

    public String initiatorIdentificatie;

    public boolean isOpen;

    public boolean isHeropend;

    public boolean isHoofdzaak;

    public boolean isDeelzaak;

    public boolean isOntvangstbevestigingVerstuurd;

    public boolean isBesluittypeAanwezig;

    public boolean isInIntakeFase;

    public boolean isProcesGestuurd;

    public RESTZaakRechten rechten;

    public EnumSet<ZaakIndicatie> getIndicaties() {
        final EnumSet<ZaakIndicatie> indicaties = EnumSet.noneOf(ZaakIndicatie.class);
        if (isHoofdzaak) {
            indicaties.add(ZaakIndicatie.HOOFDZAAK);
        }
        if (isDeelzaak) {
            indicaties.add(ZaakIndicatie.DEELZAAK);
        }
        if (isHeropend) {
            indicaties.add(ZaakIndicatie.HEROPEND);
        }
        if (isOpgeschort) {
            indicaties.add(ZaakIndicatie.OPSCHORTING);
        }
        if (isVerlengd) {
            indicaties.add(ZaakIndicatie.VERLENGD);
        }
        return indicaties;
    }
}
