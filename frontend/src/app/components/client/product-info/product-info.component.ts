import {Component, OnInit, inject, LOCALE_ID} from '@angular/core';
import {GalleriaModule} from 'primeng/galleria';
import {carouselResponsiveOptions, galleryResponsiveOptions} from '../../../app.config';
import {Product} from '../../../models/product.model';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {FormsModule} from '@angular/forms';
import {InputNumber} from 'primeng/inputnumber';
import {Button} from 'primeng/button';
import {Carousel} from 'primeng/carousel';
import {LoadingSectionComponent} from '../../common/loading-section/loading-section.component';
import {ProductCardComponent} from '../product-card/product-card.component';
import {ActivatedRoute, Router} from '@angular/router';
import {MessageService} from 'primeng/api';
import {ProductService} from '../../../services/product.service';
import {formatPrice} from '../../../utils/textFormat.util';
import {CategoryService} from '../../../services/category.service';
import {Category} from '../../../models/category.model';
import {Dialog} from 'primeng/dialog';
import {Avatar} from 'primeng/avatar';
import {Rating} from 'primeng/rating';
import {MeterGroupModule} from 'primeng/metergroup';
import {ReviewService} from '../../../services/review.service';
import {Review} from '../../../models/review.model';
import {AuthService} from '../../../services/auth.service';
import {LoginInfo} from '../../../models/loginInfo.model';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {Textarea} from 'primeng/textarea';
import {TableModule} from 'primeng/table';
import {ShopStock} from '../../../models/shopStock.model';
import {OrderService} from '../../../services/order.service';
import {HttpErrorResponse} from '@angular/common/http';
import {Image} from 'primeng/image';
import {Shop} from '../../../models/shop.model';
import {ShopService} from '../../../services/shop.service';
import {StockTagComponent} from '../../common/stock-tag/stock-tag.component';
import {BreadcrumbReloadComponent} from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {Tag} from 'primeng/tag';
import {ChartModule} from 'primeng/chart';
import {Select} from 'primeng/select';
import {RegistryService} from '../../../services/registry.service';
import {formatDate} from '@angular/common';
import {DatePicker} from 'primeng/datepicker';


@Component({
  selector: 'app-product-info',
  standalone: true,
  imports: [
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
    LoadingSectionComponent,
    ProductCardComponent,
    Dialog,
    Avatar,
    Rating,
    MeterGroupModule,
    LoadingScreenComponent,
    Textarea,
    TableModule,
    Image,
    StockTagComponent,
    BreadcrumbReloadComponent,
    Tag,
    ChartModule,
    Select,
    DatePicker
  ],
  templateUrl: './product-info.component.html'
})
export class ProductInfoComponent implements OnInit {

  protected readonly galleryResponsiveOptions = galleryResponsiveOptions;
  protected readonly carouselResponsiveOptions = carouselResponsiveOptions;
  protected readonly formatPrice = formatPrice;
  protected readonly Math = Math;

  protected images: any[] = [];

  protected quantity: number = 1;

  protected loading: boolean = true;
  protected error: boolean = false;

  protected relatedLoading: boolean = true;
  protected relatedError: boolean = false;
  protected relatedProducts: Product[] = [];

  protected product!: Product;
  protected inCartUnits: number = 0;
  protected inFavourites: boolean = false;
  protected productCategory!: Category;

  protected visibleShippingDialog: boolean = false;
  protected visibleAvailabilityDialog: boolean = false;

  protected stars: any[] = [];
  protected recommendedCount: number = 0;
  protected recommendationPercentage: number = 0;
  protected productReviews: Review[] = [];

  protected userReviewed: boolean = false;
  protected newReview: Partial<Review> = { rating: 5, recommended: true };

  protected selectedShop: Shop | null = null;
  protected stocks: ShopStock[] = [];

  protected loggedUserInfo!: LoginInfo;

  private registryService = inject(RegistryService);
  private locale = inject(LOCALE_ID);

  protected viewsToday: number = 0;
  protected isViewsLoading: boolean = false;

  protected viewsEndDate: Date = new Date();
  protected viewsStartDate: Date = new Date(new Date().setMonth(new Date().getMonth() - 12));

  protected selectedViewInterval = 'week';
  protected viewIntervalOptions = [
    { label: 'Diario', value: 'day' },
    { label: 'Semanal', value: 'week' },
    { label: 'Mensual', value: 'month' },
    { label: 'Anual', value: 'year' }
  ];
  protected viewsChartData: any;
  protected viewsChartOptions: any;

  constructor(private productService: ProductService,
              private categoryService: CategoryService,
              private reviewService: ReviewService,
              private orderService: OrderService,
              private shopService: ShopService,
              protected authService: AuthService,
              private route: ActivatedRoute,
              private router: Router,
              private messageService: MessageService,
              private breadcrumbService: BreadcrumbService) {}

