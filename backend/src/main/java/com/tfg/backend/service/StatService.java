package com.tfg.backend.service;

import com.tfg.backend.model.User;
import com.tfg.backend.utils.StatDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatService {

    private final OrderService orderService;
    private final UserService userService;
    private final ShopService shopService;
    private final TruckService truckService;

    public List<StatDTO> getOrdersStatsByRole() {
        User loggedUser = userService.findLoggedUserHelper();
        return orderService.getOrdersStatistics(loggedUser);
    }

    public List<StatDTO> getShopsStatsByRole() {
        User loggedUser = userService.findLoggedUserHelper();
        return shopService.getShopsStatistics(loggedUser);
    }

    public List<StatDTO> getTrucksStatsByRole() {
        User loggedUser = userService.findLoggedUserHelper();
        return truckService.getTruckStatistics(loggedUser);
    }

}
