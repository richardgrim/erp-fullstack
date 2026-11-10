import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthShell } from './auth-shell';
import { sessionInterceptor, SessionService } from './session.service';

describe('Acceso y roles', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [AuthShell],
      providers: [
        provideHttpClient(withInterceptors([sessionInterceptor])),
        provideHttpClientTesting(),
      ],
    }),
  );
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  function setup(role: 'ADMIN' | 'OPERATOR' | null) {
    const fixture = TestBed.createComponent(AuthShell);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/auth/csrf').flush({ token: 'initial-test-token' });
    const me = http.expectOne('/api/auth/me');
    if (role) me.flush({ id: 1, username: 'tester', displayName: 'Usuario de prueba', role });
    else me.flush({}, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    if (role) {
      http.expectOne('/api/customers').flush([]);
      http
        .expectOne('/api/orders')
        .flush([
          {
            id: 1,
            customerName: 'Cliente ficticio',
            customerId: 1,
            description: 'Prueba',
            status: 'COMPLETED',
            createdAt: '2026-11-09T12:00:00Z',
          },
        ]);
      fixture.detectChanges();
    }
    return { fixture, http, el: fixture.nativeElement as HTMLElement };
  }
  it('inicia sesión con CSRF y renueva el token después de autenticar', async () => {
    const { fixture, http, el } = setup(null);
    expect(el.querySelector('h1')?.textContent).toContain('Accede a tu taller');
    await fixture.whenStable();
    for (const [id, value] of [
      ['login-user', 'admin'],
      ['login-password', 'Test-only-password!'],
    ]) {
      const input = el.querySelector('#' + id) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    fixture.detectChanges();
    await fixture.whenStable();
    el.querySelector('form')!.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    http.expectOne('/api/auth/csrf').flush({ token: 'login-test-token' });
    const login = http.expectOne('/api/auth/login');
    expect(login.request.headers.get('X-CSRF-TOKEN')).toBe('login-test-token');
    expect(login.request.body.get('username')).toBe('admin');
    login.flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne('/api/auth/csrf').flush({ token: 'after-login-token' });
    http
      .expectOne('/api/auth/me')
      .flush({ id: 1, username: 'admin', displayName: 'Administrador', role: 'ADMIN' });
    fixture.detectChanges();
    http.expectOne('/api/customers').flush([]);
    http.expectOne('/api/orders').flush([]);
    fixture.detectChanges();
    expect(el.textContent).toContain('Gestionar usuarios');
    expect(fixture.componentInstance.password).toBe('');
    expect(TestBed.inject(SessionService).csrf).toBe('after-login-token');
  });
  it('oculta recepción, usuarios y entrega al operador', () => {
    const { el } = setup('OPERATOR');
    expect(el.textContent).not.toContain('Gestionar usuarios');
    expect(el.querySelector('#name')).toBeNull();
    expect(el.querySelector('.advance')).toBeNull();
    expect(el.textContent).toContain('El administrador confirmará la entrega.');
  });
  it('cierra sesión en el servidor antes de ocultar los datos', () => {
    const { fixture, http, el } = setup('ADMIN');
    const button = Array.from(el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Cerrar sesión'),
    )!;
    button.click();
    http.expectOne('/api/auth/csrf').flush({ token: 'logout-test-token' });
    const logout = http.expectOne('/api/auth/logout');
    expect(logout.request.headers.get('X-CSRF-TOKEN')).toBe('logout-test-token');
    logout.flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();
    expect(el.querySelector('#login-password')).not.toBeNull();
    expect(el.textContent).not.toContain('Cliente ficticio');
    expect(TestBed.inject(SessionService).csrf).toBe('');
  });
  it('retira los datos de la pantalla si expira la sesión', () => {
    const { fixture, http, el } = setup('ADMIN');
    Array.from(el.querySelectorAll('button'))
      .find((b) => b.textContent?.trim() === 'Actualizar')!
      .click();
    http.expectOne('/api/customers').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/orders').flush({}, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    expect(el.querySelector('#login-user')).not.toBeNull();
    expect(el.textContent).not.toContain('Cliente ficticio');
  });
});
