package org.marvin.dealership;

import net.gtaun.shoebill.constant.VehicleModel;
import net.gtaun.shoebill.data.AngledLocation;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.object.*;

import java.util.List;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/**
 * Created by Marvin on 27.05.2014.
 */
public class VehicleOffer implements Destroyable {
    private int modelId;
    private int price;
    private Vehicle preview;
    private AngledLocation spawnLocation;
    private VehicleProvider provider;
    private int databaseId;
    private WeakHashMap<Player, PlayerLabel> playerLabels;

    public VehicleOffer(int model, int price, float x, float y, float z, float a, VehicleProvider provider) {
        this.modelId = model;
        this.price = price;
        this.spawnLocation = new AngledLocation(x, y, z, a);
        this.preview = Vehicle.create(model, x, y, z, a, 1, 1, 10);
        this.provider = provider;
        this.playerLabels = new WeakHashMap<>();
        Player.getHumans().forEach((pl) -> playerLabels.put(pl, null));
        updateLabel();
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    int getPrice() {
        return price;
    }

    void setPrice(int price) {
        this.price = price;
    }

    int getModelId() {
        return modelId;
    }

    void setModelId(int modelId) {
        this.modelId = modelId;
    }

    Vehicle getPreview() {
        return preview;
    }

    AngledLocation getSpawnLocation() {
        return spawnLocation;
    }

    VehicleProvider getProvider() {
        return provider;
    }

    WeakHashMap<Player, PlayerLabel> getPlayerLabels() {
        return playerLabels;
    }

    void setSpawnLocation(AngledLocation spawnLocation) {
        this.spawnLocation = spawnLocation;
        preview.destroy();
        preview = Vehicle.create(modelId, spawnLocation, 1, 1, 20);
    }

    public void updateLabel() {
        playerLabels.forEach((player, playerLabel) -> {
            if(playerLabel == null) {
                playerLabel = PlayerLabel.create(player, "~ Zu verkaufen ~\nFahrzeug: " + VehicleModel.getName(modelId) + "\nPreis: " + price + "$\nVerkäufer: " + provider.getName(), Color.GREEN, spawnLocation, 20, false);
                playerLabel.attach(preview, 0, 0, 0);
            } else
                playerLabel.update(Color.GREEN, "~ Zu verkaufen ~\nFahrzeug: " + VehicleModel.getName(modelId) + "\nPreis: " + price + "$\nVerkäufer: " + provider.getName());
        });
    }

    @Override
    public void destroy() {
        preview.destroy();
        playerLabels.forEach((player, playerLabel) -> {
            if(playerLabel != null) {
                playerLabel.destroy();
            }
        });
        playerLabels.clear();
    }

    @Override
    public boolean isDestroyed() {
        return preview.isDestroyed();
    }
}
