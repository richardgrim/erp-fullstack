import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { switchMap, tap, catchError, throwError } from 'rxjs';

export interface SessionUser { id: number; username: string; displayName: string; role: 'ADMIN' | 'OPERATOR'; }

@Injectable({ providedIn: 'root' })
export class SessionService {
  private http = inject(HttpClient);
  user = signal<SessionUser | null>(null);
  checking = signal(true);
  csrf = '';
  private renewCsrf() {
    return this.http.get<{ token: string }>('/api/auth/csrf').pipe(tap(r => this.csrf = r.token));
  }
  restore() {
    this.checking.set(true);
    return this.renewCsrf().pipe(switchMap(() => this.http.get<SessionUser>('/api/auth/me')),
      tap(user => { this.user.set(user); this.checking.set(false); }),
      catchError(error => { this.user.set(null); this.checking.set(false); return throwError(() => error); }));
  }
  login(username: string, password: string) {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.renewCsrf().pipe(
      switchMap(() => this.http.post('/api/auth/login', body)),
      switchMap(() => this.renewCsrf()),
      switchMap(() => this.http.get<SessionUser>('/api/auth/me')),
      tap(user => this.user.set(user)));
  }
  logout() {
    return this.renewCsrf().pipe(switchMap(() => this.http.post('/api/auth/logout', {})),
      tap(() => this.expire()));
  }
  expire() { this.user.set(null); this.csrf = ''; }
}

export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionService);
  const isApi = request.url.startsWith('/api/');
  if (isApi && !['GET', 'HEAD', 'OPTIONS'].includes(request.method) && session.csrf) {
    request = request.clone({ setHeaders: { 'X-CSRF-TOKEN': session.csrf } });
  }
  return next(request).pipe(catchError((error: HttpErrorResponse) => {
    if (isApi && error.status === 401 && !request.url.startsWith('/api/auth/')) session.expire();
    return throwError(() => error);
  }));
};
