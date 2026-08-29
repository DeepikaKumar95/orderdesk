import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { LineItem } from './orders-api.service';

/**
 * Draft order that survives navigation between screens. This is the one place a store is justified:
 * shared across routes, non-trivial transitions. Everything else stays local component state.
 */
export const OrderDraftStore = signalStore(
  { providedIn: 'root' },
  withState({ customerId: 'C-1001', lines: [] as LineItem[] }),
  withComputed(({ lines }) => ({
    total: computed(() => lines().reduce((s, l) => s + l.qty * l.unitPrice, 0)),
    isValid: computed(() => lines().length > 0),
  })),
  withMethods(store => ({
    setCustomer: (customerId: string) => patchState(store, { customerId }),
    addLine: (line: LineItem) => patchState(store, { lines: [...store.lines(), line] }),
    removeLine: (i: number) => patchState(store, { lines: store.lines().filter((_, idx) => idx !== i) }),
    reset: () => patchState(store, { lines: [] }),
  })),
);
