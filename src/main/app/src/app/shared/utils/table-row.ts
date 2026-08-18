/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { MatTableDataSource } from "@angular/material/table";

/**
 * `matCellDef` exposes its row as `any`: `CdkCellDef` carries no
 * `ngTemplateContextGuard`, so nothing on the component side can type the cell
 * context. Passing the data source alongside the row infers the row type
 * instead of naming it a second time, which keeps template and data source from
 * drifting apart. The cast itself stays unchecked, exactly as a context guard
 * would be.
 */
export function rowOf<T>(source: MatTableDataSource<T> | T[], row: unknown): T {
  return row as T;
}
