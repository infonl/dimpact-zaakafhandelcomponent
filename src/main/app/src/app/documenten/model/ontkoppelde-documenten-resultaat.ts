/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { User } from "../../identity/model/user";
import { Resultaat } from "../../shared/model/resultaat";
import { OntkoppeldDocument } from "./ontkoppeld-document";

export class OntkoppeldeDocumentenResultaat extends Resultaat<OntkoppeldDocument> {
  filterOntkoppeldDoor: User[];
}
