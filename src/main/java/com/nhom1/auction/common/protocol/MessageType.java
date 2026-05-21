package com.nhom1.auction.common.protocol;

public enum MessageType {
    LOGIN,
    REGISTER,

    LIST_AUCTIONS,
    GET_AUCTION_DETAIL,
    PLACE_BID,
    LIST_MY_BIDS, //to show all the user's bids in the My Bids screen

    CREATE_AUCTION,
    DELETE_AUCTION,
    LIST_MY_LISTINGS, //to show all the user's listings in the My Listings screen

    AUTO_BID_CONFIG,
    GET_AUTO_BID_CONFIG,
    DELETE_AUTO_BID_CONFIG,
    ADMIN_LIST_USERS,
    ADMIN_DELETE_USER,
    ADMIN_LIST_AUCTIONS,
    ADMIN_CANCEL_AUCTION,
    PROCESS_PAYMENT,
    LIST_PENDING_PAYMENTS,
    LIST_PAYMENT_HISTORY,

    //Push notifications
    PUSH_BID_UPDATE,
    PUSH_AUCTION_ENDED,
    PUSH_NEW_AUCTION,
    PUSH_AUCTION_DELETED,
}
