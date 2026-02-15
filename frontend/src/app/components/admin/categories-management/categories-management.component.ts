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

    // Update charts when treeTableNodes changes
    effect(() => {
      const nodes = this.treeTableNodes();
      if (nodes && nodes.length > 0) {
        this.updateChartsData(nodes);
      }
    });
  }

  ngOnInit() {
    this.initCharts();
    this.loadCategories();
  }

  onCategoryPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadCategories();
  }

  loadCategories() {
    const processResponse = (items: Category[], isFullList: boolean) => {
      const mappedChildren = items.map(c => this.mapToOrgChart(c));

      const rootNode: TreeNode = {
        expanded: true,
        type: 'category',
        styleClass: 'bg-transparent',
        data: {
          id: -1,
          name: isFullList ? 'Catálogo Completo' : 'Vista Paginada',
          icon: 'pi pi-server',
          count: items.length // First level children count only
        },
        children: mappedChildren
      };

      // Metrics calculation
      const stats = this.calculateMetrics([rootNode]);

      this.totalCategories.set(stats.totalNodes);
      this.maxDepth.set(stats.maxDepth);

      const percentage = stats.totalNodes > 0
        ? (stats.activeNodes / stats.totalNodes) * 100
        : 0;
      this.usagePercentage.set(parseFloat(percentage.toFixed(2)));

      this.orgChartNodes.set([rootNode]);
      this.treeTableNodes.set(items.map(c => this.mapToTreeTable(c)));

      this.loading = false;
    };

    // Conditional execution depending on the mode selected
    if (this.listModeSelected) {
      this.categoryService.getAllCategories().subscribe({
        next: (list) => processResponse(list, true),
        error: (err) => { console.error(err); this.loading = false; }
      });
    } else {
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

  private calculateMetrics(nodes: TreeNode[], currentDepth: number = 0): { totalNodes: number, activeNodes: number, maxDepth: number } {
    let stats = { totalNodes: 0, activeNodes: 0, maxDepth: currentDepth };

    for (const node of nodes) {
      // The added root node does not count, but its children do
      if (node.data?.id === -1) {
        // Reset the depth for the root children
        const childStats = this.calculateMetrics(node.children || [], 0);
        return childStats;
      }

      // Real category node
      stats.totalNodes++;
      if ((node.data?.count || 0) > 0) {
        stats.activeNodes++;
      }

      if (node.children && node.children.length > 0) {
        const childStats = this.calculateMetrics(node.children, currentDepth + 1);

        // Sum of children results (recursive)
        stats.totalNodes += childStats.totalNodes;
        stats.activeNodes += childStats.activeNodes;
        stats.maxDepth = Math.max(stats.maxDepth, childStats.maxDepth);
      } else {
        // If leaf node, the depth is the current level (starting by 1)
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
    const childrenMapped = cat.children ? cat.children.map(c => this.mapToTreeTable(c)) : [];
    return {
      data: {
        id: cat.id,
        name: cat.name,
        icon: cat.icon && cat.icon.trim() !== '' ? cat.icon : 'pi pi-folder',
        description: cat.bannerText,
        count: cat.productsCount || 0,
        childrenCount: childrenMapped.length,
        active: true
      },
      children: childrenMapped,
      expanded: false
    };
  }

  addCategory(parentId?: number) {
    console.log("Crear categoría bajo ID:", parentId || 'Raíz');
  }

  deleteCategory(id: number) {
    this.messageService.add({ severity: 'warn', summary: 'Eliminar', detail: `Categoría ${id} eliminada` });
  }


  private initCharts() {
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
    // 1. Pie Chart: Number of products that use the current category (NOT the sum of the children data)
    this.chartData = {
      labels: nodes.map(n => n.data.name),
      datasets: [{
        data: nodes.map(n => n.data.count || 0),
        backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#6366f1', '#ec4899', '#14b8a6', '#94a3b8']
      }]
    };

    // Bar chart: noStock = Unused, withStock = In use
    const noStockCount = nodes.filter(n => (n.data.count || 0) === 0).length;
    const withStockCount = nodes.filter(n => (n.data.count || 0) > 0).length;

    this.barData = {
      labels: ['Sin productos', 'Con productos'],
      datasets: [{
        label: 'Categorías',
        data: [noStockCount, withStockCount],
        backgroundColor: ['#fb2c36', '#00c951'], // red-300, green-300
        borderRadius: 6,
        barPercentage: 0.5
      }]
    };
  }
}
