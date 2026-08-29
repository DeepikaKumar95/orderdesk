import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { OrdersListComponent } from './orders-list.component';

describe('OrdersListComponent search', () => {
  it('debounces and only keeps the latest request (switchMap)', fakeAsync(() => {
    TestBed.configureTestingModule({ imports: [OrdersListComponent], providers: [provideHttpClient(), provideHttpClientTesting()] });
    const fixture = TestBed.createComponent(OrdersListComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne(r => r.url === '/api/v1/orders').flush({ content: [], totalElements: 0, number: 0, size: 20 }); // initial
    fixture.componentInstance.customer.setValue('C-1');
    tick(100);
    fixture.componentInstance.customer.setValue('C-1001');
    tick(300);

    const reqs = http.match(r => r.url === '/api/v1/orders');
    expect(reqs.length).toBe(1);                                   // "C-1" never hit the network
    expect(reqs[0].request.params.get('customerId')).toBe('C-1001');
    reqs[0].flush({ content: [], totalElements: 0, number: 0, size: 20 });
    http.verify();
  }));
});
