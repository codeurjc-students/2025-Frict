package com.tfg.backend.service;

import com.tfg.backend.dto.CartSummaryDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.dto.EntityType;
import com.tfg.backend.dto.EventAction;
import com.tfg.backend.event.OrderEvent;
import com.tfg.backend.model.Registry;
import com.tfg.backend.event.RegistryEvent;
import com.tfg.backend.model.RegistryType;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.utils.PdfService;
import com.tfg.backend.utils.SaveResult;
import com.tfg.backend.dto.StatDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final PdfService pdfService;
    private final UserService userService;
    private final TruckService truckService;
    private final ShopService shopService;
    private final EmailService emailService;
    private final OrderItemService orderItemService;
    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    // --- READ-ONLY METHODS ---

    public Optional<Order> findById(Long id){ return orderRepository.findById(id); }
    public Page<Order> findAll(Pageable pageable) { return orderRepository.findAll(pageable); }
    public List<Order> findAll() { return orderRepository.findAll(); }
    public Page<Order> findOrdersByManagerId(Long managerId, Pageable pageable) { return orderRepository.findByAssignedShop_AssignedManager_Id(managerId, pageable); }
    public Page<Order> findOrdersByDriverId(Long driverId, Pageable pageable) { return orderRepository.findByAssignedTruck_AssignedDriver_Id(driverId, pageable); }
    public Page<Order> findOrdersByUser(User u, Pageable pageInfo){ return orderRepository.findAllByUser(u, pageInfo); }
    public boolean existsByIdAndUser(Long orderId, User user) { return orderRepository.existsByIdAndUser(orderId, user); }

    public Order findOrderHelper(Long id) {
        return this.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order with ID " + id + " does not exist."));
    }

    public Page<Order> getOrdersByRole(Pageable pageable){
        User loggedUser = userService.findLoggedUserHelper();
        if (loggedUser.getRoles().contains("ADMIN")) return this.findAll(pageable);
        if (loggedUser.getRoles().contains("DRIVER")) return this.findOrdersByDriverId(loggedUser.getId(), pageable);
        return this.findOrdersByManagerId(loggedUser.getId(), pageable);
    }

    public Page<Order> getUserOrdersByUserId(Long id, Pageable pageable){
        User user = userService.findUserHelper(id);
        return this.findOrdersByUser(user, pageable);
    }

    public Page<Order> getAllUserOrders(Pageable pageable){
        User loggedUser = userService.findLoggedUserHelper();
        return this.findOrdersByUser(loggedUser, pageable);
    }

    public String getOrderQrToken(Long id){
        User user = userService.findLoggedUserHelper();
        Order order = this.findOrderHelper(id);

        if (!user.getUsername().equals(order.getUser().getUsername())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be the user that placed the order.");
        }
        return order.getQrDeliveryToken();
    }

    @Transactional
    public boolean checkOrderQrToken(Long id, String token){
        Order order = this.findOrderHelper(id);

        if (!order.getQrDeliveryToken().equals(token)){
            return false;
        }
        else {
            this.commentAndOrUpdateOrderStatus(id, OrderStatus.COMPLETED, "Entrega confirmada mediante validación QR del cliente en destino.");
            return true;
        }
    }

    // --- WRITING METHODS (override @Transactional) ---

    @Transactional
    public Order save(Order o) { return orderRepository.save(o); }

    @Transactional
    public void saveAll(Set<Order> o) { orderRepository.saveAll(o); }

    @Transactional
    public void delete(Order o) { orderRepository.delete(o); }

    @Transactional
    public Order setAssignedTruck(Long orderId, Long truckId, boolean state){
        Order order = this.findOrderHelper(orderId);

        //Get notification data
        String managerUsername = Optional.ofNullable(order.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        String driverUsername;

        if (state) {
            Truck truck = truckService.findTruckHelper(truckId);
            if (!Objects.equals(order.getAssignedShop().getId(), truck.getAssignedShop().getId())){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This truck and this order do not belong to the same shop.");
            }
            if (order.getAssignedTruck() != null && order.getAssignedTruck().getId().equals(truck.getId())) {
                return order;
            }
            if (truck.getOrdersToDeliver().size() >= truck.getMaxOrderCapacity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This truck capacity is full and cannot accept any more orders.");
            }
            order.setAssignedTruck(truck);

            driverUsername = Optional.of(truck).map(Truck::getAssignedDriver).map(User::getUsername).orElse(null);

        } else {
            driverUsername = Optional.ofNullable(order.getAssignedTruck()).map(Truck::getAssignedDriver).map(User::getUsername).orElse(null);
            order.setAssignedTruck(null);
        }

        // Send notifications
        OrderEvent orderEvent = new OrderEvent(EventAction.ASSIGNED, String.valueOf(order.getId()), null, null, null, managerUsername, driverUsername);
        eventPublisher.publishEvent(orderEvent);

        return order; // Saved automatically
    }


    @Transactional
    public Order unassignAsFinished(Long orderId){
        Order order = this.findOrderHelper(orderId);
        OrderStatus orderStatus = order.getCurrentStatus();
        if (!orderStatus.equals(OrderStatus.COMPLETED) && !orderStatus.equals(OrderStatus.CANCELLED)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order must be completed or cancelled to be unassigned by yourself.");
        }

        Truck assignedTruck = order.getAssignedTruck();
        if (assignedTruck == null || assignedTruck.getAssignedDriver() == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order must have a truck and a driver assigned to perform this operation.");
        }

        User loggedUser = this.userService.findLoggedUserHelper();
        if (!assignedTruck.getAssignedDriver().getUsername().equals(loggedUser.getUsername())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order must be assigned to your truck to perform this operation.");
        }

        order.setAssignedTruck(null);
        return order;
    }

    @Transactional
    public Order createOrder(Long addressId, Long cardId){
        User loggedUser = userService.findLoggedUserHelper();
        if (loggedUser.getSelectedShop() == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User must have an assigned shop to complete an order");
        }

        Shop selectedShop = shopService.findShopHelper(loggedUser.getSelectedShop().getId());

        Address address = loggedUser.getAddresses().stream().filter(addr -> addr.getId().equals(addressId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found."));

        PaymentCard card = loggedUser.getCards().stream().filter(c -> c.getId().equals(cardId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found."));

        List<OrderItem> cartItems = orderItemService.findUserCartItemsList(loggedUser.getId());

        //Stores product references for later registries
        Map<OrderItem, String> productRefMap = new HashMap<>();

        // Validate AND reduce stock in a single pass
        for (OrderItem i : cartItems) {
            ShopStock localStock = i.getProduct().getShopsStock().stream()
                    .filter(stock -> stock.getShop().getId().equals(selectedShop.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "El producto " + i.getProduct().getName() + " no está disponible en tu tienda seleccionada."));

            if (localStock.getUnits() < i.getQuantity()){
                throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "No hay suficiente stock del producto " + i.getProduct().getName() + " en tu tienda seleccionada.");
            }

            // Decrease stock (Safe because @Transactional will roll this back if a later item throws an exception)
            localStock.setUnits(localStock.getUnits() - i.getQuantity());

            //Save the product reference
            productRefMap.put(i, i.getProduct().getReferenceCode());

            // Make the order item historical
            i.setProductName(i.getProduct().getName());
            i.setProductImageUrl(i.getProduct().getImages().getFirst().getImageUrl());
            i.setProductPrice(i.getProduct().getCurrentPrice());
            i.setProduct(null);
        }

        Order newOrder = new Order(loggedUser, cartItems, selectedShop, address, card);
        newOrder.setFullSendingAddress(address);
        newOrder.setCardNumberEnding(card.getNumber().substring(card.getNumber().length() - 4));

        double orderTotal = newOrder.getTotalCost();
        selectedShop.setAssignedBudget(selectedShop.getAssignedBudget() + orderTotal);

        Order savedOrder = orderRepository.save(newOrder);

        // Send in-app notifications
        String managerUsername = Optional.ofNullable(savedOrder.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        OrderEvent orderEvent = new OrderEvent(EventAction.CREATED, String.valueOf(savedOrder.getId()), null, null, loggedUser.getUsername(), managerUsername, null);
        eventPublisher.publishEvent(orderEvent);

        //Add registries
        for (OrderItem i : cartItems) {
            Registry unitsSoldRegistry = new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_UNITS_SOLD, (double) i.getQuantity(), selectedShop.getReferenceCode(), selectedShop.getName(), loggedUser.getUsername(), loggedUser.getName(), productRefMap.get(i), i.getProductName(), savedOrder.getReferenceCode(), "Pedido " + savedOrder.getReferenceCode());
            eventPublisher.publishEvent(new RegistryEvent(unitsSoldRegistry));
        }
        Registry orderRegistry = new Registry(EntityType.ORDER, RegistryType.USER_ORDERS, 1.0, selectedShop.getReferenceCode(), selectedShop.getName(), loggedUser.getUsername(), loggedUser.getName(), null, null, savedOrder.getReferenceCode(), "Pedido " + savedOrder.getReferenceCode());
        eventPublisher.publishEvent(new RegistryEvent(orderRegistry));

        Registry budgetRegistry = new Registry(EntityType.SHOP, RegistryType.SHOP_BUDGET, orderTotal, selectedShop.getReferenceCode(), selectedShop.getName(), loggedUser.getUsername(), loggedUser.getName(), null, null, savedOrder.getReferenceCode(), "Pedido " + savedOrder.getReferenceCode());
        eventPublisher.publishEvent(new RegistryEvent(budgetRegistry));

        // Send email confirmation
        emailService.sendOrderConfirmation(loggedUser.getEmail(), loggedUser.getName(), savedOrder.getReferenceCode(), savedOrder.getItems(), savedOrder.getTotalCost());
        return savedOrder;
    }

    @Transactional
    public Order commentAndOrUpdateOrderStatus(Long id, OrderStatus orderStatus, String comment) {
        Order order = this.findOrderHelper(id);

        //Get notification data
        String managerUsername = Optional.ofNullable(order.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        String driverUsername = Optional.ofNullable(order.getAssignedTruck()).map(Truck::getAssignedDriver).map(User::getUsername).orElse(null);

        if (orderStatus == order.getCurrentStatus()) {
            order.addStatusUpdate(comment);

            // Send in-app notifications (user is set in listener, as it is not directly used here)
            OrderEvent orderEvent = new OrderEvent(EventAction.NEW_COMMENT, String.valueOf(order.getId()), null, null, order.getUser().getUsername(), managerUsername, driverUsername);
            eventPublisher.publishEvent(orderEvent);

        } else {
            if (order.getCurrentStatus() == OrderStatus.CANCELLED || order.getCurrentStatus() == OrderStatus.COMPLETED){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cancelled or completed orders cannot change status.");
            }
            if(orderStatus.equals(OrderStatus.ON_DELIVERY) && order.getAssignedTruck() == null){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The order must have an associated delivery truck to be delivered.");
            }

            OrderStatus currentStatus = order.getCurrentStatus();
            order.changeOrderStatus(orderStatus, comment);

            // Send notifications
            OrderEvent orderEvent = new OrderEvent(EventAction.STATUS_CHANGED, String.valueOf(order.getId()), currentStatus.getDescription(), orderStatus.getDescription(), order.getUser().getUsername(), managerUsername, driverUsername);
            eventPublisher.publishEvent(orderEvent);

            if (orderStatus.equals(OrderStatus.COMPLETED) || orderStatus.equals(OrderStatus.CANCELLED)){
                RegistryType registryType = switch (orderStatus) {
                    case COMPLETED -> RegistryType.ORDERS_COMPLETED;
                    case CANCELLED -> RegistryType.ORDERS_CANCELLED;
                    default -> null;
                };

                User loggedUser = userService.findLoggedUserHelper();
                Registry userOrderRegistry = new Registry(EntityType.ORDER, registryType, 1.0, order.getAssignedShop().getReferenceCode(), order.getAssignedShop().getName(), loggedUser.getUsername(), loggedUser.getName(), null, null, order.getReferenceCode(), "Pedido " + order.getReferenceCode());
                eventPublisher.publishEvent(new RegistryEvent(userOrderRegistry));

                if (order.getAssignedTruck() != null && order.getAssignedTruck().getAssignedDriver() != null){
                    User driver = order.getAssignedTruck().getAssignedDriver();
                    Registry driverOrderRegistry = new Registry(EntityType.ORDER, registryType, 1.0, order.getAssignedShop().getReferenceCode(), order.getAssignedShop().getName(), driver.getUsername(), driver.getName(), null, null, order.getReferenceCode(), "Pedido " + order.getReferenceCode());
                    eventPublisher.publishEvent(new RegistryEvent(driverOrderRegistry));
                }
            }
        }
        return order;
    }

    @Transactional
    public Order cancelOrder(Long id){
        User loggedUser = userService.findLoggedUserHelper();
        Order order = this.findOrderHelper(id);

        // Check if it is the original buyer
        boolean isBuyer = order.getUser() != null && order.getUser().getId().equals(loggedUser.getId());

        // Check if it is the delivery driver assigned to this order
        boolean isAssignedDriver = order.getAssignedTruck() != null &&
                order.getAssignedTruck().getAssignedDriver() != null &&
                order.getAssignedTruck().getAssignedDriver().getId().equals(loggedUser.getId());

        if(!isBuyer && !isAssignedDriver){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order with ID " + id + " cannot be cancelled by this user.");
        }

        String reason = loggedUser.getRoles().contains("DRIVER") ? "El pedido ha sido cancelado por el repartidor." : "Has cancelado este pedido.";

        OrderStatus currentStatus = order.getCurrentStatus();
        order.changeOrderStatus(OrderStatus.CANCELLED, reason);

        // Send notifications
        String managerUsername = Optional.ofNullable(order.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        String driverUsername = Optional.ofNullable(order.getAssignedTruck()).map(Truck::getAssignedDriver).map(User::getUsername).orElse(null);
        OrderEvent orderEvent = new OrderEvent(EventAction.STATUS_CHANGED, String.valueOf(order.getId()), currentStatus.getDescription(), OrderStatus.CANCELLED.getDescription(), loggedUser.getUsername(), managerUsername, driverUsername);
        eventPublisher.publishEvent(orderEvent);

        Registry orderRegistry = new Registry(EntityType.ORDER, RegistryType.ORDERS_CANCELLED, 1.0, order.getAssignedShop().getReferenceCode(), order.getAssignedShop().getName(), loggedUser.getUsername(), loggedUser.getName(), null, null, order.getReferenceCode(), "Pedido " + order.getReferenceCode());
        eventPublisher.publishEvent(new RegistryEvent(orderRegistry));

        return order; // Saved automatically
    }

    @Transactional
    public Order deleteFinishedOrderById(Long id){
        Order order = this.findOrderHelper(id);

        //Get notification data
        String managerUsername = Optional.ofNullable(order.getAssignedShop()).map(Shop::getAssignedManager).map(User::getUsername).orElse(null);
        String driverUsername = Optional.ofNullable(order.getAssignedTruck()).map(Truck::getAssignedDriver).map(User::getUsername).orElse(null);

        OrderStatus currentStatus = order.getCurrentStatus();
        if (currentStatus != OrderStatus.CANCELLED && currentStatus != OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only cancelled or completed orders can be deleted. Actual status: " + currentStatus);
        }

        if (order.getAssignedShop() != null) order.getAssignedShop().getAssignedOrders().remove(order);
        if (order.getAssignedTruck() != null) order.getAssignedTruck().getOrdersToDeliver().remove(order);
        if (order.getUser() != null) order.getUser().getRegisteredOrders().remove(order);

        orderRepository.delete(order);

        // Send in-app notifications (user is set in listener, as it is not directly used here)
        OrderEvent orderEvent = new OrderEvent(EventAction.DELETED, String.valueOf(order.getId()), null, null, order.getUser().getUsername(), managerUsername, driverUsername);
        eventPublisher.publishEvent(orderEvent);

        return order;
    }

    // --- CART METHODS ---

    public CartSummaryDTO getCartSummary(){
        User loggedUser = userService.findLoggedUserHelper();
        List<OrderItem> cartItems = orderItemService.findUserCartItemsList(loggedUser.getId());

        int totalItems = 0;
        double subtotal = 0.0;
        double totalDiscount = 0.0;
        double total = 0.0;

        for (OrderItem item : cartItems) {
            Product p = item.getProduct();
            double currentPrice = item.getProductPrice();
            double previousPrice = (p != null) ? p.getPreviousPrice() : 0.0;
            int quantity = item.getQuantity();

            totalItems += quantity;
            double unitSubtotal = (previousPrice > 0) ? previousPrice : currentPrice;
            subtotal += unitSubtotal * quantity;
            total += currentPrice * quantity;

            if (previousPrice > currentPrice) {
                totalDiscount += (previousPrice - currentPrice) * quantity;
            }
        }

        double shippingCost = (total > 50.0) ? 0.0 : 5.0;
        return new CartSummaryDTO(totalItems, Math.round(subtotal * 100.0) / 100.0, Math.round(totalDiscount * 100.0) / 100.0, shippingCost, Math.round(total * 100.0) / 100.0);
    }

    public Page<OrderItem> getCartItemsPage(Pageable pageable) {
        User loggedUser = userService.findLoggedUserHelper();
        Page<OrderItem> cartItems = orderItemService.findUserCartItemsPage(loggedUser.getId(), pageable);
        List<Product> productsInCart = cartItems.getContent().stream().map(OrderItem::getProduct).filter(Objects::nonNull).toList();
        productService.enrichWithStock(productsInCart);
        return cartItems;
    }

    public OrderItem getCartItemByProductId(Long id){
        User loggedUser = userService.findLoggedUserHelper();
        Optional<OrderItem> itemOptional = orderItemService.findUserCartItemByProductId(id, loggedUser.getId());
        if (itemOptional.isEmpty()) return null;

        OrderItem item = itemOptional.get();
        productService.enrichWithStock(item.getProduct());
        return item;
    }

    @Transactional
    public CartSummaryDTO clearCartItems(){
        User loggedUser = userService.findLoggedUserHelper();
        List<OrderItem> itemsToRemove = loggedUser.getItemsInCart();

        if (!itemsToRemove.isEmpty()) {
            loggedUser.getAllOrderItems().removeAll(itemsToRemove);
        }
        return this.getCartSummary();
    }

    @Transactional
    public SaveResult<OrderItem> addItemToCart(Long id, int quantity){
        User loggedUser = userService.findLoggedUserHelper();
        Product product = productService.findProductHelper(id);

        int inCartUnits = orderItemService.findProductUnitsInCart(id).stream().mapToInt(OrderItem::getQuantity).sum();

        if(inCartUnits + quantity > product.getAvailableUnits()){
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Stock of product with ID " + id + " is not enough.");
        }

        Optional<OrderItem> itemInCart = loggedUser.getItemsInCart().stream().filter(item -> item.getProduct().getId().equals(id)).findFirst();

        if (itemInCart.isPresent()) {
            OrderItem resultItem = itemInCart.get();
            resultItem.setQuantity(resultItem.getQuantity() + quantity);
            return new SaveResult<>(resultItem, false);
        } else {
            OrderItem resultItem = new OrderItem(product, loggedUser, quantity);
            loggedUser.getAllOrderItems().add(resultItem);
            return new SaveResult<>(resultItem, true);
        }
    }

    @Transactional
    public void updateItemQuantity(Long id, int quantity){
        User loggedUser = userService.findLoggedUserHelper();
        OrderItem itemToUpdate = loggedUser.getItemsInCart().stream().filter(item -> item.getProduct().getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item in cart with ID " + id + " does not exist."));

        int maxAchievableQuantity = itemToUpdate.getProduct().getShopsStock().stream().mapToInt(ShopStock::getUnits).sum();

        if (quantity > maxAchievableQuantity) {
            quantity = maxAchievableQuantity;
        } else if (quantity < 0) {
            if(maxAchievableQuantity > 0){
                quantity = 1;
            } else {
                throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Updating order item not available.");
            }
        }
        itemToUpdate.setQuantity(quantity);
    }

    @Transactional
    public void deleteCartItem(Long id){
        User loggedUser = userService.findLoggedUserHelper();
        boolean removed = loggedUser.getAllOrderItems().removeIf(i -> i.getOrder() == null && i.getProduct() != null && i.getProduct().getId() != null && i.getId().equals(id));
        if(!removed){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not deleted as it does not exist.");
        }
    }

    public byte[] generateOrderPdfInvoice(Long orderId) {
        Order order = this.findOrderHelper(orderId);
        String qrToken = this.getOrderQrToken(orderId);
        return pdfService.generateOrderInvoicePdf(order, qrToken);
    }

    // --- METRICS METHOD ---

    public List<StatDTO> getOrdersStatistics(User currentUser) {
        Long userId = currentUser.getId();
        long orderMade = 0, sent = 0, onDelivery = 0, completed = 0;

        if (currentUser.hasRole("ADMIN")) {
            orderMade = orderRepository.countOrdersByStatusIn(List.of(OrderStatus.ORDER_MADE));
            sent = orderRepository.countOrdersByStatusIn(List.of(OrderStatus.SENT));
            onDelivery = orderRepository.countOrdersByStatusIn(List.of(OrderStatus.ON_DELIVERY));
            completed = orderRepository.countOrdersByStatusIn(List.of(OrderStatus.COMPLETED));
        } else if (currentUser.hasRole("MANAGER")) {
            orderMade = orderRepository.countOrdersByManagerIdAndStatusIn(userId, List.of(OrderStatus.ORDER_MADE));
            sent = orderRepository.countOrdersByManagerIdAndStatusIn(userId, List.of(OrderStatus.SENT));
            onDelivery = orderRepository.countOrdersByManagerIdAndStatusIn(userId, List.of(OrderStatus.ON_DELIVERY));
            completed = orderRepository.countOrdersByManagerIdAndStatusIn(userId, List.of(OrderStatus.COMPLETED));
        } else if (currentUser.hasRole("DRIVER")) {
            orderMade = orderRepository.countOrdersByDriverIdAndStatusIn(userId, List.of(OrderStatus.ORDER_MADE));
            sent = orderRepository.countOrdersByDriverIdAndStatusIn(userId, List.of(OrderStatus.SENT));
            onDelivery = orderRepository.countOrdersByDriverIdAndStatusIn(userId, List.of(OrderStatus.ON_DELIVERY));
            completed = orderRepository.countOrdersByDriverIdAndStatusIn(userId, List.of(OrderStatus.COMPLETED));
        }

        return List.of(
                new StatDTO("Realizados", orderMade),
                new StatDTO("Enviados", sent),
                new StatDTO("En Reparto", onDelivery),
                new StatDTO("Completados", completed)
        );
    }
}