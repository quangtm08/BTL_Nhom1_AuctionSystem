package com.nhom1.auction.common.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.nhom1.auction.common.entity.Art;
import com.nhom1.auction.common.entity.Electronics;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.entity.Vehicle;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;

public class ItemFactoryTest {

    @Test
    public void testCreateElectronics_Success() {
        
        // Act
        Item electronics = ItemFactory.createElectronics(
            "Gaming Laptop", "High performance laptop",
            ItemCondition.NEW, "Asus", 24
        );

        // Assert
        assertNotNull(electronics, "The created item should not be null");
        assertTrue(electronics instanceof Electronics, "The item should be an instance of Electronics");
        assertEquals(ItemCategory.ELECTRONICS, electronics.getCategory(), "The category should be ELECTRONICS");
        assertEquals("Gaming Laptop", electronics.getName(), "The name should match the input");
    }


    @Test
    public void testCreateArt_Success() {
     
        // Act
        Item art = ItemFactory.createArt(
            "Starry Night Replica", "Beautiful painting",
            ItemCondition.NEW, "Vincent van Gogh", "Post-Impressionism"
        );

        // Assert
        assertNotNull(art, "The created item should not be null");
        assertTrue(art instanceof Art, "The item should be an instance of Art");
        assertEquals(ItemCategory.ART, art.getCategory(), "The category should be ART");
        assertEquals("Starry Night Replica", art.getName(), "The name should match the input");
    }

    @Test
    public void testCreateVehicle_Success() {
        
        // Act
        Item vehicle = ItemFactory.createVehicle(
             "Honda Civic", "Used sedan",
            ItemCondition.USED, "Honda", 2018, VehicleFuelType.PETROL

        );

        // Assert
        assertNotNull(vehicle, "The created item should not be null");
        assertTrue(vehicle instanceof Vehicle, "The item should be an instance of Vehicle");
        assertEquals(ItemCategory.VEHICLE, vehicle.getCategory(), "The category should be VEHICLE");
        assertEquals("Honda Civic", vehicle.getName(), "The name should match the input");
    }
}
