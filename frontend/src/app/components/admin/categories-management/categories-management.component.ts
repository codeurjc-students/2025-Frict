import {Component, OnInit, signal, effect, computed} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { MessageService, TreeNode } from 'primeng/api';
import { Tag } from 'primeng/tag';
import { TreeTableModule } from 'primeng/treetable';
import { UIChart } from 'primeng/chart';
import { OrganizationChart } from 'primeng/organizationchart';
import { Category } from '../../../models/category.model';
import { CategoryService } from '../../../services/category.service';
import { SelectButton } from 'primeng/selectbutton';
import { Tooltip } from 'primeng/tooltip';
import { Button } from 'primeng/button';
import { RouterLink } from '@angular/router';
import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { Paginator, PaginatorState } from 'primeng/paginator';
import { PageResponse } from '../../../models/pageResponse.model';

@Component({
  selector: 'app-categories-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, Tag, TreeTableModule, UIChart, OrganizationChart, SelectButton, Tooltip, Button, RouterLink, LoadingScreenComponent, Paginator,
  ],
  templateUrl: './categories-management.component.html',
  styleUrl: 'categories-management.component.css'
})
export class CategoriesManagementComponent implements OnInit {

  displayMode: any[] = [{ label: 'Página', value: false }, { label: 'Todas', value: true }];
  listModeSelected: boolean = false;

  chartMode: any[] = [{ label: 'Relevancia', value: true }, { label: 'Uso', value: false }];
  productsViewSelected: boolean = true;

  // Datos separados para evitar conflictos de estado (expansión/selección)
  orgChartNodes = signal<TreeNode[]>([]);
  treeTableNodes = signal<TreeNode[]>([]);
  totalCategories = signal<number>(0);
  maxDepth = signal<number>(0);
  usagePercentage = signal<number>(0);

  loading: boolean = true;
  error: boolean = false;

  chartData: any;
  chartOptions: any;
  barData: any;
  barOptions: any;

