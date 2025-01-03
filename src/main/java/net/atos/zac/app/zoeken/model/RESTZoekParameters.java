/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zoeken.model;

import java.util.Map;

import net.atos.zac.app.shared.RestPageParameters;
import net.atos.zac.zoeken.model.DatumVeld;
import net.atos.zac.zoeken.model.FilterParameters;
import net.atos.zac.zoeken.model.FilterVeld;
import net.atos.zac.zoeken.model.SorteerVeld;
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType;

public class RESTZoekParameters extends RestPageParameters {

    public ZoekObjectType type;

    public Map<String, String> zoeken;

    public Map<FilterVeld, FilterParameters> filters;

    public Map<DatumVeld, RESTDatumRange> datums;

    public SorteerVeld sorteerVeld;

    public String sorteerRichting;

    public boolean alleenMijnZaken;

    public boolean alleenOpenstaandeZaken;

    public boolean alleenAfgeslotenZaken;

    public boolean alleenMijnTaken;
}
