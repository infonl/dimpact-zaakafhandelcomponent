/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { RouterStateSnapshot, TitleStrategy } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { breadcrumbs } from "./route-title";

@Injectable({ providedIn: "root" })
export class ZacTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly translate = inject(TranslateService);

  override updateTitle(snapshot: RouterStateSnapshot) {
    const titles = breadcrumbs(snapshot.root).map(({ title }) => title);
    this.title.setTitle(
      titles.length
        ? this.translate.instant("title", { title: titles.join(" | ") })
        : "Zaakafhandelcomponent",
    );
  }
}
