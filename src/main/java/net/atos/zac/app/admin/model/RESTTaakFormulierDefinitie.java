/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model;

import java.util.List;

public record RESTTaakFormulierDefinitie(String id, List<RESTTaakFormulierVeldDefinitie> veldDefinities) {
}
