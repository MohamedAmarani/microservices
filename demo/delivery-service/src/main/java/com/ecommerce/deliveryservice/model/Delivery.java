package com.ecommerce.deliveryservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Calendar;
import java.util.Date;

@Document
public class Delivery {
    @Id
    String id;
    String orderId;
    DeliveryState deliveryState;
    DeliveryCompany deliveryCompany;
    Date estimatedDateOfArrival;

    public Delivery(String orderId) {
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

    public void setNextDeliveryEvent()
    {
        int index = deliveryState.ordinal();
        if (index != DeliveryState.values().length - 1) {
            int nextIndex = index + 1;
            DeliveryState[] deliveryStates = DeliveryState.values();
            nextIndex %= deliveryStates.length;
            deliveryState = deliveryStates[nextIndex];
        }
    }
}

enum DeliveryState {
    pendingToSend, alreadySent, arrived, finished
}

enum DeliveryCompany {
    MRW, SEUR, DHL
}
