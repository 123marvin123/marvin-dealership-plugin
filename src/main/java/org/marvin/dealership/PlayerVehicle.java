package org.marvin.dealership;
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
    }

    public AngledLocation getVehicleLocation() {
        if(vehicle != null && !vehicle.isDestroyed())
            return vehicle.getLocation();
        else
            return new AngledLocation();
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getColor1() {
        return color1;
    }

    public int getColor2() {
        return color2;
    }

    public void setColor1(int color1) {
        this.color1 = color1;
        vehicle.setColor(color1, color2);
    }

    public void setColor2(int color2) {
        this.color2 = color2;
        vehicle.setColor(color1, color2);
    }

    public int getPrice() {
        return price;
    }

    public int getModel() {
        return model;
    }

    public float getSpawnX() {
        return spawnX;
    }

    public float getSpawnA() {
        return spawnA;
    }

    public float getSpawnY() {
        return spawnY;
    }

    public float getSpawnZ() {
        return spawnZ;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    public String getModelName() {
        return VehicleModel.getName(model);
    }

    public Date getBoughtDate() {
        return boughtDate;
    }

    public void setBoughtDate(Date boughtDate) {
        this.boughtDate = boughtDate;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getSellersName() {
        return sellersName;
    }

    public void setSellersName(String sellersName) {
        this.sellersName = sellersName;
    }

    public void setSpawnA(float spawnA) {
        this.spawnA = spawnA;
    }

    public void setSpawnX(float spawnX) {
        this.spawnX = spawnX;
    }

    public void setSpawnY(float spawnY) {
        this.spawnY = spawnY;
    }

    public void setSpawnZ(float spawnZ) {
        this.spawnZ = spawnZ;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void flashLights() {
        setLights(true);
        Timer.create(200, 3, i -> toggleLights()).start();
    }

    public void spawnVehicle() {
        if(vehicle != null && !vehicle.isDestroyed())
            vehicle.destroy();
        vehicle = Vehicle.create(model, spawnX, spawnY, spawnZ, spawnA, color1, color2, -1);
        vehicle.getState().setEngine(0);
    }

    public void destoryVehicle() {
        if(vehicle != null && !vehicle.isDestroyed())
            vehicle.destroy();
    }

    public boolean sell(Player player) {
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

    public void toggleEngine() {
        vehicle.getState().setEngine((vehicle.getState().getEngine() == 1) ? 0 : 1);
    }
    public void setEngine(boolean engineOn) {
        vehicle.getState().setEngine((engineOn) ? 1 : 0);
    }
    public boolean getEngine() {
        return (vehicle.getState().getEngine() != 0);
    }

    public void toggleLights() {
        vehicle.getState().setLights((vehicle.getState().getLights() == 0) ? 1 : 0);
    }
    public void setLights(boolean lightsOn) {
        vehicle.getState().setLights((lightsOn) ? 1 : 0);
    }
    public boolean getLights() {
        return (vehicle.getState().getLights() != 0);
    }

    public void toggleDoors() {
        vehicle.getState().setDoors((vehicle.getState().getDoors() == 0) ? 1 : 0);
    }
    public void setDoors(boolean doorsClosed) {
        vehicle.getState().setDoors((doorsClosed) ? 1 : 0);
    }
    public boolean getDoors() {
        return (vehicle.getState().getDoors() != 0);
    }
}
