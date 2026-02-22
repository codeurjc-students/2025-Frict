import {computed, effect, Injectable, signal, WritableSignal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {LoginInfo} from '../models/loginInfo.model';
import {catchError, map, Observable, of, tap} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  // Añadimos selectedShopId: null al estado por defecto
  private defaultLoginInfo: LoginInfo = {
    isLogged: false,
    imageUrl: '',
    id: '0',
    name: '',
    username: '',
    roles: [],
    selectedShopId: null
  };

  private loginInfoSignal: WritableSignal<LoginInfo> = signal(this.defaultLoginInfo);

  // --- SEÑALES PARA LA TIENDA (Estrictamente en memoria) ---
  private selectedShopIdSignal: WritableSignal<string | null> = signal(null);

  public selectedShopId = computed(() => this.selectedShopIdSignal());
  public hasShopSelected = computed(() => this.selectedShopIdSignal() !== null);
  // ---------------------------------------------------------

  public userRoles = computed(() => this.loginInfoSignal().roles);
  public isLogged = computed(() => this.loginInfoSignal().isLogged);
  public isUser = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('USER'));
  public isManager = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('MANAGER'));
  public isDriver = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('DRIVER'));
  public isAdmin = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('ADMIN'));

  constructor(private http: HttpClient) {
    effect(() => {
      console.log('--- ESTADO DE AUTENTICACIÓN ---');
      console.log('LoginInfo:', this.loginInfoSignal());
      console.log('Tienda ID seleccionada:', this.selectedShopId());
      console.log('¿Hay tienda elegida?:', this.hasShopSelected());
    });
  }

  private apiUrl = '/api/v1';

  // Métodos para cambiar la tienda manualmente desde la interfaz de usuario
  public setSelectedShopId(id: string | null): void {
    this.selectedShopIdSignal.set(id);
  }

  public clearSelectedShopId(): void {
    this.selectedShopIdSignal.set(null);
  }

  public signup(userData: FormData): Observable<LoginInfo> {
    return this.http.post<LoginInfo>(this.apiUrl + '/auth/signup', userData);
  }

  public googleLogin(token: string): Observable<any> {
    return this.http.post(this.apiUrl + "/auth/google", { token: token }, { withCredentials: true });
  }

  public login(user: string, pass: string): Observable<any> {
    return this.http.post(this.apiUrl + "/auth/login", { username: user, password: pass }, { withCredentials: true })
      .pipe(tap(() => this.getLoginInfo().subscribe()));
  }

  public getLoginInfo(): Observable<LoginInfo> {
    // It may return a LoginInfo object o null (204 no content)
    return this.http.get<LoginInfo | null>(this.apiUrl + "/users/session", { withCredentials: true }).pipe(
      map(info => {
        if (!info) {
          return this.defaultLoginInfo;
        }
        return { ...info, isLogged: true };
      }),
      // catchError will catch any error (500 Server Error, Network Error, etc.)
      catchError(() => of(this.defaultLoginInfo)),
      tap(info => {
        this.loginInfoSignal.set(info);

        // ¡Aquí ocurre la magia! Sincronizamos la señal de la tienda con lo que diga el backend.
        // Usamos ?? null para asegurar que si viene undefined, se ponga a null explícitamente.
        this.selectedShopIdSignal.set(info.selectedShopId ?? null);
      })
    );
  }

  public logout(): Observable<any> {
    return this.http.post(this.apiUrl + "/auth/logout", { withCredentials: true }).pipe(
      tap(() => {
        this.loginInfoSignal.set(this.defaultLoginInfo);
        this.selectedShopIdSignal.set(null); // Vaciamos la tienda elegida al salir
      }),
      catchError(err => {
        this.loginInfoSignal.set(this.defaultLoginInfo);
        this.selectedShopIdSignal.set(null); // Vaciamos también si el logout da error
        return of(null);
      })
    );
  }

  //All of the above use @RequestBody to avoid sending sensible data via URL
  public initPasswordRecovery(username: string) {
    return this.http.post(this.apiUrl + "/auth/recovery", { username: username });
  }

  verifyOtp(username: string, otpCode: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/auth/verification`, { username, otpCode });
  }

  resetPassword(username: string, otpCode: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/reset`, { username, otpCode, newPassword });
  }

  //Only works when changing an administration account password (expected behavior)
  public changeInternalUserPassword(id: string, passwordData: FormData){
    return this.http.put(this.apiUrl + `/auth/reset/${id}`, passwordData);
  }
}
