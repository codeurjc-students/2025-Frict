
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface StatDTO {
  label: string;
  value: string | number;
}

@Injectable({
  providedIn: 'root'
})
export class StatService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/stats';

  getOrdersStatsByRole(): Observable<StatDTO[]> {
    return this.http.get<StatDTO[]>(this.apiUrl + `/orders`);
  }

  getShopsStatsByRole(): Observable<StatDTO[]> {
    return this.http.get<StatDTO[]>(this.apiUrl + `/shops`);
  }
}
