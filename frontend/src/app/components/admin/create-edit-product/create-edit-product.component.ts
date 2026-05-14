import {Component, inject, OnInit, signal} from '@angular/core';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';

import {InputText} from 'primeng/inputtext';
import {InputNumber} from 'primeng/inputnumber';
import {Button} from 'primeng/button';
import {Editor} from 'primeng/editor';
import {FileUpload} from 'primeng/fileupload';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {MessageService, TreeNode} from 'primeng/api';
import {TreeSelect} from 'primeng/treeselect';
import {AutoComplete} from 'primeng/autocomplete';
import {DomSanitizer} from '@angular/platform-browser';

import {ProductService} from '../../../services/product.service';
import {CategoryService} from '../../../services/category.service';
import {fixKeys, mapToCategories, mapToTreeNodes} from '../../../utils/nodeMapper.util';
import {formatPrice} from '../../../utils/textFormat.util';
import {Product} from '../../../models/product.model';
import {ProductSpec} from '../../../models/product-spec.model';
import {ImageInfo} from '../../../models/imageInfo.model';
import {LocalImage} from '../../../models/localImage.model';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {forkJoin} from 'rxjs';
import {BreadcrumbReloadComponent} from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';

@Component({
  selector: 'app-create-edit-product',
  standalone: true,
  imports: [
    Button,
    ReactiveFormsModule,
    InputText,
    InputNumber,
    Editor,
    ToggleSwitch,
    FileUpload,
    TreeSelect,
    AutoComplete,
    FormsModule,
    RouterLink,
    LoadingScreenComponent,
    BreadcrumbReloadComponent
  ],
  templateUrl: './create-edit-product.component.html',
  styleUrl: 'create-edit-product.component.css'
})
export class CreateEditProductComponent implements OnInit {

  private fb = inject(FormBuilder);
  private router = inject(Router);
  private messageService = inject(MessageService);
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private sanitizer = inject(DomSanitizer);
  private breadcrumbService = inject(BreadcrumbService);

