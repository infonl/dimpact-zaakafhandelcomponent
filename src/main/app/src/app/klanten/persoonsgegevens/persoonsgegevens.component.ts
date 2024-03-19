/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output, input } from "@angular/core";
import { toObservable } from "@angular/core/rxjs-interop";
import { switchMap } from "rxjs";
import { KlantenService } from "../klanten.service";
import { Persoon } from "../model/personen/persoon";

@Component({
  selector: "zac-persoongegevens",
  styleUrls: ["./persoonsgegevens.component.less"],
  templateUrl: "./persoonsgegevens.component.html",
})
export class PersoonsgegevensComponent {
  @Output() delete = new EventEmitter<Persoon>();
  @Output() edit = new EventEmitter<Persoon>();

  isVerwijderbaar = input<boolean>();
  isWijzigbaar = input<boolean>();
  bsn = input<string>();

  bsn$ = toObservable(this.bsn);

  persoon$ = this.bsn$.pipe(
    switchMap((bsn) => this.klantenService.readPersoon(bsn)),
  );

  constructor(private klantenService: KlantenService) {}
}
