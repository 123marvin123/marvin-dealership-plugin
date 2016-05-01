package org.marvin.dealership;

import java.sql.Date;

/**
 * Created by Marvin on 29.05.2014.
 */
public class BuyableVehicleLicense {
    private int price;
    private int modelid;
    private int databaseId;
    private Date expire;
    private int validDays;
    private Date boughtDate;

    public BuyableVehicleLicense(int modelid, int price, int validDays) {
        this.price = price;
        this.modelid = modelid;
        this.validDays = validDays;
        this.expire = new Date(System.currentTimeMillis() + (validDays * 864_000_00));
    }

    boolean isExpired() {
        return new Date(System.currentTimeMillis()).after(this.expire);
    }

    void setBoughtDate(Date boughtDate) {
        this.boughtDate = boughtDate;
    }

    Date getBoughtDate() {
        return boughtDate;
    }

    public int getValidDays() {
        return validDays;
    }

    Date getExpire() {
        return expire;
    }

    void setExpire(Date expire) {
        this.expire = expire;
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
