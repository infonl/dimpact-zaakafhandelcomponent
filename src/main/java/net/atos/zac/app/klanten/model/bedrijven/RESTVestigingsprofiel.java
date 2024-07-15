/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klanten.model.bedrijven;

import java.util.List;

public class RESTVestigingsprofiel {

    public String vestigingsnummer;

    public String kvkNummer;

    public String eersteHandelsnaam;

    public String rsin;

    public String type;

    public int totaalWerkzamePersonen;

    public int deeltijdWerkzamePersonen;

    public int voltijdWerkzamePersonen;

    public List<RestKlantenAdres> adressen;

    public String website;

    public String sbiHoofdActiviteit;

    public List<String> sbiActiviteiten;

    public boolean commercieleVestiging;
}
