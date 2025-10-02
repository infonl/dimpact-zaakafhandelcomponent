// process-outlet.component.ts
import { Component, Injector, OnInit, Type, Inject } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ParameterEditComponent } from "../parameter-edit/parameter-edit.component";

@Component({
  selector: "app-parameters-outlet",
  template: `
    <ng-container
      *ngComponentOutlet="component; injector: componentInjector"
    ></ng-container>
  `,
})
export class ParametersOutletComponent implements OnInit {
  data: any;
  selected!: number;
  component!: Type<any>;
  componentInjector!: Injector;

  constructor(
    private route: ActivatedRoute,
    private injector: Injector,
  ) {}

  ngOnInit() {
    this.route.data.subscribe(({ parameters }) => {
      this.data = parameters;

      console.log("ProcessOutletComponent data:", this.data);
      //   this.selected = process.selected;
      this.selected = 1;
      this.loadComponent(this.selected);
    });
  }

  loadComponent(selected: number) {
    // pick component class dynamically
    switch (selected) {
      case 1:
        this.component = ParameterEditComponent;
        break;
      case 2:
        this.component = ParameterEditComponent;
        break;
      case 3:
        this.component = ParameterEditComponent;
        break;
    }

    // create injector to pass inputs
    this.componentInjector = Injector.create({
      providers: [
        { provide: "processData", useValue: this.data },
        { provide: "switchFn", useValue: (n: number) => this.switch(n) },
      ],
      parent: this.injector,
    });
  }

  switch(to: number) {
    this.selected = to;
    this.loadComponent(to);
  }
}
