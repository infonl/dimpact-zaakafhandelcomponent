/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit, ViewChild } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatListModule } from "@angular/material/list";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { ReferentieTabelService } from "../referentie-tabel.service";

/**
 * Experimental accordion-based reference table administration, offered next to
 * the existing screens so the two can be compared side by side. Each table is a
 * collapsible panel; its values are lazily loaded when the panel is expanded.
 */
@Component({
  templateUrl: "./referentie-tabellen-v2.component.html",
  styleUrls: ["./referentie-tabellen-v2.component.less"],
  standalone: true,
  imports: [
    MatSidenavModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    TranslateModule,
    EmptyPipe,
    SideNavComponent,
  ],
})
export class ReferentieTabellenV2Component
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer")
  protected sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") protected menuSidenav!: MatSidenav;

  protected tabellen: GeneratedType<"RestReferenceTable">[] = [];
  protected loading = false;

  private readonly loadedTabellen = new Map<
    number,
    GeneratedType<"RestReferenceTable">
  >();
  protected loadingTabelId: number | null = null;

  constructor(
    public readonly utilService: UtilService,
    public readonly configuratieService: ConfiguratieService,
    private readonly service: ReferentieTabelService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.setupMenu("title.referentietabellen.v2");
    this.laadReferentieTabellen();
  }

  protected laadReferentieTabellen() {
    this.loading = true;
    this.service.listReferentieTabellen().subscribe((tabellen) => {
      this.tabellen = tabellen;
      this.loading = false;
    });
  }

  protected onPanelOpened(tabel: GeneratedType<"RestReferenceTable">) {
    if (tabel.id == null || this.loadedTabellen.has(tabel.id)) {
      return;
    }
    this.loadingTabelId = tabel.id;
    this.service.readReferentieTabel(tabel.id).subscribe((geladenTabel) => {
      this.loadedTabellen.set(tabel.id!, geladenTabel);
      this.loadingTabelId = null;
    });
  }

  protected isLoadingWaarden(tabel: GeneratedType<"RestReferenceTable">) {
    return tabel.id != null && this.loadingTabelId === tabel.id;
  }

  protected getWaarden(tabel: GeneratedType<"RestReferenceTable">) {
    return (
      (tabel.id != null && this.loadedTabellen.get(tabel.id)?.waarden) || []
    );
  }
}
