import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

// Interfaz para tipar estrictamente los parámetros permitidos
export interface RegistryFilterParams {
  startDate: string;
  endDate: string;
  viewType: 'GRAPH' | 'TABLE';
  interval?: 'day' | 'week' | 'month' | 'year' | string;
  entityType?: string;
  dataType?: string;
  storeIds?: string[];
  userIds?: string[];
  productIds?: string[];
  orderIds?: string[];
}

// NUEVO: Interfaz para el mapa de IDs asociados devuelto por el backend
export interface CrossReferencesMap {
  storeId: string[];
  productId: string[];
  userId: string[];
  orderId: string[];
}

@Injectable({
  providedIn: 'root'
})
export class RegistryService {

  private http = inject(HttpClient);
  private apiUrl = '/api/v1/registry';

  /**
   * Obtiene los datos agrupados para la gráfica o los registros detallados para la tabla.
   */
  loadRegistry(params: RegistryFilterParams): Observable<any[]> {
    let httpParams = new HttpParams();

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '' && !(Array.isArray(value) && value.length === 0)) {
        if (Array.isArray(value)) {
          value.forEach(v => {
            httpParams = httpParams.append(key, v);
          });
        } else {
          httpParams = httpParams.set(key, value);
        }
      }
    });

    return this.http.get<any[]>(`${this.apiUrl}/stats`, { params: httpParams });
  }

  // --- NUEVOS MÉTODOS OPTIMIZADOS PARA LOS SELECTORES DINÁMICOS ---

  /**
   * 1. Obtiene la lista de entidades principales que tienen datos.
   */
  getEntityTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/entities`);
  }

  /**
   * 2. Obtiene las métricas disponibles para una entidad específica.
   */
  getMetrics(entityType: string): Observable<string[]> {
    const params = new HttpParams().set('entityType', entityType);
    return this.http.get<string[]>(`${this.apiUrl}/metrics`, { params });
  }

  /**
   * 3. Obtiene el diccionario completo de IDs asociados a una entidad y métrica.
   */
  getCrossReferences(entityType: string, dataType: string): Observable<CrossReferencesMap> {
    const params = new HttpParams()
      .set('entityType', entityType)
      .set('dataType', dataType);

    return this.http.get<CrossReferencesMap>(`${this.apiUrl}/references`, { params });
  }
}
