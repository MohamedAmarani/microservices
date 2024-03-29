package com.ecommerce.deliveryservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Calendar;
import java.util.Date;

@Document
@ApiModel(description = "Details obout a product")
public class Delivery {
    @ApiModelProperty(notes = "Unique id of the delivery")
    @Id
    String id;
    @ApiModelProperty(notes = "Unique id of the order that has to be delivered")
    String orderId;
    @ApiModelProperty(notes = "Address where the order has to be delivered")
    String deliveryAddress;
    @ApiModelProperty(notes = "State of the delivery")
    DeliveryState deliveryState;
    @ApiModelProperty(notes = "Company in charge of the delivery")
    DeliveryCompany deliveryCompany;
    @ApiModelProperty(notes = "Estimated date on which the delivery will be carried out")
    Date estimatedDateOfArrival;
    @ApiModelProperty(notes = "Creation date of the delivery")
    Date creationDate = new Date();

    public Delivery(String orderId, String deliveryAddress, Date creationDate) {
        this.orderId = orderId;
        this.deliveryState = DeliveryState.pendingToSend;
        this.deliveryCompany = DeliveryCompany.DHL;
        this.deliveryAddress = deliveryAddress;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, 7);
        this.estimatedDateOfArrival = cal.getTime();
        this.creationDate = creationDate;
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

    public void updateEstimatedDateOfArrival(int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.estimatedDateOfArrival);
        cal.add(Calendar.DAY_OF_YEAR, offsetDays);
        this.estimatedDateOfArrival = cal.getTime();
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
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

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}

@ApiModel(description = "States of the delivery")
enum DeliveryState {
    pendingToSend, alreadySent, arrived, finished
}

@ApiModel(description = "Delivery companies")
enum DeliveryCompany {
    MRW, SEUR, DHL
}
