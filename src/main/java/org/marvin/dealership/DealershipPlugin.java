package org.marvin.dealership;

import net.gtaun.shoebill.common.player.PlayerLifecycleHolder;
import net.gtaun.shoebill.constant.PlayerKey;
import net.gtaun.shoebill.constant.TextDrawAlign;
import net.gtaun.shoebill.constant.TextDrawFont;
import net.gtaun.shoebill.data.AngledLocation;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.data.Location;
import net.gtaun.shoebill.data.Vector3D;
import net.gtaun.shoebill.object.Player;
import net.gtaun.shoebill.object.Server;
import net.gtaun.shoebill.object.Textdraw;
import net.gtaun.shoebill.object.World;
import net.gtaun.shoebill.resource.Plugin;
import net.gtaun.util.event.EventManager;

import org.slf4j.Logger;

import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by Marvin on 26.05.2014.
 */
public class DealershipPlugin extends Plugin {
    private static DealershipPlugin instance;

    public static DealershipPlugin getInstance() {
        if(instance == null)
            instance = new DealershipPlugin();
        return instance;
    }

    private Logger logger;
    private EventManager eventManager;
    private MysqlConnection mysqlConnection;
    private List<PlayerVehicle> playerVehicles;
    private List<VehicleProvider> vehicleProviderList;
    private PlayerLifecycleHolder playerLifecycleHolder;
    private PlayerManager playerManager;
    private Function<Player, Integer> moneyGetter = Player::getMoney;
    private BiConsumer<Player, Integer> addMoneyFunction = Player::giveMoney;
    private PlayerKey engineKey = PlayerKey.YES;
    private boolean findCarEnabled = true;
    private Textdraw offerBoxTextdraw;
    private List<BuyableVehicleLicense> buyableLicenses;
    @Override
    protected void onEnable() throws Throwable {
        instance = this;
        logger = getLogger();
        eventManager = getEventManager();
        mysqlConnection = new MysqlConnection();
        mysqlConnection.initConnection();
        mysqlConnection.makeDatabase();
        playerVehicles = new ArrayList<>();
        vehicleProviderList = new ArrayList<>();
        playerLifecycleHolder = new PlayerLifecycleHolder(eventManager);
        playerLifecycleHolder.registerClass(PlayerData.class);
        playerManager = new PlayerManager();
        buyableLicenses = new ArrayList<>();
        loadPlayerVehicles();
        loadVehicleProviders();
        
        offerBoxTextdraw = Textdraw.create(382.666717f, 164.937103f, "usebox");
        offerBoxTextdraw.setLetterSize(0.000000f, 15.363155f);
        offerBoxTextdraw.setTextSize(240.666687f, 0.000000f);
        offerBoxTextdraw.setAlignment(TextDrawAlign.LEFT);
        offerBoxTextdraw.setColor(new Color(0));
        offerBoxTextdraw.setUseBox(true);
        offerBoxTextdraw.setBoxColor(new Color(670054));
        offerBoxTextdraw.setShadowSize(0);
        offerBoxTextdraw.setOutlineSize(0);
        offerBoxTextdraw.setFont(TextDrawFont.FONT2);
    }

    @Override
    protected void onDisable() throws Throwable {
        savePlayerVehicles();
        vehicleProviderList.forEach(this::saveVehicleProvider);
        mysqlConnection.closeConnection();
    }