  categoriesPage: PageResponse<Category> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 };
  first = 0;
  rows = 5;

  constructor(private messageService: MessageService,
              private categoryService: CategoryService) {

    // REACCIÓN AUTOMÁTICA: Actualiza las gráficas cuando treeTableNodes cambia
    effect(() => {
      const nodes = this.treeTableNodes();
      if (nodes && nodes.length > 0) {
        this.updateChartsData(nodes);
      }
    });
  }

  ngOnInit() {
    this.initCharts(); // Inicializamos las opciones de visualización
    this.loadCategories();
  }

  onCategoryPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadCategories();
  }

  private calculateNodeDepth(node: TreeNode): number {
    if (!node.children || node.children.length === 0) {
      return 1;
    }

    const childrenDepths = node.children.map(child => this.calculateNodeDepth(child));
    return 1 + Math.max(...childrenDepths);
  }

  loadCategories() {

    // 1. Definimos la lógica común de procesamiento (DRY)
    const processResponse = (items: Category[], isFullList: boolean) => {
      // Mapeo a estructura visual
      const mappedChildren = items.map(c => this.mapToOrgChart(c));

      // Creación del Nodo Raíz Virtual
      const rootNode: TreeNode = {
        expanded: true,
        type: 'category', // Tipo específico para estilizar diferente si quieres
        styleClass: 'bg-transparent',
        data: {
          id: -1,
          name: isFullList ? 'Catálogo Completo' : 'Vista Paginada',
          icon: 'pi pi-server',
          count: items.length // Solo conteo directo del primer nivel para el label
        },
        children: mappedChildren
      };

      // 2. CÁLCULO DE MÉTRICAS (Usando la función auxiliar única)
      // Pasamos el rootNode para que analice todo el árbol descendiente
      const stats = this.calculateMetrics([rootNode]);

      // 3. Actualización de Signals
      this.totalCategories.set(stats.totalNodes);
      this.maxDepth.set(stats.maxDepth);

      // Cálculo seguro del porcentaje (evitar división por cero)
      const percentage = stats.totalNodes > 0
        ? (stats.activeNodes / stats.totalNodes) * 100
        : 0;
      this.usagePercentage.set(parseFloat(percentage.toFixed(2)));

      // 4. Actualización de datos visuales
      this.orgChartNodes.set([rootNode]);
      this.treeTableNodes.set(items.map(c => this.mapToTreeTable(c)));

      this.loading = false;
    };

    // 5. Ejecución condicional según el modo
    if (this.listModeSelected) {
      this.categoryService.getAllCategories().subscribe({
        next: (list) => processResponse(list, true),
        error: (err) => { console.error(err); this.loading = false; }
      });
    } else {
      // Asumiendo que usas PrimeNG table lazy load event (first/rows)
      const pageIndex = this.first / this.rows;
      this.categoryService.getAllCategoriesPage(pageIndex, this.rows).subscribe({
        next: (page) => {
          this.categoriesPage = page;
          processResponse(page.items, false)
        },
        error: (err) => { console.error(err); this.loading = false; }
      });
    }
  }

  /**
   * Recorre el árbol recursivamente para extraer métricas.
   * Ignora el nodo raíz virtual (id: -1) para los conteos.
   */
  private calculateMetrics(nodes: TreeNode[], currentDepth: number = 0): { totalNodes: number, activeNodes: number, maxDepth: number } {
    let stats = { totalNodes: 0, activeNodes: 0, maxDepth: currentDepth };

    for (const node of nodes) {
      // CASO A: Nodo Virtual (Raíz visual) -> No cuenta, pero sus hijos sí
      if (node.data?.id === -1) {
        // Reiniciamos la profundidad a 0 para los hijos de la raíz virtual
        const childStats = this.calculateMetrics(node.children || [], 0);
        return childStats;
      }

      // CASO B: Nodo Categoría Real
      stats.totalNodes++; // Contamos este nodo

      // Verificamos si tiene productos (usage)
      if ((node.data?.count || 0) > 0) {
        stats.activeNodes++;
      }

      // Recursión hacia los hijos
      if (node.children && node.children.length > 0) {
        const childStats = this.calculateMetrics(node.children, currentDepth + 1);

        // Sumamos los resultados de los hijos
        stats.totalNodes += childStats.totalNodes;
        stats.activeNodes += childStats.activeNodes;

        // La profundidad es la mayor entre la actual y la que venga de abajo
        stats.maxDepth = Math.max(stats.maxDepth, childStats.maxDepth);
      } else {
        // Si es hoja, la profundidad es el nivel actual (empezando en 1)
        stats.maxDepth = Math.max(stats.maxDepth, currentDepth + 1);
      }
    }
    console.log(stats);
    return stats;
  }

  mapToOrgChart(cat: Category): TreeNode {
    return {
      expanded: true,
      type: 'category',
      styleClass: 'bg-transparent',
      data: {
        id: cat.id,
        name: cat.name,
        icon: cat.icon && cat.icon.trim() !== '' ? cat.icon : 'pi pi-folder',
        count: cat.productsCount || 0
      },
      children: cat.children ? cat.children.map(c => this.mapToOrgChart(c)) : []
    };
  }

  mapToTreeTable(cat: Category): TreeNode {
    return {
      data: {
        id: cat.id,
        name: cat.name,
        icon: cat.icon && cat.icon.trim() !== '' ? cat.icon : 'pi pi-folder',
        description: cat.shortDescription,
        count: cat.productsCount || 0,
        active: true
      },
      children: cat.children ? cat.children.map(c => this.mapToTreeTable(c)) : [],
      expanded: false
    };
  }

  addCategory(parentId?: number) {
    console.log("Crear categoría bajo ID:", parentId || 'Raíz');
  }

  deleteCategory(id: number) {
    this.messageService.add({ severity: 'warn', summary: 'Eliminar', detail: `Categoría ${id} eliminada` });
  }

  // --- CONFIGURACIÓN GRÁFICAS ---

  private initCharts() {
    // Configuración de Estructura y Estilos
    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      radius: '90%',
      plugins: {
        legend: {
          position: 'right',
          align: 'center',
          labels: { boxWidth: 15, usePointStyle: true, padding: 20 }
        }
      }
    };

    this.barOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false }
      },
      scales: {
        y: { beginAtZero: true, grid: { display: false } },
        x: { grid: { display: false } }
      }
    };
  }

  private updateChartsData(nodes: TreeNode[]) {
    // 1. Pie Chart: Cantidad de productos de las categorías en la vista actual
    this.chartData = {
      labels: nodes.map(n => n.data.name),
      datasets: [{
        data: nodes.map(n => n.data.count || 0),
        backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#6366f1', '#ec4899', '#14b8a6', '#94a3b8']
      }]
    };

    // 2. Bar Chart: Conteo de categorías con 0 productos vs 1 o más
    const noStockCount = nodes.filter(n => (n.data.count || 0) === 0).length;
    const withStockCount = nodes.filter(n => (n.data.count || 0) > 0).length;

    this.barData = {
      labels: ['Sin productos', 'Con productos'],
      datasets: [{
        label: 'Categorías',
        data: [noStockCount, withStockCount],
        backgroundColor: ['#fb2c36', '#00c951'], // red-300 y green-300
        borderRadius: 6,
        barPercentage: 0.5
      }]
    };
  }
}
