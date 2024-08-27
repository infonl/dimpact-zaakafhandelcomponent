/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { EnkelvoudigInformatieobject } from "../../informatie-objecten/model/enkelvoudig-informatieobject";
import { Besluittype } from "./besluittype";
import { VervalReden } from "./vervalReden";

export class Besluit {
  url: string;
  uuid: string;
  identificatie: string;
  toelichting: string;
  datum: string;
  ingangsdatum: string;
  vervaldatum: string;
  vervalreden: VervalReden;
  besluittype: Besluittype;
  isIngetrokken: boolean;
  informatieobjecten: EnkelvoudigInformatieobject[];
}
