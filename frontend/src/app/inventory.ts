import { Component, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule, NgForm } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { SessionService } from './session.service';
interface Material { id: number; name: string; unit: string; stock: number; }
interface Movement { id: number; kind: string; quantity: number; orderId: number | null; reason: string; actorUsername: string; createdAt: string; }
interface ActiveOrder { id: number; customerName: string; status: string; }
@Component({
 selector: 'app-inventory', imports: [FormsModule, DatePipe, DecimalPipe],
 templateUrl: './inventory.html', styleUrl: './inventory.css',
})
export class Inventory {
 private http = inject(HttpClient);
 admin = computed(() => this.session.user()?.role === 'ADMIN');
 private session = inject(SessionService);
 materials = signal<Material[]>([]);
 orders = signal<ActiveOrder[]>([]);
 selected = signal<Material | null>(null);
 movements = signal<Movement[]>([]);
 loading = signal(false);
 historyLoading = signal(false);
 busy = signal(false);
 error = signal('');
 notice = signal('');
 material = { name: '', unit: 'UNIDAD' };
 movement = { kind: 'CONSUME', quantity: null as number | null, orderId: null as number | null, reason: '' };
 private historyRequest = 0;
 constructor() { this.refresh(); }
 refresh() {
  this.error.set(''); this.loading.set(true);
  this.http.get<Material[]>('/api/materials').subscribe({ next: items => {
   this.materials.set(items); this.loading.set(false);
   if(this.selected()) { const current = items.find(m => m.id === this.selected()?.id); if(current) this.select(current); }
  }, error: e => {this.loading.set(false); this.fail(e);} });
  this.http.get<ActiveOrder[]>('/api/orders').subscribe({next: orders => this.orders.set(orders.filter(o => o.status === 'IN_PROGRESS')), error: e => this.fail(e)});
 }
 select(material: Material) {
  this.selected.set(material); this.movements.set([]); this.historyLoading.set(true);
  const request = ++this.historyRequest;
  this.http.get<Movement[]>(`/api/materials/${material.id}/movements`).subscribe({
   next: items => {if(request === this.historyRequest) {this.movements.set(items); this.historyLoading.set(false);}},
   error: e => {if(request === this.historyRequest) {this.historyLoading.set(false); this.fail(e);}}
  });
 }
 create(form: NgForm) {
  if(form.invalid || this.busy() || !this.admin() || !this.material.name.trim()) return;
  this.busy.set(true); this.error.set(''); this.notice.set('');
  this.http.post<Material>('/api/materials', this.material).subscribe({next: item => {
   this.materials.update(items => [...items,item].sort((a,b) => a.name.localeCompare(b.name)));
   form.resetForm({name:'',unit:'UNIDAD'}); this.busy.set(false); this.select(item); this.notice.set('Material registrado con stock cero.');
  }, error: e => {this.busy.set(false); this.fail(e);} });
 }
 save(form: NgForm) {
  const material = this.selected();
  if(!material || form.invalid || this.busy() || !this.movement.reason.trim()) return;
  if(this.movement.kind === 'ENTRY' && !this.admin()) return;
  this.busy.set(true); this.error.set(''); this.notice.set('');
  const input = {...this.movement, orderId: this.movement.kind === 'ENTRY' ? null : this.movement.orderId};
  this.http.post<Material>(`/api/materials/${material.id}/movements`,input).subscribe({next: updated => {
   this.materials.update(items => items.map(m => m.id === updated.id ? updated : m));
   this.select(updated); this.busy.set(false);
   form.resetForm({kind: input.kind, quantity:null, orderId:null, reason:''}); this.notice.set('Movimiento registrado. Existencias actualizadas.');
  }, error: e => {this.busy.set(false); this.fail(e);} });
 }
 private fail(e: HttpErrorResponse) {this.error.set(e.error?.detail ?? 'No se pudo cargar o guardar el inventario. Vuelve a intentar.');}
}


