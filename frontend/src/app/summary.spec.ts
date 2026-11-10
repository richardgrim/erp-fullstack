import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { App } from './app';
describe('Resumen operativo', () => {
 beforeEach(() => TestBed.configureTestingModule({imports:[App],providers:[provideHttpClient(),provideHttpClientTesting()]}));
 afterEach(() => TestBed.inject(HttpTestingController).verify());
 function setup() {
  const fixture=TestBed.createComponent(App); const http=TestBed.inject(HttpTestingController);
  http.expectOne('/api/customers').flush([]);
  const base={customerId:1,customerName:'Taller',description:'Eje'};
  http.expectOne('/api/orders').flush([
   {...base,id:1,status:'PENDING',createdAt:new Date(2026,10,8,23,59,59).toISOString()},
   {...base,id:2,status:'IN_PROGRESS',createdAt:new Date(2026,10,9,0,0,0).toISOString()},
   {...base,id:3,status:'DELIVERED',createdAt:new Date(2026,10,9,23,59,59).toISOString()},
   {...base,id:4,status:'COMPLETED',createdAt:new Date(2026,10,10,0,0,0).toISOString()}
  ]); return {fixture,app:fixture.componentInstance};
 }
 it('incluye ambos límites del día local y excluye los días adyacentes', () => {
  const {app}=setup(); app.dateFrom.set('2026-11-09'); app.dateTo.set('2026-11-09');
  expect(app.visibleOrders().map(o=>o.id)).toEqual([2,3]);
  expect(app.summary()).toEqual({PENDING:0,IN_PROGRESS:1,COMPLETED:0,DELIVERED:1});
 });
 it('combina fechas, búsqueda y estado; limpiar restaura el resumen', () => {
  const {app}=setup(); app.dateFrom.set('2026-11-09'); app.search.set('TALLER'); app.statusFilter.set('COMPLETED');
  expect(app.visibleOrders().map(o=>o.id)).toEqual([4]); expect(app.summary().COMPLETED).toBe(1);
  app.search.set('inexistente'); expect(app.visibleOrders()).toHaveLength(0);
  app.clearFilters(); expect(app.visibleOrders()).toHaveLength(4); expect(app.hasFilters()).toBe(false);
 });
 it('advierte un intervalo invertido sin mostrar un resumen engañoso', () => {
  const {app,fixture}=setup(); app.dateFrom.set('2026-11-10'); app.dateTo.set('2026-11-09'); fixture.detectChanges();
  expect(app.invalidPeriod()).toBe(true); expect(app.visibleOrders()).toHaveLength(0);
  expect(fixture.nativeElement.textContent).toContain('La fecha Desde no puede ser posterior a Hasta.');
 });
 it('permite un límite superior abierto y actualiza contadores tras cambiar un estado', () => {
  const {app}=setup(); app.dateTo.set('2026-11-08'); expect(app.pending()).toBe(1);
  app.orders.update(items=>items.map(o=>o.id===1?{...o,status:'IN_PROGRESS'}:o));
  expect(app.pending()).toBe(0); expect(app.summary().IN_PROGRESS).toBe(1);
 });
});
