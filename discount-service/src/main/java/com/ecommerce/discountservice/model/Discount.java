package com.ecommerce.discountservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document
@ApiModel(description = "Details of a discount")
public class Discount {
    @ApiModelProperty(notes = "Unique id of the discount")
    @Id
    String id;
    @ApiModelProperty(notes = "Code of the discount")
    @Indexed(unique=true)
    String code;
    @ApiModelProperty(notes = "Indicates whether it is a discount over the purchase or a EUR amount to subtract from the purchase price")
    boolean isPercentage;
    @ApiModelProperty(notes = "Value of the discount. It could be either a percentage or a EUR amount, it depends on the 'percentage' boolean")
    double value;
    @ApiModelProperty(notes = "Minimum amount of money that the order has to be in order to apply the discount")
    double minimumAmount;
    @ApiModelProperty(notes = "Start date of the availability discount")
    Date startDate;
    @ApiModelProperty(notes = "End date of the availability of the discount")
    Date endDate;
    @ApiModelProperty(notes = "Current uses of the discount")
    int currentUses = 0;
    @ApiModelProperty(notes = "Maximum number of times that the discount can be used")
    int maxUses;

    public Discount() {
    }

    public Discount(String id, String code, Date startDate, Date endDate, int currentUses, int maxUses) {
        this.id = id;
        this.code = code;
        this.startDate = startDate;
        this.endDate = endDate;
        //this.currentUses = currentUses;
        this.maxUses = maxUses;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isPercentage() {
        return isPercentage;
    }

    public void setPercentage(boolean percentage) {
        isPercentage = percentage;
    }

    public double getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public double getMinimumAmount() {
        return minimumAmount;
    }

    public void setMinimumAmount(double minimumAmount) {
        this.minimumAmount = minimumAmount;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getCurrentUses() {
        return currentUses;
    }

    public void setCurrentUses(int currentUses) {
        this.currentUses = currentUses;
    }

    public void incrementCurrentUses() {
        ++this.currentUses;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
    }
}

