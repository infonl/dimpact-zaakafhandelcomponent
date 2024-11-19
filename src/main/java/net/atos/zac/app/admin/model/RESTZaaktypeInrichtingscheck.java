/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model;

import java.util.List;

/**
 * 5 statustype; Intake, In behandeling, Heropend, Wacht op aanvullende informatie,
 * Afgerond: met Afgerond als laatste statustypevolgnummer
 * <br/>
 * min 1 resultaattype
 * <br/>
 * Roltypen, omschrijving generiek: initiator en behandelaar. 1 overig roltype
 * <br/>
 * Informatieobjecttype: e-mail
 * <br/>
 * indien zaak besluit heeft, Besluittype
 */
public class RESTZaaktypeInrichtingscheck {

    public RESTZaaktypeOverzicht zaaktype;

    public boolean statustypeIntakeAanwezig;

    public boolean statustypeInBehandelingAanwezig;

    public boolean statustypeHeropendAanwezig;

    public boolean statustypeAanvullendeInformatieVereist;

    public boolean statustypeAfgerondAanwezig;

    public boolean statustypeAfgerondLaatsteVolgnummer;

    public boolean resultaattypeAanwezig;

    public boolean rolInitiatorAanwezig;

    public boolean rolBehandelaarAanwezig;

    public boolean rolOverigeAanwezig;

    public boolean informatieobjecttypeEmailAanwezig;

    public boolean besluittypeAanwezig;

    public List<String> resultaattypesMetVerplichtBesluit;

    public boolean zaakafhandelParametersValide;

    public boolean valide;
}