  ngOnInit() {
    this.route.params.subscribe(() => {
      this.loadProduct();
    });
  }

  protected submitReview(){
    this.newReview.productId = this.product.id;
    this.newReview.creatorId = this.loggedUserInfo.id;
    this.reviewService.submitReview(this.newReview).subscribe({
      next: () => {
        this.userReviewed = true;
        this.loadReviews();
      }
    })
  }

  protected editReview(id: string) {
    const publishedReview = this.productReviews.find(r => r.id === id);
    if(publishedReview){
      this.newReview = { ...publishedReview };
      this.productReviews = this.productReviews.filter(r => r.id !== id);
      this.userReviewed = false;
    }
  }

  protected deleteReview(id: string) {
    this.reviewService.deleteReviewById(id).subscribe({
      next: () => {
        this.newReview = {};
        this.userReviewed = false;
        this.loadReviews();
      }
    })
  }

  protected setRecommendation(b: boolean) {
    this.newReview.recommended = b;
  }

  protected loadProduct() {
    this.loading = true;
    this.error = false;

    const id = this.route.snapshot.paramMap.get('id');
    const navState = history.state;

    if (id) {
      this.productService.getProductById(id).subscribe({
        next: (product) => {
          this.product = product;

          const currentUrl = this.router.url;

          this.breadcrumbService.setNodesForUrl(currentUrl, [{ label: product.name }]);

          if (navState.from === 'search') {
            this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
              { label: 'Búsqueda', routerLink: '/search' }
            ]);
          } else if (navState.from === 'category' && navState.categoryName) {
            this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
              { label: navState.categoryName, routerLink: `/category/${navState.categoryId}` }
            ]);
          } else if (navState.from === 'products-management') {
            this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
              { label: 'Gestor de productos', routerLink: `/admin/products` }
            ]);
          } else {
            if (product.categories && product.categories.length > 0) {
              this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
                { label: product.categories[0].name, routerLink: `/category/${product.categories[0].id}` }
              ]);
            } else {
              this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, []);
            }
          }

          if (product.imagesInfo && Array.isArray(product.imagesInfo)) {
            this.images = product.imagesInfo.map((imgInfo) => ({
              itemImageSrc: imgInfo.imageUrl
            }));
          } else {
            this.images = [];
          }

          if (typeof window !== 'undefined') {
            window.scrollTo({ top: 0, behavior: 'instant' });
          }

          if(this.authService.selectedShopId()){
            this.loadSelectedShop();
          }

          this.loadCartItemUnits();
          this.loadProductCategory();
          this.checkInFavourites();
          this.loadShopStocks();
          this.loadReviews();

          // --- Load analytics ---
          this.loadTodayViews();
          this.loadViewsData();
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      });
    }
  }

  // --- VIEWS CHART VISUALIZATION METHODS ---
  protected loadTodayViews() {
    const today = new Date();
    const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0);
    const endOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59);

    this.registryService.loadPublicRegistry({
      startDate: startOfToday.toISOString(),
      endDate: endOfToday.toISOString(),
      entityType: 'PRODUCT',
      dataType: 'PRODUCT_VIEWS',
      metricMode: 'VALUE',
      productIds: [this.product.referenceCode],
      viewType: 'GRAPH',
      interval: 'day'
    }).subscribe({
      next: (res: any) => {
        const data = res.items || res;
        this.viewsToday = data && data.length > 0 ? data[0].totalValue : 0;
      }
    });
  }

  protected loadViewsData() {
    this.isViewsLoading = true;

    this.registryService.loadPublicRegistry({
      startDate: this.viewsStartDate.toISOString(),
      endDate: this.viewsEndDate.toISOString(),
      entityType: 'PRODUCT',
      dataType: 'PRODUCT_VIEWS',
      metricMode: 'VALUE',
      productIds: [this.product.referenceCode],
      viewType: 'GRAPH',
      interval: this.selectedViewInterval
    }).subscribe({
      next: (res: any) => {
        console.log(res);
        const rawData = res.items || res;
        this.buildViewsChart(rawData);
        this.isViewsLoading = false;
      },
      error: () => {
        this.isViewsLoading = false;
      }
    });
  }

  private buildViewsChart(rawData: any[]) {
    const labels = rawData.map(item => formatDate(item._id, 'dd MMM yyyy', this.locale));
    const dataValues = rawData.map(item => item.totalValue);

    this.viewsChartData = {
      labels: labels,
      datasets: [
        {
          label: 'Visualizaciones',
          data: dataValues,
          backgroundColor: 'rgba(59, 130, 246, 0.1)',
          borderColor: 'rgb(59, 130, 246)',
          borderWidth: 2,
          fill: true,
          tension: 0.4
        }
      ]
    };

    this.viewsChartOptions = {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: {
        legend: { display: false },
        tooltip: { mode: 'index', intersect: false }
      },
      scales: {
        x: { display: true },
        y: { display: true, beginAtZero: true }
      }
    };
  }


  protected loadCartItemUnits(){
    this.orderService.getCartItemByProductId(this.product.id).subscribe({
      next: (item) => {
        if (item){
          this.inCartUnits = item.quantity;
        }
      }
    })
  }

  protected loadSelectedShop(){
    const selectedShopId = this.authService.selectedShopId();
    if(selectedShopId){
      this.shopService.getShopById(selectedShopId).subscribe({
        next: (shop) => {
          this.selectedShop = shop;
        }
      })
    }
  }

  protected loadProductCategory() {
    this.categoryService.getCategoryById(this.product.categories[0].id).subscribe({
      next: (category) => {
        this.productCategory = category;
        this.loadRelatedProducts();
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  protected loadRelatedProducts() {
    const currentProductId = this.route.snapshot.paramMap.get('id');

    this.productService.getRecommendedProducts(10).subscribe({
      next: (pageResponse) => {
        this.relatedProducts = pageResponse.items.filter((p: Product) => p.id.toString() !== currentProductId?.toString());
        this.relatedLoading = false;
      },
      error: (err) => {
        this.relatedLoading = false;
        this.relatedError = true;
      }
    });
  }

  protected showShippingDialog() {
    this.visibleShippingDialog = true;
  }

  protected showAvailabilityDialog() {
    this.visibleAvailabilityDialog = true;
  }


  protected loadReviews() {
    this.reviewService.getReviewsByProductId(this.product.id).subscribe({
      next: (reviews) => {
        this.productReviews = reviews;
        const stars = [
          { value: 5, count: 0 },
          { value: 4, count: 0 },
          { value: 3, count: 0 },
          { value: 2, count: 0 },
          { value: 1, count: 0 },
        ];

        this.product.totalReviews = this.productReviews.length;
        this.product.averageRating = this.productReviews.length
          ? this.productReviews.reduce((acc, r) => acc + r.rating, 0) / this.productReviews.length
          : 0;
        this.productReviews.forEach((review: any) => {
          const star = stars.find(s => s.value === review.rating);
          if (star) {
            star.count += 1;
          }
        });

        this.stars = stars;

        if(this.productReviews.length > 0){
          this.recommendedCount = this.productReviews.filter(r => r.recommended).length;
          this.recommendationPercentage = (this.recommendedCount / this.productReviews.length) * 100;
        }

        this.authService.getLoginInfo().subscribe({
          next: (loginInfo) => {
            this.userReviewed = this.productReviews.some(r => r.creatorId === loginInfo.id)
            this.loggedUserInfo = loginInfo;
            this.loading = false;
          }
        })
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  protected loadShopStocks() {
    this.productService.getStockByProductId(this.product.id).subscribe({
      next: (s) => {
        this.stocks = s;
      }
    })
  }

  protected addItemToCart() {
    if(!this.authService.isLogged()){
      this.router.navigate(['/login']);
    }

    if(this.quantity == 0){
      this.messageService.add({ severity: 'info', summary: 'Cantidad no válida', detail: 'Introduce una cantidad válida' });
    }
    else{
      this.orderService.addItemToCart(this.product.id, this.quantity).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Producto añadido correctamente al carrito' });
          this.orderService.incrementItemsCount(this.quantity);
          if (this.product.availableUnits - this.inCartUnits == 0){
            this.quantity = 0;
          }
          this.loadCartItemUnits();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 405){
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No hay suficiente stock disponible' });
          }
          else{
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error añadiendo el producto al carrito' });
          }
        }
      })
    }
  }

  protected checkInFavourites() {
    this.productService.checkInFavourites(this.product.id).subscribe({
      next: (state) => {
        this.inFavourites = state;
      }
    })
  }

  protected toggleInFavourites() {
    if(!this.authService.isLogged()){
      this.router.navigate(['/login']);
    }

    if(!this.inFavourites){
      this.productService.addProductToFavourites(this.product.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'info', summary: 'Añadido', detail: 'Producto añadido a tus favoritos' });
          this.inFavourites = true;
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error añadiendo el producto a tus favoritos' });
        }
      })
    }
    else{
      this.productService.deleteProductFromFavourites(this.product.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'info', summary: 'Añadido', detail: 'Producto eliminado de tus favoritos' });
          this.inFavourites = false;
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error eliminando el producto de tus favoritos' });
        }
      })
    }

  }
}
