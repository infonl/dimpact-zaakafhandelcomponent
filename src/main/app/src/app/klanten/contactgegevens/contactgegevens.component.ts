/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";

@Component({
  selector: "zac-contactgegevens",
  styleUrls: ["./contactgegevens.component.less"],
  templateUrl: "./contactgegevens.component.html",
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    MatExpansionModule,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    StaticTextComponent,
  ],
})
export class ContactgegevensComponent {
  protected toevoegenToegestaan = input.required<boolean>();
  protected contactDetails = input.required<GeneratedType<"ContactDetails">>();

  protected add = output<void>();
}
