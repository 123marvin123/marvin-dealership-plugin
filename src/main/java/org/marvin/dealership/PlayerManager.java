package org.marvin.dealership;

import net.gtaun.shoebill.common.command.PlayerCommandManager;
import net.gtaun.shoebill.common.dialog.InputDialog;
import net.gtaun.shoebill.common.dialog.MsgboxDialog;
import net.gtaun.shoebill.constant.PlayerState;
import net.gtaun.shoebill.constant.VehicleModel;
import net.gtaun.shoebill.data.AngledLocation;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.data.Radius;
import net.gtaun.shoebill.event.player.*;
import net.gtaun.shoebill.event.vehicle.VehicleModEvent;
import net.gtaun.shoebill.event.vehicle.VehicleResprayEvent;
import net.gtaun.shoebill.object.Checkpoint;
import net.gtaun.shoebill.object.Player;
import net.gtaun.shoebill.object.Vehicle;
import net.gtaun.util.event.EventHandler;
import net.gtaun.util.event.HandlerPriority;

import java.sql.Date;
import java.util.Random;

/**
 * Created by Marvin on 26.05.2014.
 */
public class PlayerManager {
    private PlayerCommandManager commandManager;
    public PlayerManager() {

        commandManager = new PlayerCommandManager(DealershipPlugin.getInstance().getEventManagerInstance());
        commandManager.registerCommands(new Commands());
        commandManager.installCommandHandler(HandlerPriority.NORMAL);
        commandManager.setUsageMessageSupplier((player, s, s2, strings, s3) -> {
            String message = "Benutzung: " + s + s2;
            for (String param : strings) {
                message += " [" + param + "]";
            }
            if(s3 != null)
                message += " - " + s3;
            return message;
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerConnectEvent.class, event -> {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(event.getPlayer(), PlayerData.class);
            DealershipPlugin.getInstance().getPlayerVehicles().forEach(playerVehicle -> {
                if(playerVehicle.getOwner().equals(event.getPlayer().getName())) {
                    playerData.getPlayerVehicles().add(playerVehicle);
                    playerVehicle.spawnVehicle();
                    if(playerVehicle.getComponentsString() != null) {
                        String[] splits = playerVehicle.getComponentsString().split("[,]");
                        for (String split : splits) {
                            try {
                                int component = Integer.parseInt(split);
                                playerVehicle.getVehicle().getComponent().add(component);
                            } catch (Exception ignored) {}
                        }
                    }
                    playerVehicle.refreshComponentList();
                }
            });
            playerData.setProvider(DealershipPlugin.getInstance().getVehicleProviderList().stream().filter(provider -> provider.getOwner().equals(playerData.getPlayer().getName())).findAny().orElse(null));
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerDisconnectEvent.class, event -> {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(event.getPlayer(), PlayerData.class);
            playerData.getPlayerVehicles().forEach(PlayerVehicle::destoryVehicle);
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerClickPlayerTextDrawEvent.class, (e) -> {
        	PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(e.getPlayer(), PlayerData.class);
            if(e.getPlayerTextdraw() != null) {
                if (playerData.getLastOffer() != null) {
                    playerData.getOfferDelete().hide();
                    playerData.getOfferModelBox().hide();
                    playerData.getOfferVehicleModel().hide();
                    playerData.getOfferVehicleName().hide();
                    playerData.getOfferVehiclePrice().hide();
                    playerData.getOfferCancel().hide();
                    DealershipPlugin.getInstance().getOfferBoxTextdraw().hide(e.getPlayer());
                    e.getPlayer().cancelSelectTextDraw();
                    if (e.getPlayerTextdraw() == playerData.getOfferVehiclePrice()) {
                        InputDialog.create(e.getPlayer(), DealershipPlugin.getInstance().getEventManagerInstance())
                                .buttonOk("Ändern")
                                .buttonCancel("Abbrechen")
                                .caption("Preis ändern von " + VehicleModel.getName(playerData.getLastOffer().getModelId()))
                                .message("Gebe nun den neuen Preis für " + VehicleModel.getName(playerData.getLastOffer().getModelId()) + " ein." +
                                        "\nAktueller Preis: " + playerData.getLastOffer().getPrice() + "$")
                                .onClickCancel((event) -> playerData.setLastOffer(null))
                                .onClickOk((inputDialog, s) -> {
                                    try {
                                        int price = Integer.parseInt(s);
                                        if (price < 1) {
                                            e.getPlayer().sendMessage(Color.RED, "* Der Preis darf nicht unter 1$ liegen!");
                                            inputDialog.show();
                                        } else {
                                            playerData.getLastOffer().setPrice(price);
                                            playerData.getLastOffer().updateLabel();
                                            e.getPlayer().sendMessage(Color.ORANGE, "* Der Preis von " + VehicleModel.getName(playerData.getLastOffer().getModelId()) + " wurde auf " + price + "$ gesetzt.");
                                            playerData.setLastOffer(null);
                                        }
                                    } catch (Exception ex) {
                                        e.getPlayer().sendMessage(Color.RED, "* Bitte nur Zahlen eingeben!");
                                        inputDialog.show();
                                    }
                                })
                                .build()
                                .show();
                    } else if (e.getPlayerTextdraw() == playerData.getOfferDelete()) {
                        MsgboxDialog.create(e.getPlayer(), DealershipPlugin.getInstance().getEventManagerInstance())
                                .message("Bist du dir sicher, dass du " + VehicleModel.getName(playerData.getLastOffer().getModelId()) + " aus dem Sortiment entfernen möchdest?" +
                                        "\n" + Color.RED.toEmbeddingString() + "Es gibt kein zurück mehr!")
                                .buttonOk("Löschen")
                                .buttonCancel("Abbrechen")
                                .onClickOk(msgboxDialog -> {
                                    playerData.getLastOffer().getProvider().getOfferList().remove(playerData.getLastOffer());
                                    playerData.getLastOffer().destroy();
                                    e.getPlayer().sendMessage(Color.GREEN, "* Das Fahrzeug " + VehicleModel.getName(playerData.getLastOffer().getModelId()) + " wurde aus dem Sortiment entfernt.");
                                    playerData.setLastOffer(null);
                                })
                                .build()
                                .show();
                    } else if (e.getPlayerTextdraw() == playerData.getOfferCancel()) {
                        e.getPlayer().sendMessage(Color.ORANGE, "* Du hast den Vorgang abgebrochen.");
                    }
                }
            }
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(VehicleModEvent.class, vehicleModEvent -> {
            Player playerInVehicle = Player.getHumans().stream().filter(player -> player.getVehicle() != null && player.getVehicle() == vehicleModEvent.getVehicle()).findAny().orElse(null);
            if(playerInVehicle != null) {
                PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(playerInVehicle, PlayerData.class);
                PlayerVehicle playerVehicle = playerData.getPlayerVehicles().stream().filter(veh -> veh.getVehicle() != null && veh.getVehicle() == vehicleModEvent.getVehicle()).findAny().orElse(null);
                if(playerVehicle != null) {
                    playerVehicle.refreshComponentList();
                }
            }
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(VehicleResprayEvent.class, vehicleResprayEvent -> {
            Player playerInVehicle = Player.getHumans().stream().filter(player -> player.getVehicle() != null && player.getVehicle() == vehicleResprayEvent.getVehicle()).findAny().orElse(null);
            if(playerInVehicle != null) {
                PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(playerInVehicle, PlayerData.class);
                PlayerVehicle playerVehicle = playerData.getPlayerVehicles().stream().filter(veh -> veh.getVehicle() != null && veh.getVehicle() == vehicleResprayEvent.getVehicle()).findAny().orElse(null);
                if(playerVehicle != null) {
                    playerVehicle.setColor1(vehicleResprayEvent.getColor1());
                    playerVehicle.setColor2(vehicleResprayEvent.getColor2());
                }
            }
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerStateChangeEvent.class, event -> {
            if(event.getOldState() != PlayerState.DRIVER && event.getPlayer().getState() == PlayerState.DRIVER) {
                VehicleOffer offer = null;
                Vehicle playerVehicle = event.getPlayer().getVehicle();
                for(VehicleProvider provider : DealershipPlugin.getInstance().getVehicleProviderList()) {
                    offer = provider.hasVehicle(playerVehicle);
                    if(offer != null)
                        break;
                }
                PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(event.getPlayer(), PlayerData.class);
                if(offer != null && offer.getProvider() != playerData.getProvider()) {
                    if (offer.getProvider().getParkingList().size() < 1)
                        event.getPlayer().sendMessage(Color.RED, "* Dieses Autohaus kann nicht benutzt werden, da es noch keine Parkplätze besitzt.");
                    else {
                        event.getPlayer().toggleControllable(false);
                        event.getPlayer().getVehicle().getState().setEngine(0);
                        final VehicleOffer finalOffer = offer;
                        MsgboxDialog.create(event.getPlayer(), DealershipPlugin.getInstance().getEventManagerInstance())
                                .message("Möchdest du dieses Fahrzeug erwerben?\nFahrzeugname: " + VehicleModel.getName(offer.getModelId()) + "\nFahrzeugtyp: " + VehicleModel.getType(offer.getModelId()) +
                                        "\nPreis: " + offer.getPrice() + "$")
                                .buttonCancel("Aussteigen")
                                .buttonOk("Kaufen")
                                .onClickCancel(abstractDialog -> {
                                    event.getPlayer().removeFromVehicle();
                                    event.getPlayer().toggleControllable(true);
                                })
                                .onClickOk(msgboxDialog -> {
                                    if (DealershipPlugin.getInstance().getMoneyGetter().apply(event.getPlayer()) >= finalOffer.getPrice()) {
                                        Random rnd = new Random();
                                        event.getPlayer().removeFromVehicle();
                                        event.getPlayer().toggleControllable(true);
                                        int index = rnd.nextInt(finalOffer.getProvider().getParkingList().size());
                                        AngledLocation parkingSpot = finalOffer.getProvider().getParkingList().get(index);
                                        PlayerVehicle newPlayerVehicle = new PlayerVehicle(finalOffer.getModelId(), event.getPlayer().getName(), parkingSpot.x, parkingSpot.y, parkingSpot.z, parkingSpot.angle, 1, 1);
                                        playerData.getPlayerVehicles().add(newPlayerVehicle);
                                        DealershipPlugin.getInstance().getPlayerVehicles().add(newPlayerVehicle);
                                        newPlayerVehicle.spawnVehicle();
                                        newPlayerVehicle.setDoors(true);
                                        int databaseId = DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("INSERT INTO playervehicles (owner, modelid) VALUES ('" + event.getPlayer().getName() + "', '" + finalOffer.getModelId() + "')");
                                        newPlayerVehicle.setDatabaseId(databaseId);
                                        newPlayerVehicle.setPrice(finalOffer.getPrice());
                                        newPlayerVehicle.setBoughtDate(new Date(System.currentTimeMillis()));
                                        newPlayerVehicle.setSellersName(finalOffer.getProvider().getOwner());
                                        finalOffer.getProvider().setCash(finalOffer.getProvider().getCash() + finalOffer.getPrice());
                                        DealershipPlugin.getInstance().getAddMoneyFunction().accept(event.getPlayer(), -finalOffer.getPrice());
                                        event.getPlayer().sendMessage(Color.GREEN, "* Du hast dir erfolgreich für " + finalOffer.getPrice() + "$ eine/n " + VehicleModel.getName(finalOffer.getModelId()) + " gekauft.");
                                        event.getPlayer().sendMessage(Color.GREEN, "* Dein Fahrzeug steht nun an einem Parkplatz des Autohauses. Es wurde Rot auf der Karte martkiert.");
                                        event.getPlayer().sendMessage(Color.GREEN, "* Hilfe zu deinem Fahrzeug findest du unter: /phelp");
                                        event.getPlayer().setCheckpoint(new Checkpoint() {
                                            @Override
                                            public Radius getLocation() {
                                                return new Radius(parkingSpot, 5);
                                            }

                                            @Override
                                            public void onEnter(Player player) {
                                                player.disableCheckpoint();
                                            }
                                        });
                                    } else {
                                        event.getPlayer().sendMessage(Color.RED, "* Du hast leider nicht genug Geld, um dieses Fahrzeug zu erwerben.");
                                        event.getPlayer().sendMessage(Color.RED, "* Du brauchst " + finalOffer.getPrice() + "$ um dir eine/n " + VehicleModel.getName(finalOffer.getModelId()) + " zu kaufen");
                                        msgboxDialog.show();
                                    }
                                })
                                .build()
                                .show();
                    }
                }
            }
         });
    }
}