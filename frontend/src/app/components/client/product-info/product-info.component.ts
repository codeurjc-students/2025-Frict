import {Component, OnInit} from '@angular/core';
import {FooterComponent} from '../../common/footer/footer.component';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {GalleriaModule} from 'primeng/galleria';
import {carouselResponsiveOptions, galleryResponsiveOptions} from '../../../app.config';
import {Product} from '../../../models/product.model';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {FormsModule} from '@angular/forms';
import {InputNumber} from 'primeng/inputnumber';
import {Button} from 'primeng/button';
import {Carousel} from 'primeng/carousel';
import {LoadingComponent} from '../../common/loading/loading.component';
import {ProductCardComponent} from '../product-card/product-card.component';
import {ActivatedRoute} from '@angular/router';
import {Breadcrumb} from 'primeng/breadcrumb';
import {MenuItem} from 'primeng/api';
import {ProductService} from '../../../services/product.service';
import {formatPrice} from '../../../utils/numberFormat.util';
import {CategoryService} from '../../../services/category.service';


@Component({
  selector: 'app-product-info',
  standalone: true,
  imports: [
    FooterComponent,
    NavbarComponent,
    GalleriaModule,
    Tabs,
    TabList,
    TabPanels,
    Tab,
    TabPanel,
    FormsModule,
    InputNumber,
    Button,
    Carousel,
    LoadingComponent,
    ProductCardComponent,
    Breadcrumb
  ],
  templateUrl: './product-info.component.html'
})
export class ProductInfoComponent implements OnInit {

  images: any[] = [];

  protected readonly galleryResponsiveOptions = galleryResponsiveOptions;
  protected readonly carouselResponsiveOptions = carouselResponsiveOptions;

  protected quantity: number = 1;

  protected loading: boolean = true;
  protected error: boolean = false;
  protected categoryProducts: Product[] = [];

  previousPages: any[] = [{ icon: 'pi pi-home', route: '/installation' }, { label: 'Buscar' }, { label: 'Ordenadores', route: '/inputtext' }];
  homePage: MenuItem | undefined;

  product!: Product;

  constructor(private productService: ProductService,
              private route: ActivatedRoute) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productService.getProductById(id).subscribe({
        next: (product) => {
          this.product = product;
          this.images.push({itemImageSrc: product.imageUrl,
                            thumbnailImageSrc: product.imageUrl
          });
        }
      });
    }


  }

  protected readonly formatPrice = formatPrice;
}
