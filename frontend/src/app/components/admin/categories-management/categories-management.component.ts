import {Component, effect, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

import {TreeNode} from 'primeng/api';
import {Tag} from 'primeng/tag';
import {TreeTableModule} from 'primeng/treetable';
import {UIChart} from 'primeng/chart';
import {OrganizationChart} from 'primeng/organizationchart';
import {Category} from '../../../models/category.model';
import {CategoryService} from '../../../services/category.service';
import {SelectButton} from 'primeng/selectbutton';
import {Tooltip} from 'primeng/tooltip';
import {Button} from 'primeng/button';
import {RouterLink} from '@angular/router';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {PageResponse} from '../../../models/pageResponse.model';

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

  // Métricas
  totalCategories = signal<number>(0);
  maxDepth = signal<number>(0);
  usagePercentage = signal<number>(0);
  totalVolume = signal<number>(0);

  loading: boolean = true;
  error: boolean = false;

  chartData: any;
  chartOptions: any;
  barData: any;
  barOptions: any;

  categoriesPage: PageResponse<Category> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 };
  first = 0;
  rows = 5;

  constructor(private categoryService: CategoryService) {

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
      // Total usages count: sum of children usages
      const totalUsages = items.reduce((acc, curr) => acc + (curr.timesUsed || 0), 0);

      const mappedChildren = items.map(c => this.mapToOrgChart(c));

      // VIRTUAL ROOT NODE CREATION
      const rootNode: TreeNode = {
        expanded: true,
        type: 'category',
        styleClass: 'bg-transparent',
        data: {
          id: -1,
          name: isFullList ? 'Catálogo Completo' : 'Vista Paginada',
          icon: 'pi pi-server',
          timesUsed: totalUsages
        },
        children: mappedChildren
      };

      // METRICS CALCULATION
      const stats = this.calculateMetrics([rootNode]);

      this.totalCategories.set(stats.totalNodes);
      this.maxDepth.set(stats.maxDepth);
      this.totalVolume.set(totalUsages);

      // Secure percentage calculation
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


  private calculateMetrics(nodes: TreeNode[], currentDepth: number = 0): { totalNodes: number, activeNodes: number, maxDepth: number, totalVolume: number } {
    let stats = { totalNodes: 0, activeNodes: 0, maxDepth: currentDepth, totalVolume: 0 };

    for (const node of nodes) {
      // Ignore virtual root node and descend to its children
      if (node.data?.id === -1) {
        return this.calculateMetrics(node.children || [], 0);
      }

      stats.totalNodes++;

      const nodeCount = node.data?.timesUsed || 0;

      if (nodeCount > 0) {
        stats.activeNodes++;
      }

      if (!node.children || node.children.length === 0) {
        stats.totalVolume += nodeCount;
      }

      // Recursive
      if (node.children && node.children.length > 0) {
        const childStats = this.calculateMetrics(node.children, currentDepth + 1);

        stats.totalNodes += childStats.totalNodes;
        stats.activeNodes += childStats.activeNodes;
        stats.totalVolume += childStats.totalVolume;
        stats.maxDepth = Math.max(stats.maxDepth, childStats.maxDepth);
      } else {
        // If leaf node, update depth
        stats.maxDepth = Math.max(stats.maxDepth, currentDepth + 1);
      }
    }

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
        timesUsed: cat.timesUsed || 0
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
        description: cat.bannerText,
        timesUsed: cat.timesUsed || 0,
        count: cat.timesUsed || 0,
        active: true
      },
      children: cat.children ? cat.children.map(c => this.mapToTreeTable(c)) : [],
      expanded: false
    };
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

  deleteCategory(id: string){
    this.categoryService.deleteCategory(id).subscribe({
      next: () => {
        this.loadCategories();
      }
    })
  }

  private updateChartsData(nodes: TreeNode[]) {
    // Pie Chart (root categories only)
    this.chartData = {
      labels: nodes.map(n => n.data.name),
      datasets: [{
        data: nodes.map(n => n.data.timesUsed || 0),
        backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#6366f1', '#ec4899', '#14b8a6', '#94a3b8']
      }]
    };

    // Bar Chart
    let totalActive = 0;
    let totalInactive = 0;

    const traverseAndCount = (nodeList: TreeNode[]) => {
      for (const node of nodeList) {
        if ((node.data.timesUsed || 0) > 0) {
          totalActive++;
        } else {
          totalInactive++;
        }

        // If there are children, descend recursively
        if (node.children && node.children.length > 0) {
          traverseAndCount(node.children);
        }
      }
    };

    // Start the count with the root nodes
    traverseAndCount(nodes);

    this.barData = {
      labels: ['Sin uso', 'En uso'],
      datasets: [{
        label: 'Total de Categorías',
        data: [totalInactive, totalActive],
        backgroundColor: ['#ef4444', '#22c55e'], // red-500, green-500
        borderRadius: 6,
        barPercentage: 0.6
      }]
    };
  }
}
