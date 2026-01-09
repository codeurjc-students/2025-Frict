package com.tfg.backend.unit;

import com.tfg.backend.model.Truck;
import com.tfg.backend.repository.TruckRepository;
import com.tfg.backend.service.TruckService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TruckServiceUTest {

    @Mock
    private TruckRepository repository;

    @InjectMocks
    private TruckService truckService;

    @Test
    void findAll_ShouldReturnList_WhenTrucksExist() {
        List<Truck> trucks = Arrays.asList(new Truck(), new Truck());
        when(repository.findAll()).thenReturn(trucks);

        List<Truck> result = truckService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(trucks, result);
        verify(repository, times(1)).findAll();
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoTrucksExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Truck> result = truckService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findAll();
    }

    @Test
    void save_ShouldReturnSavedTruck() {
        Truck truck = new Truck();
        when(repository.save(truck)).thenReturn(truck);

        Truck result = truckService.save(truck);

        assertNotNull(result);
        assertEquals(truck, result);
        verify(repository, times(1)).save(truck);
    }
}
