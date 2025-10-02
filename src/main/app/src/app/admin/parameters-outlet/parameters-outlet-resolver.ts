import { Injectable } from "@angular/core";
import { Resolve, ActivatedRouteSnapshot } from "@angular/router";
import { forkJoin, Observable } from "rxjs";
import { map } from "rxjs/operators";

@Injectable({ providedIn: "root" })
export class ParametersOutletResolver implements Resolve<any> {
  constructor() {}

  resolve(route: ActivatedRouteSnapshot): Observable<any> {
    const uuid = route.paramMap.get("uuid")!;
    return forkJoin({}).pipe(
      map(() => ({
        selected: 1,
      })),
    );
  }
}
