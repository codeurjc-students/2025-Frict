import { Component, OnInit, signal } from '@angular/core';
import { NgIf } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';

import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';
import { Editor } from 'primeng/editor';
import { FileUpload } from 'primeng/fileupload';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { MessageService, TreeNode } from 'primeng/api';
import { TreeSelect } from 'primeng/treeselect';
import { DomSanitizer } from '@angular/platform-browser';

import { ProductService } from '../../../services/product.service';
import { CategoryService } from '../../../services/category.service';
import { fixKeys, mapToCategories, mapToTreeNodes } from '../../../utils/nodeMapper.util';
import { formatPrice } from '../../../utils/textFormat.util';
import { Product } from '../../../models/product.model';
import { ImageInfo } from '../../../models/imageInfo.model';
import { LocalImage } from '../../../models/localImage.model';
import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { forkJoin } from 'rxjs';

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
    NgIf,
    TreeSelect,
    FormsModule,
    RouterLink,
    LoadingScreenComponent
  ],
  templateUrl: './create-edit-product.component.html',
  styleUrl: 'create-edit-product.component.css'
})
export class CreateEditProductComponent implements OnInit {

  constructor(private fb: FormBuilder,
              private router: Router,
              private messageService: MessageService,
              private route: ActivatedRoute,
              private productService: ProductService,
              private categoryService: CategoryService,
              private sanitizer: DomSanitizer) {

    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      currentPrice: [0, [Validators.required, Validators.min(0.01)]],
      description: ['', [Validators.required]],
      selectedCategories: ['', []],
      active: [true],
      referenceCode: [{ value: '', disabled: true }]
    });
  }

  productId = signal<string | null>(null);
  product = signal<Product | null>(null);
  existingImages = signal<ImageInfo[]>([]);
  newImages = signal<LocalImage[]>([]);

  protected readonly MAX_SIZE = 5000000;
  productForm: FormGroup;
  categories: TreeNode[] = [];
  selectedCategories: TreeNode[] = [];

  loading: boolean = true;
  error: boolean = false;

  protected readonly formatPrice = formatPrice;

  ngOnInit() {
    this.productId.set(this.route.snapshot.paramMap.get('id'));
    this.loadData();
  }

  loadData() {
    const productId = this.productId();

    if (productId) {
      // EDIT
      forkJoin({
        list: this.categoryService.getAllCategories(),
        product: this.productService.getProductById(productId)
      }).subscribe({
        next: ({ list, product }) => {
          const cleanCategories = this.removeOthersCategoryFromTree(list || []);
          this.categories = mapToTreeNodes(cleanCategories);

          const cleanProductCategories = this.removeOthersCategoryFromTree(product.categories || []);
          this.selectedCategories = mapToTreeNodes(cleanProductCategories);

          this.product.set(product);
          this.productForm.patchValue(product);
          this.existingImages.set(product.imagesInfo);

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
      // CREATE
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

  onSubmit() {
    if (this.productForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos requeridos.' });
      return;
    }

    const productIdVal = this.productId();

    if (productIdVal) {
      // EDIT
      const editingProduct = this.product();
      if (editingProduct) {
        const formValue = this.productForm.getRawValue();
        const productData: Product = {
          ...formValue,
          categories: mapToCategories(formValue.selectedCategories),
          images: this.existingImages()
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
      const formValue = this.productForm.getRawValue();
      const productData: Product = {
        ...formValue,
        categories: mapToCategories(formValue.selectedCategories),
        images: this.existingImages()
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
