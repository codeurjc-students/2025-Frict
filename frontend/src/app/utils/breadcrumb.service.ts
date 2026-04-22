import { Injectable, signal, computed } from '@angular/core';
import { MenuItem } from 'primeng/api';

@Injectable({
  providedIn: 'root'
})
export class BreadcrumbService {
  private readonly baseBreadcrumbs = signal<MenuItem[]>([]);

  // 1. CAMBIO AQUÍ: Ahora el diccionario guarda un arreglo de MenuItem[]
  private readonly appendedNodes = signal<Record<string, MenuItem[]>>({});

  private readonly currentUrl = signal<string>('');

  public readonly breadcrumbs = computed(() => {
    const base = this.baseBreadcrumbs();
    const url = this.currentUrl();
    const extraNodes = this.appendedNodes()[url];

    // 2. CAMBIO AQUÍ: Verificamos que haya nodos extra y propagamos ambos arreglos
    if (extraNodes && extraNodes.length > 0) {
      return [...base, ...extraNodes];
    }
    return base;
  });

  setBaseBreadcrumbs(items: MenuItem[], url: string): void {
    this.baseBreadcrumbs.set(items);
    this.currentUrl.set(url);
  }

  // 3. CAMBIO AQUÍ: El método ahora recibe un arreglo de elementos
  setNodesForUrl(url: string, items: MenuItem[]): void {
    this.appendedNodes.update(nodes => ({
      ...nodes,
      [url]: items // Guardamos la lista completa para esta URL
    }));
  }
}
