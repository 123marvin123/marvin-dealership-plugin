package org.marvin.dealership;

import net.gtaun.shoebill.data.AngledLocation;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.data.Location;
import net.gtaun.shoebill.data.Vector3D;
import net.gtaun.shoebill.object.Label;
import net.gtaun.shoebill.object.Pickup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marvin on 27.05.2014.
 */
public class VehicleProvider {
    private String owner;
    private Vector3D cameraViewPostion;
    private Vector3D cameraLookAt;
    private List<VehicleOffer> offerList;
    private List<VehicleBoughtLogEntry> boughtLogEntryList;
    private List<AngledLocation> parkingList;
    private int cash;
    private Location pickupPosition;
    private Label informationLabel;
    private Pickup pickup;
    private int databaseId;

    public VehicleProvider(String owner, Location pickupPosition, Vector3D cameraViewPostion, Vector3D cameraLookAt) {
        this.owner = owner;
        this.pickupPosition = pickupPosition;
        this.offerList = new ArrayList<>();
        this.boughtLogEntryList = new ArrayList<>();
        this.parkingList = new ArrayList<>();
        this.informationLabel = Label.create("|-- Autohändler --|\nBesitzer: " + owner + "\nMenge an Angeboten: " + offerList.size() + "\nBenutze: /pbuy", Color.BEIGE, pickupPosition, 0, 20, false);
        this.pickup = Pickup.create(1275, 1, pickupPosition);
        this.cameraLookAt = cameraLookAt;
        this.cameraViewPostion = cameraViewPostion;
    }

    public void update3DTextLabel() {
        informationLabel.update(Color.BEIGE, "|-- Autohändler --|\nBesitzer: " + owner + "\nMenge an Angeboten: " + offerList.size() + "\nBenutze: /pbuy");
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Vector3D getCameraViewPostion() {
        return cameraViewPostion;
    }

    public void setCameraViewPostion(Vector3D cameraViewPostion) {
        this.cameraViewPostion = cameraViewPostion;
    }

    public Vector3D getCameraLookAt() {
        return cameraLookAt;
    }

    public void setCameraLookAt(Vector3D cameraLookAt) {
        this.cameraLookAt = cameraLookAt;
    }

    public List<VehicleOffer> getOfferList() {
        return offerList;
    }

    public void setOfferList(List<VehicleOffer> offerList) {
        this.offerList = offerList;
    }

    public List<VehicleBoughtLogEntry> getBoughtLogEntryList() {
        return boughtLogEntryList;
    }

    public void setBoughtLogEntryList(List<VehicleBoughtLogEntry> boughtLogEntryList) {
        this.boughtLogEntryList = boughtLogEntryList;
    }

    public List<AngledLocation> getParkingList() {
        return parkingList;
    }

    public void setParkingList(List<AngledLocation> parkingList) {
        this.parkingList = parkingList;
    }

    public int getCash() {
        return cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
    }

    public Location getPickupPosition() {
        return pickupPosition;
    }

    public void setPickupPosition(Location pickupPosition) {
        this.pickupPosition = pickupPosition;
    }

    public Label getInformationLabel() {
        return informationLabel;
    }

    public void setInformationLabel(Label informationLabel) {
        this.informationLabel = informationLabel;
    }

    public Pickup getPickup() {
        return pickup;
    }

    public void setPickup(Pickup pickup) {
        this.pickup = pickup;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }
}
