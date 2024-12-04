/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZaakStatusmailOptie } from "../../zaken/model/zaak-statusmail-optie";
import { Zaaktype } from "../../zaken/model/zaaktype";
import { CaseDefinition } from "./case-definition";
import { HumanTaskParameter } from "./human-task-parameter";
import { MailtemplateKoppeling } from "./mailtemplate-koppeling";
import { UserEventListenerParameter } from "./user-event-listener-parameter";
import { ZaakAfzender } from "./zaakafzender";
import { ZaakbeeindigParameter } from "./zaakbeeindig-parameter";

export class ZaakafhandelParameters {
  zaaktype: Zaaktype;
  caseDefinition: CaseDefinition;
  domein: string;
  defaultBehandelaarId: string;
  defaultGroepId: string;
  creatiedatum: string;
  einddatumGeplandWaarschuwing: number;
  uiterlijkeEinddatumAfdoeningWaarschuwing: number;
  zaakNietOntvankelijkResultaattype: GeneratedType<'RestResultaattype'>;
  humanTaskParameters: HumanTaskParameter[];
  userEventListenerParameters: UserEventListenerParameter[];
  mailtemplateKoppelingen: MailtemplateKoppeling[];
  zaakbeeindigParameters: ZaakbeeindigParameter[];
  zaakAfzenders: ZaakAfzender[];
  intakeMail: ZaakStatusmailOptie;
  afrondenMail: ZaakStatusmailOptie;
  productaanvraagtype: string;
  valide: boolean;
  smartDocuments: GeneratedType<"RestSmartDocuments">;
}
