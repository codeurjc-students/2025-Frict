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
  similarCategories: Category[] = []; //Main category siblings

  loading: boolean = true;
  error: boolean = false;

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
    this.route.params.subscribe(() => { //If a related product is clicked when visualizing a product, the page should refresh the information
      this.loadMainCategory();
      if (typeof window !== 'undefined') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  loadMainCategory() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.categoryService.getCategoryById(id).subscribe({
        next: (category) => {
          this.mainCategory = category;
          this.loadSimilarCategories();
          this.loadTopSalesProducts();
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      })
    }
  }

  loadSimilarCategories(){
    if(this.mainCategory.parentId){
      this.categoryService.getCategoryById(this.mainCategory.parentId).subscribe({
        next: (category) => {
          this.similarCategories = category.children.filter(c => c.id !== this.mainCategory.id);
          console.log(this.similarCategories);
        }
      })
    }
    else{
      this.categoryService.getAllCategories().subscribe({
        next: (c) => {
          this.similarCategories = c.categories.filter(c => c.id !== this.mainCategory.id);
        }
      })
    }
  }

  loadTopSalesProducts(){
    this.productService.getProductsByCategoryName(this.mainCategory.name).subscribe({
      next: (page) => {
        this.topSalesProducts = page.products;
        this.loading = false;
      }
    })
  }
}
