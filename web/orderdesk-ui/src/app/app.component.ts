import { Component, ViewChild } from '@angular/core';
import { CreateOrderComponent } from './create-order.component';
import { OrdersListComponent } from './orders-list.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CreateOrderComponent, OrdersListComponent],
  template: `
    <main style="font-family: system-ui; max-width: 960px; margin: 2rem auto; display: grid; gap: 2rem">
      <h1>OrderDesk</h1>
      <app-create-order (created)="list.refresh()" />
      <app-orders-list #list />
    </main>
  `,
})
export class AppComponent {
  @ViewChild('list') list!: OrdersListComponent;
}
