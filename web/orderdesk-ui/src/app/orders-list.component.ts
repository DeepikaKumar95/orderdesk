import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, combineLatest, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, startWith, switchMap } from 'rxjs/operators';
import { OrdersApi } from './orders-api.service';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [AsyncPipe, CurrencyPipe, DatePipe, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Orders</h2>
    <label>Customer
      <input [formControl]="customer" placeholder="C-1001" data-testid="customer-filter" />
    </label>
    <button (click)="refresh()" data-testid="refresh">Refresh</button>

    @if (page$ | async; as page) {
      <table data-testid="orders-table">
        <thead><tr><th>Id</th><th>Customer</th><th>Status</th><th>Total</th><th>Created</th></tr></thead>
        <tbody>
          @for (o of page.content; track o.id) {
            <tr [attr.data-order-id]="o.id">
              <td>{{ o.id.slice(0, 8) }}</td>
              <td>{{ o.customerId }}</td>
              <td [attr.data-status]="o.status">{{ o.status }}</td>
              <td>{{ o.total | currency }}</td>
              <td>{{ o.createdAt | date:'short' }}</td>
            </tr>
          } @empty {
            <tr><td colspan="5">No orders</td></tr>
          }
        </tbody>
      </table>
      <p>{{ page.totalElements }} orders</p>
    }
  `,
})
export class OrdersListComponent {
  private api = inject(OrdersApi);
  customer = new FormControl('', { nonNullable: true });
  private refresh$ = new BehaviorSubject<void>(undefined);

  // debounce -> distinctUntilChanged -> switchMap: an older, slower response can never overwrite a newer one.
  page$ = combineLatest([
    this.customer.valueChanges.pipe(startWith(''), debounceTime(300), distinctUntilChanged()),
    this.refresh$,
  ]).pipe(
    switchMap(([customerId]) => this.api.search(customerId.trim()).pipe(
      catchError(() => of({ content: [], totalElements: 0, number: 0, size: 20 })))),
    takeUntilDestroyed(),
  );

  refresh() { this.refresh$.next(); }
}
