import { Component, inject, OnInit } from '@angular/core';
import { NavbarComponent } from "../navbar/navbar.component";
import { FooterComponent } from '../footer/footer.component';
import { ActivatedRoute, Router } from '@angular/router'; // Importamos Router
import { ProductsPage } from '../../../models/productsPage.model';
import { ProductService } from '../../../services/product.service';
import { Paginator, PaginatorState } from 'primeng/paginator';
import { NgForOf, NgIf } from '@angular/common';
import { ProductCardComponent } from '../../client/product-card/product-card.component';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import { Checkbox } from 'primeng/checkbox';
import { CategoryService } from '../../../services/category.service';
import { Drawer } from 'primeng/drawer';
import { PrimeTemplate } from 'primeng/api';

interface SortOption {
  name: string;
  value: string;
}

@Component({
  selector: 'app-search',
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
    Checkbox,
    Drawer,
    PrimeTemplate
  ],
  templateUrl: './search.component.html',
  standalone: true,
  styleUrl: './search.component.css'
})
export class SearchComponent implements OnInit {

  constructor(private productService: ProductService,
              private categoryService: CategoryService,
              private router: Router,
              private route: ActivatedRoute) {
  }

  searchQuery: string | null = null;
  foundProducts: ProductsPage = { products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 0 };

  sortOptions: SortOption[] = [
    { name: 'Nombre', value: 'name,asc' },
    { name: 'Precio: Menor a Mayor', value: 'currentPrice,asc' },
    { name: 'Precio: Mayor a Menor', value: 'currentPrice,desc' }
  ];
  selectedSortOption: SortOption = this.sortOptions[0];

  visibleDrawer: boolean = false;

  categories: any[] = [];
  selectedCategories: any[] = []; // Synchronized with URL

  first: number = 0;
  rows: number = 10;

  loading: boolean = true;
  error: boolean = false;

  ngOnInit(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (c) => {
        this.categories = c.categories;

        //Subscribe to URL changes and refresh page state
        this.route.queryParamMap.subscribe(params => {
          this.syncStateWithUrl(params);
        });
      },
      error: () => {
        this.route.queryParamMap.subscribe(params => this.syncStateWithUrl(params));
      }
    });
  }

  //Sync logic (page <-> URL)
  private syncStateWithUrl(params: any) {
    this.searchQuery = params.get('query');

    const sortVal = params.get('sort');
    if (sortVal) {
      const foundSort = this.sortOptions.find(s => s.value === sortVal);
      if (foundSort) this.selectedSortOption = foundSort;
    } else {
      this.selectedSortOption = this.sortOptions[0];
    }

    const page = parseInt(params.get('page') ?? '0');
    const size = parseInt(params.get('size') ?? '10');
    this.first = page * size;
    this.rows = size;

    // Convert the clean string into array
    const rawCategories = params.get('categories');
    const urlCategoryIds = rawCategories ? rawCategories.split(',') : []; // Ex: ["14", "1", "5"]

    if (this.categories.length > 0 && urlCategoryIds.length > 0) {
      this.selectedCategories = this.categories.filter(cat =>
        urlCategoryIds.includes(cat.id.toString()) //Ignores unexistent category ids
      );
    } else {
      this.selectedCategories = [];
    }

    this.loadProducts();
  }

  onSortChange() {
    this.updateQueryParams({ page: 0 }); // Return to page 0 on sort option change
  }

  onPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    const nextPage = this.first / this.rows;
    this.updateQueryParams({ page: nextPage, size: this.rows });
  }

  onCategoryChange(event: any) {
    this.first = 0;
    this.updateQueryParams({ page: 0 }); // Reset to page 0 on any category change
  }

  private updateQueryParams(extraParams: any = {}) {
    // Extract category ids
    const categoryIds = this.selectedCategories.map(c => c.id);

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        query: this.searchQuery || null,
        sort: this.selectedSortOption.value,
        categories: categoryIds.length > 0 ? categoryIds.join(',') : null, // Join with commas for more than one category (clean URL)
        page: (this.first / this.rows),
        size: this.rows,
        ...extraParams
      },
      queryParamsHandling: 'merge',
    });
  }

  loadProducts() {
    this.loading = true;
    this.productService.getFilteredProducts(
      this.first / this.rows,
      this.rows,
      this.searchQuery ?? '',
      this.selectedCategories.map(category => category.id),
      this.selectedSortOption.value
    ).subscribe({
      next: (page) => {
        this.foundProducts = page;
        this.loading = false;
        // Light scroll to the top of the page on page change
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
