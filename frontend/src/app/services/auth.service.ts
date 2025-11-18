import { Injectable, signal, WritableSignal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LoginInfo } from '../models/loginInfo.model';
import { Observable, tap, catchError, map, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private defaultLoginInfo: LoginInfo = {isLogged: false, id: "0", name: '', username: '', roles: []};
  private loginInfoSignal: WritableSignal<LoginInfo> = signal(this.defaultLoginInfo);

  public isLogged = computed(() => this.loginInfoSignal().isLogged);
  public isUser = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('USER'));
  public isManager = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('MANAGER'));
  public isDriver = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('DRIVER'));
  public isAdmin = computed(() => this.loginInfoSignal().isLogged && this.loginInfoSignal().roles.includes('ADMIN'));

  constructor(private http: HttpClient) {}
  private apiUrl = '/api/v1';

  public signup(userData: FormData) {
    return this.http.post<any>(this.apiUrl + '/auth/signup', userData);
  }

  public login(user: string, pass: string): Observable<any> {
    return this.http.post(this.apiUrl + "/auth/login", { username: user, password: pass }, { withCredentials: true })
      .pipe(tap(() => this.getLoginInfo().subscribe()));
  }

  public getLoginInfo(): Observable<LoginInfo> {
    return this.http.get<LoginInfo>(this.apiUrl + "/users/me", { withCredentials: true }).pipe(
      map(info => ({...info, isLogged: true})),
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
}
