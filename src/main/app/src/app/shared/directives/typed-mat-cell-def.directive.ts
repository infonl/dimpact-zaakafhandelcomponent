/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import type { DataSource } from "@angular/cdk/collections";
import type { CdkCellOutletRowContext } from "@angular/cdk/table";
import { Directive, input } from "@angular/core";
import type { Observable } from "rxjs";

type TableDataSource<T> =
  | readonly T[]
  | DataSource<T>
  | Observable<readonly T[]>;

/**
 * `matCellDef` exposes its row as `any`, because `CdkCellDef` carries no
 * `ngTemplateContextGuard`: a template could read a field that no longer exists
 * on the row type and still compile. Passing the table's data source with
 * `*matCellDef="let row; dataSource: rows()"` lets the compiler infer the row
 * type from it.
 */
@Directive({ selector: "[matCellDef][matCellDefDataSource]" })
export class TypedMatCellDefDirective<T> {
  readonly matCellDefDataSource = input.required<TableDataSource<T>>();

  static ngTemplateContextGuard<T>(
    _directive: TypedMatCellDefDirective<T>,
    _context: unknown,
  ): _context is Required<CdkCellOutletRowContext<T>> {
    return true;
  }
}
