package com.ecommerce.cartservice.model;

import java.util.Calendar;
import java.util.Date;

public class DeliveryDTO {
    String id;
    String orderId;
    DeliveryState deliveryState;
    DeliveryCompany deliveryCompany;
    Date estimatedDateOfArrival;

    public DeliveryDTO() {
    }

    public DeliveryDTO(String orderId) {
        this.orderId = orderId;
        this.deliveryState = DeliveryState.pendingToSend;
        this.deliveryCompany = DeliveryCompany.DHL;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, 7);
        this.estimatedDateOfArrival = cal.getTime();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public DeliveryState getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(DeliveryState deliveryState) {
        this.deliveryState = deliveryState;
    }

    public DeliveryCompany getDeliveryCompany() {
        return deliveryCompany;
    }

    public void setDeliveryCompany(DeliveryCompany deliveryCompany) {
        this.deliveryCompany = deliveryCompany;
    }

    public Date getEstimatedDateOfArrival() {
        return estimatedDateOfArrival;
    }

    public void setEstimatedDateOfArrival(Date estimatedDateOfArrival) {
        this.estimatedDateOfArrival = estimatedDateOfArrival;
    }
}

enum DeliveryState {
    pendingToSend, alreadySent, arrived, finished
}

enum DeliveryCompany {
    MRW, SEUR, DHL
}