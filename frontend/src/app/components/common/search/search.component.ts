import { Component, OnInit } from '@angular/core';
import { NavbarComponent } from "../navbar/navbar.component";
import { FooterComponent } from '../footer/footer.component';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductsPage } from '../../../models/productsPage.model';
import { ProductService } from '../../../services/product.service';
import { Paginator, PaginatorState } from 'primeng/paginator';
import { NgForOf, NgIf } from '@angular/common';
import { ProductCardComponent } from '../../client/product-card/product-card.component';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import { CategoryService } from '../../../services/category.service';
import { Drawer } from 'primeng/drawer';
import { TreeNode, PrimeTemplate } from 'primeng/api';
import { Tree } from 'primeng/tree';

interface SortOption {
  name: string;
  value: string;
}

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    NavbarComponent,
    FooterComponent,
    Paginator,
    NgIf,
    ProductCardComponent,
    NgForOf,
    Select,
    FormsModule,
    Button,
    Drawer,
    PrimeTemplate,
    Tree
  ],
  templateUrl: './search.component.html',
  styleUrl: './search.component.css'
})
export class SearchComponent implements OnInit {

  searchQuery: string | null = null;
  foundProducts: ProductsPage = { products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 0 };

  sortOptions: SortOption[] = [
    { name: 'Nombre', value: 'name,asc' },
    { name: 'Precio: Menor a Mayor', value: 'currentPrice,asc' },
    { name: 'Precio: Mayor a Menor', value: 'currentPrice,desc' }
  ];
  selectedSortOption: SortOption = this.sortOptions[0];

  categories: TreeNode[] = [];
  selectedCategories: TreeNode[] = [];

  first: number = 0;
  rows: number = 10;

  visibleDrawer: boolean = false;
  loading: boolean = true;
  error: boolean = false;

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  printCategories(){
    console.log(this.categories);
  }

  ngOnInit(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (response) => {
        const rawCategories = response.categories || [];
        this.categories = this.mapToTreeNodes(rawCategories);

        this.route.queryParamMap.subscribe(params => {
          this.syncStateWithUrl(params);
        });
      },
      error: (err) => {
        console.error('Error cargando categorías', err);
        this.route.queryParamMap.subscribe(params => this.syncStateWithUrl(params));
      }
    });
  }


  private mapToTreeNodes(categories: any[], parentKey: string | null = null): TreeNode[] {
    return categories.map((cat, index) => {
      const currentKey = parentKey !== null ? `${parentKey}-${index}` : `${index}`;
      return {
        key: currentKey,
        label: cat.name,
        data: cat.id,
        children: cat.children ? this.mapToTreeNodes(cat.children, currentKey) : [],
      };
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

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        query: this.searchQuery || null,
        sort: this.selectedSortOption.value,
        categories: categoryIds.length > 0 ? categoryIds.join(',') : null,
        page: (this.first / this.rows),
        size: this.rows,
        ...extraParams
      },
      queryParamsHandling: 'merge',
    });
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
      this.selectedSortOption.value
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
