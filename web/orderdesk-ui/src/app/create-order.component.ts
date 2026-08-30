import { ChangeDetectionStrategy, Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { OrdersApi } from './orders-api.service';
import { OrderDraftStore } from './order-draft.store';

@Component({
  selector: 'app-create-order',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>New order</h2>
    <label>Customer
      <select [value]="draft.customerId()" (change)="draft.setCustomer($any($event.target).value)" data-testid="customer">
        <option value="C-1001">C-1001 Acme Logistics (limit 50,000)</option>
        <option value="C-1002">C-1002 Peachtree Retail (limit 10,000)</option>
        <option value="C-1003">C-1003 Cumming Hardware (limit 2,500)</option>
      </select>
    </label>

    <form [formGroup]="lineForm" (ngSubmit)="addLine()">
      <input formControlName="sku" placeholder="SKU-1" data-testid="sku" />
      <input formControlName="qty" type="number" min="1" data-testid="qty" />
      <input formControlName="unitPrice" type="number" step="0.01" data-testid="price" />
      <button type="submit" [disabled]="lineForm.invalid" data-testid="add-line">Add line</button>
    </form>

    <ul>
      @for (l of draft.lines(); track $index) {
        <li>{{ l.sku }} × {{ l.qty }} &#64; {{ l.unitPrice | currency }}
          <button (click)="draft.removeLine($index)">remove</button></li>
      }
    </ul>
    <p>Total: <strong data-testid="total">{{ draft.total() | currency }}</strong></p>

    <button (click)="submit()" [disabled]="!draft.isValid() || submitting()" data-testid="place-order">Place order</button>
    @if (error()) { <p role="alert" data-testid="error">{{ error() }}</p> }
    @if (createdId()) { <p data-testid="created">Created order {{ createdId() }}</p> }
  `,
})
export class CreateOrderComponent {
  private api = inject(OrdersApi);
  private fb = inject(FormBuilder);
  draft = inject(OrderDraftStore);
  @Output() created = new EventEmitter<string>();

  lineForm = this.fb.nonNullable.group({
    sku: ['SKU-1', Validators.required],
    qty: [1, [Validators.required, Validators.min(1)]],
    unitPrice: [10, [Validators.required, Validators.min(0)]],
  });
  submitting = signal(false);
  error = signal<string | null>(null);
  createdId = signal<string | null>(null);
  private idempotencyKey = crypto.randomUUID();   // stable per draft: a double-click cannot create two orders

  addLine() {
    this.draft.addLine(this.lineForm.getRawValue());
    this.lineForm.patchValue({ sku: '', qty: 1 });
  }

  submit() {
    this.submitting.set(true); this.error.set(null);
    this.api.create({ customerId: this.draft.customerId(), lines: this.draft.lines() }, this.idempotencyKey).subscribe({
      next: o => { this.createdId.set(o.id); this.created.emit(o.id); this.draft.reset(); this.idempotencyKey = crypto.randomUUID(); this.submitting.set(false); },
      error: e => { this.error.set(e.error?.detail ?? 'Request failed'); this.submitting.set(false); },
    });
  }
}
