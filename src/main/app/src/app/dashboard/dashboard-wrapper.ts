/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe } from "@angular/common";
import { Component } from "@angular/core";
import { PolicyService } from "../policy/policy.service";
import { DashboardModule } from "./dashboard.module";

@Component({
  template: `
    @if ((werklijsten$ | async).zakenTaken ) {
    <dashboard-component></dashboard-component>
    } @else {
    <p>Welkom bij de Zaakafhandelcomponent (ZAC)</p>
    }
  `,
  standalone: true,
  imports: [AsyncPipe, DashboardModule],
  selector: "dashboard-wrapper",
})
export class DashboardWrapper {
  constructor(private policyService: PolicyService) {}

  werklijsten$ = this.policyService.readWerklijstRechten();
}
