import { inject, Injectable, OnDestroy } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export abstract class AbstractTaakFormulier implements OnDestroy {
  protected readonly formBuilder = inject(FormBuilder);
  protected readonly translateService = inject(TranslateService);
  protected readonly destroy$ = new Subject<void>();

  abstract requestForm(zaak: GeneratedType<"RestZaak">): Promise<FormField[]>;
  abstract handleForm(
    taak: GeneratedType<"RestTask">,
    zaak?: GeneratedType<"RestZaak">,
  ): Promise<FormField[]>;

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
