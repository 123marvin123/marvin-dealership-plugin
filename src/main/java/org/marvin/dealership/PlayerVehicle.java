package org.marvin.dealership;
import net.gtaun.shoebill.constant.VehicleComponentSlot;
import net.gtaun.shoebill.constant.VehicleModel;
import net.gtaun.shoebill.data.AngledLocation;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.data.Location;
import net.gtaun.shoebill.data.Vector3D;
import net.gtaun.shoebill.object.Player;
import net.gtaun.shoebill.object.Timer;
import net.gtaun.shoebill.object.Vehicle;

import java.sql.Date;

/**
 * Created by Marvin on 26.05.2014.
 */
public class PlayerVehicle {
    private int price;
    private String owner;
    private Vehicle vehicle;
    private int color1;
    private int color2;
    private int model;
    private float spawnX, spawnY, spawnZ, spawnA;
    private int databaseId;
    private Date boughtDate;
    private String sellersName;
    private int[] componentSlots;
    private String componentsString;

    public PlayerVehicle(int model, String owner,
                         float spawnX, float spawnY,
                         float spawnZ, float spawnA,
                         int color1, int color2) {
        this.owner = owner;
        this.model = model;
        this.spawnX = spawnX;
        this.spawnA = spawnA;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.color1 = color1;
        this.color2 = color2;
        this.componentSlots = new int[14];
    }

    void setComponentInSlot(int slot, int component) {
        try {
            componentSlots[slot] = component;
            vehicle.getComponent().add(component);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    int[] getComponents() {
        return componentSlots;
    }

    void refreshComponentList() {
        for(int i = 0; i < 13; i++) {
            componentSlots[i] = vehicle.getComponent().get(VehicleComponentSlot.get(i));
        }
    }

    String getComponentsString() {
        return componentsString;
    }

    void setComponentsString(String componentsString) {
        this.componentsString = componentsString;
    }

    AngledLocation getVehicleLocation() {
        if(vehicle != null && !vehicle.isDestroyed())
            return vehicle.getLocation();
        else
            return new AngledLocation();
    }

    String getOwner() {
        return owner;
    }

    void setOwner(String owner) {
        this.owner = owner;
    }

    int getColor1() {
        return color1;
    }

    int getColor2() {
        return color2;
    }

    void setColor1(int color1) {
        this.color1 = color1;
        vehicle.setColor(color1, color2);
    }

    void setColor2(int color2) {
        this.color2 = color2;
        vehicle.setColor(color1, color2);
    }

    int getPrice() {
        return price;
    }

    int getModel() {
        return model;
    }

    float getSpawnX() {
        return spawnX;
    }

    float getSpawnA() {
        return spawnA;
    }

    float getSpawnY() {
        return spawnY;
    }

    float getSpawnZ() {
        return spawnZ;
    }

    int getDatabaseId() {
        return databaseId;
    }

    void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    String getModelName() {
        return VehicleModel.getName(model);
    }

    Date getBoughtDate() {
        return boughtDate;
    }

    void setBoughtDate(Date boughtDate) {
        this.boughtDate = boughtDate;
    }

    Vehicle getVehicle() {
        return vehicle;
    }

    String getSellersName() {
        return sellersName;
    }

     void setSellersName(String sellersName) {
        this.sellersName = sellersName;
    }

    void setSpawnA(float spawnA) {
        this.spawnA = spawnA;
    }

    void setSpawnX(float spawnX) {
        this.spawnX = spawnX;
    }

    void setSpawnY(float spawnY) {
        this.spawnY = spawnY;
    }

    void setSpawnZ(float spawnZ) {
        this.spawnZ = spawnZ;
    }

    void setPrice(int price) {
        this.price = price;
    }

    public void flashLights() {
        setLights(true);
        Timer.create(200, 3, i -> toggleLights()).start();
    }

    void spawnVehicle() {
        if(vehicle != null && !vehicle.isDestroyed())
            vehicle.destroy();
        vehicle = Vehicle.create(model, spawnX, spawnY, spawnZ, spawnA, color1, color2, -1);
        setDoors(true);
    }

    void destoryVehicle() {
        if(vehicle != null && !vehicle.isDestroyed())
            vehicle.destroy();
    }

    boolean sell(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        destoryVehicle();
        DealershipPlugin.getInstance().getAddMoneyFunction().accept(player, price / 2);
        DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM playervehicles WHERE Id = " + databaseId);
        if(DealershipPlugin.getInstance().getPlayerVehicles().contains(this)) {
            try {
                DealershipPlugin.getInstance().getPlayerVehicles().remove(this);
                if(playerData.getPlayerVehicles().contains(this))
                    playerData.getPlayerVehicles().remove(this);
                player.sendMessage(Color.CORAL, ">> Dein/e " + getModelName() + " wurde erfolgreich für " + price / 2 + "$ verkauft.");
            } catch (Exception ex) {
                DealershipPlugin.getInstance().getLoggerInstance().error("Vehicle couldn't get removed from list. Stacktrace: ");
                ex.printStackTrace();
            }
        }
        return true;
    }

    void toggleEngine() {
        vehicle.getState().setEngine((vehicle.getState().getEngine() == 1) ? 0 : 1);
    }
    void setEngine(boolean engineOn) {
        vehicle.getState().setEngine((engineOn) ? 1 : 0);
    }
    boolean getEngine() {
        return (vehicle.getState().getEngine() != 0);
    }

    void toggleLights() {
        vehicle.getState().setLights((vehicle.getState().getLights() == 0) ? 1 : 0);
    }
    void setLights(boolean lightsOn) {
        vehicle.getState().setLights((lightsOn) ? 1 : 0);
    }
    boolean getLights() {
        return (vehicle.getState().getLights() != 0);
    }

    void toggleDoors() {
        vehicle.getState().setDoors((vehicle.getState().getDoors() == 0) ? 1 : 0);
    }
    void setDoors(boolean doorsClosed) {
        vehicle.getState().setDoors((doorsClosed) ? 1 : 0);
    }
    boolean getDoors() {
        return (vehicle.getState().getDoors() != 0);
    }
}
