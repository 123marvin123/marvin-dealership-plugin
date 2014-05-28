package org.marvin.dealership;

/**
 * Created by Marvin on 27.05.2014.
 */
public class VehicleBoughtLogEntry {
    private String buyer;
    private int boughtModel;
    private int price;

    public String getBuyer() {
        return buyer;
    }

    public void setBoughtModel(int boughtModel) {
        this.boughtModel = boughtModel;
    }

    public int getBoughtModel() {
        return boughtModel;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
