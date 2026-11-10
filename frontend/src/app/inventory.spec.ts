import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Inventory } from './inventory';
import { SessionService } from './session.service';
import { NgForm } from '@angular/forms';

describe('Inventario', () => {
 beforeEach(() => TestBed.configureTestingModule({imports:[Inventory],providers:[provideHttpClient(),provideHttpClientTesting()]}));
 afterEach(() => TestBed.inject(HttpTestingController).verify());
 function setup(role: 'ADMIN' | 'OPERATOR') {
  TestBed.inject(SessionService).user.set({id:1,username:'prueba',displayName:'Prueba',role});
  const fixture=TestBed.createComponent(Inventory);
  const http=TestBed.inject(HttpTestingController);
  const material={id:1,name:'Acero',unit:'KG',stock:5};
  http.expectOne('/api/materials').flush([material]);
  http.expectOne('/api/orders').flush([{id:2,customerName:'Taller demo',status:'IN_PROGRESS'},{id:3,customerName:'Otro',status:'DELIVERED'}]);
  fixture.componentInstance.select(material);
  http.expectOne('/api/materials/1/movements').flush([]);
  fixture.detectChanges();
  return {fixture,http,component:fixture.componentInstance};
 }
 it('limita los controles de operador y las órdenes disponibles', () => {
  const {fixture,component}=setup('OPERATOR');
  const text=fixture.nativeElement.textContent;
  expect(text).not.toContain('Registrar material');
  expect(text).not.toContain('Entrada de almacén');
  expect(text).toContain('Consumo en orden');
  expect(component.orders().map(o=>o.id)).toEqual([2]);
 });
 it('actualiza stock e historial al registrar un consumo', () => {
  const {fixture,http,component}=setup('ADMIN');
  component.movement={kind:'CONSUME',quantity:2,orderId:2,reason:'Corte de eje'};
  const resetForm=vi.fn();
  component.save({invalid:false,resetForm} as unknown as NgForm);
  const request=http.expectOne('/api/materials/1/movements');
  expect(request.request.body).toEqual({kind:'CONSUME',quantity:2,orderId:2,reason:'Corte de eje'});
  request.flush({id:1,name:'Acero',unit:'KG',stock:3});
  http.expectOne('/api/materials/1/movements').flush([{id:1,kind:'CONSUME',quantity:2,orderId:2,reason:'Corte de eje',actorUsername:'prueba',createdAt:'2026-11-09T12:00:00Z'}]);
  fixture.detectChanges();
  expect(component.selected()?.stock).toBe(3);
  expect(fixture.nativeElement.textContent).toContain('Corte de eje');
  expect(fixture.nativeElement.textContent).toContain('prueba');
  expect(resetForm).toHaveBeenCalled();
 });
 it('conserva formulario y existencias ante stock insuficiente', () => {
  const {http,component}=setup('ADMIN');
  component.movement={kind:'CONSUME',quantity:6,orderId:2,reason:'Corte'};
  const resetForm=vi.fn();
  component.save({invalid:false,resetForm} as unknown as NgForm);
  http.expectOne('/api/materials/1/movements').flush({detail:'Stock insuficiente.'},{status:409,statusText:'Conflict'});
  expect(component.selected()?.stock).toBe(5);
  expect(component.movement.quantity).toBe(6);
  expect(component.busy()).toBe(false);
  expect(component.error()).toBe('Stock insuficiente.');
  expect(resetForm).not.toHaveBeenCalled();
 });
});
