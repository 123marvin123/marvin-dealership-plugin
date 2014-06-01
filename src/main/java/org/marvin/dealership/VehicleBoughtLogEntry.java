package org.marvin.dealership;

import java.sql.Date;

/**
 * Created by Marvin on 27.05.2014.
 */
public class VehicleBoughtLogEntry {
    private String buyer;
    private int boughtModel;
    private int price;
    private Date boughtDate;
    private int databaseId;

    String getBuyer() {
        return buyer;
    }

    void setBoughtModel(int boughtModel) {
        this.boughtModel = boughtModel;
    }

    int getBoughtModel() {
        return boughtModel;
    }

    void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    int getPrice() {
        return price;
    }

    void setPrice(int price) {
        this.price = price;
    }

    Date getBoughtDate() {
        return boughtDate;
    }

    void setBoughtDate(Date boughtDate) {
        this.boughtDate = boughtDate;
    }

    int getDatabaseId() {
        return databaseId;
    }

    void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }
}
