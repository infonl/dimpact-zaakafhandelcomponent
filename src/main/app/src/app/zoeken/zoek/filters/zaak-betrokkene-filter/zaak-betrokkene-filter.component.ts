/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  input,
  Input,
  OnInit,
  Output,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZoekVeld } from "../../../model/zoek-veld";
import { KlantZoekDialog } from "./klant-zoek-dialog.component";

@Component({
  selector: "zac-zaak-betrokkene-filter",
  templateUrl: "./zaak-betrokkene-filter.component.html",
  styleUrls: ["./zaak-betrokkene-filter.component.less"],
})
export class ZaakBetrokkeneFilterComponent implements OnInit {
  @Input() zoekparameters!: GeneratedType<"RestZoekParameters">;
  @Output() changed = new EventEmitter<string>();
  dialogOpen: boolean = false;
  betrokkeneSelectControl = new FormControl<ZoekVeld>(ZoekVeld.ZAAK_INITIATOR);
  klantIdControl = new FormControl("");
  huidigeRoltype!: ZoekVeld;
  ZoekVeld = ZoekVeld;

  context = input.required<string>();

  constructor(public dialog: MatDialog) {}

  ngOnInit(): void {
    this.bepaalHuidigRoltype();
    if (this.zoekparameters?.zoeken && this.huidigeRoltype != null) {
      this.klantIdControl.setValue(
        this.zoekparameters.zoeken[this.huidigeRoltype] ?? ""
      );
    }
  }

  openDialog(): void {
    this.dialogOpen = true;
    const dialogRef = this.dialog.open(KlantZoekDialog, {
      minWidth: "750px",
      backdropClass: "noColor",
      data: { context: this.context() },
    });

    dialogRef.afterClosed().subscribe((result) => {
      this.dialogOpen = false;

      if (
        result &&
        this.zoekparameters?.zoeken &&
        this.huidigeRoltype != null
      ) {
        this.klantIdControl.setValue(result.identificatie ?? "");
        this.zoekparameters.zoeken[this.huidigeRoltype] =
          result.identificatie ?? "";
      }

      this.changed.emit();
    });
  }

  idChanged(): void {
    if (!this.zoekparameters?.zoeken || this.huidigeRoltype == null) return;

    const huidigId = this.zoekparameters.zoeken[this.huidigeRoltype];
    const nieuwId = this.klantIdControl.value ?? "";

    if (huidigId !== nieuwId) {
      this.zoekparameters.zoeken[this.huidigeRoltype] = nieuwId;
      this.changed.emit();
    }
  }

  roltypeChanged(): void {
    if (!this.zoekparameters?.zoeken) return;

    const id = this.zoekparameters.zoeken[this.huidigeRoltype];

    // Delete old key safely
    if (this.huidigeRoltype != null) {
      delete this.zoekparameters.zoeken[this.huidigeRoltype];
    }

    // Get new roltype safely, fallback to current if null
    const newRoltype =
      this.betrokkeneSelectControl.value ?? this.huidigeRoltype!;
    this.huidigeRoltype = newRoltype;

    // Reassign old value
    if (newRoltype != null) {
      this.zoekparameters.zoeken[newRoltype] = id ?? "";
    }

    if (id) {
      this.changed.emit();
    }
  }

  bepaalHuidigRoltype() {
    this.huidigeRoltype = ZoekVeld.ZAAK_INITIATOR;
    if (!this.zoekparameters?.zoeken) return;

    if (this.zoekparameters.zoeken.ZAAK_BETROKKENEN) {
      this.huidigeRoltype = ZoekVeld.ZAAK_BETROKKENEN;
    } else if (this.zoekparameters.zoeken.ZAAK_INITIATOR) {
      this.huidigeRoltype = ZoekVeld.ZAAK_INITIATOR;
    } else if (this.zoekparameters.zoeken.ZAAK_BETROKKENE_BELANGHEBBENDE) {
      this.huidigeRoltype = ZoekVeld.ZAAK_BETROKKENE_BELANGHEBBENDE;
    } else if (this.zoekparameters.zoeken.ZAAK_BETROKKENE_ADVISEUR) {
      this.huidigeRoltype = ZoekVeld.ZAAK_BETROKKENE_ADVISEUR;
    } else if (this.zoekparameters.zoeken.ZAAK_BETROKKENE_BESLISSER) {
      this.huidigeRoltype = ZoekVeld.ZAAK_BETROKKENE_BESLISSER;
    } else if (this.zoekparameters.zoeken.ZAAK_BETROKKENE_ZAAKCOORDINATOR) {
      this.huidigeRoltype = ZoekVeld.ZAAK_BETROKKENE_ZAAKCOORDINATOR;
    } else if (this.zoekparameters.zoeken.ZAAK_BETROKKENE_MEDE_INITIATOR) {
      this.huidigeRoltype = ZoekVeld.ZAAK_BETROKKENE_MEDE_INITIATOR;
    }
  }
}
