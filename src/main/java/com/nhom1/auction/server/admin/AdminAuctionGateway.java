package com.nhom1.auction.server.admin;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import java.util.List;

/**
 * Integration points with other members:
 * - Duy provides the concrete AuctionRepository or mapper that can expose
 *   auctions to admin in AuctionSummaryDto form.
 * - Quang wires that concrete implementation into AdminModule from ServerContext.
 */
public interface AdminAuctionGateway {
    List<AuctionSummaryDto> findAllAuctionSummaries();
}
