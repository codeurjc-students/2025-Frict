import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportsComponent } from './reports.component';
import { of, throwError, Subject } from 'rxjs';
import { LOCALE_ID } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { RegistryService } from '../../../services/registry.service';
import { BreadcrumbService } from '../../../utils/breadcrumb.service';
import { AuthService } from '../../../services/auth.service';

const mockGraphResponse = [
  { _id: '2025-01-01', totalValue: 100 },
  { _id: '2025-01-08', totalValue: 200 }
];

const mockTableResponse = {
  items: [
    {
      timestamp: '2025-01-01T10:00:00Z',
      metrics: { value: 100, total: 500 },
      metadata: { shopId: 'shop-1', shopName: 'Tienda Central', productId: 'prod-1', productName: 'Prod A' }
    }
  ],
  totalItems: 1
};

const mockCrossReferences = {
  shopId: ['shop-1', 'shop-2'],
  productId: ['prod-1'],
  userId: [],
  orderId: []
};

describe('ReportsComponent', () => {
  let component: ReportsComponent;
  let fixture: ComponentFixture<ReportsComponent>;
  let registryServiceSpy: jasmine.SpyObj<RegistryService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let routerEvents$: Subject<any>;

  beforeEach(async () => {
    routerEvents$ = new Subject<any>();

    registryServiceSpy = jasmine.createSpyObj('RegistryService', [
      'getEntityTypes', 'getMetrics', 'getCrossReferences', 'loadInternalRegistry', 'exportCustomPdf'
    ]);

    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree', 'serializeUrl'], {
      events: routerEvents$.asObservable(),
      url: '/admin/reports'
    });
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');

    registryServiceSpy.getEntityTypes.and.callFake(() => of(['SHOP', 'PRODUCT', 'USER']));
    registryServiceSpy.getMetrics.and.callFake(() => of(['SALES', 'COUNT']));
    registryServiceSpy.getCrossReferences.and.callFake(() => of({ ...mockCrossReferences }));
    registryServiceSpy.loadInternalRegistry.and.callFake((params: any) => {
      if (params.viewType === 'GRAPH') return of([...mockGraphResponse] as any);
      return of({ ...mockTableResponse } as any);
    });
    registryServiceSpy.exportCustomPdf.and.callFake(() =>
      of(new Blob(['pdf'], { type: 'application/pdf' }))
    );

    await TestBed.configureTestingModule({
      imports: [ReportsComponent],
      providers: [
        provideNoopAnimations(),
        { provide: RegistryService, useValue: registryServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            root: { children: [] }
          }
        },
        BreadcrumbService,
        {
          provide: AuthService,
          useValue: {
            isAdmin: jasmine.createSpy('isAdmin').and.returnValue(false),
            isManager: jasmine.createSpy('isManager').and.returnValue(false),
            isDriver: jasmine.createSpy('isDriver').and.returnValue(false),
            isLogged: jasmine.createSpy('isLogged').and.returnValue(false)
          }
        },
        { provide: LOCALE_ID, useValue: 'en-US' }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // --- ngOnInit ---

  describe('ngOnInit', () => {
    it('should call getEntityTypes on init', () => {
      expect(registryServiceSpy.getEntityTypes).toHaveBeenCalled();
    });

    it('should populate entityOptions from response', () => {
      expect(component.entityOptions()).toEqual(['SHOP', 'PRODUCT', 'USER']);
    });

    it('should set loading to false after success', () => {
      expect(component.loading).toBeFalse();
    });

    it('should set error=true and loading=false when getEntityTypes fails', () => {
      registryServiceSpy.getEntityTypes.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.error).toBeTrue();
      expect(component.loading).toBeFalse();
    });
  });

  // --- Entity selection effect ---

  describe('selectedEntity effect', () => {
    it('should call getMetrics when selectedEntity is set', () => {
      registryServiceSpy.getMetrics.calls.reset();
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      expect(registryServiceSpy.getMetrics).toHaveBeenCalledWith('SHOP');
    });

    it('should update metricOptions after getMetrics returns', () => {
      registryServiceSpy.getMetrics.and.callFake(() => of(['SALES', 'REVENUE']));
      component.selectedEntity.set('PRODUCT');
      fixture.detectChanges();
      expect(component.metricOptions()).toEqual(['SALES', 'REVENUE']);
    });

    it('should reset selectedMetric to null when entity changes', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      component.selectedEntity.set('PRODUCT');
      fixture.detectChanges();
      expect(component.selectedMetric()).toBeNull();
    });

    it('should clear metricOptions when entity is set to null', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.selectedEntity.set(null);
      fixture.detectChanges();
      expect(component.metricOptions()).toEqual([]);
    });

    it('should set metricOptions to empty array when getMetrics fails', () => {
      registryServiceSpy.getMetrics.and.returnValue(throwError(() => new Error('fail')));
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      expect(component.metricOptions()).toEqual([]);
    });
  });

  // --- Metric selection effect ---

  describe('selectedMetric effect', () => {
    beforeEach(() => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
    });

    it('should call getCrossReferences when both entity and metric are set', () => {
      registryServiceSpy.getCrossReferences.calls.reset();
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      expect(registryServiceSpy.getCrossReferences).toHaveBeenCalledWith('SHOP', 'SALES');
    });

    it('should populate shopOptions from cross-references response', () => {
      registryServiceSpy.getCrossReferences.and.callFake(() =>
        of({ shopId: ['s1', 's2'], productId: [], userId: [], orderId: [] })
      );
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      expect(component.shopOptions()).toEqual(['s1', 's2']);
    });

    it('should populate productOptions from cross-references response', () => {
      registryServiceSpy.getCrossReferences.and.callFake(() =>
        of({ shopId: [], productId: ['p1'], userId: [], orderId: [] })
      );
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      expect(component.productOptions()).toEqual(['p1']);
    });

    it('should filter out null values from cross-references', () => {
      registryServiceSpy.getCrossReferences.and.callFake(() =>
        of({ shopId: ['s1', null, 's2'] as any, productId: [], userId: [], orderId: [] })
      );
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      expect(component.shopOptions()).toEqual(['s1', 's2']);
    });

    it('should reset associated entities when getCrossReferences fails', () => {
      component.shopOptions.set(['shop-1']);
      registryServiceSpy.getCrossReferences.and.returnValue(throwError(() => new Error('fail')));
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      expect(component.shopOptions()).toEqual([]);
    });
  });

  // --- fetchReportData ---

  describe('fetchReportData', () => {
    beforeEach(() => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      registryServiceSpy.loadInternalRegistry.calls.reset();
    });

    it('should do nothing when entity is missing', () => {
      component.selectedEntity.set(null);
      fixture.detectChanges();
      component.fetchReportData();
      expect(registryServiceSpy.loadInternalRegistry).not.toHaveBeenCalled();
    });

    it('should do nothing when metric is missing', () => {
      component.selectedMetric.set(null);
      component.fetchReportData();
      expect(registryServiceSpy.loadInternalRegistry).not.toHaveBeenCalled();
    });

    it('should call loadInternalRegistry twice (GRAPH and TABLE)', () => {
      component.fetchReportData();
      expect(registryServiceSpy.loadInternalRegistry).toHaveBeenCalledTimes(2);
    });

    it('should call GRAPH view with correct interval', () => {
      component.selectedInterval.set('day');
      component.fetchReportData();
      const graphCallArgs = registryServiceSpy.loadInternalRegistry.calls.allArgs()
        .find((args: any[]) => args[0].viewType === 'GRAPH');
      expect(graphCallArgs).toBeTruthy();
      expect(graphCallArgs![0].interval).toBe('day');
    });

    it('should set graphData from GRAPH response', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake((params: any) => {
        if (params.viewType === 'GRAPH') return of([{ _id: '2025-02-01', totalValue: 999 }] as any);
        return of({ items: [], totalItems: 0 } as any);
      });
      component.fetchReportData();
      expect(component.graphData()[0].totalValue).toBe(999);
    });

    it('should set graphData to empty array on GRAPH error', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake((params: any) => {
        if (params.viewType === 'GRAPH') return throwError(() => new Error('fail'));
        return of({ items: [], totalItems: 0 } as any);
      });
      component.fetchReportData();
      expect(component.graphData()).toEqual([]);
    });

    it('should set tableData and totalRecords from TABLE response', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake((params: any) => {
        if (params.viewType === 'GRAPH') return of([] as any);
        return of({ items: [{ timestamp: 't1' }], totalItems: 42 } as any);
      });
      component.fetchReportData();
      expect(component.tableData()[0].timestamp).toBe('t1');
      expect(component.totalRecords()).toBe(42);
    });

    it('should set isLoadingReport to false after TABLE success', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake((params: any) => {
        if (params.viewType === 'GRAPH') return of([] as any);
        return of({ items: [], totalItems: 0 } as any);
      });
      component.fetchReportData();
      expect(component.isLoadingReport()).toBeFalse();
    });

    it('should set tableData to empty and isLoadingReport false on TABLE error', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake((params: any) => {
        if (params.viewType === 'GRAPH') return of([] as any);
        return throwError(() => new Error('fail'));
      });
      component.fetchReportData();
      expect(component.tableData()).toEqual([]);
      expect(component.isLoadingReport()).toBeFalse();
    });

    it('should reset currentPage to 0 when called', () => {
      registryServiceSpy.loadInternalRegistry.and.callFake((params: any) => {
        if (params.viewType === 'GRAPH') return of([] as any);
        return of({ items: [], totalItems: 0 } as any);
      });
      component.currentPage.set(3);
      component.fetchReportData();
      expect(component.currentPage()).toBe(0);
    });
  });

  // --- onPageChange ---

  describe('onPageChange', () => {
    beforeEach(() => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      registryServiceSpy.loadInternalRegistry.calls.reset();
      registryServiceSpy.loadInternalRegistry.and.callFake(() =>
        of({ items: [], totalItems: 0 } as any)
      );
    });

    it('should update currentPage based on first/rows', () => {
      component.onPageChange({ first: 10, rows: 5 });
      expect(component.currentPage()).toBe(2);
    });

    it('should update pageSize', () => {
      component.onPageChange({ first: 0, rows: 25 });
      expect(component.pageSize()).toBe(25);
    });

    it('should call loadInternalRegistry with TABLE viewType', () => {
      component.onPageChange({ first: 0, rows: 10 });
      const tableCall = registryServiceSpy.loadInternalRegistry.calls.allArgs()
        .find((args: any[]) => args[0].viewType === 'TABLE');
      expect(tableCall).toBeTruthy();
    });

    it('should not call service when entity is missing', () => {
      component.selectedEntity.set(null);
      fixture.detectChanges();
      component.onPageChange({ first: 0, rows: 10 });
      expect(registryServiceSpy.loadInternalRegistry).not.toHaveBeenCalled();
    });
  });

  // --- updateMetricMode ---

  describe('updateMetricMode', () => {
    beforeEach(() => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      registryServiceSpy.loadInternalRegistry.calls.reset();
      registryServiceSpy.loadInternalRegistry.and.callFake((params: any) => {
        if (params.viewType === 'GRAPH') return of([] as any);
        return of({ items: [], totalItems: 0 } as any);
      });
    });

    it('should change metricMode and trigger refetch when mode differs', () => {
      component.metricMode.set('VALUE');
      component.updateMetricMode('TOTAL');
      expect(component.metricMode()).toBe('TOTAL');
      expect(registryServiceSpy.loadInternalRegistry).toHaveBeenCalled();
    });

    it('should not trigger refetch when mode is already set to the given value', () => {
      component.metricMode.set('VALUE');
      component.updateMetricMode('VALUE');
      expect(registryServiceSpy.loadInternalRegistry).not.toHaveBeenCalled();
    });
  });

  // --- getColumnLabel ---

  describe('getColumnLabel', () => {
    it('should return "Fecha" for timestamp', () => {
      expect(component.getColumnLabel('timestamp')).toBe('Fecha');
    });

    it('should return "Variación" for metric_value', () => {
      expect(component.getColumnLabel('metric_value')).toBe('Variación');
    });

    it('should return "Acumulado" for metric_total', () => {
      expect(component.getColumnLabel('metric_total')).toBe('Acumulado');
    });

    it('should return "Tienda" for meta_shopName', () => {
      expect(component.getColumnLabel('meta_shopName')).toBe('Tienda');
    });

    it('should return cleaned key for unknown column', () => {
      expect(component.getColumnLabel('meta_unknownField')).toBe('unknownField');
    });
  });

  // --- getCellValue ---

  describe('getCellValue', () => {
    const mockRow = {
      timestamp: '2025-01-15T10:00:00Z',
      metrics: { value: 42, total: 500 },
      metadata: { shopId: 'shop-1', shopName: 'Tienda Central' }
    };

    it('should return timestamp for timestamp column', () => {
      expect(component.getCellValue(mockRow, 'timestamp')).toBe('2025-01-15T10:00:00Z');
    });

    it('should return metric value for metric_value column', () => {
      expect(component.getCellValue(mockRow, 'metric_value')).toBe(42);
    });

    it('should return metric total for metric_total column', () => {
      expect(component.getCellValue(mockRow, 'metric_total')).toBe(500);
    });

    it('should return metadata value for meta_shopId column', () => {
      expect(component.getCellValue(mockRow, 'meta_shopId')).toBe('shop-1');
    });

    it('should return "-" when metric key is missing', () => {
      expect(component.getCellValue(mockRow, 'metric_missing')).toBe('-');
    });

    it('should return "-" when metadata key is missing', () => {
      expect(component.getCellValue(mockRow, 'meta_missing')).toBe('-');
    });

    it('should return "-" for completely unknown column', () => {
      expect(component.getCellValue(mockRow, 'unknownCol')).toBe('-');
    });
  });

  // --- resetAll ---

  describe('resetAll', () => {
    beforeEach(() => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
    });

    it('should reset selectedEntity to null', () => {
      component.resetAll();
      expect(component.selectedEntity()).toBeNull();
    });

    it('should reset selectedMetric to null', () => {
      component.resetAll();
      expect(component.selectedMetric()).toBeNull();
    });

    it('should reset graphData to empty array', () => {
      component.graphData.set([...mockGraphResponse]);
      component.resetAll();
      expect(component.graphData()).toEqual([]);
    });

    it('should reset tableData to empty array', () => {
      component.tableData.set([{ timestamp: 't' }]);
      component.resetAll();
      expect(component.tableData()).toEqual([]);
    });

    it('should reset totalRecords to 0', () => {
      component.totalRecords.set(99);
      component.resetAll();
      expect(component.totalRecords()).toBe(0);
    });

    it('should reset selectedInterval to "week"', () => {
      component.selectedInterval.set('day');
      component.resetAll();
      expect(component.selectedInterval()).toBe('week');
    });
  });

  // --- chartData computed ---

  describe('chartData computed', () => {
    it('should return empty object when graphData is empty', () => {
      component.graphData.set([]);
      expect(component.chartData()).toEqual({});
    });

    it('should return labels and datasets when graphData has items', () => {
      component.graphData.set([{ _id: '2025-01-01', totalValue: 50 }]);
      const data: any = component.chartData();
      expect(data.labels.length).toBe(1);
      expect(data.datasets.length).toBe(1);
      expect(data.datasets[0].data[0]).toBe(50);
    });

    it('should use selectedMetric as dataset label', () => {
      component.selectedMetric.set('REVENUE');
      component.graphData.set([{ _id: '2025-01-01', totalValue: 10 }]);
      expect((component.chartData() as any).datasets[0].label).toBe('REVENUE');
    });

    it('should use array colors for pie chart', () => {
      component.chartType.set('pie');
      component.graphData.set([{ _id: '2025-01-01', totalValue: 10 }]);
      expect(Array.isArray((component.chartData() as any).datasets[0].backgroundColor)).toBeTrue();
    });

    it('should use single string color for bar chart', () => {
      component.chartType.set('bar');
      component.graphData.set([{ _id: '2025-01-01', totalValue: 10 }]);
      expect(typeof (component.chartData() as any).datasets[0].backgroundColor).toBe('string');
    });
  });

  // --- chartOptions computed ---

  describe('chartOptions computed', () => {
    it('should hide legend for bar chart', () => {
      component.chartType.set('bar');
      expect(component.chartOptions().plugins.legend.display).toBeFalse();
    });

    it('should show legend on the right for pie chart', () => {
      component.chartType.set('pie');
      const opts = component.chartOptions();
      expect(opts.plugins.legend.display).toBeTrue();
      expect(opts.plugins.legend.position).toBe('right');
    });

    it('should hide x and y scales for pie chart', () => {
      component.chartType.set('pie');
      const opts = component.chartOptions();
      expect(opts.scales.x.display).toBeFalse();
      expect(opts.scales.y.display).toBeFalse();
    });

    it('should show x and y scales for line chart', () => {
      component.chartType.set('line');
      const opts = component.chartOptions();
      expect(opts.scales.x.display).toBeTrue();
      expect(opts.scales.y.display).toBeTrue();
    });
  });

  // --- availableDynamicEntities computed ---

  describe('availableDynamicEntities computed', () => {
    it('should return empty array when tableData is empty', () => {
      component.tableData.set([]);
      expect(component.availableDynamicEntities()).toEqual([]);
    });

    it('should return empty array when selectedEntity is null', () => {
      component.selectedEntity.set(null);
      fixture.detectChanges();
      component.tableData.set([{ metadata: { shopId: 'shop-1' } }]);
      expect(component.availableDynamicEntities()).toEqual([]);
    });

    it('should always include the main entity when tableData has items', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.tableData.set([{ metadata: {} }]);
      const entities = component.availableDynamicEntities();
      expect(entities.some((e: any) => e.value === 'SHOP')).toBeTrue();
    });

    it('should include PRODUCT when metadata has productId', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.tableData.set([{ metadata: { productId: 'prod-1' } }]);
      const entities = component.availableDynamicEntities();
      expect(entities.some((e: any) => e.value === 'PRODUCT')).toBeTrue();
    });

    it('should include USER when metadata has userId', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.tableData.set([{ metadata: { userId: 'user-1' } }]);
      const entities = component.availableDynamicEntities();
      expect(entities.some((e: any) => e.value === 'USER')).toBeTrue();
    });

    it('should use column translation as label', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.tableData.set([{ metadata: {} }]);
      const shopEntry = component.availableDynamicEntities().find((e: any) => e.value === 'SHOP');
      expect(shopEntry?.label).toBe('Tienda');
    });
  });

  // --- tableColumns computed ---

  describe('tableColumns computed', () => {
    it('should return empty array when entity is null', () => {
      component.selectedEntity.set(null);
      fixture.detectChanges();
      component.tableData.set([{ timestamp: 't' }]);
      expect(component.tableColumns()).toEqual([]);
    });

    it('should return empty array when tableData is empty', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.tableData.set([]);
      expect(component.tableColumns()).toEqual([]);
    });

    it('should always include timestamp, metric_value and metric_total', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.tableData.set([{ timestamp: 't' }]);
      const cols = component.tableColumns();
      expect(cols).toContain('timestamp');
      expect(cols).toContain('metric_value');
      expect(cols).toContain('metric_total');
    });

    it('should include entity id and name columns when entity is in selectedDynamicColumns', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.tableData.set([{ timestamp: 't' }]);
      // effect sets selectedDynamicColumns to ['SHOP'] automatically
      const cols = component.tableColumns();
      expect(cols).toContain('meta_shopId');
      expect(cols).toContain('meta_shopName');
    });

    it('should not include entity columns when entity is not in selectedDynamicColumns', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.tableData.set([{ timestamp: 't' }]);
      component.selectedDynamicColumns.set([]);
      const cols = component.tableColumns();
      expect(cols).not.toContain('meta_shopId');
    });

    it('should include secondary entity columns when added to selectedDynamicColumns', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.tableData.set([{ timestamp: 't' }]);
      component.selectedDynamicColumns.set(['SHOP', 'PRODUCT']);
      const cols = component.tableColumns();
      expect(cols).toContain('meta_productId');
      expect(cols).toContain('meta_productName');
    });
  });

  // --- downloadCustomPdf ---

  describe('downloadCustomPdf', () => {
    it('should not call exportCustomPdf when entity is missing', () => {
      component.selectedEntity.set(null);
      fixture.detectChanges();
      component.downloadCustomPdf();
      expect(registryServiceSpy.exportCustomPdf).not.toHaveBeenCalled();
    });

    it('should not call exportCustomPdf when metric is missing', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      // metric remains null
      component.downloadCustomPdf();
      expect(registryServiceSpy.exportCustomPdf).not.toHaveBeenCalled();
    });

    it('should call exportCustomPdf and trigger file download', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.selectedMetric.set('SALES');
      fixture.detectChanges();

      (component as any).chartComponent = {
        chart: { toBase64Image: jasmine.createSpy('toBase64Image').and.returnValue('data:image/png;base64,abc') }
      };

      spyOn(URL, 'createObjectURL').and.returnValue('blob:test-url');
      spyOn(URL, 'revokeObjectURL');
      const originalCreate = document.createElement.bind(document);
      const mockAnchor = jasmine.createSpyObj('anchor', ['click']);
      (mockAnchor as any).href = '';
      (mockAnchor as any).download = '';
      spyOn(document, 'createElement').and.callFake((tag: string) =>
        tag === 'a' ? (mockAnchor as any) : originalCreate(tag)
      );

      component.downloadCustomPdf();

      expect(registryServiceSpy.exportCustomPdf).toHaveBeenCalled();
      expect(URL.createObjectURL).toHaveBeenCalled();
      expect(mockAnchor.click).toHaveBeenCalled();
      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test-url');
      expect(component.isLoadingReport()).toBeFalse();
    });

    it('should set isLoadingReport false on export error', () => {
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.selectedMetric.set('SALES');
      fixture.detectChanges();

      (component as any).chartComponent = {
        chart: { toBase64Image: () => 'data:...' }
      };
      registryServiceSpy.exportCustomPdf.and.returnValue(throwError(() => new Error('fail')));

      component.downloadCustomPdf();

      expect(component.isLoadingReport()).toBeFalse();
    });
  });

  // --- DOM ---

  describe('DOM', () => {
    it('should render the "Gestor de Informes" heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Gestor de Informes');
    });

    it('should render the filter panel heading', () => {
      expect(fixture.nativeElement.textContent).toContain('Filtros de informe');
    });

    it('should show loading screen when loading is true', () => {
      component.loading = true;
      component.error = false;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeTruthy();
    });

    it('should hide main content when loading is true', () => {
      component.loading = true;
      fixture.detectChanges();
      const allText = fixture.nativeElement.textContent as string;
      expect(allText).not.toContain('Gestor de Informes');
    });

    it('should show placeholder message when graphData is empty', () => {
      component.graphData.set([]);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Configura los filtros a la izquierda');
    });

    it('should show empty message in table when tableData is empty', () => {
      component.tableData.set([]);
      component.selectedEntity.set('SHOP');
      fixture.detectChanges();
      component.selectedMetric.set('SALES');
      fixture.detectChanges();
      component.tableData.set([]);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('No hay registros asociados');
    });
  });
});
