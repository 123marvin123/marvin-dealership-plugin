package org.marvin.dealership;

import net.gtaun.shoebill.common.player.PlayerLifecycleHolder;
import net.gtaun.shoebill.constant.PlayerKey;
import net.gtaun.shoebill.constant.TextDrawAlign;
import net.gtaun.shoebill.constant.TextDrawFont;
import net.gtaun.shoebill.constant.VehicleModel;
import net.gtaun.shoebill.data.AngledLocation;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.data.Location;
import net.gtaun.shoebill.data.Vector3D;
import net.gtaun.shoebill.object.*;
import net.gtaun.shoebill.resource.Plugin;
import net.gtaun.shoebill.service.Service;
import net.gtaun.util.event.Event;
import net.gtaun.util.event.EventHandler;
import net.gtaun.util.event.EventManager;

import net.gtaun.wl.lang.Language;
import net.gtaun.wl.lang.LanguageService;
import net.gtaun.wl.lang.LocalizedStringSet;
import net.gtaun.wl.lang.event.PlayerChangeLanguageEvent;
import org.slf4j.Logger;

import javax.security.auth.callback.LanguageCallback;
import java.io.File;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Marvin on 26.05.2014.
 */
public class DealershipPlugin extends Plugin {
    private static DealershipPlugin instance;

    static DealershipPlugin getInstance() {
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
    private LanguageService languageService;
    private LocalizedStringSet localizedStringSet;
    private Function<Player, Integer> moneyGetter = Player::getMoney;
    private BiConsumer<Player, Integer> addMoneyFunction = Player::giveMoney;
    private Consumer<Player> showLanguageDialog = player -> languageService.showLanguageSelectionDialog(player, languageService::setPlayerLanguage);
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
        buyableLicenses = new ArrayList<>();
        languageService = Service.get(LanguageService.class);
        localizedStringSet = languageService.createStringSet(new File(getDataDir(), "text"));
        playerManager = new PlayerManager();
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

        eventManager.registerHandler(PlayerChangeLanguageEvent.class, playerChangeLanguageEvent -> {
            DealershipPlugin.getInstance().getVehicleProviderList().forEach(provider -> {
                if(provider.getInformationLabels().containsKey(playerChangeLanguageEvent.getPlayer())) {
                    provider.getInformationLabels().get(playerChangeLanguageEvent.getPlayer()).update(Color.ORANGE, DealershipPlugin.getInstance().getLocalizedStringSet().format(playerChangeLanguageEvent.getPlayer(), "Labels.DealershipInformation", provider.getName(), provider.getOwner(), provider.getOfferList().size()));
                } else {
                    PlayerLabel infoLabel = PlayerLabel.create(playerChangeLanguageEvent.getPlayer(), DealershipPlugin.getInstance().getLocalizedStringSet().format(playerChangeLanguageEvent.getPlayer(), "Labels.DealershipInformation", provider.getName(), provider.getOwner(), provider.getOfferList().size()), Color.ORANGE, provider.getPickupPosition(), 20, false);
                    provider.getInformationLabels().put(playerChangeLanguageEvent.getPlayer(), infoLabel);
                }
                provider.getOfferList().forEach(offer -> {
                    if(offer.getPlayerLabels().containsKey(playerChangeLanguageEvent.getPlayer())) {
                        offer.getPlayerLabels().get(playerChangeLanguageEvent.getPlayer()).update(Color.GREEN, localizedStringSet.format(playerChangeLanguageEvent.getPlayer(), "Labels.ForSale", VehicleModel.getName(offer.getModelId()), offer.getPrice(), provider.getName()));
                    } else {
                        PlayerLabel label = PlayerLabel.create(playerChangeLanguageEvent.getPlayer(), localizedStringSet.format(playerChangeLanguageEvent.getPlayer(), "Labels.ForSale", VehicleModel.getName(offer.getModelId()), offer.getPrice(), provider.getName()), Color.GREEN, offer.getSpawnLocation(), 20, false);
                        offer.getPlayerLabels().put(playerChangeLanguageEvent.getPlayer(), label);
                        label.attach(offer.getPreview(), 0, 0, 0);
                    }
                });
            });
        });
    }

