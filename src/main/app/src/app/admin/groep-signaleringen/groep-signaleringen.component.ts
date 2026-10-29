/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass } from "@angular/common";
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  OnInit,
  signal,
  viewChild,
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
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { finalize } from "rxjs";
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
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    I18nKeyPipe,
    NgClass,
    MatSidenavModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTableModule,
    MatCheckboxModule,
    SideNavComponent,
    TranslatePipe,
  ],
})
export class GroepSignaleringenComponent
  extends AdminComponent
  implements OnInit
{
  private readonly signaleringenSettingsBeheerService = inject(
    SignaleringenSettingsBeheerService,
  );

  protected readonly sideNavContainer =
    viewChild.required<MatSidenavContainer>("sideNavContainer");
  protected readonly menuSidenav =
    viewChild.required<MatSidenav>("menuSidenav");

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
  private readonly identityService = inject(IdentityService);

  protected readonly groepenQuery = injectQuery(() =>
    this.identityService.listGroups(),
  );

  protected readonly instellingenQuery = injectQuery(() => ({
    ...this.signaleringenSettingsBeheerService.list(this.groepId()!),
    enabled: this.groepId() != null,
  }));

  constructor() {
    super();

    effect(() => {
      this.dataSource.data = this.instellingenQuery.data() ?? [];
    });
  }

  ngOnInit(): void {
    this.setupMenu("title.signaleringen.settings.groep");
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
    runMutation(
      this.queryClient,
      this.signaleringenSettingsBeheerService.put(groepId),
      row,
    )
      .pipe(finalize(() => this.utilService.setLoading(false)))
      .subscribe({ error: () => undefined });
  }
}
