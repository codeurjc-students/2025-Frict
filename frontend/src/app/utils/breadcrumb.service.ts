import { Injectable, signal, computed } from '@angular/core';
import { MenuItem } from 'primeng/api';

@Injectable({
  providedIn: 'root'
})
export class BreadcrumbService {
  private readonly baseBreadcrumbs = signal<MenuItem[]>([]);

  // Nodos que van al final (ej: Nombre del Producto)
  private readonly appendedNodes = signal<Record<string, MenuItem[]>>({});

  // NUEVO: Nodos que van antes del último (ej: Categorías, Búsqueda)
  private readonly penultimateNodes = signal<Record<string, MenuItem[]>>({});

  private readonly currentUrl = signal<string>('');

  public readonly breadcrumbs = computed(() => {
    const base = this.baseBreadcrumbs();
    const url = this.currentUrl();

    const extraNodes = this.appendedNodes()[url] || [];
    const middleNodes = this.penultimateNodes()[url] || [];

    // There are no penultimate nodes
    if (middleNodes.length === 0) {
      return [...base, ...extraNodes];
    }

    // There are penultimate nodes
    if (base.length > 0) {
      const lastBaseItem = base[base.length - 1];
      const initialBaseItems = base.slice(0, -1);

      // Order: [Initial Base] + [Penultimate] + [Last Base] + [Last]
      return [...initialBaseItems, ...middleNodes, lastBaseItem, ...extraNodes];
    }

    // Fallback if base route is empty
    return [...middleNodes, ...extraNodes];
  });

  setBaseBreadcrumbs(items: MenuItem[], url: string): void {
    this.baseBreadcrumbs.set(items);
    this.currentUrl.set(url);
  }

  setNodesForUrl(url: string, items: MenuItem[]): void {
    this.appendedNodes.update(nodes => ({
      ...nodes,
      [url]: items
    }));
  }

  insertPenultimateNodesForUrl(url: string, items: MenuItem[]): void {
    this.penultimateNodes.update(nodes => ({
      ...nodes,
      [url]: items
    }));
  }
}
