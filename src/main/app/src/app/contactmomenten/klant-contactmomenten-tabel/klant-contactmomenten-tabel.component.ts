/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  AfterViewInit,
  Component,
  Input,
  OnChanges,
  OnInit,
  ViewChild,
} from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatPaginator, MatPaginatorModule } from "@angular/material/paginator";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { TranslateModule } from "@ngx-translate/core";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { PutBody } from "../../shared/http/http-client";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ContactmomentenService } from "../contactmomenten.service";

@Component({
  selector: "zac-klant-contactmomenten-tabel",
  templateUrl: "./klant-contactmomenten-tabel.component.html",
  styleUrls: ["./klant-contactmomenten-tabel.component.less"],
  standalone: true,
  imports: [
    NgIf,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    TranslateModule,
    DatumPipe,
    EmptyPipe,
  ],
})
export class KlantContactmomentenTabelComponent
  implements OnInit, AfterViewInit, OnChanges
{
  @Input() bsn?: string;
  @Input() vestigingsnummer?: string;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestContactmoment">
  >();
  protected columns: string[] = [
    "registratiedatum",
    "kanaal",
    "initiatiefnemer",
    "medewerker",
    "tekst",
  ];
  private listParameters: PutBody<"/rest/klanten/contactmomenten"> = {
    page: 0,
  };
  private init = false;
  protected isLoadingResults = true;

  constructor(
    private readonly contactmomentenService: ContactmomentenService,
    private readonly utilService: UtilService,
  ) {}

  ngOnInit(): void {
    this.listParameters.bsn = this.bsn;
    this.listParameters.vestigingsnummer = this.vestigingsnummer;
  }

  ngAfterViewInit(): void {
    this.init = true;
    this.paginator.page
      .pipe(
        startWith({}),
        switchMap(() => {
          this.isLoadingResults = true;
          this.utilService.setLoading(true);
          return this.loadContactmomenten();
        }),
        map((resultaat) => {
          this.isLoadingResults = false;
          this.utilService.setLoading(false);
          return resultaat;
        }),
      )
      .subscribe((resultaat) => {
        this.paginator.length = resultaat.totaal ?? 0;
        this.dataSource.data = resultaat.resultaten ?? [];
      });
  }

  ngOnChanges(): void {
    if (this.init) {
      this.paginator.pageIndex = 0;
      this.paginator.page.emit();
    }
  }

  private loadContactmomenten() {
    this.listParameters.page = this.paginator.pageIndex;
    return this.contactmomentenService.listContactmomenten(this.listParameters);
  }
}
