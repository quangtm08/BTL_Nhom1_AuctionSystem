package com.nhom1.auction.server;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.utils.AppContext;
import com.nhom1.auction.server.admin.AdminModule;
import com.nhom1.auction.server.auction.AuctionModule;
import com.nhom1.auction.server.auth.AuthModule;
import com.nhom1.auction.server.automation.AutoBidModule;
import com.nhom1.auction.server.bidding.BidModule;
import com.nhom1.auction.server.infrastructure.ResponseFactory;
import com.nhom1.auction.server.payment.PaymentModule;
import com.nhom1.auction.server.wallet.WalletModule;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

public class ModulesTest {

  @Test
  public void testModuleConstructors() throws Exception {
    // Instantiate using default constructor to cover the compiler-generated or default public
    // constructors
    assertNotNull(new PaymentModule());
    assertNotNull(new WalletModule());
    assertNotNull(new AutoBidModule());
    assertNotNull(new BidModule());
    assertNotNull(new AdminModule());
    assertNotNull(new AuthModule());
    assertNotNull(new AuctionModule());
    assertNotNull(new AppContext());

    // ResponseFactory private constructor reflection
    Constructor<ResponseFactory> rfConstructor = ResponseFactory.class.getDeclaredConstructor();
    rfConstructor.setAccessible(true);
    ResponseFactory rf = rfConstructor.newInstance();
    assertNotNull(rf);
  }

  @Test
  public void testAppContextMethods() {
    AppContext.setServer(true);
    assertTrue(AppContext.isServer());

    AppContext.setServer(false);
    assertFalse(AppContext.isServer());

    AuthResponse user = new AuthResponse();
    AppContext.setCurrentUser(user);
    assertSame(user, AppContext.getCurrentUser());

    AppContext.clearSession();
    assertNull(AppContext.getCurrentUser());

    AppContext.setSelectedAuctionId("auc-123");
    assertEquals("auc-123", AppContext.getSelectedAuctionId());
  }
}
