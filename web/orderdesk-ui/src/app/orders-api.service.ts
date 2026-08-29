import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type OrderStatus = 'PENDING' | 'RESERVED' | 'REJECTED' | 'SHIPPED' | 'CANCELLED';
export interface OrderSummary { id: string; customerId: string; status: OrderStatus; total: number; createdAt: string; }
export interface Page<T> { content: T[]; totalElements: number; number: number; size: number; }
export interface LineItem { sku: string; qty: number; unitPrice: number; }
export interface CreateOrderRequest { customerId: string; lines: LineItem[]; }
export interface OrderResponse extends OrderSummary { lines: (LineItem & { lineNo: number })[]; }

@Injectable({ providedIn: 'root' })
export class OrdersApi {
  private http = inject(HttpClient);
  private base = '/api/v1/orders';

  search(customerId: string, page = 0, size = 20): Observable<Page<OrderSummary>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    if (customerId) params = params.set('customerId', customerId);
    return this.http.get<Page<OrderSummary>>(this.base, { params });
  }

  get(id: string): Observable<OrderResponse> { return this.http.get<OrderResponse>(`${this.base}/${id}`); }

  create(req: CreateOrderRequest, idempotencyKey: string): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(this.base, req, { headers: { 'Idempotency-Key': idempotencyKey } });
  }

  cancel(id: string): Observable<OrderResponse> { return this.http.post<OrderResponse>(`${this.base}/${id}/cancel`, {}); }
}
