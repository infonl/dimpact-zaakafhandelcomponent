import { computed, effect, signal } from "@angular/core";
import { MatSnackBarRef } from "@angular/material/snack-bar";
import { UtilService } from "src/app/core/service/util.service";
import { ObjectType } from "src/app/core/websocket/model/object-type";
import { Opcode } from "src/app/core/websocket/model/opcode";
import { WebsocketListener } from "src/app/core/websocket/model/websocket-listener";
import { WebsocketService } from "src/app/core/websocket/websocket.service";

type SubscriptionType = {
  opcode: Opcode;
  objectType: ObjectType;
};

type Options = {
  ids: string[];
  progressSubscription: SubscriptionType & {
    onNotification?: (id: string) => void;
  };
  finalSubscription?: SubscriptionType & { screenEventResourceId: string };
  finally: () => void;
};

export class BatchProcessService {
  private state = signal<Record<string, boolean>>({});
  private values = computed(() => Object.values(this.state()));
  private progress = computed(() => {
    const v = this.values();
    return v.length
      ? Math.round((v.filter((done) => done).length / v.length) * 100)
      : undefined;
  });
  private subscriptions: WebsocketListener[] = [];
  private snackBarRef: MatSnackBarRef<unknown>;
  private options: Options;

  constructor(
    private websocketService: WebsocketService,
    private utilService: UtilService,
  ) {
    effect(() => {
      if (this.progress() === 100 && !this.options.finalSubscription) {
        this.options.finally();
      }
    });
  }

  start(options: Options) {
    this.stop();
    this.options = options;
    this.state.set(Object.fromEntries(options.ids.map((x) => [x, false])));
    this.subscriptions = options.ids.map((id) => {
      const subscription = this.websocketService.addListener(
        options.progressSubscription.opcode,
        options.progressSubscription.objectType,
        id,
        () => {
          this.removeSubscription(subscription);
          this.state.update((state) => ({
            ...state,
            [id]: true,
          }));
          options.progressSubscription.onNotification?.(id);
        },
      );
      return subscription;
    });
    if (options.finalSubscription) {
      const finalSubscription = this.websocketService.addListener(
        options.finalSubscription.opcode,
        options.finalSubscription.objectType,
        options.finalSubscription.screenEventResourceId,
        () => {
          this.stop();
          options.finally();
        },
      );
      this.subscriptions.push(finalSubscription);
    }
  }

  showProgress(message: string) {
    this.snackBarRef = this.utilService.openProgressSnackbar({
      progressPercentage: this.progress,
      message,
    });
  }

  stop() {
    this.snackBarRef?.dismiss();
    this.clearSubscriptions();
  }

  update(ids: string[]) {
    this.state.update((state) => ({
      ...state,
      ...Object.fromEntries(ids.map((x) => [x, true])),
    }));
  }

  private clearSubscriptions() {
    this.websocketService.removeListeners(this.subscriptions);
    this.subscriptions = [];
  }

  private removeSubscription(subscription: WebsocketListener) {
    const i = this.subscriptions.indexOf(subscription);
    i !== -1 && this.subscriptions.splice(i, 1);
    this.websocketService.removeListener(subscription);
  }
}
