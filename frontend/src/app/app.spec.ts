import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { App } from './app';
import { SessionService } from './session.service';

describe('Recepción de trabajos', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  beforeEach(() =>
    TestBed.inject(SessionService).user.set({
      id: 1,
      username: 'admin',
      displayName: 'Administrador',
      role: 'ADMIN',
    }),
  );
  it('permite registrar un cliente y asociarlo a una nueva orden', async () => {
    const fixture = TestBed.createComponent(App);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/customers').flush([]);
    http.expectOne('/api/orders').flush([]);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const set = (id: string, value: string) => {
      const input = el.querySelector('#' + id) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    set('name', 'Taller de prueba');
    set('phone', '70000000');
    set('email', 'demo@example.com');
    fixture.detectChanges();
    await fixture.whenStable();
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    const req = http.expectOne('/api/customers');
    expect(req.request.body.name).toBe('Taller de prueba');
    req.flush({ id: 7, name: 'Taller de prueba', phone: '70000000', email: 'demo@example.com' });
    fixture.detectChanges();
    await fixture.whenStable();
    set('description', 'Fabricar eje de prueba');
    fixture.detectChanges();
    await fixture.whenStable();
    el.querySelectorAll('form')[1].dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    const order = http.expectOne('/api/orders');
    expect(order.request.body.customerId).toBe(7);
    order.flush({
      id: 12,
      customerId: 7,
      customerName: 'Taller de prueba',
      description: 'Fabricar eje de prueba',
      status: 'PENDING',
      createdAt: '2026-11-09T12:00:00Z',
    });
    fixture.detectChanges();
    await fixture.whenStable();
    expect(el.textContent).toContain('OT-0012');
    expect(el.textContent).toContain('Fabricar eje de prueba');
  });
  it('muestra el fallo de conexión y conserva los formularios', () => {
    const f = TestBed.createComponent(App);
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/customers')
      .flush('Unavailable', { status: 503, statusText: 'Unavailable' });
    http.expectOne('/api/orders').flush('Unavailable', { status: 503, statusText: 'Unavailable' });
    f.detectChanges();
    expect(f.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'No se pudo conectar',
    );
    expect(f.nativeElement.querySelectorAll('form').length).toBe(2);
  });
});
