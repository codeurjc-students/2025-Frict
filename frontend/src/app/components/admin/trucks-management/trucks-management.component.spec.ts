import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {TrucksManagementComponent} from './trucks-management.component';
import {getTruckHistoryStatusTagInfo} from '../../../utils/tagManager.util';
import {TruckService} from '../../../services/truck.service';
import {UserService} from '../../../services/user.service';
import {ConfirmationService, MessageService} from 'primeng/api';
import {ActivatedRoute, Router} from '@angular/router';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {of, Subject, throwError} from 'rxjs';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {Truck} from '../../../models/truck.model';
import {User} from '../../../models/user.model';
import {PageResponse} from '../../../models/pageResponse.model';
import {TruckStatusLog} from '../../../models/truckStatusLog.model';

// ConfirmPopup (PrimeNG) subscribes to ConfirmationService.requireConfirmation$ in its
// constructor, so the real service must be provided — a jasmine.SpyObj would lack
// the internal Subject and cause a runtime error during template rendering.
// BreadcrumbReloadComponent (child) injects Router, ActivatedRoute, BreadcrumbService,
// so all of them must be properly provided.

describe('TrucksManagementComponent', () => {
  let component: TrucksManagementComponent;
  let fixture: ComponentFixture<TrucksManagementComponent>;
  let truckServiceSpy: jasmine.SpyObj<TruckService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let confirmationService: ConfirmationService;
  let breadcrumbServiceSpy: jasmine.SpyObj<BreadcrumbService>;
  let routerSpy: jasmine.SpyObj<Router>;

  // ─── Mock data ────────────────────────────────────────────────────────────────

  const mockAddress = {
    id: 'addr-1', alias: 'Almacén Central', street: 'Calle Industria', number: '42',
    floor: '', postalCode: '28001', city: 'Madrid', country: 'España',
    latitude: 40.4168, longitude: -3.7038
  };

  const mockDriver: User = {
    id: 'driver-1',
    name: 'Pedro Sánchez',
    username: 'pedrosanchez',
    roles: ['DRIVER'],
    email: 'pedro@test.com',
    phone: '+34 600111222',
    addresses: [],
    cards: [],
    imageInfo: { id: 'img-1', imageUrl: 'http://img.test/avatar.png', s3Key: 'users/avatar.png', fileName: 'avatar.png' } as any,
    banned: false,
    deleted: false,
    selectedShopId: null,
    ordersCount: 0,
    favouriteProductsCount: 0,
    connection: null
  };

  const mockDriver2: User = {
    ...mockDriver,
    id: 'driver-2',
    name: 'María López',
    username: 'marialopez',
    email: 'maria@test.com'
  };

  const mockStatusLogAvailable: TruckStatusLog = {
    id: 'log-1', icon: 'pi pi-check-circle', status: 'Descanso',
    updates: [{ date: '2026-05-01', description: 'Camión disponible en depósito' } as any]
  };

  const mockStatusLogOnRoute: TruckStatusLog = {
    id: 'log-2', icon: 'pi pi-send', status: 'En Reparto',
    updates: [{ date: '2026-05-09', description: 'Salida hacia zona norte' } as any]
  };

  const mockStatusLogMaintenance: TruckStatusLog = {
    id: 'log-3', icon: 'pi pi-wrench', status: 'Fuera de servicio',
    updates: [{ date: '2026-05-08', description: 'Revisión ITV programada' } as any]
  };

  const mockTruckAvailable: Truck = {
    id: 'truck-1',
    referenceCode: 'TRK-001',
    plateNumber: '1234-ABC',
    history: [mockStatusLogAvailable],
    assignedDriver: mockDriver,
    address: mockAddress,
    ordersToDeliver: 3,
    maxCapacity: 10,
    currentCapacity: 0
  };

  const mockTruckOnRoute: Truck = {
    id: 'truck-2',
    referenceCode: 'TRK-002',
    plateNumber: '5678-DEF',
    history: [mockStatusLogOnRoute],
    assignedDriver: undefined,
    address: { ...mockAddress, id: 'addr-2', latitude: 41.3851, longitude: 2.1734 },
    ordersToDeliver: 7,
    maxCapacity: 10,
    currentCapacity: 0
  };

  const mockTruckMaintenance: Truck = {
    id: 'truck-3',
    referenceCode: 'TRK-003',
    plateNumber: '9999-GHI',
    history: [mockStatusLogMaintenance],
    assignedDriver: undefined,
    address: { ...mockAddress, id: 'addr-3', latitude: undefined, longitude: undefined },
    ordersToDeliver: 0,
    maxCapacity: 10,
    currentCapacity: 0
  };

  const mockTrucksPage: PageResponse<Truck> = {
    items: [mockTruckAvailable, mockTruckOnRoute, mockTruckMaintenance],
    totalItems: 3,
    currentPage: 0,
    lastPage: 0,
    pageSize: 5
  };

  const emptyPage: PageResponse<Truck> = {
    items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0
  };

  // ─── Setup ────────────────────────────────────────────────────────────────────

  beforeEach(async () => {
    truckServiceSpy = jasmine.createSpyObj('TruckService', [
      'getAllTrucksPage', 'deleteTruck', 'assignDriver', 'commentAndOrUpdateTruckStatus'
    ]);
    // callFake returns a fresh copy to prevent test pollution when component mutates items
    truckServiceSpy.getAllTrucksPage.and.callFake(() =>
      of({
        ...mockTrucksPage,
        items: [
          { ...mockTruckAvailable, history: [{ ...mockStatusLogAvailable }], assignedDriver: { ...mockDriver } },
          { ...mockTruckOnRoute,   history: [{ ...mockStatusLogOnRoute }]   },
          { ...mockTruckMaintenance, history: [{ ...mockStatusLogMaintenance }] }
        ]
      })
    );
    truckServiceSpy.deleteTruck.and.returnValue(of({ ...mockTruckAvailable }));
    truckServiceSpy.assignDriver.and.returnValue(of({ ...mockTruckAvailable }));
    truckServiceSpy.commentAndOrUpdateTruckStatus.and.returnValue(of({ ...mockTruckAvailable }));

    userServiceSpy = jasmine.createSpyObj('UserService', ['getAvailableDrivers']);
    userServiceSpy.getAvailableDrivers.and.returnValue(of([mockDriver, mockDriver2]));

    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    breadcrumbServiceSpy = jasmine.createSpyObj('BreadcrumbService', [
      'setBaseBreadcrumbs', 'insertPenultimateNodesForUrl', 'breadcrumbs'
    ]);
    breadcrumbServiceSpy.breadcrumbs.and.returnValue([]);

    routerSpy = jasmine.createSpyObj(
      'Router',
      ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
      { url: '/admin/trucks', events: new Subject<any>(), navigated: false }
    );
    routerSpy.createUrlTree.and.returnValue({} as any);
    routerSpy.serializeUrl.and.returnValue('/');
    routerSpy.navigateByUrl.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [TrucksManagementComponent, BrowserAnimationsModule],
      providers: [
        { provide: TruckService,       useValue: truckServiceSpy       },
        { provide: UserService,        useValue: userServiceSpy        },
        { provide: MessageService,     useValue: messageServiceSpy     },
        { provide: BreadcrumbService,  useValue: breadcrumbServiceSpy  },
        { provide: Router,             useValue: routerSpy             },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => null }, url: [], data: {} },
            params: of({}),
            root: { children: [] }
          }
        },
        ConfirmationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    confirmationService = TestBed.inject(ConfirmationService);

    fixture = TestBed.createComponent(TrucksManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ─── Creation ─────────────────────────────────────────────────────────────────

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // ─── ngOnInit calls ───────────────────────────────────────────────────────────

  it('should call getAllTrucksPage on init with page 0 and 5 rows', () => {
    expect(truckServiceSpy.getAllTrucksPage).toHaveBeenCalledWith(0, 5);
  });

  it('should initialise chartOptions on init', () => {
    expect(component.chartOptions).not.toBeNull();
    expect(component.chartOptions.responsive).toBeTrue();
  });

  // ─── State after happy-path load ──────────────────────────────────────────────

  it('should set loading=false after successful load', () => {
    expect(component.loading).toBeFalse();
  });

  it('should set error=false after successful load', () => {
    expect(component.error).toBeFalse();
  });

  it('should set isInitialLoad=false after successful load', () => {
    expect(component.isInitialLoad).toBeFalse();
  });

  it('should populate trucksPage after successful load', () => {
    expect(component.trucksPage.items.length).toBe(3);
    expect(component.trucksPage.totalItems).toBe(3);
  });

  // ─── calculateKPIs ────────────────────────────────────────────────────────────

  it('should set totalTrucks from page totalItems', () => {
    expect(component.totalTrucks).toBe(3);
  });

  it('should count onRouteTrucks from items with "En Reparto" status', () => {
    expect(component.onRouteTrucks).toBe(1);
  });

  it('should count noDriverTrucks from items without assignedDriver', () => {
    expect(component.noDriverTrucks).toBe(2);
  });

  it('should count maintenanceTrucks from items with "Fuera de servicio" status', () => {
    expect(component.maintenanceTrucks).toBe(1);
  });

  // ─── updateChartData ──────────────────────────────────────────────────────────

  it('should build chartData with 4 labels after load', () => {
    expect(component.chartData.labels).toEqual(['Descanso', 'En ruta a la tienda', 'En Reparto', 'Fuera de servicio']);
  });

  it('should build chartData dataset with correct counts', () => {
    const data = component.chartData.datasets[0].data;
    expect(data[0]).toBe(1); // Descanso
    expect(data[1]).toBe(0); // En ruta a la tienda
    expect(data[2]).toBe(1); // En Reparto
    expect(data[3]).toBe(1); // Fuera de servicio
  });

  it('should build assignedChartData with 2 labels after load', () => {
    expect(component.assignedChartData.labels).toEqual(['Conductor Asignado', 'Sin Conductor Asignado']);
  });

  it('should build assignedChartData dataset with correct counts', () => {
    const data = component.assignedChartData.datasets[0].data;
    expect(data[0]).toBe(1); // assigned
    expect(data[1]).toBe(2); // unassigned
  });

  // ─── Error states ─────────────────────────────────────────────────────────────

  it('should set error=true and loading=false when getAllTrucksPage fails', () => {
    truckServiceSpy.getAllTrucksPage.and.returnValue(throwError(() => new Error('500')));
    component.loadTrucks();
    expect(component.error).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should set loading=true while getAllTrucksPage is pending (initial load)', () => {
    const pending$ = new Subject<PageResponse<Truck>>();
    truckServiceSpy.getAllTrucksPage.and.returnValue(pending$.asObservable());
    component.isInitialLoad = true;
    component.loading = false;
    component.loadTrucks();
    expect(component.loading).toBeTrue();
  });

  // ─── reloadAll ────────────────────────────────────────────────────────────────

  it('should set loading=true and error=false on reloadAll', () => {
    const blocker = new Subject<PageResponse<Truck>>();
    truckServiceSpy.getAllTrucksPage.and.returnValue(blocker.asObservable());
    component.error = true;
    component.reloadAll();
    expect(component.loading).toBeTrue();
    expect(component.error).toBeFalse();
  });

  it('should close all dialogs on reloadAll', () => {
    component.displayHistoryDialog    = true;
    component.displayAssignmentDialog = true;
    component.reloadAll();
    expect(component.displayHistoryDialog).toBeFalse();
    expect(component.displayAssignmentDialog).toBeFalse();
  });

  it('should clear selectedTruck and selectedDriver on reloadAll', () => {
    component.selectedTruck  = mockTruckAvailable;
    component.selectedDriver = mockDriver;
    component.reloadAll();
    expect(component.selectedTruck).toBeNull();
    expect(component.selectedDriver).toBeUndefined();
  });

  it('should call getAllTrucksPage again on reloadAll', () => {
    const prevCount = truckServiceSpy.getAllTrucksPage.calls.count();
    component.reloadAll();
    expect(truckServiceSpy.getAllTrucksPage.calls.count()).toBeGreaterThan(prevCount);
  });

  // ─── getCurrentStatus ─────────────────────────────────────────────────────────

  it('should return the last history entry status', () => {
    const truck = { ...mockTruckAvailable, history: [mockStatusLogAvailable, mockStatusLogOnRoute] };
    expect(component.getCurrentStatus(truck)).toBe('En Reparto');
  });

  it('should return "Disponible" when truck has no history', () => {
    const truck = { ...mockTruckAvailable, history: [] };
    expect(component.getCurrentStatus(truck)).toBe('Disponible');
  });

  it('should return "Disponible" when truck is null or undefined', () => {
    expect(component.getCurrentStatus(null as any)).toBe('Disponible');
  });

  // ─── getTruckHistoryStatusTagInfo (tagManager) ───────────────────────────────

  it('should return success severity for Descanso', () => {
    expect(getTruckHistoryStatusTagInfo('Descanso').severity).toBe('success');
  });

  it('should return info severity for En ruta a la tienda', () => {
    expect(getTruckHistoryStatusTagInfo('En ruta a la tienda').severity).toBe('info');
  });

  it('should return warn severity for En Reparto', () => {
    expect(getTruckHistoryStatusTagInfo('En Reparto').severity).toBe('warn');
  });

  it('should return danger severity for Fuera de servicio', () => {
    expect(getTruckHistoryStatusTagInfo('Fuera de servicio').severity).toBe('danger');
  });

  it('should return secondary severity for unknown status', () => {
    expect(getTruckHistoryStatusTagInfo('Desconocido').severity).toBe('secondary');
  });

  it('should return moon icon for Descanso', () => {
    expect(getTruckHistoryStatusTagInfo('Descanso').icon).toBe('pi pi-moon');
  });

  it('should return send icon for En Reparto', () => {
    expect(getTruckHistoryStatusTagInfo('En Reparto').icon).toBe('pi pi-send');
  });

  it('should return times-circle icon for Fuera de servicio', () => {
    expect(getTruckHistoryStatusTagInfo('Fuera de servicio').icon).toBe('pi pi-times-circle');
  });

  // ─── getLoadPercentage ────────────────────────────────────────────────────────

  it('should return 0 when maxCapacity is 0', () => {
    expect(component.getLoadPercentage(5, 0)).toBe(0);
  });

  it('should return 30 for 3 out of 10 orders', () => {
    expect(component.getLoadPercentage(3, 10)).toBe(30);
  });

  it('should return 100 when all capacity is used', () => {
    expect(component.getLoadPercentage(10, 10)).toBe(100);
  });

  // ─── Pagination ───────────────────────────────────────────────────────────────

  it('should update first/rows and reload trucks on onPageChange', () => {
    const prevCount = truckServiceSpy.getAllTrucksPage.calls.count();
    component.onPageChange({ first: 5, rows: 5, page: 1, pageCount: 2 });
    expect(component.first).toBe(5);
    expect(component.rows).toBe(5);
    expect(truckServiceSpy.getAllTrucksPage.calls.count()).toBeGreaterThan(prevCount);
  });

  it('should use defaults when onPageChange event fields are undefined', () => {
    component.onPageChange({});
    expect(component.first).toBe(0);
    expect(component.rows).toBe(5);
  });

  it('should reset first to 0 and reload trucks on onSearch', () => {
    component.first = 10;
    const prevCount = truckServiceSpy.getAllTrucksPage.calls.count();
    component.onSearch();
    expect(component.first).toBe(0);
    expect(truckServiceSpy.getAllTrucksPage.calls.count()).toBeGreaterThan(prevCount);
  });

  // ─── openHistory ──────────────────────────────────────────────────────────────

  it('should set selectedTruck and open history dialog on openHistory', () => {
    component.openHistory(mockTruckAvailable);
    expect(component.selectedTruck).toBe(mockTruckAvailable);
    expect(component.displayHistoryDialog).toBeTrue();
  });

  it('should set newHistoryStatus to current status on openHistory', () => {
    component.openHistory(mockTruckAvailable);
    expect(component.newHistoryStatus).toBe('Descanso');
  });

  it('should clear newHistoryComment on openHistory', () => {
    component.newHistoryComment = 'old comment';
    component.openHistory(mockTruckAvailable);
    expect(component.newHistoryComment).toBe('');
  });

  // ─── addHistoryComment ────────────────────────────────────────────────────────

  it('should NOT call commentAndOrUpdateTruckStatus when comment is empty', () => {
    component.selectedTruck = mockTruckAvailable;
    component.newHistoryComment = '   ';
    component.addHistoryComment();
    expect(truckServiceSpy.commentAndOrUpdateTruckStatus).not.toHaveBeenCalled();
  });

  it('should NOT call commentAndOrUpdateTruckStatus when selectedTruck is null', () => {
    component.selectedTruck = null;
    component.newHistoryComment = 'Some comment';
    component.addHistoryComment();
    expect(truckServiceSpy.commentAndOrUpdateTruckStatus).not.toHaveBeenCalled();
  });

  it('should call commentAndOrUpdateTruckStatus with correct args', () => {
    component.selectedTruck = { ...mockTruckAvailable };
    component.newHistoryStatus = 'En mantenimiento';
    component.newHistoryComment = 'Revisión programada';
    truckServiceSpy.commentAndOrUpdateTruckStatus.and.returnValue(of({ ...mockTruckAvailable }));
    component.addHistoryComment();
    expect(truckServiceSpy.commentAndOrUpdateTruckStatus).toHaveBeenCalledWith(
      mockTruckAvailable.id, 'En mantenimiento', 'Revisión programada'
    );
  });

  it('should clear newHistoryComment after successful addHistoryComment', () => {
    component.selectedTruck = { ...mockTruckAvailable };
    component.newHistoryComment = 'Un comentario';
    truckServiceSpy.commentAndOrUpdateTruckStatus.and.returnValue(of({ ...mockTruckAvailable }));
    component.addHistoryComment();
    expect(component.newHistoryComment).toBe('');
  });

  it('should show success message when status changes via addHistoryComment', () => {
    component.selectedTruck = { ...mockTruckAvailable };
    component.newHistoryStatus = 'En mantenimiento';
    component.newHistoryComment = 'Avería';
    truckServiceSpy.commentAndOrUpdateTruckStatus.and.returnValue(of({ ...mockTruckAvailable }));
    component.addHistoryComment();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
  });

  it('should show info message when status stays the same via addHistoryComment', () => {
    component.selectedTruck = { ...mockTruckAvailable };
    component.newHistoryStatus = 'Descanso';
    component.newHistoryComment = 'Todo en orden';
    truckServiceSpy.commentAndOrUpdateTruckStatus.and.returnValue(of({ ...mockTruckAvailable }));
    component.addHistoryComment();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'info' }));
  });

  it('should show error message when commentAndOrUpdateTruckStatus fails', () => {
    component.selectedTruck = { ...mockTruckAvailable };
    component.newHistoryComment = 'Error test';
    truckServiceSpy.commentAndOrUpdateTruckStatus.and.returnValue(throwError(() => new Error('500')));
    component.addHistoryComment();
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  it('should update the matching item in trucksPage.items after addHistoryComment', () => {
    const updatedTruck = { ...mockTruckAvailable, referenceCode: 'TRK-UPDATED' };
    component.selectedTruck = { ...mockTruckAvailable };
    component.newHistoryComment = 'Update';
    component.newHistoryStatus = 'Disponible';
    truckServiceSpy.commentAndOrUpdateTruckStatus.and.returnValue(of(updatedTruck));
    component.addHistoryComment();
    const found = component.trucksPage.items.find(t => t.id === mockTruckAvailable.id);
    expect(found?.referenceCode).toBe('TRK-UPDATED');
  });

  // ─── openAssignmentDialog ─────────────────────────────────────────────────────

  it('should call getAvailableDrivers and open assignment dialog', () => {
    component.openAssignmentDialog(mockTruckOnRoute);
    expect(userServiceSpy.getAvailableDrivers).toHaveBeenCalled();
    expect(component.displayAssignmentDialog).toBeTrue();
  });

  it('should set currentTruckForAssignment to the passed truck', () => {
    component.openAssignmentDialog(mockTruckOnRoute);
    expect(component.currentTruckForAssignment).toBe(mockTruckOnRoute);
  });

  it('should include the existing assigned driver in the list even if not in available drivers', () => {
    const extraDriver: User = { ...mockDriver, id: 'driver-extra', name: 'Extra Driver' };
    const truckWithExtra: Truck = { ...mockTruckAvailable, assignedDriver: extraDriver };
    userServiceSpy.getAvailableDrivers.and.returnValue(of([mockDriver, mockDriver2]));
    component.openAssignmentDialog(truckWithExtra);
    expect(component.drivers.some(d => d.id === 'driver-extra')).toBeTrue();
  });

  it('should not duplicate the assigned driver if already in available drivers list', () => {
    const truckWithDriver: Truck = { ...mockTruckAvailable, assignedDriver: mockDriver };
    userServiceSpy.getAvailableDrivers.and.returnValue(of([mockDriver, mockDriver2]));
    component.openAssignmentDialog(truckWithDriver);
    const count = component.drivers.filter(d => d.id === mockDriver.id).length;
    expect(count).toBe(1);
  });

  it('should set selectedDriver to current assigned driver on openAssignmentDialog', () => {
    const truckWithDriver: Truck = { ...mockTruckAvailable, assignedDriver: mockDriver };
    userServiceSpy.getAvailableDrivers.and.returnValue(of([mockDriver, mockDriver2]));
    component.openAssignmentDialog(truckWithDriver);
    expect(component.selectedDriver?.id).toBe(mockDriver.id);
  });

  it('should set selectedDriver to undefined when truck has no assigned driver', () => {
    userServiceSpy.getAvailableDrivers.and.returnValue(of([mockDriver, mockDriver2]));
    component.openAssignmentDialog(mockTruckOnRoute);
    expect(component.selectedDriver).toBeUndefined();
  });

  it('should show error message when getAvailableDrivers fails', () => {
    userServiceSpy.getAvailableDrivers.and.returnValue(throwError(() => new Error('500')));
    component.openAssignmentDialog(mockTruckOnRoute);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  // ─── cancelAssignment ─────────────────────────────────────────────────────────

  it('should close dialog and clear state on cancelAssignment', () => {
    component.displayAssignmentDialog    = true;
    component.currentTruckForAssignment  = mockTruckAvailable;
    component.selectedDriver             = mockDriver;
    component.cancelAssignment();
    expect(component.displayAssignmentDialog).toBeFalse();
    expect(component.currentTruckForAssignment).toBeUndefined();
    expect(component.selectedDriver).toBeUndefined();
  });

  // ─── confirmAssignment ────────────────────────────────────────────────────────

  it('should call assignDriver with selected driver id and truck id', () => {
    component.selectedDriver = mockDriver;
    component.confirmAssignment(mockTruckOnRoute.id);
    expect(truckServiceSpy.assignDriver).toHaveBeenCalledWith(mockDriver.id, mockTruckOnRoute.id, true);
  });

  it('should NOT call assignDriver when selectedDriver is undefined', () => {
    component.selectedDriver = undefined;
    component.confirmAssignment(mockTruckOnRoute.id);
    expect(truckServiceSpy.assignDriver).not.toHaveBeenCalled();
  });

  it('should update assignedDriver in trucksPage after confirmAssignment', () => {
    component.selectedDriver = mockDriver2;
    truckServiceSpy.assignDriver.and.returnValue(of({ ...mockTruckOnRoute, assignedDriver: mockDriver2 }));
    component.confirmAssignment(mockTruckOnRoute.id);
    const truck = component.trucksPage.items.find(t => t.id === mockTruckOnRoute.id);
    expect(truck?.assignedDriver?.id).toBe(mockDriver2.id);
  });

  it('should close assignment dialog after confirmAssignment succeeds', () => {
    component.selectedDriver             = mockDriver;
    component.displayAssignmentDialog    = true;
    component.currentTruckForAssignment  = mockTruckAvailable;
    truckServiceSpy.assignDriver.and.returnValue(of({ ...mockTruckAvailable }));
    component.confirmAssignment(mockTruckAvailable.id);
    expect(component.displayAssignmentDialog).toBeFalse();
  });

  // ─── unassignDriver ───────────────────────────────────────────────────────────

  it('should call assignDriver with id -1 and state false for unassign', () => {
    truckServiceSpy.assignDriver.and.returnValue(of({ ...mockTruckAvailable, assignedDriver: undefined }));
    component.unassignDriver(mockTruckAvailable.id);
    expect(truckServiceSpy.assignDriver).toHaveBeenCalledWith('-1', mockTruckAvailable.id, false);
  });

  it('should clear assignedDriver in trucksPage after unassign', () => {
    truckServiceSpy.assignDriver.and.returnValue(of({ ...mockTruckAvailable, assignedDriver: undefined }));
    component.currentTruckForAssignment = mockTruckAvailable;
    component.unassignDriver(mockTruckAvailable.id);
    const truck = component.trucksPage.items.find(t => t.id === mockTruckAvailable.id);
    expect(truck?.assignedDriver).toBeUndefined();
  });

  it('should show success message after successful unassign', () => {
    truckServiceSpy.assignDriver.and.returnValue(of({ ...mockTruckAvailable, assignedDriver: undefined }));
    component.currentTruckForAssignment = mockTruckAvailable;
    component.unassignDriver(mockTruckAvailable.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
  });

  it('should show error message when unassignDriver fails', () => {
    truckServiceSpy.assignDriver.and.returnValue(throwError(() => new Error('500')));
    component.unassignDriver(mockTruckAvailable.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error' }));
  });

  // ─── deleteTruck ──────────────────────────────────────────────────────────────

  it('should open a confirmation dialog on deleteTruck', () => {
    spyOn(confirmationService, 'confirm').and.callThrough();
    component.deleteTruck(mockTruckAvailable.id);
    expect(confirmationService.confirm).toHaveBeenCalled();
  });

  it('should call truckService.deleteTruck and reload when confirmed', () => {
    spyOn(confirmationService, 'confirm').and.callFake((config: any) => config.accept());
    const prevCount = truckServiceSpy.getAllTrucksPage.calls.count();
    component.deleteTruck(mockTruckAvailable.id);
    expect(truckServiceSpy.deleteTruck).toHaveBeenCalledWith(mockTruckAvailable.id);
    expect(truckServiceSpy.getAllTrucksPage.calls.count()).toBeGreaterThan(prevCount);
  });

  it('should show success message after confirmed delete', () => {
    spyOn(confirmationService, 'confirm').and.callFake((config: any) => config.accept());
    component.deleteTruck(mockTruckAvailable.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
  });

  it('should show error message when delete fails after confirmation', () => {
    spyOn(confirmationService, 'confirm').and.callFake((config: any) => config.accept());
    truckServiceSpy.deleteTruck.and.returnValue(throwError(() => new Error('500')));
    component.deleteTruck(mockTruckAvailable.id);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'danger' }));
  });

  // ─── onViewModeChange ─────────────────────────────────────────────────────────

  it('should remain on "map" mode by default', () => {
    expect(component.selectedViewMode).toBe('map');
  });

  it('should switch to chart mode when selectedViewMode is set to "chart"', () => {
    component.selectedViewMode = 'chart';
    component.onViewModeChange();
    expect(component.selectedViewMode).toBe('chart');
  });

  // ─── DOM: loading / error screen ──────────────────────────────────────────────

  it('should show the loading screen when loading=true', () => {
    component.loading = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should show the error screen when error=true', () => {
    component.error = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-loading-screen')).not.toBeNull();
  });

  it('should hide the loading screen and render main content when fully loaded', () => {
    expect(fixture.nativeElement.querySelector('app-loading-screen')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Gestor de Camiones');
  });

  // ─── DOM: KPI cards ───────────────────────────────────────────────────────────

  it('should display KPI labels in the DOM', () => {
    expect(fixture.nativeElement.textContent).toContain('Camiones Totales');
    expect(fixture.nativeElement.textContent).toContain('Sin Conductor Asignado');
    expect(fixture.nativeElement.textContent).toContain('En Ruta');
    expect(fixture.nativeElement.textContent).toContain('Fuera de Servicio');
  });

  it('should display KPI values in the DOM', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain(String(component.totalTrucks));
    expect(text).toContain(String(component.noDriverTrucks));
    expect(text).toContain(String(component.onRouteTrucks));
    expect(text).toContain(String(component.maintenanceTrucks));
  });

  // ─── DOM: trucks table ────────────────────────────────────────────────────────

  it('should display truck plate numbers in the table', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain(mockTruckAvailable.plateNumber);
    expect(text).toContain(mockTruckOnRoute.plateNumber);
  });

  it('should display reference codes in the table', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain(mockTruckAvailable.referenceCode);
  });

  it('should display the assigned driver name in the table', () => {
    expect(fixture.nativeElement.textContent).toContain(mockDriver.name);
  });

  it('should show "No asignado" for trucks without a driver', () => {
    expect(fixture.nativeElement.textContent).toContain('No asignado');
  });

  it('should show "No se encontraron camiones." when trucksPage is empty', () => {
    component.trucksPage = { ...emptyPage };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron camiones.');
  });

  // ─── DOM: "Añadir Camión" button ──────────────────────────────────────────────

  it('should render the "Añadir Camión" button', () => {
    expect(fixture.nativeElement.textContent).toContain('Añadir Camión');
  });
});