/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgClass, NgFor, NgIf } from "@angular/common";
import {
  Component,
  effect,
  inject,
  OnInit,
  signal,
  ViewChild,
} from "@angular/core";
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
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { finalize, Observable } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { runMutation } from "../../shared/http/run-mutation";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { SignaleringenSettingsBeheerService } from "../signaleringen-settings-beheer.service";

@Component({
  templateUrl: "./groep-signaleringen.component.html",
  styleUrls: ["./groep-signaleringen.component.less"],
  standalone: true,
  imports: [
    I18nKeyPipe,
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

  protected groepen!: Observable<GeneratedType<"RestGroup">[]>;
  protected readonly groepId = signal<string | undefined>(undefined);
  protected columns: string[] = ["subjecttype", "type", "dashboard", "mail"];
  protected readonly settingPerColumn: Record<
    string,
    "isDashboardEnabled" | "isMailEnabled"
  > = {
    dashboard: "isDashboardEnabled",
    mail: "isMailEnabled",
  };
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestSignaleringInstellingen">
  >();

  private readonly queryClient = inject(QueryClient);

  protected readonly instellingenQuery = injectQuery(() => ({
    ...this.service.list(this.groepId()!),
    enabled: this.groepId() != null,
  }));

  constructor(
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private identityService: IdentityService,
    private service: SignaleringenSettingsBeheerService,
  ) {
    super(utilService, configuratieService);

    effect(() => {
      this.dataSource.data = this.instellingenQuery.data() ?? [];
    });
  }

  ngOnInit(): void {
    this.setupMenu("title.signaleringen.settings.groep");
    this.groepen = this.identityService.listGroups();
  }

  protected laadSignaleringSettings(groep: GeneratedType<"RestGroup">): void {
    this.groepId.set(groep.id);
  }

  protected changed(
    row: GeneratedType<"RestSignaleringInstellingen">,
    column: string,
    checked: boolean,
  ): void {
    const groepId = this.groepId();
    if (!groepId) return;
    this.utilService.setLoading(true);
    row[this.settingPerColumn[column]] = checked;
    runMutation(this.queryClient, this.service.put(groepId), row)
      .pipe(finalize(() => this.utilService.setLoading(false)))
      .subscribe({ error: () => undefined });
  }
}
