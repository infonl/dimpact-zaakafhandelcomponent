/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { Mailtemplate } from "./mailtemplate";

export class MailtemplateKoppeling {
  id: number;
  zaakafhandelParameters: GeneratedType<"RestZaakafhandelParameters">;
  mailtemplate: Mailtemplate;
}
