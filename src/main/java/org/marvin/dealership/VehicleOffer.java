package org.marvin.dealership;

/**
 * Created by Marvin on 27.05.2014.
 */
public class VehicleOffer {
    private int modelId;
    private int price;

    public VehicleOffer(int model, int price) {
        this.modelId = model;
        this.price = price;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }
}
