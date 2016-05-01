package org.marvin.dealership;

import net.gtaun.shoebill.common.command.CommandParameter;
import net.gtaun.shoebill.common.command.PlayerCommandManager;
import net.gtaun.shoebill.common.dialog.InputDialog;
import net.gtaun.shoebill.common.dialog.ListDialog;
import net.gtaun.shoebill.common.dialog.MsgboxDialog;
import net.gtaun.shoebill.constant.PlayerState;
import net.gtaun.shoebill.constant.VehicleModel;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.data.Radius;
import net.gtaun.shoebill.data.Vector3D;
import net.gtaun.shoebill.event.player.*;
import net.gtaun.shoebill.event.vehicle.*;
import net.gtaun.shoebill.object.*;
import net.gtaun.util.event.HandlerPriority;
import net.gtaun.wl.lang.LocalizedStringSet;

import java.sql.Date;
import java.util.Random;

/**
 * Created by Marvin on 26.05.2014.
 */
public class PlayerManager implements Destroyable {
    private PlayerCommandManager commandManager;
    private LocalizedStringSet localizedStringSet = DealershipPlugin.getInstance().getLocalizedStringSet();
    public PlayerManager() {

        commandManager = new PlayerCommandManager(DealershipPlugin.getInstance().getEventManagerInstance());
        commandManager.registerCommands(new Commands(DealershipPlugin.getInstance().getLocalizedStringSet()));
        commandManager.installCommandHandler(HandlerPriority.NORMAL);

        commandManager.setUsageMessageSupplier((player, s, commandEntry) -> {
            String message = localizedStringSet.get(player, "Cmds.Usage") + s + commandEntry.getCommand();
            for (CommandParameter param : commandEntry.getParameters()) {
                message += " [" + param.name() + "]";
            }
            if (commandEntry.getHelpMessage() != null && commandEntry.getHelpMessage().length() > 0)
                message += " - " + commandEntry.getHelpMessage();
            return message;
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerConnectEvent.class, event -> {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(event.getPlayer(), PlayerData.class);
            DealershipPlugin.getInstance().getVehicleProviderList().forEach(provider -> {
                PlayerLabel infoLabel = PlayerLabel.create(playerData.getPlayer(), DealershipPlugin.getInstance().getLocalizedStringSet().format(playerData.getPlayer(), "Labels.DealershipInformation", provider.getName(), provider.getOwner(), provider.getOfferList().size()), Color.ORANGE, provider.getPickupPosition(), 20, false);
                provider.getInformationLabels().put(playerData.getPlayer(), infoLabel);
                provider.getOfferList().forEach(offer -> {
                    PlayerLabel label = PlayerLabel.create(event.getPlayer(), localizedStringSet.format(playerData.getPlayer(), "Labels.ForSale", VehicleModel.getName(offer.getModelId()), offer.getPrice(), provider.getName()), Color.GREEN, offer.getSpawnLocation(), 20, false);
                    offer.getPlayerLabels().put(event.getPlayer(), label);
                    label.attach(offer.getPreview(), 0, 0, 0);
                });
            });
            DealershipPlugin.getInstance().getPlayerVehicles().forEach(playerVehicle -> {
                if (playerVehicle.getOwner().equals(event.getPlayer().getName())) {
                    playerData.getPlayerVehicles().add(playerVehicle);
                    playerVehicle.spawnVehicle();
                    if (playerVehicle.getComponentsString() != null) {
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

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(UnoccupiedVehicleUpdateEvent.class, unoccupiedVehicleUpdateEvent -> {
            if(unoccupiedVehicleUpdateEvent.getPlayer().getLocation().distance(unoccupiedVehicleUpdateEvent.getVehicle().getLocation()) < 5) {
                VehicleOffer offer = null;
                for(VehicleProvider provider : DealershipPlugin.getInstance().getVehicleProviderList()) {
                    offer = provider.hasVehicle(unoccupiedVehicleUpdateEvent.getVehicle());
                    if(offer != null) break;
                }
                if(offer != null)
                    unoccupiedVehicleUpdateEvent.getVehicle().setLocation(offer.getSpawnLocation());
            }
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(VehicleSpawnEvent.class, vehicleSpawnEvent -> {
            for(PlayerData playerData : DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObjects(PlayerData.class)) {
                PlayerVehicle vehicle = playerData.getPlayerVehicles().stream().filter(veh -> veh != null && veh.getVehicle() == vehicleSpawnEvent.getVehicle()).findAny().orElse(null);
                if(vehicle != null) {
                    if(vehicle.getComponentsString() != null) {
                        String[] splits = vehicle.getComponentsString().split("[,]");
                        for (String split : splits) {
                            try {
                                int component = Integer.parseInt(split);
                                vehicle.getVehicle().getComponent().add(component);
                            } catch (Exception ignored) {}
                        }
                        break;
                    }
                }
            }
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerDisconnectEvent.class, event -> {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(event.getPlayer(), PlayerData.class);
            DealershipPlugin.getInstance().getVehicleProviderList().forEach(provider -> {
                if(provider.getInformationLabels().containsKey(event.getPlayer())) {
                    provider.getInformationLabels().get(event.getPlayer()).destroy();
                    provider.getInformationLabels().remove(event.getPlayer());
                }
                provider.getOfferList().stream().forEach(offer -> {
                    if(offer.getPlayerLabels().containsKey(event.getPlayer())) {
                        PlayerLabel label = offer.getPlayerLabels().get(event.getPlayer());
                        if(label != null && !label.isDestroyed()) {
                            offer.getPlayerLabels().get(event.getPlayer()).destroy();
                        }
                        offer.getPlayerLabels().remove(event.getPlayer());
                    }
                });
            });
            playerData.getPlayerVehicles().forEach(PlayerVehicle::destroy);
            if(playerData.getTestDriver() != null) {
                playerData.getTestDriveRemover().stop();
                playerData.getTestDriver().getVehicle().destroy();
                playerData.getPlayer().setLocation(playerData.getTestDriver().getProvider().getPickupPosition());
                playerData.setTestDriver(null);
            }
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
                                .buttonOk(localizedStringSet.get(playerData.getPlayer(), "Dialog.Change"))
                                .buttonCancel(localizedStringSet.get(playerData.getPlayer(), "Dialog.Cancel"))
                                .caption(localizedStringSet.format(playerData.getPlayer(), "Dialog.ChangePriceOfTitle", VehicleModel.getName(playerData.getLastOffer().getModelId())))
                                .message(localizedStringSet.format(playerData.getPlayer(), "Dialog.ChangePriceOf", VehicleModel.getName(playerData.getLastOffer().getModelId()), playerData.getLastOffer().getPrice()))
                                .onClickCancel((event) -> playerData.setLastOffer(null))
                                .onClickOk((inputDialog, s) -> {
                                    try {
                                        int price = Integer.parseInt(s);
                                        if (price < 1) {
                                            e.getPlayer().sendMessage(Color.RED, localizedStringSet.format(playerData.getPlayer(), "Errors.PriceTooLow", "1"));
                                            inputDialog.show();
                                        } else {
                                            playerData.getLastOffer().setPrice(price);
                                            playerData.getLastOffer().updateLabel();
                                            e.getPlayer().sendMessage(Color.ORANGE, localizedStringSet.format(playerData.getPlayer(), "Dialog.ChangedPrice", VehicleModel.getName(playerData.getLastOffer().getModelId()), price));
                                            playerData.setLastOffer(null);
                                        }
                                    } catch (Exception ex) {
                                        e.getPlayer().sendMessage(Color.RED, localizedStringSet.format(playerData.getPlayer(), "Errors.OnlyDigits"));
                                        inputDialog.show();
                                    }
                                })
                                .build()
                                .show();
                    } else if (e.getPlayerTextdraw() == playerData.getOfferDelete()) {
                        MsgboxDialog.create(e.getPlayer(), DealershipPlugin.getInstance().getEventManagerInstance())
                                .message(localizedStringSet.format(playerData.getPlayer(),"Dialog.RemovePreviewModel", VehicleModel.getName(playerData.getLastOffer().getModelId()), Color.RED.toEmbeddingString()))
                                .buttonOk(localizedStringSet.get(playerData.getPlayer(), "Dialog.Delete"))
                                .buttonCancel(localizedStringSet.get(playerData.getPlayer(), "Dialog.Cancel"))
                                .onClickOk(msgboxDialog -> {
                                    DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM vehicleoffers WHERE Id = '" + playerData.getLastOffer().getDatabaseId() + "'");
                                    playerData.getLastOffer().getProvider().getOfferList().remove(playerData.getLastOffer());
                                    playerData.getLastOffer().destroy();
                                    e.getPlayer().sendMessage(Color.GREEN, localizedStringSet.format(playerData.getPlayer(), "Main.DeletedFromModels", VehicleModel.getName(playerData.getLastOffer().getModelId())));
                                    playerData.setLastOffer(null);
                                })
                                .build()
                                .show();
                    } else if (e.getPlayerTextdraw() == playerData.getOfferCancel()) {
                        e.getPlayer().sendMessage(Color.ORANGE, localizedStringSet.get(playerData.getPlayer(), "Dialog.CanceledProcess"));
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
                    StringBuilder components = new StringBuilder();
                    for(int component : playerVehicle.getComponents()) {
                        components.append(component).append(",");
                    }
                    playerVehicle.setComponentsString(components.toString());
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
                        event.getPlayer().sendMessage(Color.RED, localizedStringSet.get(playerData.getPlayer(), "Main.CantUseDealer"));
                    else {
                        final VehicleOffer finalOffer1 = offer;
                        ListDialog.create(playerData.getPlayer(), DealershipPlugin.getInstance().getEventManagerInstance())
                                .caption(localizedStringSet.get(playerData.getPlayer(), "Dialog.VehicleSelection"))
                                .buttonOk(localizedStringSet.get(playerData.getPlayer(), "Main.Ok"))
                                .buttonCancel(localizedStringSet.get(playerData.getPlayer(), "Dialog.StepOut"))
                                .onClickCancel(abstractDialog -> playerData.getPlayer().removeFromVehicle())
                                .item(localizedStringSet.get(playerData.getPlayer(), "Dialog.BuyVehicleTitle"), (e) -> {
                                    event.getPlayer().toggleControllable(false);
                                    event.getPlayer().getVehicle().getState().setEngine(0);
                                    BuyableVehicleLicense license = finalOffer1.getProvider().getBoughtLicenses().stream().filter(lic -> lic.getModelid() == finalOffer1.getModelId()).findAny().orElse(null);
                                    if (license != null && license.isExpired()) {
                                        event.getPlayer().sendMessage(Color.RED, localizedStringSet.get(playerData.getPlayer(), "Licenses.LicenseExpired"));
                                        event.getPlayer().toggleControllable(true);
                                        e.getCurrentDialog().show();
                                    } else {
                                        MsgboxDialog.create(event.getPlayer(), DealershipPlugin.getInstance().getEventManagerInstance())
                                                .message(localizedStringSet.format(playerData.getPlayer(), "Dialog.BuyVehicle", VehicleModel.getName(finalOffer1.getModelId()), VehicleModel.getType(finalOffer1.getModelId()), finalOffer1.getPrice()))
                                                .buttonCancel(localizedStringSet.get(playerData.getPlayer(), "Dialog.GoBack"))
                                                .buttonOk(localizedStringSet.get(playerData.getPlayer(), "Dialog.Buy"))
                                                .caption(localizedStringSet.get(playerData.getPlayer(), "Dialog.BuyVehicleTitle"))
                                                .parentDialog(e.getCurrentDialog())
                                                .onClickCancel(abstractDialog -> {
                                                    event.getPlayer().toggleControllable(true);
                                                    abstractDialog.showParentDialog();
                                                })
                                                .onClickOk(msgboxDialog -> {
                                                    if (DealershipPlugin.getInstance().getMoneyGetter().apply(event.getPlayer()) >= finalOffer1.getPrice()) {
                                                        Random rnd = new Random();
                                                        event.getPlayer().removeFromVehicle();
                                                        event.getPlayer().toggleControllable(true);
                                                        int index = rnd.nextInt(finalOffer1.getProvider().getParkingList().size());
                                                        VehicleParkingspot parkingSpot = finalOffer1.getProvider().getParkingList().get(index);
                                                        PlayerVehicle newPlayerVehicle = new PlayerVehicle(finalOffer1.getModelId(), event.getPlayer().getName(), parkingSpot.getLocation().x, parkingSpot.getLocation().y, parkingSpot.getLocation().z, parkingSpot.getLocation().angle, 1, 1);
                                                        playerData.getPlayerVehicles().add(newPlayerVehicle);
                                                        DealershipPlugin.getInstance().getPlayerVehicles().add(newPlayerVehicle);
                                                        newPlayerVehicle.spawnVehicle();
                                                        newPlayerVehicle.setDoors(true);
                                                        int databaseId = DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("INSERT INTO playervehicles (owner, modelid) VALUES ('" + event.getPlayer().getName() + "', '" + finalOffer1.getModelId() + "')");
                                                        newPlayerVehicle.setDatabaseId(databaseId);
                                                        newPlayerVehicle.setPrice(finalOffer1.getPrice());
                                                        newPlayerVehicle.setBoughtDate(new Date(System.currentTimeMillis()));
                                                        newPlayerVehicle.setSellersName(finalOffer1.getProvider().getOwner());
                                                        VehicleBoughtLogEntry boughtLogEntry = new VehicleBoughtLogEntry();
                                                        boughtLogEntry.setBuyer(playerData.getPlayer().getName());
                                                        boughtLogEntry.setPrice(finalOffer1.getPrice());
                                                        boughtLogEntry.setBoughtDate(new Date(System.currentTimeMillis()));
                                                        boughtLogEntry.setBoughtModel(finalOffer1.getModelId());
                                                        finalOffer1.getProvider().getBoughtLogEntryList().add(boughtLogEntry);
                                                        DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("INSERT INTO messagelog (providerId, buyer, price, modelid, boughtDate) VALUES (" + "" +
                                                                "'" + finalOffer1.getProvider().getDatabaseId() + "', '" + event.getPlayer().getName() + "', '" + finalOffer1.getPrice() + "', '" + finalOffer1.getModelId() + "', '" + System.currentTimeMillis() + "')");
                                                        finalOffer1.getProvider().setCash(finalOffer1.getProvider().getCash() + finalOffer1.getPrice());
                                                        DealershipPlugin.getInstance().getAddMoneyFunction().accept(event.getPlayer(), -finalOffer1.getPrice());
                                                        event.getPlayer().sendMessage(Color.GREEN, localizedStringSet.format(playerData.getPlayer(), "Dialog.BoughtVehicle1", finalOffer1.getPrice(), VehicleModel.getName(finalOffer1.getModelId())));
                                                        event.getPlayer().sendMessage(Color.GREEN, localizedStringSet.get(playerData.getPlayer(), "Dialog.BoughtVehicle2"));
                                                        event.getPlayer().sendMessage(Color.GREEN, localizedStringSet.get(playerData.getPlayer(), "Dialog.BoughtVehicle3"));
                                                        event.getPlayer().setCheckpoint(Checkpoint.create(new Radius(parkingSpot.getLocation(), 5), Player::disableCheckpoint, null));
                                                    } else {
                                                        event.getPlayer().sendMessage(Color.RED, localizedStringSet.get(playerData.getPlayer(), "Dialog.NotEnoughMoney"));
                                                        event.getPlayer().sendMessage(Color.RED, localizedStringSet.format(playerData.getPlayer(), "Dialog.YouNeed", finalOffer1.getPrice(), VehicleModel.getName(finalOffer1.getModelId())));
                                                        msgboxDialog.show();
                                                    }
                                                })
                                                .build()
                                                .show();
                                    }
                                })
                                .item((finalOffer1.getProvider().isTestDrives() ? Color.GREEN.toEmbeddingString() : Color.RED.toEmbeddingString()) + localizedStringSet.get(playerData.getPlayer(), "Dialog.TestDriveTitle"), (e) -> {
                                    if (!finalOffer1.getProvider().isTestDrives() || finalOffer1.getProvider().getTestDriveTime() < 1 || finalOffer1.getProvider().getTestDriveLocation() == null || new Vector3D(0, 0, 0).distance(finalOffer1.getProvider().getTestDriveLocation()) < 15) {
                                        playerData.getPlayer().sendMessage(Color.RED, localizedStringSet.get(playerData.getPlayer(), "Dialog.NotAllowingTestDrives"));
                                        e.getCurrentDialog().show();
                                    } else {
                                        Vehicle testDriveVehicle = Vehicle.create(finalOffer1.getModelId(), finalOffer1.getProvider().getTestDriveLocation(), 1, 1, -1, false);
                                        playerData.setTestDriver(new TestDriver(testDriveVehicle, finalOffer1.getProvider()));
                                        playerData.getPlayer().setVehicle(testDriveVehicle, 0);
                                        playerData.getPlayer().sendMessage(Color.GREEN, localizedStringSet.get(playerData.getPlayer(), "Testdrive.Started"));
                                        playerData.getPlayer().sendMessage(Color.GREEN, localizedStringSet.format(playerData.getPlayer(), "Testdrive.DriveTime", finalOffer1.getProvider().getTestDriveTime()));
                                        playerData.setTestDriveRemover(Timer.create(finalOffer1.getProvider().getTestDriveTime() * 60000, 1, i -> {
                                            if (playerData.getTestDriver() != null) {
                                                playerData.getTestDriveRemover().stop();
                                                playerData.getTestDriver().getVehicle().destroy();
                                                playerData.getPlayer().setLocation(playerData.getTestDriver().getProvider().getPickupPosition());
                                                playerData.setTestDriver(null);
                                                playerData.getPlayer().sendMessage(Color.RED, localizedStringSet.get(playerData.getPlayer(), "Testdrive.Ended"));
                                            }
                                        }));
                                        playerData.getTestDriveRemover().start();
                                    }
                                })
                                .build()
                                .show();
                    }
                }
            }
         });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(VehicleExitEvent.class, vehicleExitEvent -> {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(vehicleExitEvent.getPlayer(), PlayerData.class);
            if(playerData.getTestDriver() != null) {
                playerData.getTestDriveRemover().stop();
                playerData.getTestDriver().getVehicle().destroy();
                playerData.getPlayer().setLocation(playerData.getTestDriver().getProvider().getPickupPosition());
                playerData.setTestDriver(null);
                playerData.getPlayer().sendMessage(Color.RED, localizedStringSet.get(playerData.getPlayer(), "Testdrive.Ended"));
            }
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerDeathEvent.class, playerDeathEvent -> {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(playerDeathEvent.getPlayer(), PlayerData.class);
            if(playerData.getTestDriver() != null) {
                playerData.getTestDriveRemover().stop();
                playerData.getTestDriver().getVehicle().destroy();
                playerData.setTestDriver(null);
                playerData.getPlayer().sendMessage(Color.RED, localizedStringSet.get(playerData.getPlayer(), "Testdrive.Ended"));
            }
        });
    }

    @Override
    public void destroy() {
        commandManager.destroy();
    }

    @Override
    public boolean isDestroyed() {
        return commandManager.isDestroyed();
    }
}