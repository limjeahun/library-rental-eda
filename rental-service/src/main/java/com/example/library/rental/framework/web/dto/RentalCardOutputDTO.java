package com.example.library.rental.framework.web.dto;

import com.example.library.rental.domain.model.RentStatus;
import com.example.library.rental.domain.model.RentalCard;
import java.util.List;

public class RentalCardOutputDTO {
    private String rentalCardNo;
    private String userId;
    private String userNm;
    private RentStatus rentStatus;
    private long lateFee;
    private List<RentItemOutputDTO> rentItems;
    private List<RetrunItemOupputDTO> returnItems;

    public RentalCardOutputDTO() {
    }

    public RentalCardOutputDTO(
        String rentalCardNo,
        String userId,
        String userNm,
        RentStatus rentStatus,
        long lateFee,
        List<RentItemOutputDTO> rentItems,
        List<RetrunItemOupputDTO> returnItems
    ) {
        this.rentalCardNo = rentalCardNo;
        this.userId = userId;
        this.userNm = userNm;
        this.rentStatus = rentStatus;
        this.lateFee = lateFee;
        this.rentItems = rentItems;
        this.returnItems = returnItems;
    }

    public static RentalCardOutputDTO from(RentalCard rentalCard) {
        return new RentalCardOutputDTO(
            rentalCard.getRentalCardNo(),
            rentalCard.getMember().getId(),
            rentalCard.getMember().getName(),
            rentalCard.getRentStatus(),
            rentalCard.getLateFee().getPoint(),
            rentalCard.getRentItemList().stream().map(RentItemOutputDTO::from).toList(),
            rentalCard.getReturnItemList().stream().map(RetrunItemOupputDTO::from).toList()
        );
    }

    public String getRentalCardNo() {
        return rentalCardNo;
    }

    public void setRentalCardNo(String rentalCardNo) {
        this.rentalCardNo = rentalCardNo;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserNm() {
        return userNm;
    }

    public void setUserNm(String userNm) {
        this.userNm = userNm;
    }

    public RentStatus getRentStatus() {
        return rentStatus;
    }

    public void setRentStatus(RentStatus rentStatus) {
        this.rentStatus = rentStatus;
    }

    public long getLateFee() {
        return lateFee;
    }

    public void setLateFee(long lateFee) {
        this.lateFee = lateFee;
    }

    public List<RentItemOutputDTO> getRentItems() {
        return rentItems;
    }

    public void setRentItems(List<RentItemOutputDTO> rentItems) {
        this.rentItems = rentItems;
    }

    public List<RetrunItemOupputDTO> getReturnItems() {
        return returnItems;
    }

    public void setReturnItems(List<RetrunItemOupputDTO> returnItems) {
        this.returnItems = returnItems;
    }
}
