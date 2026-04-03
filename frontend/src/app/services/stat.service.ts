import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Stat} from '../models/stat.model';

@Injectable({
  providedIn: 'root'
})
export class StatService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/stats';

  getOrdersStatsByRole(): Observable<Stat[]> {
    return this.http.get<Stat[]>(this.apiUrl + `/orders`);
  }

  getShopsStatsByRole(): Observable<Stat[]> {
    return this.http.get<Stat[]>(this.apiUrl + `/shops`);
  }

  getTrucksStatsByRole(): Observable<Stat[]> {
    return this.http.get<Stat[]>(this.apiUrl + `/trucks`);
  }
}