    private void saveVehicleProvider(VehicleProvider provider) {
        Map<String, Object> keyList = new HashMap<>();
        keyList.put("owner", provider.getOwner());
        keyList.put("pickupLocationX", provider.getPickupPosition().x);
        keyList.put("pickupLocationY", provider.getPickupPosition().y);
        keyList.put("pickupLocationZ", provider.getPickupPosition().z);
        keyList.put("kasse", provider.getCash());
        keyList.put("name", provider.getName());
        StringBuilder offerList = new StringBuilder();
        for(VehicleOffer offer : provider.getOfferList()) {
            AngledLocation vehicleLocation = offer.getSpawnLocation();
            offerList.append(offer.getModelId()).append(",").append(offer.getPrice()).append(",").append(vehicleLocation.x).append(",").append(vehicleLocation.y).append(",")
                    .append(vehicleLocation.z).append(",").append(vehicleLocation.angle).append("|");
        }
        if(offerList.toString().length() > 0)
            keyList.put("offers", offerList.toString().substring(0, offerList.toString().length()-1));
        else
            keyList.put("offers", "");
        StringBuilder parkingList = new StringBuilder();
        for(AngledLocation parkingSpot : provider.getParkingList()) {
            parkingList.append(parkingSpot.x).append(",").append(parkingSpot.y).append(",").append(parkingSpot.z).append(",").append(parkingSpot.angle).append("|");
        }
        if(parkingList.toString().length() > 0)
            keyList.put("parkingList", parkingList.toString().substring(0, parkingList.toString().length()-1));
        else
            keyList.put("parkingList", "");
        StringBuilder licenseBuilder = new StringBuilder();
        for(BuyableVehicleLicense license : provider.getBoughtLicenses()) {
            licenseBuilder.append(license.getModelid()).append(",").append(license.getPrice()).append("|");
        }
        if(licenseBuilder.toString().length() > 0)
            keyList.put("licenses", licenseBuilder.toString().substring(0, licenseBuilder.toString().length()-1));
        else
            keyList.put("licenses", "");
        keyList.put("messageLog", "empty");
        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE vehicleproviders SET ");
        Iterator<Map.Entry<String, Object>> it = keyList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> pairs = it.next();
            builder.append(pairs.getKey()).append(" = '").append(pairs.getValue()).append("'");
            if (it.hasNext()) {
                builder.append(", ");
            } else {
                builder.append(" ");
            }
        }
        builder.append("WHERE Id = '").append(provider.getDatabaseId()).append("'");
        mysqlConnection.executeUpdate(builder.toString());
    }

    private void loadVehicleProviders() {
        vehicleProviderList.clear();
        try {
            ResultSet providerSet = mysqlConnection.executeQuery("SELECT * FROM vehicleproviders");
            while(providerSet.next()) {
                VehicleProvider provider = new VehicleProvider(providerSet.getString("owner"), new Location(providerSet.getFloat("pickupLocationX"), providerSet.getFloat("pickupLocationY"), providerSet.getFloat("pickupLocationZ")));
                provider.setCash(providerSet.getInt("kasse"));
                provider.setDatabaseId(providerSet.getInt("Id"));
                provider.setName(providerSet.getString("name"));
                String offerList = providerSet.getString("offers");
                if(offerList != null && !offerList.equals("")) {
                    String[] parts = offerList.split("[|]");
                    for(String part : parts) {
                        String[] offerInformation = part.split("[,]");
                        VehicleOffer offer = new VehicleOffer(Integer.parseInt(offerInformation[0]), Integer.parseInt(offerInformation[1]), Float.parseFloat(offerInformation[2]), Float.parseFloat(offerInformation[3]),
                            Float.parseFloat(offerInformation[4]), Float.parseFloat(offerInformation[5]), provider);
                        provider.getOfferList().add(offer);
                    }
                }
                String messageLog = providerSet.getString("messageLog");
                if(messageLog != null) {
                    String[] parts = messageLog.split("[|]");
                    for(String part : parts) {
                        //TODO: add messagelog load system
                    }
                }
                String parkingList = providerSet.getString("parkingList");
                if(parkingList != null && !parkingList.equals("")) {
                    String[] parts = parkingList.split("[|]");
                    for(String part : parts) {
                        String[] content = part.split("[,]");
                        float x = Float.parseFloat(content[0]);
                        float y = Float.parseFloat(content[1]);
                        float z = Float.parseFloat(content[2]);
                        float a = Float.parseFloat(content[3]);
                        AngledLocation parkignSpot = new AngledLocation(x, y, z, a);
                        provider.getParkingList().add(parkignSpot);
                    }
                }
                String licenses = providerSet.getString("licenses");
                if(licenses != null && !licenses.equals("")) {
                    String[] parts = licenses.split("[|]");
                    for(String part : parts) {
                        String[] information = part.split("[,]");
                        BuyableVehicleLicense license = new BuyableVehicleLicense(Integer.parseInt(information[0]), Integer.parseInt(information[1]));
                        provider.getBoughtLicenses().add(license);
                    }
                }
                provider.update3DTextLabel();
                vehicleProviderList.add(provider);
            }
        } catch (Exception ex) {
            logger.error("Es konnten nicht alle Autohänder geladen werden. Stacktrace: ");
            ex.printStackTrace();
        }
    }

    private void loadPlayerVehicles() {
        playerVehicles.clear();
        try {
            ResultSet vehicleSet = mysqlConnection.executeQuery("SELECT * FROM playervehicles");
            while(vehicleSet.next()) {
                PlayerVehicle vehicle = new PlayerVehicle(vehicleSet.getInt("modelid"), vehicleSet.getString("owner"), vehicleSet.getFloat("spawnX"),
                        vehicleSet.getFloat("spawnY"), vehicleSet.getFloat("spawnZ"), vehicleSet.getFloat("spawnA"), vehicleSet.getInt("c1"), vehicleSet.getInt("c2"));
                vehicle.setBoughtDate(new java.sql.Date(vehicleSet.getLong("bought")));
                vehicle.setSellersName(vehicleSet.getString("sellerName"));
                vehicle.setDatabaseId(vehicleSet.getInt("Id"));
                vehicle.setPrice(vehicleSet.getInt("price"));
                playerVehicles.add(vehicle);
            }
        } catch (Exception ex) {
            logger.error("Es konnten nicht alle Fahrzeuge geladen werden. Stacktrace:");
            ex.printStackTrace();
        }
        logger.info("Es wurden " + playerVehicles.size() + " Spielerfahrzeuge geladen.");
    }

    Textdraw getOfferBoxTextdraw() {
		return offerBoxTextdraw;
	}
    
    public PlayerKey getEngineKey() {
        return engineKey;
    }

    public void setEngineKey(PlayerKey engineKey) {
        this.engineKey = engineKey;
    }

    private void savePlayerVehicles() {
        playerVehicles.forEach(this::savePlayerVehicle);
    }

    public Logger getLoggerInstance() {
        return logger;
    }

    public EventManager getEventManagerInstance() {
        return eventManager;
    }

    public List<PlayerVehicle> getPlayerVehicles() {
        return playerVehicles;
    }

    public PlayerLifecycleHolder getPlayerLifecycleHolder() {
        return playerLifecycleHolder;
    }

    public void setMoneyGetter(Function<Player, Integer> moneyGetter) {
        this.moneyGetter = moneyGetter;
    }

    public List<VehicleProvider> getVehicleProviderList() {
        return vehicleProviderList;
    }

    public void setAddMoneyFunction(BiConsumer<Player, Integer> addMoneyFunction) {
        this.addMoneyFunction = addMoneyFunction;
    }

    public List<BuyableVehicleLicense> getBuyableLicenses() {
        return buyableLicenses;
    }

    public BiConsumer<Player, Integer> getAddMoneyFunction() {
        return addMoneyFunction;
    }

    public Function<Player, Integer> getMoneyGetter() {
        return moneyGetter;
    }

    public boolean isFindCarEnabled() {
        return findCarEnabled;
    }

    public void setFindCarEnabled(boolean findCarEnabled) {
        this.findCarEnabled = findCarEnabled;
    }

    public MysqlConnection getMysqlConnection() {
        return mysqlConnection;
    }

    public void savePlayerVehicle(PlayerVehicle vehicle) {
        Map<String, Object> keyList = new HashMap<>();
        keyList.put("modelid", vehicle.getModel());
        keyList.put("c1", vehicle.getColor1());
        keyList.put("c2", vehicle.getColor2());
        keyList.put("spawnX", vehicle.getSpawnX());
        keyList.put("spawnY", vehicle.getSpawnY());
        keyList.put("spawnZ", vehicle.getSpawnZ());
        keyList.put("spawnA", vehicle.getSpawnA());
        keyList.put("owner", vehicle.getOwner());
        keyList.put("price", vehicle.getPrice());
        keyList.put("bought", vehicle.getBoughtDate().getTime());
        keyList.put("sellerName", vehicle.getSellersName());
        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE playervehicles SET ");
        Iterator<Map.Entry<String, Object>> it = keyList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> pairs = it.next();
            builder.append(pairs.getKey()).append(" = '").append(pairs.getValue()).append("'");
            if (it.hasNext()) {
                builder.append(", ");
            } else {
                builder.append(" ");
            }
        }
        builder.append("WHERE Id = '").append(vehicle.getDatabaseId()).append("'");
        mysqlConnection.executeUpdate(builder.toString());
    }
}
