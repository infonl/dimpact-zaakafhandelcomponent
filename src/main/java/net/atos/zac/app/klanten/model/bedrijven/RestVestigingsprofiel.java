/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klanten.model.bedrijven;

import java.util.List;

public class RestVestigingsprofiel {

    public List<RestKlantenAdres> adressen;

    public boolean commercieleVestiging;

    public int deeltijdWerkzamePersonen;

    public String eersteHandelsnaam;

    public String kvkNummer;

    public String rsin;

    public List<String> sbiActiviteiten;

    public String sbiHoofdActiviteit;

    public String type;

    public int totaalWerkzamePersonen;

    public String vestigingsnummer;

    public int voltijdWerkzamePersonen;

    public String website;
}
