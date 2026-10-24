/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { finalize } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { rotate180, sideNavToggle } from "../animations/animations";
import { ButtonMenuItem } from "./menu-item/button-menu-item";
import { HrefMenuItem } from "./menu-item/href-menu-item";
import { LinkMenuItem } from "./menu-item/link-menu-item";
import { MenuItem, MenuItemType } from "./menu-item/menu-item";
import { AsyncButtonMenuItem } from "./menu-item/subscription-button-menu-item";
import { SideNavUtil } from "./side-nav.util";

@Component({
  selector: "zac-side-nav",
  templateUrl: "./side-nav.component.html",
  styleUrls: ["./side-nav.component.less"],
  animations: [rotate180, sideNavToggle],
})
export class SideNavComponent implements OnInit {
  @Input() menu: MenuItem[];
  @Input() activeItem: string | null;
  @Output() mode = new EventEmitter<string>();
  @Output() activeItemChange = new EventEmitter<string | null>();

  readonly menuItemType = MenuItemType;
  menuMode = SideNavUtil.load();
  menuState: string;

  constructor(private utilService: UtilService) {}

  ngOnInit(): void {
    this.menuState =
      this.menuMode === MenuMode.OPEN ? MenuMode.OPEN : MenuMode.CLOSED;
  }

  toggleMenu(): void {
    switch (this.menuMode) {
      case MenuMode.CLOSED:
        this.menuMode = MenuMode.AUTO;
        this.mode.emit("over");
        break;
      case MenuMode.AUTO:
        this.menuMode = MenuMode.OPEN;
        this.menuState = MenuState.OPEN;
        this.mode.emit("side");
        break;
      case MenuMode.OPEN:
      default:
        this.menuMode = MenuMode.CLOSED;
        this.menuState = MenuState.CLOSED;
        this.mode.emit("side");
    }

    SideNavUtil.store(this.menuMode);
  }

  mouseEnter() {
    this.menuState =
      this.menuMode === MenuMode.AUTO ? MenuState.OPEN : this.menuMode;
  }

  mouseLeave() {
    this.menuState =
      this.menuMode === MenuMode.AUTO ? MenuState.CLOSED : this.menuMode;
  }

  onClick(buttonMenuItem: ButtonMenuItem): void {
    if (buttonMenuItem.disabled) return;
    this.activeItemChange.emit(buttonMenuItem.title);

    if (buttonMenuItem instanceof AsyncButtonMenuItem) {
      this.utilService.setLoading(true);
      buttonMenuItem.disabled = true;
      buttonMenuItem
        .fn()
        .pipe(
          finalize(() => {
            this.utilService.setLoading(false);
            buttonMenuItem.disabled = false;
          }),
        )
        .subscribe();

      return;
    }

    buttonMenuItem.fn();
  }

  asButtonMenuItem(menuItem: MenuItem): ButtonMenuItem {
    return menuItem as ButtonMenuItem;
  }

  asHrefMenuItem(menuItem: MenuItem): HrefMenuItem {
    return menuItem as HrefMenuItem;
  }

  asLinkMenuItem(menuItem: MenuItem): LinkMenuItem {
    return menuItem as LinkMenuItem;
  }

  get MenuMode() {
    return MenuMode;
  }

  get MenuState() {
    return MenuState;
  }
}

export enum MenuMode {
  AUTO = "auto",
  OPEN = "open",
  CLOSED = "closed",
}

export enum MenuState {
  OPEN = "open",
  CLOSED = "closed",
}
