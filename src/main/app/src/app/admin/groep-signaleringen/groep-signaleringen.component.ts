/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit, ViewChild } from "@angular/core";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { Observable } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { Group } from "../../identity/model/group";
import { SignaleringSettings } from "../../signaleringen/model/signalering-settings";
import { AdminComponent } from "../admin/admin.component";
import { SignaleringenSettingsBeheerService } from "../signaleringen-settings-beheer.service";

@Component({
  templateUrl: "./groep-signaleringen.component.html",
  styleUrls: ["./groep-signaleringen.component.less"],
})
export class GroepSignaleringenComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer") sideNavContainer: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav: MatSidenav;

  isLoadingResults = false;
  groepen: Observable<Group[]>;
  groepId: string;
  columns: string[] = ["subjecttype", "type", "dashboard", "mail"];
  dataSource: MatTableDataSource<SignaleringSettings> =
    new MatTableDataSource<SignaleringSettings>();

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

  laadSignaleringSettings(groep: Group): void {
    this.isLoadingResults = true;
    this.service.list(groep.id).subscribe((instellingen) => {
      this.dataSource.data = instellingen;
      this.groepId = groep.id;
      this.isLoadingResults = false;
    });
  }

  changed(row: SignaleringSettings, column: string, checked: boolean): void {
    this.utilService.setLoading(true);
    row[column] = checked;
    this.service.put(this.groepId, row).subscribe(() => {
      this.utilService.setLoading(false);
    });
  }
}
