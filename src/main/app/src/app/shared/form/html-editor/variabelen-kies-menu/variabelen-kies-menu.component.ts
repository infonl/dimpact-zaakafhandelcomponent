/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor } from "@angular/common";
import { Component, Input } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { TranslatePipe } from "@ngx-translate/core";
import { Editor } from "ngx-editor";

@Component({
  selector: "variabelen-kies-menu",
  templateUrl: "./variabelen-kies-menu.component.html",
  styleUrls: ["./variabelen-kies-menu.component.less"],
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatMenuModule, NgFor, TranslatePipe],
})
export class VariabelenKiesMenuComponent {
  @Input({ required: true }) editor!: Editor;
  @Input({ required: true }) variabelen!: string[];

  plakExpressie(variabele: string) {
    this.editor.commands.insertText("{" + variabele + "}").exec();
  }
}
