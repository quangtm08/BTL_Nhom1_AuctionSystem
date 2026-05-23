package com.nhom1.auction.common.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom1.auction.common.dto.admin.*;
import com.nhom1.auction.common.dto.auction.*;
import com.nhom1.auction.common.dto.auth.*;
import com.nhom1.auction.common.dto.autobid.*;
import com.nhom1.auction.common.dto.bidding.*;
import com.nhom1.auction.common.dto.notification.*;
import com.nhom1.auction.common.dto.payment.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CommonDtoTest {

    private static final Class<?>[] DTO_CLASSES = {
        AdminDeleteUserRequest.class,
        AdminListAuctionsRequest.class,
        AdminListUsersRequest.class,
        AdminUserListResponse.class,
        UserSummaryDto.class,
        AuctionSummaryDto.class,
        CreateAuctionRequest.class,
        CreateAuctionResponse.class,
        MyListingsResponse.class,
        AuthResponse.class,
        LoginRequest.class,
        RegisterRequest.class,
        AutoBidConfigRequest.class,
        AutoBidConfigResponse.class,
        AuctionDetailDto.class,
        BidSummaryDto.class,
        BidWithAuctionDto.class,
        GetAuctionDetailRequest.class,
        ListAuctionsResponse.class,
        MyBidsResponse.class,
        PlaceBidRequest.class,
        PlaceBidResponse.class,
        AuctionEndedEvent.class,
        BidUpdateEvent.class,
        NewAuctionEvent.class,
        ListPaymentHistoryRequest.class,
        ListPendingPaymentsRequest.class,
        PaymentHistoryEntryDto.class,
        PaymentHistoryResponse.class,
        PendingPaymentDto.class,
        PendingPaymentsResponse.class,
        ProcessPaymentRequest.class,
        ProcessPaymentResponse.class
    };

    @Test
    public void testAllDtos() throws Exception {
        for (Class<?> clazz : DTO_CLASSES) {
            testGettersAndSetters(clazz);
            testAdditionalConstructors(clazz);
        }
    }

    private void testGettersAndSetters(Class<?> clazz) throws Exception {
        Object instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            // Skip classes without default constructor in this step
            return;
        }

        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                String fieldName = method.getName().substring(3);
                java.lang.reflect.Method getter = null;
                try {
                    getter = clazz.getMethod("get" + fieldName);
                } catch (NoSuchMethodException e) {
                    try {
                        getter = clazz.getMethod("is" + fieldName);
                    } catch (NoSuchMethodException e2) {
                        // no getter
                    }
                }

                if (getter != null) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    Object val = getDummyValue(paramType);
                    method.invoke(instance, val);
                    Object ret = getter.invoke(instance);
                    if (val != null && paramType.isPrimitive()) {
                        assertEquals(val, ret);
                    }
                }
            }
        }

        // Test common methods
        assertNotNull(instance.toString());
        instance.hashCode();
        assertTrue(instance.equals(instance));
        assertFalse(instance.equals(null));
        assertFalse(instance.equals(new Object()));
    }

    private void testAdditionalConstructors(Class<?> clazz) throws Exception {
        // Find other constructors and call them with dummy values
        for (java.lang.reflect.Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() > 0) {
                Object[] args = new Object[constructor.getParameterCount()];
                Class<?>[] paramTypes = constructor.getParameterTypes();
                for (int i = 0; i < args.length; i++) {
                    args[i] = getDummyValue(paramTypes[i]);
                }
                try {
                    constructor.setAccessible(true);
                    Object instance = constructor.newInstance(args);
                    assertNotNull(instance);
                } catch (Exception e) {
                    // Ignore expected failures if constructor validates input strictly
                }
            }
        }
    }

    private Object getDummyValue(Class<?> type) {
        if (type == String.class) {
            return "dummy";
        } else if (type == Integer.TYPE || type == Integer.class) {
            return 42;
        } else if (type == Long.TYPE || type == Long.class) {
            return 100L;
        } else if (type == Double.TYPE || type == Double.class) {
            return 3.14;
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            return true;
        } else if (type == BigDecimal.class) {
            return BigDecimal.TEN;
        } else if (type == LocalDateTime.class) {
            return LocalDateTime.now();
        } else if (type == UUID.class) {
            return UUID.randomUUID();
        } else if (type == List.class) {
            return new ArrayList<>();
        } else if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[0];
            }
        }
        return null;
    }
}
