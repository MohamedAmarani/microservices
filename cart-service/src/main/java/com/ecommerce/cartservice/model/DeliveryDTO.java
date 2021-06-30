package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;

import java.util.Calendar;
import java.util.Date;

@ApiModel(description = "Details obout a delivery")
public class DeliveryDTO {
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
    Date creationDate;

    public DeliveryDTO() {
    }

    public DeliveryDTO(String id, String orderId, String deliveryAddress, DeliveryState deliveryState, DeliveryCompany deliveryCompany,
                       Date estimatedDateOfArrival, Date creationDate) {
        this.id = id;
        this.orderId = orderId;
        this.deliveryAddress = deliveryAddress;
        this.deliveryState = deliveryState;
        this.deliveryCompany = deliveryCompany;
        this.estimatedDateOfArrival = estimatedDateOfArrival;
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

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
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
