- Đối tượng được theo dõi (observe) là common/entity/Auction
- Auction chứa một list những lớp khác quan tâm tới sự thay đổi của auction (những lớp thay đổi khi
auction thay đổi)
- Auction có thể add, remove... observer ra khỏi list
- Các observer implement interface common/observer/AuctionObserver. 
- Trong lớp Auction, khi có sự thay đổi, sử dụng method notifyObserver sẽ loop qua từng observer và 
gọi method obNewBid (observer đó sẽ phải làm gì khi auction nhận new bid - hiện tại mới chỉ có 
method này thôi)

NOTE: Trong placeBid(...), nếu bid hợp lệ thì: tạo BidTransaction =>
cập nhật bidHistory, highestBidderId, currentHighestBid => gọi notifyObserver

VD: khi có bid mới, placeBid gọi notifyObserver. NotifyObserver gọi tới method onNewBid của 
auctionScreenController (có implements AuctionObserver). Method này sẽ refresh lại màn hình đó...

