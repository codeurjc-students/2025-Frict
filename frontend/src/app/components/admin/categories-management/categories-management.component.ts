import { Component, OnInit, signal, effect } from '@angular/core';
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

  chartMode: any[] = [{ label: 'Productos', value: true }, { label: 'Stock', value: false }];
  productsViewSelected: boolean = true;

  // Datos separados para evitar conflictos de estado (expansión/selección)
  orgChartNodes = signal<TreeNode[]>([]);
  treeTableNodes = signal<TreeNode[]>([]);
  totalCategories = signal<number>(0);

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

  loadCategories() {
    if (this.listModeSelected) {
      this.categoryService.getAllCategories().subscribe({
        next: (list) => {
          const rawData: Category[] = list;
          const totalCount = this.countTotalNodes(rawData);
          this.totalCategories.set(totalCount);
          const mappedChildren = rawData.map(c => this.mapToOrgChart(c));

          // Create root node
          const rootNode: TreeNode = {
            expanded: true,
            type: 'category',
            styleClass: 'bg-transparent',
            data: {
              id: -1,
              name: 'Catálogo Completo',
              icon: 'pi pi-sitemap',
              count: totalCount,
              active: true
            },
            children: mappedChildren
          };

          this.orgChartNodes.set([rootNode]);
          this.treeTableNodes.set(rawData.map(c => this.mapToTreeTable(c)));

          this.loading = false;
        },
        error: (err) => {
          console.error('Error cargando categorías', err);
          this.loading = false;
        }
      });
    }
    else {
      this.categoryService.getAllCategoriesPage(this.first / this.rows, this.rows).subscribe({
        next: (page) => {
          this.categoriesPage = page;
          const totalCount = this.countTotalNodes(this.categoriesPage.items);
          this.totalCategories.set(totalCount);
          const mappedChildren = this.categoriesPage.items.map(c => this.mapToOrgChart(c));

          // Create root node
          const rootNode: TreeNode = {
            expanded: true,
            type: 'category',
            styleClass: 'bg-transparent',
            data: {
              id: -1,
              name: 'Página actual',
              icon: 'pi pi-sitemap',
              count: totalCount,
              active: true
            },
            children: mappedChildren
          };

          this.orgChartNodes.set([rootNode]);
          this.treeTableNodes.set(this.categoriesPage.items.map(c => this.mapToTreeTable(c)));

          this.loading = false;
        },
        error: (err) => {
          console.error('Error cargando categorías', err);
          this.loading = false;
        }
      });
    }
  }

  countTotalNodes(categories: Category[]): number {
    let count = 0;
    for (const cat of categories) {
      count++;
      if (cat.children && cat.children.length > 0) {
        count += this.countTotalNodes(cat.children);
      }
    }
    return count;
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
        count: cat.productsCount || 0,
        active: true
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

  onNodeSelect(event: any) {
    this.messageService.add({ severity: 'info', summary: 'Nodo Seleccionado', detail: event.node.data.name });
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
