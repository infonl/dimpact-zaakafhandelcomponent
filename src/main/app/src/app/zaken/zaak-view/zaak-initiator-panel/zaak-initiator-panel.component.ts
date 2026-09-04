/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, input, output } from "@angular/core";
import { BedrijfsgegevensComponent } from "../../../klanten/bedrijfsgegevens/bedrijfsgegevens.component";
import { ContactgegevensComponent } from "../../../klanten/contactgegevens/contactgegevens.component";
import { PersoonsgegevensComponent } from "../../../klanten/persoonsgegevens/persoonsgegevens.component";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZaakInitiatorToevoegenComponent } from "../../zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
import {
  allowedToAddBetrokkene,
  initiatorViewType,
  showInitiator,
} from "../utils/zaak-view.predicates";

@Component({
  selector: "zac-zaak-initiator-panel",
  templateUrl: "./zaak-initiator-panel.component.html",
  standalone: true,
  imports: [
    ZaakInitiatorToevoegenComponent,
    ContactgegevensComponent,
    PersoonsgegevensComponent,
    BedrijfsgegevensComponent,
  ],
})
export class ZaakInitiatorPanelComponent {
  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly hasBrpSearchRight = input(false);

  readonly addOrEdit = output<void>();
  readonly delete = output<void>();

  protected readonly isVisible = computed(() => showInitiator(this.zaak()));

  protected readonly viewType = computed(() => initiatorViewType(this.zaak()));

  protected readonly mayAddBetrokkene = computed(() =>
    allowedToAddBetrokkene(this.zaak(), this.hasBrpSearchRight()),
  );
}
