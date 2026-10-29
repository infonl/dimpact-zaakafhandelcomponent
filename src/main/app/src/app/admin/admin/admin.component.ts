/*
 * SPDX-FileCopyrightText: 2022 - 2024-2025 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { ViewComponent } from "../../shared/abstract-view/view-component";
import { HeaderMenuItem } from "../../shared/side-nav/menu-item/header-menu-item";
import { LinkMenuItem } from "../../shared/side-nav/menu-item/link-menu-item";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";

@Component({
  template: "",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
})
export abstract class AdminComponent extends ViewComponent {
  readonly utilService = inject(UtilService);
  readonly configuratieService = inject(ConfiguratieService);

  protected menu: MenuItem[] = [];
  protected activeMenu = "";

  protected setupMenu(title: string) {
    this.activeMenu = title;
    this.menu = [];
    this.menu.push(new HeaderMenuItem("actie.admin"));
    this.menu.push(
      this.getMenuLink(
        "title.signaleringen.settings.groep",
        "/admin/groepen",
        "notifications_active",
      ),
    );
    this.menu.push(
      this.getMenuLink(
        "title.referentietabellen",
        "/admin/referentietabellen",
        "schema",
      ),
    );
    this.menu.push(
      this.getMenuLink("title.mailtemplates", "/admin/mailtemplates", "mail"),
    );
    this.menu.push(
      this.getMenuLink("title.parameters", "/admin/parameters", "tune"),
    );
    this.menu.push(
      this.getMenuLink(
        "title.bpmn-procesdefinities",
        "/admin/bpmn-procesdefinities",
        "design_services",
      ),
    );
    this.menu.push(
      this.getMenuLink(
        "title.inrichtingscheck",
        "/admin/check",
        "health_and_safety",
      ),
    );
  }

  private getMenuLink(title: string, url: string, icon: string) {
    const menuItem = new LinkMenuItem(title, url, icon);
    menuItem.activated = this.activeMenu === title;
    return menuItem;
  }
}
