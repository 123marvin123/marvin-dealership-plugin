package org.marvin.dealership;

import net.gtaun.shoebill.data.AngledLocation;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.data.Location;
import net.gtaun.shoebill.object.*;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by Marvin on 27.05.2014.
 */
public class VehicleProvider implements Destroyable {
    private String owner;
    private List<VehicleOffer> offerList;
    private List<VehicleBoughtLogEntry> boughtLogEntryList;
    private List<VehicleParkingspot> parkingList;
    private int cash;
    private Location pickupPosition;
    private WeakHashMap<Player, PlayerLabel> informationLabels;
    private Pickup pickup;
    private int databaseId;
    private String name;
    private List<BuyableVehicleLicense> boughtLicenses;
    private boolean isLabelShown;
    private List<PlayerLabel> parkingSpotLabels;
    private boolean testDrives;
    private AngledLocation testDriveLocation;
    private int testDriveTime;

    public VehicleProvider(String owner, Location pickupPosition, String name) {
        this.owner = owner;
        this.pickupPosition = pickupPosition;
        this.offerList = new ArrayList<>();
        this.boughtLogEntryList = new ArrayList<>();
        this.parkingList = new ArrayList<>();
        this.pickup = Pickup.create(1275, 1, pickupPosition);
        this.boughtLicenses = new ArrayList<>();
        this.parkingSpotLabels = new ArrayList<>();
        this.informationLabels = new WeakHashMap<>();
        this.name = name;
        Player.getHumans().forEach(player -> informationLabels.put(player, null));
        update3DTextLabel();
    }

    public void update3DTextLabel() {
        informationLabels.forEach((player, playerLabel) -> {
            if(playerLabel == null || playerLabel.isDestroyed()) {
                playerLabel = PlayerLabel.create(player, DealershipPlugin.getInstance().getLocalizedStringSet().format(player, "Labels.DealershipInformation", name, owner, offerList.size()), Color.GREEN, pickupPosition, 20, false);
                informationLabels.put(player, playerLabel);
            } else {
                playerLabel.update(Color.ORANGE, DealershipPlugin.getInstance().getLocalizedStringSet().format(player, "Labels.DealershipInformation", name, owner, offerList.size()));
            }
        });
    }

    int getTestDriveTime() {
        return testDriveTime;
    }

    void setTestDriveTime(int testDriveTime) {
        this.testDriveTime = testDriveTime;
    }

    boolean isTestDrives() {
        return testDrives;
    }

    AngledLocation getTestDriveLocation() {
        return testDriveLocation;
    }

    void setTestDriveLocation(AngledLocation testDriveLocation) {
        this.testDriveLocation = testDriveLocation;
    }

    void setTestDrives(boolean testDrives) {
        this.testDrives = testDrives;
    }

    List<PlayerLabel> getParkingSpotLabels() {
        return parkingSpotLabels;
    }

    boolean isLabelShown() {
        return isLabelShown;
    }

    void setLabelShown(boolean isLabelShown) {
        this.isLabelShown = isLabelShown;
    }

    String getOwner() {
        return owner;
    }

    void setOwner(String owner) {
        this.owner = owner;
    }

    List<VehicleOffer> getOfferList() {
        return offerList;
    }

    void setOfferList(List<VehicleOffer> offerList) {
        this.offerList = offerList;
    }

    List<VehicleBoughtLogEntry> getBoughtLogEntryList() {
        return boughtLogEntryList;
    }

    WeakHashMap<Player, PlayerLabel> getInformationLabels() {
        return informationLabels;
    }

    void setBoughtLogEntryList(List<VehicleBoughtLogEntry> boughtLogEntryList) {
        this.boughtLogEntryList = boughtLogEntryList;
    }

    List<VehicleParkingspot> getParkingList() {
        return parkingList;
    }

    void setParkingList(List<VehicleParkingspot> parkingList) {
        this.parkingList = parkingList;
    }

    int getCash() {
        return cash;
    }

    void setCash(int cash) {
        this.cash = cash;
    }

    Location getPickupPosition() {
        return pickupPosition;
    }

    void setPickupPosition(Location pickupPosition) {
        this.pickupPosition = pickupPosition;
    }

    Pickup getPickup() {
        return pickup;
    }

    void setPickup(Pickup pickup) {
        this.pickup = pickup;
    }

    int getDatabaseId() {
        return databaseId;
    }

    void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    List<BuyableVehicleLicense> getBoughtLicenses() {
        return boughtLicenses;
    }

    VehicleOffer hasVehicle(Vehicle vehicle) {
        return offerList.stream().filter(voffer -> voffer.getPreview() == vehicle).findAny().orElse(null);
    }

    boolean hasLicense(int modelid) {
        return boughtLicenses.stream().filter(lic -> lic.getModelid() == modelid).findAny().orElse(null) != null;
    }

    String getName() {
		return name;
	}

    void setName(String name) {
		this.name = name;
	}

    @Override
    public void destroy() {
        offerList.forEach(VehicleOffer::destroy);
        getBoughtLogEntryList().clear();
        parkingList.clear();
        boughtLicenses.clear();
        pickup.destroy();
        informationLabels.forEach((player, playerLabel) -> {
            if(playerLabel != null && !playerLabel.isDestroyed())
                playerLabel.destroy();
        });
    }

    @Override
    public boolean isDestroyed() {
        return pickup.isDestroyed();
    }
}
