import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ActivatedRoute, RouterModule} from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { RatingModule } from 'primeng/rating';
import { FormsModule } from '@angular/forms';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {FooterComponent} from '../../common/footer/footer.component';
import {ProductCardComponent} from '../product-card/product-card.component';
import {Product} from '../../../models/product.model';
import {CategoryService} from '../../../services/category.service';
import {ProductService} from '../../../services/product.service';
import {Category} from '../../../models/category.model';
import {Breadcrumb} from 'primeng/breadcrumb';
import {MenuItem} from 'primeng/api';

@Component({
  selector: 'app-category-info',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    TagModule,
    RatingModule,
    FormsModule,
    NavbarComponent,
    FooterComponent,
    ProductCardComponent,
    Breadcrumb
  ],
  templateUrl: './category-info.component.html',
  styleUrl: './category-info.component.css'
})
export class CategoryInfoComponent {

  breadcrumbItems: MenuItem[] | undefined = [{ icon: 'pi pi-home', route: '/installation' }, { label: 'Components' }, { label: 'Form' }, { label: 'InputText', route: '/inputtext' }];
  home: MenuItem | undefined;

  topSalesProducts: Product[] = []; //Top sales products of this main category
  mainCategory!: Category;

  loading: boolean = true;
  error: boolean = false;

  buyingGuides = [
    { title: '¿Cámara para interior?', image: 'http://localhost:9000/images/categories/ed6acdb5-02f0-451d-964c-1d5f6fae49c2_defaultCategoryImage.jpg', subtitle: 'Vigila tu hogar desde dentro' },
    { title: '¿Cámara para exterior?', image: 'http://localhost:9000/images/categories/ed6acdb5-02f0-451d-964c-1d5f6fae49c2_defaultCategoryImage.jpg', subtitle: 'Resistentes al agua y clima' },
    { title: 'Kits de Videovigilancia', image: 'http://localhost:9000/images/categories/ed6acdb5-02f0-451d-964c-1d5f6fae49c2_defaultCategoryImage.jpg', subtitle: 'Seguridad completa' },
    { title: 'Videoporteros Smart', image: 'http://localhost:9000/images/categories/ed6acdb5-02f0-451d-964c-1d5f6fae49c2_defaultCategoryImage.jpg', subtitle: 'Mira quién llama' }
  ];

  useCases = [
    { title: 'Seguridad para tu Negocio', icon: 'pi pi-briefcase' },
    { title: 'Vigila a tu Mascota', icon: 'pi pi-heart' },
    { title: 'Monitor de Bebés', icon: 'pi pi-users' },
    { title: 'Segunda Residencia', icon: 'pi pi-home' }
  ];

  constructor(private categoryService: CategoryService,
              private productService: ProductService,
              private route: ActivatedRoute) {
  }

  ngOnInit() {
    this.loadMainCategory();
  }

  loadMainCategory() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.categoryService.getCategoryById(id).subscribe({
        next: (category) => {
          this.mainCategory = category;
          this.loadTopSalesProducts();
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      })
    }
  }

  loadTopSalesProducts(){
    this.productService.getProductsByCategoryName(this.mainCategory.name).subscribe({
      next: (page) => {
        this.topSalesProducts = page.products;
      }
    })
  }
}
