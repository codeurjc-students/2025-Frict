package com.tfg.backend.service;

import com.tfg.backend.model.User;
import com.tfg.backend.utils.StatDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private TruckService truckService;

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
