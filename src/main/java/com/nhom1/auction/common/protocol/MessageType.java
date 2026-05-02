package com.nhom1.auction.common.protocol;

public enum MessageType {
    LOGIN,
    REGISTER,

    LIST_AUCTIONS, //to show all active auctions in the explore/browse screen
    GET_AUCTION_DETAIL, //to fetch data displayed inside the page of that auction (description, brand,..)
    PLACE_BID,
    LIST_MY_BIDS, //to show all the user's bids in the My Bids screen

    CREATE_AUCTION,
    DELETE_AUCTION,
    LIST_MY_LISTINGS, //to show all the user's listings in the My Listings screen

    AUTO_BID_CONFIG,
    ADMIN_LIST_USERS,
    ADMIN_DELETE_USER,
    ADMIN_LIST_AUCTIONS,
    PROCESS_PAYMENT,

    //Push notifications
    PUSH_BID_UPDATE,
    PUSH_AUCTION_ENDED,
    PUSH_NEW_AUCTION,
}