    @Override
    protected void onDisable() throws Throwable {
        savePlayerVehicles();
        vehicleProviderList.forEach(this::saveVehicleProvider);
        mysqlConnection.closeConnection();
    }

    private void saveVehicleProvider(VehicleProvider provider) {
        Server.get();
        Map<String, Object> keyList = new HashMap<>();
        keyList.put("owner", provider.getOwner());
        keyList.put("pickupLocationX", provider.getPickupPosition().x);
        keyList.put("pickupLocationY", provider.getPickupPosition().y);
        keyList.put("pickupLocationZ", provider.getPickupPosition().z);
        keyList.put("kasse", provider.getCash());
        keyList.put("name", provider.getName());
        keyList.put("testDrives", provider.isTestDrives() ? 1 : 0);
        if(provider.getTestDriveLocation() != null) {
            keyList.put("testDriveX", provider.getTestDriveLocation().x);
            keyList.put("testDriveY", provider.getTestDriveLocation().y);
            keyList.put("testDriveZ", provider.getTestDriveLocation().z);
            keyList.put("testDriveA", provider.getTestDriveLocation().angle);
        }
        keyList.put("testDriveTime", provider.getTestDriveTime());
        for(VehicleOffer offer : provider.getOfferList()) {
            mysqlConnection.executeUpdate("UPDATE vehicleoffers set spawnX = '" + offer.getSpawnLocation().x + "', spawnY = '" + offer.getSpawnLocation().y + "', spawnZ = '" + offer.getSpawnLocation().z + "', " +
                    "price = '" + offer.getPrice() + "', spawnA = '" + offer.getSpawnLocation().angle + "', modelid = '" + offer.getModelId() + "' WHERE Id = '" + offer.getDatabaseId() + "'");
        }
        StringBuilder licenseBuilder = new StringBuilder();
        for(BuyableVehicleLicense license : provider.getBoughtLicenses()) {
            licenseBuilder.append(license.getModelid()).append(",").append(license.getPrice()).append("|");
        }
        if(licenseBuilder.toString().length() > 0)
            keyList.put("licenses", licenseBuilder.toString().substring(0, licenseBuilder.toString().length()-1));
        else
            keyList.put("licenses", "");
        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE vehicleproviders SET ");
        Iterator<Map.Entry<String, Object>> it = keyList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> pairs = it.next();
            builder.append(pairs.getKey()).append(" = \"").append(pairs.getValue()).append("\"");
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
                VehicleProvider provider = new VehicleProvider(providerSet.getString("owner"), new Location(providerSet.getFloat("pickupLocationX"), providerSet.getFloat("pickupLocationY"), providerSet.getFloat("pickupLocationZ")), providerSet.getString("name"));
                provider.setCash(providerSet.getInt("kasse"));
                provider.setDatabaseId(providerSet.getInt("Id"));
                provider.setTestDrives(providerSet.getBoolean("testDrives"));
                provider.setTestDriveTime(providerSet.getInt("testDriveTime"));
                provider.setTestDriveLocation(new AngledLocation(providerSet.getFloat("testDriveX"), providerSet.getFloat("testDriveY"), providerSet.getFloat("testDriveZ"), providerSet.getFloat("testDriveA")));
                ResultSet vehicleOffers = mysqlConnection.executeQuery("SELECT * FROM vehicleoffers WHERE providerId = '" + provider.getDatabaseId() + "'");
                while(vehicleOffers.next()) {
                    VehicleOffer offer = new VehicleOffer(vehicleOffers.getInt("modelid"), vehicleOffers.getInt("price"), vehicleOffers.getFloat("spawnX"), vehicleOffers.getFloat("spawnY"),
                            vehicleOffers.getFloat("spawnZ"), vehicleOffers.getFloat("spawnA"), provider);
                    offer.setDatabaseId(vehicleOffers.getInt("Id"));
                    provider.getOfferList().add(offer);
                }
                ResultSet parkingSpots = mysqlConnection.executeQuery("SELECT * FROM parkingspots WHERE providerId = '" + provider.getDatabaseId() + "'");
                while(parkingSpots.next()) {
                    provider.getParkingList().add(new VehicleParkingspot(new AngledLocation(parkingSpots.getFloat("spawnX"), parkingSpots.getFloat("spawnY"), parkingSpots.getFloat("spawnZ"), parkingSpots.getFloat("spawnA")), parkingSpots.getInt("Id")));
                }
                ResultSet licenses = mysqlConnection.executeQuery("SELECT * FROM licenses WHERE providerId = '" + provider.getDatabaseId() + "'");
                while(licenses.next()) {
                    BuyableVehicleLicense license = new BuyableVehicleLicense(licenses.getInt("modelid"), licenses.getInt("price"));
                    license.setDatabaseId(licenses.getInt("Id"));
                    provider.getBoughtLicenses().add(license);
                }
                ResultSet messageLog = mysqlConnection.executeQuery("SELECT * FROM messagelog WHERE providerId = '" + provider.getDatabaseId() + "'");
                while(messageLog.next()) {
                    VehicleBoughtLogEntry entry = new VehicleBoughtLogEntry();
                    entry.setDatabaseId(messageLog.getInt("Id"));
                    entry.setPrice(messageLog.getInt("price"));
                    entry.setBuyer(messageLog.getString("buyer"));
                    entry.setBoughtModel(messageLog.getInt("modelid"));
                    entry.setBoughtDate(new Date(messageLog.getLong("boughtDate")));
                    provider.getBoughtLogEntryList().add(entry);
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
                vehicle.setComponentsString(vehicleSet.getString("components"));
                playerVehicles.add(vehicle);
            }
        } catch (Exception ex) {
            logger.error("Es konnten nicht alle Fahrzeuge geladen werden. Stacktrace:");
            ex.printStackTrace();
        }
        logger.info("Es wurden " + playerVehicles.size() + " Spielerfahrzeuge geladen.");
    }

    LocalizedStringSet getLocalizedStringSet() {
        return localizedStringSet;
    }

    Textdraw getOfferBoxTextdraw() {
		return offerBoxTextdraw;
	}

    private void savePlayerVehicles() {
        playerVehicles.forEach(this::savePlayerVehicle);
    }

    Logger getLoggerInstance() {
        return logger;
    }

    EventManager getEventManagerInstance() {
        return eventManager;
    }

    List<PlayerVehicle> getPlayerVehicles() {
        return playerVehicles;
    }

    PlayerLifecycleHolder getPlayerLifecycleHolder() {
        return playerLifecycleHolder;
    }

    public void setMoneyGetter(Function<Player, Integer> moneyGetter) {
        this.moneyGetter = moneyGetter;
    }

    List<VehicleProvider> getVehicleProviderList() {
        return vehicleProviderList;
    }

    public void setAddMoneyFunction(BiConsumer<Player, Integer> addMoneyFunction) {
        this.addMoneyFunction = addMoneyFunction;
    }

    public List<BuyableVehicleLicense> getBuyableLicenses() {
        return buyableLicenses;
    }

    BiConsumer<Player, Integer> getAddMoneyFunction() {
        return addMoneyFunction;
    }

    Function<Player, Integer> getMoneyGetter() {
        return moneyGetter;
    }

    public boolean isFindCarEnabled() {
        return findCarEnabled;
    }

    public void setFindCarEnabled(boolean findCarEnabled) {
        this.findCarEnabled = findCarEnabled;
    }

    MysqlConnection getMysqlConnection() {
        return mysqlConnection;
    }

    public Consumer<Player> getShowLanguageDialog() {
        return showLanguageDialog;
    }

    public void setPlayerLanguage(Player player, String abbr) {
        languageService.setPlayerLanguage(player, Language.get(abbr));
    }

    void savePlayerVehicle(PlayerVehicle vehicle) {
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
        StringBuilder components = new StringBuilder();
        for(int component : vehicle.getComponents()) {
            components.append(component).append(",");
        }
        if(components.toString().length() > 0)
            keyList.put("components", components.toString().substring(0, components.toString().length()-1));
        else
            keyList.put("components", "");
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
