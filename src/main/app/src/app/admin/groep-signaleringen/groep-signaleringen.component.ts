/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgClass, NgFor, NgIf } from "@angular/common";
import { Component, OnInit, ViewChild } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { TranslateModule } from "@ngx-translate/core";
import { Observable, finalize } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { SignaleringenSettingsBeheerService } from "../signaleringen-settings-beheer.service";

@Component({
  templateUrl: "./groep-signaleringen.component.html",
  styleUrls: ["./groep-signaleringen.component.less"],
  standalone: true,
  imports: [
    AsyncPipe,
    NgClass,
    NgFor,
    NgIf,
    MatSidenavModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTableModule,
    MatCheckboxModule,
    SideNavComponent,
    TranslateModule,
  ],
})
export class GroepSignaleringenComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer")
  protected sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") protected menuSidenav!: MatSidenav;

  protected isLoadingResults = false;
  protected groepen!: Observable<GeneratedType<"RestGroup">[]>;
  protected groepId: string | undefined;
  protected columns: string[] = ["subjecttype", "type", "dashboard", "mail"];
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestSignaleringInstellingen">
  >();

  constructor(
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private identityService: IdentityService,
    private service: SignaleringenSettingsBeheerService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit(): void {
    this.setupMenu("title.signaleringen.settings.groep");
    this.groepen = this.identityService.listGroups();
  }

  protected laadSignaleringSettings(groep: GeneratedType<"RestGroup">): void {
    this.isLoadingResults = true;
    this.service.list(groep.id).subscribe((instellingen) => {
      this.dataSource.data = instellingen;
      this.groepId = groep.id;
      this.isLoadingResults = false;
    });
  }

  protected changed(
    row: GeneratedType<"RestSignaleringInstellingen">,
    column: string,
    checked: boolean,
  ): void {
    if (!this.groepId) return;
    this.utilService.setLoading(true);
    (row as Record<string, unknown>)[column] = checked;
    this.service
      .put(this.groepId, row)
      .pipe(finalize(() => this.utilService.setLoading(false)))
      .subscribe();
  }
}
