import { HttpInterceptorFn } from '@angular/common/http';

/** Every request carries a correlation id that the API attaches to its trace/log context. */
export const correlationInterceptor: HttpInterceptorFn = (req, next) =>
  next(req.clone({ setHeaders: { 'X-Correlation-Id': crypto.randomUUID() } }));