  constructor() {

    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      supplyPrice: [0, [Validators.required, Validators.min(0.01)]],
      currentPrice: [0, [Validators.required, Validators.min(0.01)]],
      description: ['', []],
      selectedCategories: ['', []],
      active: [true],
      referenceCode: [{ value: '', disabled: true }]
    });
  }

  productId = signal<string | null>(null);
  product = signal<Product | null>(null);
  existingImages = signal<ImageInfo[]>([]);
  newImages = signal<LocalImage[]>([]);
  specs = signal<ProductSpec[]>([]);

  protected readonly MAX_SIZE = 5000000;
  productForm: FormGroup;
  categories: TreeNode[] = [];
  selectedCategories: TreeNode[] = [];

  pendingSpecName = '';
  pendingSpecValue = '';
  allSpecs: Record<string, string[]> = {};
  filteredSpecNames: string[] = [];
  filteredSpecValues: string[] = [];

  loading: boolean = true;
  error: boolean = false;

  protected readonly formatPrice = formatPrice;

  public reloadAll() {
    //If is creation mode, clear TS memory
    if (!this.productId()) {
      this.productForm.reset({
        active: true,
        supplyPrice: 0,
        currentPrice: 0
      });
      this.selectedCategories = [];
      this.existingImages.set([]);
      this.newImages.set([]);
      this.specs.set([]);
    }
    this.loadData();
  }

  ngOnInit() {
    this.productId.set(this.route.snapshot.paramMap.get('id'));
    this.loadData();
  }

  loadData() {
    this.loading = true;
    this.error = false;

    const productId = this.productId();
    const currentUrl = this.router.url;

    if (productId) {
      // EDIT
      forkJoin({
        list: this.categoryService.getAllCategories(),
        product: this.productService.getProductById(productId)
      }).subscribe({
        next: ({ list, product }) => {
          this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
            { label: 'Gestor de Productos', routerLink: '/admin/products' },
            { label: product.name, routerLink: `/product/${product.id}`, state: { from: 'products-management' } }
          ]);

          const cleanCategories = this.removeOthersCategoryFromTree(list || []);
          this.categories = mapToTreeNodes(cleanCategories);

          const cleanProductCategories = this.removeOthersCategoryFromTree(product.categories || []);
          this.selectedCategories = mapToTreeNodes(cleanProductCategories);

          this.product.set(product);
          this.productForm.patchValue(product);
          this.existingImages.set(product.imagesInfo);
          this.specs.set(product.specifications ?? []);
          this.loadSpecsCatalog();

          // Map selection to treeSelect
          if (this.categories.length > 0 && this.selectedCategories.length > 0) {
            this.productForm.patchValue({ selectedCategories: fixKeys(this.selectedCategories, this.categories) });
          }

          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      });
    } else {
      this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
        { label: 'Gestor de Productos', routerLink: '/admin/products' }
      ]);
      // CREATE
      this.specs.set([]);
      this.loadSpecsCatalog();
      this.categoryService.getAllCategories().subscribe({
        next: (list) => {
          const cleanCategories = this.removeOthersCategoryFromTree(list || []);
          this.categories = mapToTreeNodes(cleanCategories);

          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      });
    }
  }


  private removeOthersCategoryFromTree(categories: any[]): any[] {
    if (!categories) return [];

    return categories
      // Remove "Others" category from treeSelect options (recursively)
      .filter(c => c.name && c.name.trim().toLowerCase() !== 'otros')
      .map(c => ({
        ...c,
        children: (c.children && c.children.length > 0)
          ? this.removeOthersCategoryFromTree(c.children)
          : []
      }));
  }

  private disableParents(nodes: TreeNode[]) {
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        node.selectable = false;
        this.disableParents(node.children);
      } else {
        node.selectable = true;
      }
    }
  }

  onFileSelect(event: any) {
    const incomingFiles: File[] = Array.from(event.files || []);
    const validFiles: File[] = [];

    incomingFiles.forEach(file => {
      if (file.size <= this.MAX_SIZE) {
        validFiles.push(file);
      }
    });

    if (validFiles.length > 0) {
      const currentNewImages = this.newImages();
      const processedFiles: LocalImage[] = validFiles.map(file => ({
        file: file,
        previewUrl: this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(file))
      }));

      this.newImages.set([...currentNewImages, ...processedFiles]);
    }
  }

  removeNewImage(index: number) {
    this.newImages.update(imgs => imgs.filter((_, i) => i !== index));
  }

  removeExistingImage(index: number) {
    this.existingImages.update(imgs => imgs.filter((_, i) => i !== index));
  }

  private loadSpecsCatalog() {
    this.productService.getSpecsCatalog().subscribe(s => this.allSpecs = s);
  }

  searchSpecNames(query: string) {
    this.filteredSpecNames = Object.keys(this.allSpecs)
      .filter(n => n.toLowerCase().includes(query.toLowerCase()));
  }

  searchSpecValues(query: string) {
    const known = this.allSpecs[this.pendingSpecName] ?? [];
    this.filteredSpecValues = known.filter(v => v.toLowerCase().includes(query.toLowerCase()));
  }

  addSpec() {
    if (!this.pendingSpecName.trim() || !this.pendingSpecValue.trim()) return;
    const name = this.pendingSpecName.trim();
    const value = this.pendingSpecValue.trim();
    this.specs.update(s => {
      const existing = s.find(e => e.name === name);
      if (existing) {
        if (!existing.values.includes(value)) existing.values = [...existing.values, value];
        return [...s];
      }
      return [...s, { name, values: [value] }];
    });
    this.pendingSpecName = '';
    this.pendingSpecValue = '';
  }

  removeSpecValue(specName: string, value: string) {
    this.specs.update(s =>
      s.map(e => e.name === specName ? { ...e, values: e.values.filter(v => v !== value) } : e)
       .filter(e => e.values.length > 0)
    );
  }

  onSubmit() {
    if (this.productForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos requeridos.' });
      return;
    }

    const formValue = this.productForm.getRawValue();
    const { selectedCategories, ...cleanFormValue } = formValue;

    const productIdVal = this.productId();

    if (productIdVal) {
      // EDIT
      const editingProduct = this.product();
      if (editingProduct) {
        const productData: Product = {
          ...cleanFormValue,
          categories: mapToCategories(selectedCategories),
          images: this.existingImages(),
          specifications: this.specs()
        };

        this.productService.updateProduct(editingProduct.id, productData).subscribe({
          next: (product) => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Producto ${editingProduct.name} actualizado correctamente.` });
            this.updateProductImages(product.id);
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: `No se ha podido actualizar el producto ${editingProduct.name}.` });
          }
        });
      }
    } else {
      // CREATE
      const productData: Product = {
        ...cleanFormValue,
        categories: mapToCategories(selectedCategories),
        images: this.existingImages(),
        specifications: this.specs()
      };

      this.productService.createProduct(productData).subscribe({
        next: (product) => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Producto creado correctamente.' });
          this.updateProductImages(product.id);
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: `No se ha podido crear el nuevo producto.` });
        }
      });
    }
  }

  updateProductImages(id: string) {
    this.productService.updateProductImages(id, this.existingImages(), this.newImages().map(img => img.file)).subscribe({
      next: () => {
        if (!this.productId()) {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Imágenes actualizadas.` });
        } else {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Imágenes del producto actualizadas correctamente.` });
        }
        this.router.navigate(['/admin/products']);
      }
    });
  }
}
