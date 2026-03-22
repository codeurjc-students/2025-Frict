package com.tfg.backend.controller;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.*;
import com.tfg.backend.service.OrderService;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.TruckService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/trucks")
@Tag(name = "Trucks Management", description = "System trucks data management")
public class TruckRestController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private TruckService truckService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;


    @Operation(summary = "(Admin) Get all trucks information (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<TruckDTO>> getAllTrucksPage(Pageable pageable) {
        Page<Truck> trucks = truckService.findAll(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(trucks, TruckDTO::new));
    }


    @Operation(summary = "(Admin, Manager) Get truck information by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TruckDTO> getTruckById(@PathVariable Long id) {
        Optional<Truck> truckOptional = truckService.findById(id);
        if(truckOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Truck with ID " + id + " does not exist.");
        }
        return ResponseEntity.ok(new TruckDTO(truckOptional.get()));
    }


    @Operation(summary = "(Driver) Get assigned truck by driver ID")
    @GetMapping("/user/{driverId}")
    public ResponseEntity<TruckDTO> getAssignedTruckByDriverId(@PathVariable Long driverId) {
        User loggedUser = userService.findUserHelper(driverId);
        Truck assignedTruck = loggedUser.getAssignedTruck();
        if (assignedTruck == null){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new TruckDTO(loggedUser.getAssignedTruck()));
    }


    @Operation(summary = "(Admin, Manager) Get trucks list by shop ID")
    @GetMapping("/shop/{shopId}/list")
    public ResponseEntity<List<TruckDTO>> getAllShopTrucks(@PathVariable Long shopId) {
        Shop shop = shopService.findShopHelper(shopId);
        List<TruckDTO> dtos = shop.getAssignedTrucks().stream().map(TruckDTO::new).toList();
        return ResponseEntity.ok(dtos);
    }


    @Operation(summary = "(All) Get trucks page by shop ID")
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<PageResponse<TruckDTO>> getTrucksByShopId(@PathVariable Long shopId, Pageable pageable) {
        Page<Truck> trucks = truckService.findAllByAssignedShopId(shopId, pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(trucks, TruckDTO::new));
    }


    @Operation(summary = "(Admin, Manager) Get all unassigned trucks")
    @GetMapping("/available/")
    public ResponseEntity<List<TruckDTO>> getAllUnassignedTrucks() {
        List<TruckDTO> dtos = truckService.findAllByAssignedShopIsNull().stream().map(TruckDTO::new).toList();
        return ResponseEntity.ok(dtos);
    }


    @Operation(summary = "(Admin) Set driver assignment to a truck")
    @PutMapping("/{truckId}/assign/driver/{driverId}")
    public ResponseEntity<TruckDTO> setAssignedDriver(@PathVariable Long driverId, @PathVariable Long truckId, @RequestParam boolean state) {
        Truck truck = truckService.findTruckHelper(truckId);

        if (state) {
            User user = userService.findUserHelper(driverId);
            truck.setAssignedDriver(user);
        }
        else {
            truck.setAssignedDriver(null);
        }

        Truck savedTruck = truckService.save(truck);
        return ResponseEntity.ok(new TruckDTO(savedTruck));
    }


    @Operation(summary = "(Admin) Comment and/or update truck status by ID")
    @PutMapping("/status/{id}")
    public ResponseEntity<TruckDTO> commentAndOrUpdateTruckStatus(@PathVariable Long id,
                                                                  @RequestParam TruckStatus truckStatus,
                                                                  @RequestParam(required = false) String comment){
        //Check if the order exists
        Truck truck = truckService.findTruckHelper(id);

        //Difference between commenting only or changing status and commenting
        //If status has not changed, then it is commenting only
        if (truckStatus == truck.getHistory().getLast().getStatus()) {
            truck.addStatusUpdate(comment);
        }
        else { //Change status and save the comment as the first of the updates list for that status
            truck.changeTruckStatus(truckStatus, comment);
        }

        Truck savedTruck = truckService.save(truck);
        return ResponseEntity.ok(new TruckDTO(savedTruck));
    }


    @Operation(summary = "(Admin) Create truck")
    @PostMapping
    public ResponseEntity<TruckDTO> createTruck(@RequestBody TruckDTO truckDTO) {
        AddressDTO dto = truckDTO.getAddress();
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(truckDTO.getAddress().getLatitude());
        address.setLongitude(truckDTO.getAddress().getLongitude());
        Truck truck = new Truck(truckDTO.getPlateNumber(), address, truckDTO.getMaxOrderCapacity());

        if (truckDTO.getShopId() != null){
            Shop assignedShop = shopService.findShopHelper(truckDTO.getShopId());
            truck.setAssignedShop(assignedShop);
        }

        Truck savedTruck = truckService.save(truck);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedTruck.getId())
                .toUri();

        return ResponseEntity.created(location).body(new TruckDTO(savedTruck));
    }

    @Operation(summary = "(Admin) Update truck by ID")
    @PutMapping("/{id}")
    public ResponseEntity<TruckDTO> updateTruck(@PathVariable Long id, @RequestBody TruckDTO truckDTO) {
        Truck truck = truckService.findTruckHelper(id);

        truck.setReferenceCode(truckDTO.getReferenceCode());
        truck.setPlateNumber(truckDTO.getPlateNumber());

        AddressDTO dto = truckDTO.getAddress();
        Address address = new Address(dto.getAlias(), dto.getStreet(), dto.getNumber(), dto.getFloor(), dto.getPostalCode(), dto.getCity(), dto.getCountry());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        truck.setAddress(address);

        truck.setMaxOrderCapacity(truckDTO.getMaxOrderCapacity());

        if (truckDTO.getShopId() != null){
            Shop assignedShop = shopService.findShopHelper(truckDTO.getShopId());
            truck.setAssignedShop(assignedShop);
        }

        Truck savedTruck = truckService.save(truck);
        return ResponseEntity.accepted().body(new TruckDTO(savedTruck));
    }


    @Operation(summary = "(Admin) Delete truck by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<TruckDTO> deleteTruck(@PathVariable Long id) {
        Truck truck = truckService.findTruckHelper(id);

        //Unlink orders
        Set<Order> linkedOrders = truck.getOrdersToDeliver();
        for (Order o : linkedOrders) {
            if (o.getHistory().getLast().getStatus() == OrderStatus.ON_DELIVERY){
                o.changeOrderStatus(OrderStatus.SENT, "El camión ha sido borrado y el pedido ha vuelto al estado anterior.");
            }
            o.setAssignedTruck(null);
        }
        truck.getOrdersToDeliver().clear();
        orderService.saveAll(linkedOrders);
        truckService.delete(truck);
        return ResponseEntity.ok(new TruckDTO(truck));
    }
}
