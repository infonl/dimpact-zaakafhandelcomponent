/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
} from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

@Component({
  selector: "zac-fout-afhandeling",
  templateUrl: "./fout-afhandeling.component.html",
  styleUrls: ["./fout-afhandeling.component.less"],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [MatCardModule, MatIconModule, TranslatePipe],
})
export class FoutAfhandelingComponent implements OnInit {
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly referentieTabelService = inject(ReferentieTabelService);

  protected bericht: string | null = null;
  protected foutmelding: string | null = null;
  protected readonly serverErrorTextsQuery = injectQuery(() => ({
    ...this.referentieTabelService.listServerErrorTextsQuery(),
    // A failure would open the error dialog, which loads these texts again.
    meta: { reportErrors: false },
    retry: false,
  }));

  ngOnInit() {
    this.foutmelding = this.foutAfhandelingService.foutmelding;
    this.bericht = this.foutAfhandelingService.bericht;
  }
}
