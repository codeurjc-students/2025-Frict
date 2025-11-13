import {Component, inject, OnInit} from '@angular/core';
import {NavbarComponent} from "../navbar/navbar.component";
import {FooterComponent} from '../footer/footer.component';
import {ActivatedRoute} from '@angular/router';
import {ProductsPage} from '../../../models/productsPage.model';
import {ProductService} from '../../../services/product.service';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {NgForOf, NgIf} from '@angular/common';
import {ProductCardComponent} from '../../client/product-card/product-card.component';
import {LoadingComponent} from '../loading/loading.component';
import {Select} from 'primeng/select';
import {Button} from 'primeng/button';
import {FormsModule} from '@angular/forms';
import {Checkbox} from 'primeng/checkbox';
import {CategoryService} from '../../../services/category.service';
import {Drawer} from 'primeng/drawer';

interface SortOption {
  name: string; // Etiqueta que se muestra
  value: string; // Valor que se usa para ordenar (ej: 'relevance,desc')
}

@Component({
  selector: 'app-search',
  imports: [
    NavbarComponent,
    FooterComponent,
    Paginator,
    NgIf,
    ProductCardComponent,
    LoadingComponent,
    NgForOf,
    Select,
    FormsModule,
    Button,
    Checkbox,
    Drawer
  ],
  templateUrl: './search.component.html',
  standalone: true,
  styleUrl: './search.component.css'
})
export class SearchComponent implements OnInit {

  constructor(private productService: ProductService,
              private categoryService: CategoryService) {}

  private route = inject(ActivatedRoute);
  searchQuery: string | null = null; //Needs to be null in order to be assigned directly by route object
  foundProducts : ProductsPage = {products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 0};

  sortOptions: SortOption[] = [
    { name: 'Nombre', value: 'name,asc' },
    { name: 'Precio: Menor a Mayor', value: 'currentPrice,asc' },
    { name: 'Precio: Mayor a Menor', value: 'currentPrice,desc' }
    //In order to be able to filter by rating, as only ProductDTO objects include it after being processed, the sorting should be done in the frontend for the moment
  ];
  selectedSortOption: SortOption = this.sortOptions[0];
  visibleDrawer: boolean = false;

  categories: any[] = [];
  selectedCategories: any[] = [];

  first: number = 0;
  rows: number = 10;

  loading: boolean = true;
  error: boolean = false;

  ngOnInit(): void {
    this.selectedSortOption = this.sortOptions[0];
    this.route.queryParamMap.subscribe(params => {
      this.searchQuery = params.get('query');
      this.loadProducts();
    });
    this.categoryService.getAllCategories().subscribe({
      next: (c) => {
        this.categories = c.categories;
      }
    })
  }

  onSortChange() {
    console.log(this.selectedSortOption.value);
    this.loadProducts();
  }

  onPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadProducts();
  }

  onCategoryChange(event: any) {
    this.loadProducts();
  }

  loadProducts() {
    this.loading = true;
    this.productService.getFilteredProducts(this.first/this.rows, this.rows, this.searchQuery ?? '', this.selectedCategories.map(category => category.id), this.selectedSortOption.value).subscribe({
      next: (page) => {
        this.foundProducts = page;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }
}
