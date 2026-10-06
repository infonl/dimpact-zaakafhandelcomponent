/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass, NgFor, NgIf } from "@angular/common";
import { AfterViewInit, Component, OnInit } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { injectMutation } from "../../shared/http/inject-mutation";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenSettingsService } from "../signaleringen-settings.service";

const SETTING_PER_COLUMN = {
  dashboard: "isDashboardEnabled",
  mail: "isMailEnabled",
} as const;

@Component({
  templateUrl: "./signaleringen-settings.component.html",
  styleUrls: ["./signaleringen-settings.component.less"],
  standalone: true,
  imports: [
    I18nKeyPipe,
    NgClass,
    NgFor,
    NgIf,
    MatCardModule,
    MatTableModule,
    MatCheckboxModule,
    TranslateModule,
  ],
})
export class SignaleringenSettingsComponent implements OnInit, AfterViewInit {
  protected isLoadingResults = true;
  protected readonly columns = [
    "subjecttype",
    "type",
    "dashboard",
    "mail",
  ] as const;
  protected readonly settingPerColumn = SETTING_PER_COLUMN;
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestSignaleringInstellingen">
  >();

  private readonly putMutation = injectMutation(() => this.service.put(), {
    onSettled: () => this.utilService.setLoading(false),
  });

  constructor(
    private readonly service: SignaleringenSettingsService,
    private readonly utilService: UtilService,
  ) {}

  ngOnInit() {
    this.utilService.setTitle("title.signaleringen.settings");
  }

  ngAfterViewInit() {
    this.service.list().subscribe((instellingen) => {
      this.dataSource.data = instellingen;
      this.isLoadingResults = false;
    });
  }

  protected changed(
    row: GeneratedType<"RestSignaleringInstellingen">,
    column: keyof typeof SETTING_PER_COLUMN,
    checked: boolean,
  ) {
    this.utilService.setLoading(true);
    row[this.settingPerColumn[column]] = checked;
    this.putMutation.mutate(row);
  }
}
