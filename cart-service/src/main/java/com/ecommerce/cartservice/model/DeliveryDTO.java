package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Calendar;
import java.util.Date;

@ApiModel(description = "Details of a delivery")
public class DeliveryDTO {
    @ApiModelProperty(notes = "Unique id of the delivery")
    String id;
    @ApiModelProperty(notes = "Unique id of the order to deliver")
    String orderId;
    @ApiModelProperty(notes = "State of the delivery")
    DeliveryState deliveryState;
    @ApiModelProperty(notes = "Company in charge of the delivery")
    DeliveryCompany deliveryCompany;
    @ApiModelProperty(notes = "Estimated date of the delivery arrival")
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

@ApiModel(description = "Details of a delivery state")
enum DeliveryState {
    pendingToSend, alreadySent, arrived, finished
}

@ApiModel(description = "Details of a delivery company")
enum DeliveryCompany {
    MRW, SEUR, DHL
}