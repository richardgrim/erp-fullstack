import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { App } from './app';
import { SessionService } from './session.service';

const pending = {
  id: 12,
  customerId: 7,
  customerName: 'Cliente ficticio',
  description: 'Eje de prueba',
  status: 'PENDING',
  createdAt: '2026-11-09T12:00:00Z',
};
describe('Seguimiento de órdenes', () => {
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
  function setup() {
    const fixture = TestBed.createComponent(App);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/customers').flush([]);
    http.expectOne('/api/orders').flush([pending]);
    fixture.detectChanges();
    return { fixture, http, el: fixture.nativeElement as HTMLElement };
  }
  function button(el: HTMLElement, text: string) {
    const match = Array.from(el.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === text,
    );
    if (!match) throw new Error('Botón no encontrado: ' + text);
    return match;
  }
  it('avanza y actualiza el historial abierto sin duplicar solicitudes', () => {
    const { fixture, http, el } = setup();
    button(el, 'Ver historial').click();
    http
      .expectOne('/api/orders/12/history')
      .flush([{ id: 1, fromStatus: null, toStatus: 'PENDING', changedAt: pending.createdAt }]);
    fixture.detectChanges();
    expect(el.textContent?.replace(/\s+/g, ' ')).toContain('Registro inicial → Pendiente');
    button(el, 'Iniciar trabajo').click();
    fixture.detectChanges();
    expect(button(el, 'Guardando…').disabled).toBe(true);
    const change = http.expectOne('/api/orders/12/status');
    expect(change.request.method).toBe('PATCH');
    expect(change.request.body).toEqual({ status: 'IN_PROGRESS' });
    change.flush({ ...pending, status: 'IN_PROGRESS' });
    http.expectOne('/api/orders/12/history').flush([
      { id: 1, fromStatus: null, toStatus: 'PENDING', changedAt: pending.createdAt },
      { id: 2, fromStatus: 'PENDING', toStatus: 'IN_PROGRESS', changedAt: '2026-11-09T13:00:00Z' },
    ]);
    fixture.detectChanges();
    expect(el.textContent?.replace(/\s+/g, ' ')).toContain('Pendiente → En proceso');
    expect(button(el, 'Marcar como terminada')).toBeTruthy();
    expect(fixture.componentInstance.pending()).toBe(0);
  });
  it('conserva el estado visible si el servidor rechaza un cambio obsoleto', () => {
    const { fixture, http, el } = setup();
    button(el, 'Iniciar trabajo').click();
    http
      .expectOne('/api/orders/12/status')
      .flush({ detail: 'Actualiza el listado.' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Actualiza el listado.');
    expect(fixture.componentInstance.orders()[0].status).toBe('PENDING');
    expect(button(el, 'Iniciar trabajo').disabled).toBe(false);
  });
  it('filtra por estado y no ofrece avanzar una orden entregada', () => {
    const { fixture, el } = setup();
    fixture.componentInstance.orders.set([{ ...pending, status: 'DELIVERED' }]);
    fixture.componentInstance.statusFilter.set('PENDING');
    fixture.detectChanges();
    expect(el.textContent).toContain('Sin coincidencias');
    fixture.componentInstance.statusFilter.set('DELIVERED');
    fixture.detectChanges();
    expect(el.textContent).toContain('Trabajo entregado');
    expect(el.querySelector('.advance')).toBeNull();
  });
});
