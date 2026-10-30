/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass } from "@angular/common";
import {
  Component,
  inject,
  input,
  linkedSignal,
  output,
  signal,
} from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { TranslatePipe } from "@ngx-translate/core";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZoekVeld } from "../../../model/zoek-veld";
import { KlantZoekDialog } from "./klant-zoek-dialog.component";

const ROLTYPEN = [
  { zoekVeld: ZoekVeld.ZAAK_BETROKKENEN, label: "betrokkene-roltype.alle" },
  { zoekVeld: ZoekVeld.ZAAK_INITIATOR, label: "betrokkene-roltype.initiator" },
  {
    zoekVeld: ZoekVeld.ZAAK_BETROKKENE_MEDE_INITIATOR,
    label: "betrokkene-roltype.mede-initiator",
  },
  {
    zoekVeld: ZoekVeld.ZAAK_BETROKKENE_BELANGHEBBENDE,
    label: "betrokkene-roltype.belanghebbende",
  },
  {
    zoekVeld: ZoekVeld.ZAAK_BETROKKENE_BESLISSER,
    label: "betrokkene-roltype.beslisser",
  },
  {
    zoekVeld: ZoekVeld.ZAAK_BETROKKENE_ADVISEUR,
    label: "betrokkene-roltype.adviseur",
  },
  {
    zoekVeld: ZoekVeld.ZAAK_BETROKKENE_KLANTCONTACTER,
    label: "betrokkene-roltype.klantcontacter",
  },
  {
    zoekVeld: ZoekVeld.ZAAK_BETROKKENE_ZAAKCOORDINATOR,
    label: "betrokkene-roltype.zaakcoordinator",
  },
] as const;

@Component({
  selector: "zac-zaak-betrokkene-filter",
  templateUrl: "./zaak-betrokkene-filter.component.html",
  styleUrls: ["./zaak-betrokkene-filter.component.less"],
  standalone: true,
  imports: [
    NgClass,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    TranslatePipe,
  ],
})
export class ZaakBetrokkeneFilterComponent {
  readonly zoekparameters =
    input.required<GeneratedType<"RestZoekParameters">>();
  readonly changed = output<void>();

  protected readonly roltypen = ROLTYPEN;
  protected readonly roltype = linkedSignal<ZoekVeld>(
    () =>
      ROLTYPEN.find(
        ({ zoekVeld }) => this.zoekparameters().zoeken?.[zoekVeld] != null,
      )?.zoekVeld ?? ZoekVeld.ZAAK_INITIATOR,
  );
  protected readonly klantId = linkedSignal(
    () => this.zoekparameters().zoeken?.[this.roltype()] ?? "",
  );
  protected readonly dialogOpen = signal(false);
  private readonly dialog = inject(MatDialog);

  protected openDialog(): void {
    this.dialogOpen.set(true);
    const dialogRef = this.dialog.open(KlantZoekDialog, {
      minWidth: "750px",
      backdropClass: "noColor",
    });

    dialogRef
      .afterClosed()
      .subscribe((result: GeneratedType<"RestBedrijf" | "RestPersoon">) => {
        this.dialogOpen.set(false);

        const zoeken = this.zoekparameters().zoeken;
        if (zoeken) {
          const identificatie =
            (result as GeneratedType<"RestBedrijf">).vestigingsnummer ??
            (result as GeneratedType<"RestBedrijf">).kvkNummer ??
            (result as GeneratedType<"RestPersoon">).bsn ??
            "";
          zoeken[this.roltype()] = identificatie;
          this.klantId.set(identificatie);
        }

        this.changed.emit();
      });
  }

  protected idChanged(): void {
    const zoeken = this.zoekparameters().zoeken;
    if (!zoeken || zoeken[this.roltype()] === this.klantId()) return;

    zoeken[this.roltype()] = this.klantId();
    this.changed.emit();
  }

  protected roltypeChanged(roltype: ZoekVeld): void {
    const zoeken = this.zoekparameters().zoeken;
    const id = zoeken?.[this.roltype()];
    if (zoeken) {
      delete zoeken[this.roltype()];
      zoeken[roltype] = id ?? "";
    }
    this.roltype.set(roltype);

    if (zoeken && id) {
      this.changed.emit();
    }
  }
}
