package org.marvin.dealership;

/**
 * Created by Marvin on 29.05.2014.
 */
public class BuyableVehicleLicense {
    private int price;
    private int modelid;
    private int databaseId;

    public BuyableVehicleLicense(int modelid, int price) {
        this.price = price;
        this.modelid = modelid;
    }

    public int getPrice() {
        return price;
    }

    public int getModelid() {
        return modelid;
    }

    int getDatabaseId() {
        return databaseId;
    }

    void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }
}
