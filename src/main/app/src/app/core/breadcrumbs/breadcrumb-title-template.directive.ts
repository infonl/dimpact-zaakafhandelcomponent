/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Directive } from "@angular/core";
import { Breadcrumb } from "./route-title";

// `NgTemplateOutlet` passes its context to a plain `<ng-template>` as `any`, so the
// compiler would not check what the template reads from the breadcrumb.
@Directive({ selector: "ng-template[zacBreadcrumbTitle]" })
export class BreadcrumbTitleTemplateDirective {
  static ngTemplateContextGuard(
    _directive: BreadcrumbTitleTemplateDirective,
    _context: unknown,
  ): _context is { $implicit: Breadcrumb } {
    return true;
  }
}
