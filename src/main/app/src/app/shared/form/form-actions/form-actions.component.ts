import { Component, input, output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { CreateMutationResult } from "@tanstack/angular-query-experimental";

@Component({
  selector: "zac-form-actions",
  templateUrl: "./form-actions.component.html",
})
export class ZacFormActions {
  protected readonly submitLabel = input<string>("actie.verstuur");
  protected readonly cancelLabel = input<string>("actie.annuleren");

  protected readonly form = input.required<FormGroup>();
  protected readonly mutation = input.required<CreateMutationResult>();

  // eslint-disable-next-line @angular-eslint/no-output-native
  protected readonly cancel = output<Event>();

  protected readonly onCancel = (event: MouseEvent) => {
    this.cancel.emit(
      new CustomEvent("cancel", {
        bubbles: true,
        cancelable: true,
        detail: { originalEvent: event },
      }),
    );
  };
}
