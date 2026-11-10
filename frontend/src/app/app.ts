import { Component, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule, NgForm } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { SessionService, SessionUser } from './session.service';
import { Inventory } from './inventory';
interface Customer {
  id: number;
  name: string;
  phone: string;
  email: string;
}
type OrderStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'DELIVERED';
interface HistoryEntry {
  id: number;
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  changedAt: string;
  actorUsername: string | null;
}
interface Order {
  id: number;
  customerId: number;
  customerName: string;
  description: string;
  status: OrderStatus;
  createdAt: string;
}
@Component({
  selector: 'app-workshop',
  imports: [FormsModule, DatePipe, Inventory],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  showInventory = signal(false);
  private http = inject(HttpClient);
  session = inject(SessionService);
  isAdmin = computed(() => this.session.user()?.role === 'ADMIN');
  showUsers = signal(false);
  users = signal<SessionUser[]>([]);
  savingUser = signal(false);
  newUser = { username: '', displayName: '', password: '' };
  customers = signal<Customer[]>([]);
  orders = signal<Order[]>([]);
  loading = signal(true);
  savingCustomer = signal(false);
  savingOrder = signal(false);
  error = signal('');
  notice = signal('');
  search = signal('');
  statusFilter = signal('');
  dateFrom = signal('');
  dateTo = signal('');
  invalidPeriod = computed(() => !!this.dateFrom() && !!this.dateTo() && this.dateFrom() > this.dateTo());
  hasFilters = computed(() => !!(this.search() || this.statusFilter() || this.dateFrom() || this.dateTo()));
  changingOrder = signal<number | null>(null);
  historyOrder = signal<number | null>(null);
  historyEntries = signal<HistoryEntry[]>([]);
  historyLoading = signal(false);
  historyError = signal('');
  private historyRequest = 0;
  readonly statusLabels: Record<OrderStatus, string> = {
    PENDING: 'Pendiente',
    IN_PROGRESS: 'En proceso',
    COMPLETED: 'Terminada',
    DELIVERED: 'Entregada',
  };
  readonly actions: Record<OrderStatus, { target: OrderStatus; label: string } | null> = {
    PENDING: { target: 'IN_PROGRESS', label: 'Iniciar trabajo' },
    IN_PROGRESS: { target: 'COMPLETED', label: 'Marcar como terminada' },
    COMPLETED: { target: 'DELIVERED', label: 'Registrar entrega' },
    DELIVERED: null,
  };
  customer = { name: '', phone: '', email: '' };
  order = { customerId: null as number | null, description: '' };
  visibleOrders = computed(() =>
    this.orders().filter(
      (o) =>
        !this.invalidPeriod() &&
        (!this.dateFrom() || this.localDay(o.createdAt) >= this.dateFrom()) &&
        (!this.dateTo() || this.localDay(o.createdAt) <= this.dateTo()) &&
        (!this.statusFilter() || o.status === this.statusFilter()) &&
        (o.customerName + ' ' + o.description + ' ' + o.id)
          .toLowerCase()
          .includes(this.search().toLowerCase()),
    ),
  );
  summary = computed(() => {
    const result = { PENDING: 0, IN_PROGRESS: 0, COMPLETED: 0, DELIVERED: 0 };
    for (const order of this.visibleOrders()) result[order.status]++;
    return result;
  });
  pending = computed(() => this.summary().PENDING);
  private localDay(instant: string): string {
    const date = new Date(instant);
    return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
  }
  clearFilters() {
    this.search.set(''); this.statusFilter.set(''); this.dateFrom.set(''); this.dateTo.set('');
  }
  constructor() {
    this.refresh();
  }
  refresh() {
    this.historyOrder.set(null);
    this.historyRequest++;
    this.error.set('');
    this.loading.set(true);
    this.http
      .get<Customer[]>('/api/customers')
      .subscribe({ next: (c) => this.customers.set(c), error: (e) => this.fail(e) });
    this.http.get<Order[]>('/api/orders').subscribe({
      next: (o) => {
        this.orders.set(o);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.fail(e);
      },
    });
  }
  advance(order: Order) {
    const action = this.actions[order.status];
    if (!action || this.changingOrder() !== null) return;
    if (action.target === 'DELIVERED' && !this.isAdmin()) return;
    this.changingOrder.set(order.id);
    this.error.set('');
    this.notice.set('');
    this.http.patch<Order>(`/api/orders/${order.id}/status`, { status: action.target }).subscribe({
      next: (updated) => {
        this.orders.update((list) => list.map((item) => (item.id === updated.id ? updated : item)));
        this.changingOrder.set(null);
        this.notice.set(`Orden #${updated.id}: ${this.statusLabels[updated.status]}.`);
        if (this.historyOrder() === updated.id) this.loadHistory(updated.id);
      },
      error: (e) => {
        this.changingOrder.set(null);
        this.fail(e);
      },
    });
  }
  toggleUsers() {
    this.showUsers.update((open) => !open);
    if (this.showUsers())
      this.http
        .get<SessionUser[]>('/api/users')
        .subscribe({ next: (users) => this.users.set(users), error: (e) => this.fail(e) });
  }
  saveUser(form: NgForm) {
    if (form.invalid || this.savingUser() || !this.isAdmin()) return;
    this.error.set('');
    this.notice.set('');
    this.savingUser.set(true);
    this.http.post<SessionUser>('/api/users', this.newUser).subscribe({
      next: (user) => {
        this.users.update((users) => [...users, user]);
        form.resetForm({ username: '', displayName: '', password: '' });
        this.savingUser.set(false);
        this.notice.set('Cuenta de operador creada.');
      },
      error: (e) => {
        this.newUser.password = '';
        this.savingUser.set(false);
        this.fail(e);
      },
    });
  }
  toggleHistory(id: number) {
    if (this.historyOrder() === id) {
      this.historyOrder.set(null);
      this.historyRequest++;
    } else {
      this.historyOrder.set(id);
      this.loadHistory(id);
    }
  }
  loadHistory(id: number) {
    const request = ++this.historyRequest;
    this.historyEntries.set([]);
    this.historyLoading.set(true);
    this.historyError.set('');
    this.http.get<HistoryEntry[]>(`/api/orders/${id}/history`).subscribe({
      next: (entries) => {
        if (request !== this.historyRequest) return;
        this.historyEntries.set(entries);
        this.historyLoading.set(false);
      },
      error: () => {
        if (request !== this.historyRequest) return;
        this.historyLoading.set(false);
        this.historyError.set('No se pudo cargar el historial.');
      },
    });
  }
  saveCustomer(form: NgForm) {
    if (
      form.invalid ||
      this.savingCustomer() ||
      !this.customer.name.trim() ||
      !this.customer.phone.trim()
    )
      return;
    this.error.set('');
    this.notice.set('');
    this.savingCustomer.set(true);
    this.http.post<Customer>('/api/customers', this.customer).subscribe({
      next: (c) => {
        this.customers.update((list) => [...list, c].sort((a, b) => a.name.localeCompare(b.name)));
        this.order.customerId = c.id;
        form.resetForm({ name: '', phone: '', email: '' });
        this.notice.set('Cliente registrado. Ya puedes crear su orden.');
        this.savingCustomer.set(false);
      },
      error: (e) => {
        this.savingCustomer.set(false);
        this.fail(e);
      },
    });
  }
  saveOrder(form: NgForm) {
    if (form.invalid || this.savingOrder() || !this.order.description.trim()) return;
    this.error.set('');
    this.notice.set('');
    this.savingOrder.set(true);
    this.http.post<Order>('/api/orders', this.order).subscribe({
      next: (o) => {
        this.orders.update((list) => [o, ...list]);
        form.resetForm({ customerId: null, description: '' });
        this.notice.set('Orden #' + o.id + ' creada correctamente.');
        this.savingOrder.set(false);
      },
      error: (e) => {
        this.savingOrder.set(false);
        this.fail(e);
      },
    });
  }
  private fail(e: HttpErrorResponse) {
    this.error.set(
      e.status === 0 || e.status >= 500
        ? 'No se pudo conectar con el servicio. Comprueba que esté iniciado y vuelve a intentar.'
        : (e.error?.detail ?? 'No se pudo guardar. Revisa los datos.'),
    );
  }
}
