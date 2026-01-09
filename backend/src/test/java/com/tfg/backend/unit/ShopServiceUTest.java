package com.tfg.backend.unit;

import com.tfg.backend.model.Shop;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.service.ShopService;
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
class ShopServiceUTest {

    @Mock
    private ShopRepository repository;

    @InjectMocks
    private ShopService shopService;


    // findAll() method tests
    @Test
    void findAll_ShouldReturnList_WhenShopsExist() {
        List<Shop> shops = Arrays.asList(new Shop(), new Shop());
        when(repository.findAll()).thenReturn(shops);

        List<Shop> result = shopService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(shops, result);
        verify(repository, times(1)).findAll();
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoShopsExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Shop> result = shopService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findAll();
    }


    // save() method tests
    @Test
    void save_ShouldReturnSavedShop() {
        Shop shop = new Shop();
        when(repository.save(shop)).thenReturn(shop);

        Shop result = shopService.save(shop);

        assertNotNull(result);
        assertEquals(shop, result);
        verify(repository, times(1)).save(shop);
    }
}