package com.ecommerce.discountservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

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
    boolean percentage;
    @ApiModelProperty(notes = "If the discount is a percentage, then maxDiscount defines the maximum discount that can be applied to a purchase. If it is 0, no maximum discount is applied")
    double maxDiscount;
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
    @ApiModelProperty(notes = "Indicates weather the discount can be used or has been disabled due to some reason")
    boolean enabled = true;
    @ApiModelProperty(notes = "Indicates the accountId's of the users that can use the discount. If the list is null, then it will be available for everyone")
    List<AccountIdDTO> users;
    @ApiModelProperty(notes = "Creation date of the discount")
    Date creationDate;

    public Discount() {
    }

    public Discount(String id, String code, boolean percentage, double maxDiscount, double value, double minimumAmount, Date startDate,
                    Date endDate, int currentUses, int maxUses, List<AccountIdDTO> users, Date creationDate) {
        this.id = id;
        this.code = code;
        this.percentage = percentage;
        this.maxDiscount = maxDiscount;
        this.value = value;
        this.minimumAmount = minimumAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentUses = currentUses;
        this.maxUses = maxUses;
        this.users = users;
        this.creationDate = creationDate;
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

    public void setPercentage(boolean percentage) {
        this.percentage = percentage;
    }

    public boolean getPercentage() {
        return percentage;
    }

    public double getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(double maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<AccountIdDTO> getUsers() {
        return users;
    }

    public void setUsers(List<AccountIdDTO> users) {
        this.users = users;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}

