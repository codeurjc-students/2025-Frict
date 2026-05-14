import {Component, inject, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ProductService} from '../../../services/product.service';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {ProductCardComponent} from '../../client/product-card/product-card.component';
import {Select} from 'primeng/select';
import {MultiSelect} from 'primeng/multiselect';
import {Button} from 'primeng/button';
import {FormsModule} from '@angular/forms';
import {CategoryService} from '../../../services/category.service';
import {Drawer} from 'primeng/drawer';
import {PrimeTemplate, TreeNode} from 'primeng/api';
import {Tree} from 'primeng/tree';
import {PageResponse} from '../../../models/pageResponse.model';
import {Product} from '../../../models/product.model';
import {ProductSpec} from '../../../models/product-spec.model';
import {mapToTreeNodes} from '../../../utils/nodeMapper.util';
import {BreadcrumbReloadComponent} from '../breadcrumb-reload/breadcrumb-reload.component';

interface SortOption {
  name: string;
  value: string;
}

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    Paginator,
    ProductCardComponent,
    Select,
    MultiSelect,
    FormsModule,
    Button,
    Drawer,
    PrimeTemplate,
    Tree,
    BreadcrumbReloadComponent
  ],
  templateUrl: './search.component.html',
  styleUrl: './search.component.css'
})
export class SearchComponent implements OnInit {

  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  searchQuery: string | null = null;
  foundProducts: PageResponse<Product> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0 };

  sortOptions: SortOption[] = [
    { name: 'Nombre', value: 'name,asc' },
    { name: 'Precio: Menor a Mayor', value: 'currentPrice,asc' },
    { name: 'Precio: Mayor a Menor', value: 'currentPrice,desc' }
  ];
  selectedSortOption: SortOption = this.sortOptions[0];

  categories: TreeNode[] = [];
  selectedCategories: TreeNode[] = [];

  allSpecs: Record<string, string[]> = {};
  activeSpecFilters: ProductSpec[] = [];
  pendingSpecName: string | null = null;
  pendingSpecValues: string[] = [];
  availableSpecValues: string[] = [];

  get specNameOptions(): string[] { return Object.keys(this.allSpecs); }

  first: number = 0;
  rows: number = 10;

  visibleDrawer: boolean = false;
  loading: boolean = true;
  error: boolean = false;

  ngOnInit(): void {
    this.productService.getSpecsCatalog().subscribe(s => this.allSpecs = s);
    // Le decimos que SÍ es la carga inicial
    this.getAllCategories(true);
  }

  public reloadAll() {
    this.loading = true;
    this.error = false;
    this.categories = [];
    this.foundProducts.items = [];

    this.getAllCategories(false);
  }

  protected getAllCategories(isInitialLoad: boolean = false) {
    this.categoryService.getAllCategories().subscribe({
      next: (response) => {
        const rawCategories = response || [];
        this.categories = mapToTreeNodes(rawCategories);

        if (isInitialLoad) {
          this.route.queryParamMap.subscribe(params => {
            this.syncStateWithUrl(params);
          });
        } else {
          this.syncStateWithUrl(this.route.snapshot.queryParamMap);
        }
      },
      error: (err) => {
        console.error('Error cargando categorías', err);

        if (isInitialLoad) {
          this.route.queryParamMap.subscribe(params => this.syncStateWithUrl(params));
        } else {
          this.loading = false;
          this.error = true;
        }
      }
    });
  }

  private findNodesByIds(nodes: TreeNode[], ids: string[]): TreeNode[] {
    let found: TreeNode[] = [];
    for (const node of nodes) {
      if (node.data && ids.includes(node.data.toString())) {
        found.push(node);
      }

      if (node.children && node.children.length > 0) {
        found = found.concat(this.findNodesByIds(node.children, ids));
      }
    }
    return found;
  }


  private syncStateWithUrl(params: any) {
    this.searchQuery = params.get('query');

    const sortVal = params.get('sort');
    if (sortVal) {
      const foundSort = this.sortOptions.find(s => s.value === sortVal);
      this.selectedSortOption = foundSort || this.sortOptions[0];
    } else {
      this.selectedSortOption = this.sortOptions[0];
    }

    const page = parseInt(params.get('page') ?? '0');
    const size = parseInt(params.get('size') ?? '10');
    this.first = page * size;
    this.rows = size;

    const rawCategories = params.get('categories');
    const urlCategoryIds = rawCategories ? rawCategories.split(',') : [];

    if (this.categories.length > 0 && urlCategoryIds.length > 0) {
      this.selectedCategories = this.findNodesByIds(this.categories, urlCategoryIds);
    } else {
      this.selectedCategories = [];
    }

    const rawSpecFilters: string[] = params.getAll('specFilter');
    this.activeSpecFilters = rawSpecFilters
      .filter(s => s.includes(':'))
      .map(s => {
        const [name, valsPart] = s.split(':', 2);
        return { name, values: valsPart ? valsPart.split(',') : [] };
      })
      .filter(f => f.values.length > 0);

    this.loadProducts();
  }

  onCategoryChange(event: any) {
    this.first = 0;
    this.updateQueryParams({ page: 0 }, event);
  }

  onSortChange() {
    this.updateQueryParams({ page: 0 });
  }

  onPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    const nextPage = this.first / this.rows;
    this.updateQueryParams({ page: nextPage, size: this.rows });
  }

  private updateQueryParams(extraParams: any = {}, overrideCategories: TreeNode[] | null = null) {
    const categoriesToUse = overrideCategories ?? this.selectedCategories ?? [];

    const categoryIds = categoriesToUse.map(node => node.data);
    const specFilterParams = this.activeSpecFilters.length > 0
      ? this.activeSpecFilters.map(f => `${f.name}:${f.values.join(',')}`)
      : null;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        query: this.searchQuery || null,
        sort: this.selectedSortOption.value,
        categories: categoryIds.length > 0 ? categoryIds.join(',') : null,
        specFilter: specFilterParams,
        page: (this.first / this.rows),
        size: this.rows,
        ...extraParams
      },
      queryParamsHandling: 'merge',
    });
  }

  onPendingSpecNameChange() {
    this.pendingSpecValues = [];
    this.availableSpecValues = this.allSpecs[this.pendingSpecName ?? ''] ?? [];
  }

  addSpecFilter() {
    if (!this.pendingSpecName || !this.pendingSpecValues.length) return;
    const existing = this.activeSpecFilters.find(f => f.name === this.pendingSpecName);
    if (existing) {
      existing.values = [...new Set([...existing.values, ...this.pendingSpecValues])];
    } else {
      this.activeSpecFilters.push({ name: this.pendingSpecName, values: [...this.pendingSpecValues] });
    }
    this.pendingSpecName = null;
    this.pendingSpecValues = [];
    this.first = 0;
    this.updateQueryParams({ page: 0 });
  }

  removeSpecFilter(name: string) {
    this.activeSpecFilters = this.activeSpecFilters.filter(f => f.name !== name);
    this.first = 0;
    this.updateQueryParams({ page: 0 });
  }

  loadProducts() {
    this.loading = true;

    const currentSelection = this.selectedCategories || [];
    const idsToSend = currentSelection.map(node => node.data);

    this.productService.getFilteredProducts(
      this.first / this.rows,
      this.rows,
      this.searchQuery ?? '',
      idsToSend,
      this.selectedSortOption.value,
      this.activeSpecFilters
    ).subscribe({
      next: (page) => {
        this.foundProducts = page;
        this.loading = false;
        if (typeof window !== 'undefined') {
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }
}
