package com.nhom1.auction.common.protocol;

public enum MessageType {
    LOGIN,
    REGISTER,
    GET_AUCTION_DETAIL, //to fetch data displayed inside the page of that auction (description, brand,..)
    PLACE_BID,
    AUTO_BID_CONFIG,
    LIST_AUCTIONS, //to show all ongoing auctions in the explore/browse screen
    LIST_MY_BIDS, //to show all the user's bids in the My Bids screen
    LIST_MY_LISTINGS, //to show all the user's listings in the My Listings screen
}
