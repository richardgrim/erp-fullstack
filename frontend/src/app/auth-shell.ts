import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { App } from './app';
import { SessionService } from './session.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule, App],
  template: `
    @if (session.checking()) {
      <main class="access"><p role="status">Comprobando sesión…</p></main>
    } @else if (session.user(); as user) {
      <div class="session-bar">
        <span
          >{{ user.displayName }} · {{ user.role === 'ADMIN' ? 'Administrador' : 'Operador' }}</span
        >
        <button type="button" (click)="logout()" [disabled]="busy()">Cerrar sesión</button>
      </div>
      @if (error()) {
        <p class="auth-error" role="alert">{{ error() }}</p>
      }
      <app-workshop />
    } @else {
      <main class="access">
        <section class="login-card">
          <p class="eyebrow">MAESTRANZA ERP</p>
          <h1>Accede a tu taller</h1>
          <p>Clientes, órdenes y seguimiento en un solo lugar.</p>
          @if (error()) {
            <p class="auth-error" role="alert">{{ error() }}</p>
          }
          <form #form="ngForm" (ngSubmit)="login(form)">
            <label for="login-user">Usuario</label
            ><input
              id="login-user"
              name="username"
              [(ngModel)]="username"
              required
              maxlength="40"
              autocomplete="username"
            />
            <label for="login-password">Contraseña</label
            ><input
              id="login-password"
              name="password"
              [(ngModel)]="password"
              required
              type="password"
              autocomplete="current-password"
            />
            <button class="login-button" [disabled]="form.invalid || busy()">
              {{ busy() ? 'Ingresando…' : 'Iniciar sesión' }}
            </button>
          </form>
          <p class="local-note">Entorno local · Acceso con cuenta asignada</p>
        </section>
      </main>
    }
  `,
  styles: `
    :host {
      display: block;
      color: #192e35;
    }
    .access {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
      background: #eef4f0;
    }
    .login-card {
      width: min(100%, 420px);
      padding: 38px;
      background: white;
      border: 1px solid #dbe5df;
      border-radius: 14px;
      box-shadow: 0 15px 50px #173a2610;
    }
    .eyebrow {
      font-size: 11px;
      letter-spacing: 2px;
      color: #176c59;
      font-weight: bold;
    }
    h1 {
      font-size: 28px;
      margin: 20px 0 12px;
    }
    p {
      font-size: 14px;
      line-height: 1.6;
      color: #536a61;
    }
    label {
      display: block;
      font-size: 13px;
      font-weight: bold;
      margin: 20px 0 8px;
    }
    input {
      width: 100%;
      padding: 12px;
      border: 1px solid #baccbf;
      border-radius: 7px;
      font: inherit;
    }
    input:focus {
      outline: 2px solid #77bca9;
    }
    button {
      cursor: pointer;
      border-radius: 7px;
      padding: 11px 15px;
      font: inherit;
      font-size: 13px;
    }
    button:disabled {
      opacity: 0.6;
      cursor: wait;
    }
    .login-button {
      width: 100%;
      margin-top: 24px;
      background: #176c59;
      color: #fff;
      border: 0;
    }
    .local-note {
      font-size: 11px;
      margin-top: 24px;
      text-align: center;
    }
    .session-bar {
      display: flex;
      gap: 20px;
      align-items: center;
      justify-content: flex-end;
      background: #102e36;
      color: white;
      padding: 12px 24px;
      font-size: 13px;
    }
    .session-bar button {
      background: #21434a;
      color: white;
      border: 1px solid #4b6c66;
    }
    .auth-error {
      background: #fff0ed;
      color: #983d30;
      padding: 12px;
      border-radius: 6px;
    }
  `,
})
export class AuthShell {
  session = inject(SessionService);
  busy = signal(false);
  error = signal('');
  username = '';
  password = '';
  constructor() {
    this.session.restore().subscribe({
      error: (e) => {
        if (e.status !== 401)
          this.error.set('No se pudo conectar con el servicio. Comprueba que esté iniciado.');
      },
    });
  }
  login(form: NgForm) {
    if (form.invalid || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.session.login(this.username, this.password).subscribe({
      next: () => {
        this.password = '';
        this.busy.set(false);
      },
      error: (e) => {
        this.password = '';
        this.busy.set(false);
        this.error.set(e.error?.detail ?? 'No se pudo iniciar sesión.');
      },
    });
  }
  logout() {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.session.logout().subscribe({
      next: () => this.busy.set(false),
      error: () => {
        this.busy.set(false);
        this.error.set('No se pudo cerrar la sesión. Vuelve a intentarlo.');
      },
    });
  }
}
