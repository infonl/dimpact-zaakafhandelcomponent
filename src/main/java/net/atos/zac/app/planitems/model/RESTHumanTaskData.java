/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.planitems.model;

import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import net.atos.zac.app.identity.model.RestGroup;
import net.atos.zac.app.identity.model.RestUser;

public class RESTHumanTaskData {

    public String planItemInstanceId;

    public RestGroup groep;

    @Valid
    public RestUser medewerker;

    /**
     * The 'final due date' of a task.
     * Note that this fatal date cannot come after the fatal date of the zaak to which this task belongs.
     */
    public LocalDate fataledatum;

    public String toelichting;

    public Map<String, String> taakdata;

    @NotNull
    public RESTTaakStuurGegevens taakStuurGegevens;
}
