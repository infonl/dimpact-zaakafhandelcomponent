/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klanten.model.bedrijven;

import java.util.List;

public class RESTVestigingsprofiel {

    public String vestigingsnummer;

    public String kvkNummer;

    public String handelsnaam;

    public String rsin;

    public String type;

    public int totaalWerkzamePersonen;

    public int deeltijdWerkzamePersonen;

    public int voltijdWerkzamePersonen;

    public List<RESTKlantenAdres> adressen;

    public String website;

    public String sbiHoofdActiviteit;

    public List<String> sbiActiviteiten;

    public boolean commercieleVestiging;
}
