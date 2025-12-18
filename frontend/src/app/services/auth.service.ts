import {computed, Injectable, signal, WritableSignal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {LoginInfo} from '../models/loginInfo.model';
import {catchError, map, Observable, of, tap} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private defaultLoginInfo: LoginInfo = {isLogged: false, imageUrl: '', id: '0', name: '', username: '', roles: []};
  private loginInfoSignal: WritableSignal<LoginInfo> = signal(this.defaultLoginInfo);

  public isLogged = computed(() => this.loginInfoSignal().isLogged);
  public isUser = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('USER'));
  public isManager = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('MANAGER'));
  public isDriver = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('DRIVER'));
  public isAdmin = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('ADMIN'));

  constructor(private http: HttpClient) {}
  private apiUrl = '/api/v1';

  public signup(userData: FormData): Observable<LoginInfo> {
    return this.http.post<LoginInfo>(this.apiUrl + '/auth/signup', userData);
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
      tap(info => this.loginInfoSignal.set(info))
    );
  }

  public logout(): Observable<any> {
    return this.http.post(this.apiUrl + "/auth/logout", { withCredentials: true }).pipe(
      tap(() => this.loginInfoSignal.set(this.defaultLoginInfo)),
      catchError(err => {
        this.loginInfoSignal.set(this.defaultLoginInfo);
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
}
