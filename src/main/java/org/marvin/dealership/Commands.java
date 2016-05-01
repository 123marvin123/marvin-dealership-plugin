package org.marvin.dealership;

import net.gtaun.shoebill.common.command.Command;
import net.gtaun.shoebill.common.dialog.*;
import net.gtaun.shoebill.constant.VehicleModel;
import net.gtaun.shoebill.data.AngledLocation;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.data.Location;
import net.gtaun.shoebill.data.Radius;
import net.gtaun.shoebill.object.Checkpoint;
import net.gtaun.shoebill.object.Player;
import net.gtaun.shoebill.object.PlayerLabel;
import net.gtaun.wl.lang.LocalizedStringSet;

import java.sql.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by Marvin on 26.05.2014.
 */
public class Commands {
    private LocalizedStringSet localizedStringSet;
    public Commands(LocalizedStringSet localizedStringSet) {
        this.localizedStringSet = localizedStringSet;
    }
    @Command
    public boolean plock(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if (playerData.getPlayerVehicles().size() < 1)
            player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoVehicles"));
        else {
            PlayerVehicle nearestVehicle = playerData.getPlayerVehicles().stream().filter((playerVehicle) -> playerVehicle.getVehicleLocation().distance(player.getLocation()) < 20).sorted((o1, o2) -> {
                Location loc = player.getLocation();
                return (int) (loc.distance(o1.getVehicleLocation()) - loc.distance(o2.getVehicleLocation()));
            }).findFirst().orElse(null);
            if (nearestVehicle == null)
                player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoNearPrivateVehicle"));
            else {
                if (nearestVehicle.getDoors()) {
                    player.sendMessage(Color.RED, localizedStringSet.format(player, "Main.OpenedPrivateVeh", nearestVehicle.getModelName()));
                    player.playSound(1057);
                    nearestVehicle.setDoors(false);
                } else {
                    player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Main.ClosedPrivateVeh", nearestVehicle.getModelName()));
                    player.playSound(1056);
                    nearestVehicle.setDoors(true);
                }
                nearestVehicle.flashLights();
            }
        }
        return true;
    }

    @Command
    public boolean pvehs(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if (playerData.getPlayerVehicles().size() < 1)
            player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoVehicles"));
        else {
            ListDialog vehicleDialog = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                    .caption(localizedStringSet.get(player, "Main.YourVehicles"))
                    .buttonOk(localizedStringSet.get(player, "Main.Details"))
                    .buttonCancel(localizedStringSet.get(player, "Dialog.Cancel"))
                    .build();
            playerData.getPlayerVehicles().forEach(playerVehicle -> {
                vehicleDialog.getItems().add(ListDialogItem.create()
                        .itemText(playerVehicle.getModelName() + " - " + ((playerVehicle.getDoors()) ? Color.GREEN.toEmbeddingString() + localizedStringSet.get(player, "Dialog.Locked") : Color.RED.toEmbeddingString() + localizedStringSet.get(player, "Dialog.Unlocked")))
                        .onSelect((listDialogItem, o) -> {
                            MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourVehicles") + ": " + playerVehicle.getModelName())
                                    .buttonOk(localizedStringSet.get(player, "Main.Ok"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .parentDialog(vehicleDialog)
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .onClickOk(AbstractDialog::showParentDialog)
                                    .message(Color.ALICEBLUE.toEmbeddingString() + localizedStringSet.format(player, "Dialog.VehicleInformation", playerVehicle.getModelName(),
                                            playerVehicle.getModel(), playerVehicle.getPrice(), playerVehicle.getBoughtDate().toString(), playerVehicle.getSellersName(),
                                            ((playerVehicle.getDoors()) ? Color.GREEN.toEmbeddingString() + localizedStringSet.get(player, "Dialog.Locked") : Color.RED.toEmbeddingString() + localizedStringSet.get(player, "Dialog.Unlocked"))))
                                    .build()
                                    .show();
                        })
                        .build());
            });
            vehicleDialog.show();
        }
        return true;
    }

    @Command
    public boolean ppark(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if (playerData.getPlayerVehicles().size() < 1)
            player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoVehicles"));
        else if (player.getVehicle() == null) player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.YouNeedToSitInAVeh"));
        else {
            PlayerVehicle currentVehicle = playerData.getPlayerVehicles().stream().filter(playerVehicle -> playerVehicle.getVehicle() != null && playerVehicle.getVehicle() == player.getVehicle()).findAny().orElse(null);
            if (currentVehicle == null)
                player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NotInAPrivateVeh"));
            else {
                AngledLocation playerLocation = currentVehicle.getVehicleLocation();
                currentVehicle.setSpawnA(playerLocation.angle);
                currentVehicle.setSpawnX(playerLocation.x);
                currentVehicle.setSpawnY(playerLocation.y);
                currentVehicle.setSpawnZ(playerLocation.z);
                float vHealth = currentVehicle.getVehicle().getHealth();
                currentVehicle.spawnVehicle();
                player.setVehicle(currentVehicle.getVehicle(), 0);
                currentVehicle.getVehicle().setHealth(vHealth);
                if (currentVehicle.getComponentsString() != null) {
                    String[] splits = currentVehicle.getComponentsString().split("[,]");
                    for (String split : splits) {
                        try {
                            int component = Integer.parseInt(split);
                            currentVehicle.getVehicle().getComponent().add(component);
                        } catch (Exception ignored) {}
                    }
                }
                currentVehicle.refreshComponentList();
                player.sendMessage(Color.CORAL, localizedStringSet.get(player, "Main.ParkedVehicle"));
            }
        }
        return true;
    }

    @Command
    public boolean pfind(Player player) {
        if (!DealershipPlugin.getInstance().isFindCarEnabled())
            player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.FunctionDeactivated"));
        else {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
            if (playerData.getPlayerVehicles().size() < 1)
                player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoVehicles"));
            else {
                ListDialog findCarDialog = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                        .caption(localizedStringSet.get(player, "Dialog.FindVehicle"))
                        .buttonOk(localizedStringSet.get(player, "Dialog.Search"))
                        .buttonCancel(localizedStringSet.get(player, "Dialog.Cancel"))
                        .build();
                playerData.getPlayerVehicles().stream().filter(playerVehicle -> playerVehicle.getVehicle() != null).forEach(playerVehicle -> {
                    float distanceToPlayer = playerVehicle.getVehicleLocation().distance(player.getLocation());
                    findCarDialog.getItems().add(ListDialogItem.create()
                            .itemText(playerVehicle.getModelName() + " (" + localizedStringSet.get(player, "Main.Distance") + ": " + distanceToPlayer + ")")
                            .onSelect((listDialogItem, o) -> {
                                player.setCheckpoint(Checkpoint.create(new Radius(playerVehicle.getVehicleLocation(), 3), new Consumer<Player>() {
                                    @Override
                                    public void accept(Player player) {
                                        player.disableCheckpoint();
                                        player.playSound(1057);
                                    }
                                }, null));
                                player.sendMessage(Color.CORAL, localizedStringSet.format(player, "Dialog.VehicleMarkedOnMap", playerVehicle.getModelName()));
                            })
                            .build());
                });
                findCarDialog.show();
            }
        }
        return true;
    }

    @Command
    public boolean psell(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if (player.getVehicle() == null) player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.YouNeedToSitInAVeh"));
        else if (playerData.getPlayerVehicles().size() < 1)
            player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoVehicles"));
        else {
            PlayerVehicle currentVehicle = DealershipPlugin.getInstance().getPlayerVehicles().stream().filter(playerVehicle -> playerVehicle.getVehicle() != null && playerVehicle.getOwner().equals(player.getName()) && playerVehicle.getVehicle() == player.getVehicle()).findAny().orElse(null);
            if (currentVehicle == null) player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.CantSellVehicle"));
            else {
                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                        .message(localizedStringSet.format(player, "Dialog.SellVehicle", currentVehicle.getModelName(), currentVehicle.getPrice() / 2))
                        .buttonOk(localizedStringSet.get(player, "Main.OptionYes"))
                        .buttonCancel(localizedStringSet.get(player, "Main.OptionNo"))
                        .caption(localizedStringSet.format(player, "Dialog.SellingVehicle", currentVehicle.getModelName()))
                        .onClickOk(msgboxDialog -> currentVehicle.sell(player))
                        .build()
                        .show();
            }
        }
        return true;
    }

    @Command
    public boolean ahsettings(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if (playerData.getProvider() == null)
            player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.DontOwnADealership"));
        else {
            if (playerData.getProvider().getPickupPosition().distance(player.getLocation()) > 3)
                player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NotNearYourDealership"));
            else {
                ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                        .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Main.Settings"))
                        .buttonOk(localizedStringSet.get(player, "Main.Ok"))
                        .buttonCancel(localizedStringSet.get(player, "Dialog.Cancel"))
                        .item(localizedStringSet.get(player, "Main.ModelsInSale"), e -> {
                            ListDialog models = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Main.ModelsInSale"))
                                    .buttonOk(localizedStringSet.get(player, "Main.Choose"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .build();
                            playerData.getProvider().getOfferList().forEach(offer -> {
                                models.getItems().add(ListDialogItem.create()
                                        .itemText(VehicleModel.getName(offer.getModelId()) + " - " + localizedStringSet.get(player, "Textdraws.Price") + ": " + offer.getPrice() + "$")
                                        .onSelect(event -> {
                                            DealershipPlugin.getInstance().getOfferBoxTextdraw().show(player);
                                            playerData.getOfferVehicleModel().setText("Modelid: " + offer.getModelId());
                                            playerData.getOfferModelBox().setPreviewModel(offer.getModelId());
                                            playerData.getOfferVehicleName().setText(localizedStringSet.get(player, "Textdraws.VehicleName") + VehicleModel.getName(offer.getModelId()));
                                            playerData.getOfferVehiclePrice().setText(localizedStringSet.get(player, "Textdraws.Price") + ": " + offer.getPrice() + "$");

                                            playerData.getOfferDelete().show();
                                            playerData.getOfferModelBox().show();
                                            playerData.getOfferVehicleModel().show();
                                            playerData.getOfferVehicleName().show();
                                            playerData.getOfferVehiclePrice().show();
                                            playerData.getOfferCancel().show();
                                            player.selectTextDraw(Color.ORANGE);

                                            playerData.setLastOffer(offer);

                                        })
                                        .build());
                            });
                            if (models.getItems().size() > 0)
                                models.show();
                            else {
                                player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoModelsInSale"));
                                e.getCurrentDialog().show();
                            }
                        })
                        .item(localizedStringSet.get(player, "Main.ParkingSpots"), (e) -> {
                            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Main.ParkingSpots"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .buttonOk(localizedStringSet.get(player, "Main.Ok"))
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .item(localizedStringSet.get(player, "Dialog.ListParkingSpots"), (event) -> {
                                        ListDialog parkList = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Main.AvailableParkingSpots"))
                                                .buttonOk(localizedStringSet.get(player, "Main.Find"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .parentDialog(event.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        playerData.getProvider().getParkingList().sort((o1, o2) -> {
                                            Location loc = player.getLocation();
                                            return (int) (loc.distance(o1.getLocation()) - loc.distance(o2.getLocation()));
                                        });
                                        for (VehicleParkingspot location : playerData.getProvider().getParkingList()) {
                                            parkList.addItem(localizedStringSet.get(player, "Main.ParkingSpot") + " - " + localizedStringSet.get(player, "Main.Distance") + location.getLocation().distance(player.getLocation()) + " - ID: " + playerData.getProvider().getParkingList().indexOf(location), (clickEvent) -> player.setCheckpoint(Checkpoint.create(new Radius(location.getLocation(), 5), new Consumer<Player>() {
                                                @Override
                                                public void accept(Player player) {
                                                    player.disableCheckpoint();
                                                }
                                            }, null)));
                                        }
                                        if (parkList.getItems().size() < 1) {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoParkingSpots"));
                                            event.getCurrentDialog().show();
                                        } else
                                            parkList.show();
                                    })
                                    .item(localizedStringSet.get(player, "Dialog.DeleteParkingSpots"), (event) -> {
                                        ListDialog parkList = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Dialog.DeleteParkingSpots"))
                                                .buttonOk(localizedStringSet.get(player, "Dialog.Delete"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .parentDialog(event.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        playerData.getProvider().getParkingList().sort((o1, o2) -> {
                                            Location loc = player.getLocation();
                                            return (int) (loc.distance(o1.getLocation()) - loc.distance(o2.getLocation()));
                                        });
                                        for (VehicleParkingspot location : playerData.getProvider().getParkingList()) {
                                            float distance = location.getLocation().distance(player.getLocation());
                                            parkList.addItem(localizedStringSet.get(player, "Main.ParkingSpot") + " - " + localizedStringSet.get(player, "Main.Distance") + distance + " - ID: " + playerData.getProvider().getParkingList().indexOf(location), (parkplatzEvent) -> {
                                                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                        .caption(localizedStringSet.get(player, "Dialog.SureToDeleteParkingSpotTitle"))
                                                        .message(localizedStringSet.format(player, "Dialog.SureToDeleteParkingSpot", distance))
                                                        .buttonOk(localizedStringSet.get(player, "Main.OptionYes"))
                                                        .buttonCancel(localizedStringSet.get(player, "Main.OptionNo"))
                                                        .parentDialog(parkplatzEvent.getCurrentDialog())
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .onClickOk(msgboxDialog -> {
                                                            DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM parkingspots WHERE Id = '" + location.getDatabaseId() + "'");
                                                            playerData.getProvider().getParkingList().remove(location);
                                                            player.sendMessage(Color.GREEN, localizedStringSet.get(player, "Dialog.ParkingSpotGotDeleted"));
                                                            event.getCurrentDialog().show();
                                                            if (playerData.getProvider().isLabelShown()) {
                                                                playerData.getProvider().getParkingSpotLabels().forEach(PlayerLabel::destroy);
                                                                playerData.getProvider().getParkingSpotLabels().clear();
                                                                playerData.getProvider().getParkingList().forEach(ploc -> playerData.getProvider().getParkingSpotLabels().add(PlayerLabel.create(player, "|-- " + localizedStringSet.get(player, "Main.ParkingSpot") + "--|\nID: " + playerData.getProvider().getParkingList().indexOf(ploc), Color.GREEN, ploc.getLocation(), 20, false)));
                                                            }
                                                        })
                                                        .build()
                                                        .show();
                                            });
                                        }
                                        if (parkList.getItems().size() < 1) {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoParkingSpots"));
                                            event.getCurrentDialog().show();
                                        } else
                                            parkList.show();
                                    })
                                    .item(localizedStringSet.get(player, "Dialog.AddParkingSpot"), (event) -> {
                                        player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Main.DriveToParkingSpot1"));
                                        player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Main.DriveToParkingSpot2"));
                                    })
                                    .item(ListDialogItemSwitch.create()
                                            .statusSupplier(() -> playerData.getProvider().isLabelShown())
                                            .switchColor(Color.GREEN, Color.RED)
                                            .switchText(localizedStringSet.get(player, "Main.Enabled"), localizedStringSet.get(player, "Main.Disabled"))
                                            .itemText(localizedStringSet.get(player, "Dialog.ParkingSpotMarker"))
                                            .onSelect((listDialogItem, o) -> {
                                                if (playerData.getProvider().isLabelShown()) {
                                                    playerData.getProvider().setLabelShown(false);
                                                    playerData.getProvider().getParkingSpotLabels().forEach(PlayerLabel::destroy);
                                                    listDialogItem.getCurrentDialog().show();
                                                } else {
                                                    playerData.getProvider().setLabelShown(true);
                                                    playerData.getProvider().getParkingList().forEach(location -> {
                                                        playerData.getProvider().getParkingSpotLabels().add(PlayerLabel.create(player, "|-- " + localizedStringSet.get(player, "Main.ParkingSpot") + " --|\nID: " + playerData.getProvider().getParkingList().indexOf(location), Color.GREEN, location.getLocation(), 20, false));
                                                    });
                                                    listDialogItem.getCurrentDialog().show();
                                                }
                                            })
                                            .build())
                                    .build()
                                    .show();
                        })
                        .item(localizedStringSet.get(player, "Licenses.Licenses"), (event) -> {
                            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Licenses.Licenses"))
                                    .buttonOk(localizedStringSet.get(player, "Main.Ok"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .parentDialog(event.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .item(localizedStringSet.get(player, "Licenses.Delete"), (e) -> {
                                        PageListDialog licenseDialog = PageListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Licenses.Delete"))
                                                .parentDialog(e.getCurrentDialog())
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .buttonOk(localizedStringSet.get(player, "Dialog.Delete"))
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        for (BuyableVehicleLicense license : playerData.getProvider().getBoughtLicenses()) {
                                            licenseDialog.addItem(localizedStringSet.format(player, "Licenses.LicenseForVehicle", VehicleModel.getName(license.getModelid())), (clickEvent) -> {
                                                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManager())
                                                        .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Licenses.Delete"))
                                                        .message(localizedStringSet.format(player, "Licenses.SureToDelete", VehicleModel.getName(license.getModelid()), license.getPrice()))
                                                        .buttonOk(localizedStringSet.get(player, "Dialog.Delete"))
                                                        .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                        .parentDialog(e.getCurrentDialog())
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .onClickOk(msgboxDialog -> {
                                                            DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM licenses WHERE Id = '" + license.getDatabaseId() + "'");
                                                            playerData.getProvider().getBoughtLicenses().remove(license);
                                                            player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Licenses.DeletedLicense", VehicleModel.getName(license.getModelid())));
                                                            Iterator<VehicleOffer> iterator = playerData.getProvider().getOfferList().iterator();
                                                            int removedVehicles = 0;
                                                            while (iterator.hasNext()) {
                                                                VehicleOffer offer = iterator.next();
                                                                if (offer.getModelId() == license.getModelid()) {
                                                                    iterator.remove();
                                                                    offer.destroy();
                                                                    removedVehicles++;
                                                                }
                                                            }
                                                            player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Licenses.CarDeleted", removedVehicles));
                                                            msgboxDialog.showParentDialog();
                                                        })
                                                        .build()
                                                        .show();
                                            });
                                        }
                                        if (licenseDialog.getItems().size() > 0)
                                            licenseDialog.show();
                                        else {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Licenses.DontOwnAnyLicenses"));
                                            licenseDialog.showParentDialog();
                                        }
                                    })
                                    .item(localizedStringSet.get(player, "Licenses.Buy"), (e) -> {
                                        PageListDialog licenseDialog = PageListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Licenses.Buy"))
                                                .buttonOk("Details")
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .parentDialog(e.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        DealershipPlugin.getInstance().getBuyableLicenses().stream().filter(lic -> !playerData.getProvider().hasLicense(lic.getModelid())).forEach(lic -> {
                                            licenseDialog.addItem(localizedStringSet.format(player, "Licenses.LicenseForVehicle", VehicleModel.getName(lic.getModelid())), (licenseEvent) -> {
                                                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                        .message(localizedStringSet.format(player, "Licenses.SureToBuy", VehicleModel.getName(lic.getModelid()), lic.getPrice()))
                                                        .parentDialog(licenseEvent.getCurrentDialog())
                                                        .buttonCancel(localizedStringSet.get(player, "Main.OptionNo"))
                                                        .buttonOk(localizedStringSet.get(player, "Main.OptionYes"))
                                                        .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Licenses.Buy"))
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .onClickOk(msgboxDialog -> {
                                                            if (playerData.getProvider().getCash() >= lic.getPrice()) {
                                                                BuyableVehicleLicense providerCertificate = new BuyableVehicleLicense(lic.getModelid(), lic.getPrice(), lic.getValidDays());
                                                                providerCertificate.setBoughtDate(new Date(System.currentTimeMillis()));
                                                                providerCertificate.setExpire(new Date(providerCertificate.getBoughtDate().getTime() + (providerCertificate.getValidDays() * 86400000)));
                                                                DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("INSERT INTO licenses (providerId, modelid, price, validDays, boughtDate) VALUES ('" + playerData.getProvider().getDatabaseId() + "', '" + lic.getModelid() + "', '" + lic.getPrice() + "', '" + lic.getValidDays() + "', '" + providerCertificate.getExpire().getTime() + "')");
                                                                playerData.getProvider().getBoughtLicenses().add(lic);
                                                                playerData.getProvider().setCash(playerData.getProvider().getCash() - lic.getPrice());
                                                                player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Licenses.BoughtLicense", lic.getPrice()));
                                                                msgboxDialog.getParentDialog().showParentDialog();
                                                            } else {
                                                                player.sendMessage(Color.RED, localizedStringSet.format(player, "Licenses.NotEnoughMoney", lic.getPrice()));
                                                                msgboxDialog.showParentDialog();
                                                            }
                                                        })
                                                        .build()
                                                        .show();
                                            });
                                        });
                                        if (licenseDialog.getItems().size() > 0)
                                            licenseDialog.show();
                                        else {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Licenses.AlreadyOwnedAllLicenses"));
                                            e.getCurrentDialog().show();
                                        }
                                    })
                                    .item(localizedStringSet.get(player, "Licenses.BoughtLicenses"), (e) -> {
                                        PageListDialog licensesDialog = PageListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Licenses.BoughtLicenses"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .buttonOk(localizedStringSet.get(player, "Licenses.Infos"))
                                                .parentDialog(e.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        for(BuyableVehicleLicense license : playerData.getProvider().getBoughtLicenses()) {
                                            licensesDialog.addItem(localizedStringSet.format(player, "Licenses.LicenseForVehicle", VehicleModel.getName(license.getModelid())), listDialogItem -> {
                                                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                        .caption(localizedStringSet.format(player, "Licenses.LicenseForVehicle", VehicleModel.getName(license.getModelid())))
                                                        .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                        .buttonOk(localizedStringSet.get(player, "Dialog.GoBack"))
                                                        .message(Color.ALICEBLUE.toEmbeddingString() + localizedStringSet.format(player, "Licenses.LicenseStatus", VehicleModel.getName(license.getModelid()), (license.isExpired() ? (Color.RED.toEmbeddingString() + localizedStringSet.get(player, "Licenses.Expired")) : (Color.GREEN.toEmbeddingString() + localizedStringSet.get(player, "Licenses.Valid"))), Color.ALICEBLUE.toEmbeddingString(), license.getExpire().toString(), license.getPrice(), license.getModelid()))
                                                        .parentDialog(listDialogItem.getCurrentDialog())
                                                        .onClickOk(AbstractDialog::showParentDialog)
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .build()
                                                        .show();
                                            });
                                        }
                                        if(licensesDialog.getItems().size() == 0) {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Licenses.DontOwnAnyLicenses"));
                                            licensesDialog.showParentDialog();
                                        } else {
                                            licensesDialog.show();
                                        }
                                    })
                                    .item(localizedStringSet.get(player, "Licenses.ExpiredLicenses") + " (" + playerData.getProvider().getBoughtLicenses().stream().filter(BuyableVehicleLicense::isExpired).count() + ")", listDialogItem -> {
                                        PageListDialog expiredLicenses = PageListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Licenses.ExpiredLicenses"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .buttonOk(localizedStringSet.get(player, "Licenses.ReNew"))
                                                .parentDialog(listDialogItem.getCurrentDialog())
                                                .build();
                                        playerData.getProvider().getBoughtLicenses().stream().filter(BuyableVehicleLicense::isExpired).forEach(lic -> {
                                            expiredLicenses.addItem(localizedStringSet.format(player, "Licenses.ExpiredLicenseItem", VehicleModel.getName(lic.getModelid()), lic.getExpire().toString()), listDialogItem1 -> {
                                               MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                       .caption(localizedStringSet.format(player, "Licenses.ReNewLicenseTitle", VehicleModel.getName(lic.getModelid())))
                                                       .buttonOk(localizedStringSet.get(player, "Licenses.ReNew"))
                                                       .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                       .parentDialog(listDialogItem1.getCurrentDialog())
                                                       .onClickCancel(AbstractDialog::showParentDialog)
                                                       .onClickOk(msgboxDialog -> {
                                                            if(playerData.getProvider().getCash() < lic.getPrice()) {
                                                                player.sendMessage(Color.RED, localizedStringSet.format(player, "Licenses.NotEnoughMoney", lic.getPrice()));
                                                                msgboxDialog.show();
                                                            } else {
                                                                lic.setBoughtDate(new Date(System.currentTimeMillis()));
                                                                lic.setExpire(new Date(lic.getBoughtDate().getTime() + (lic.getValidDays() * 864_000_00)));
                                                                player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Licenses.LicenseWasUpdated", lic.getValidDays()));
                                                                player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Licenses.LicenseWasUpdated2", lic.getExpire().toString()));
                                                                DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("UPDATE licenses SET boughtDate = '" + lic.getBoughtDate().getTime() + "' WHERE Id = '" + lic.getDatabaseId() + "'");
                                                                msgboxDialog.getParentDialog().getParentDialog().getParentDialog().show();
                                                            }
                                                       })
                                                       .message(localizedStringSet.format(player, "Licenses.AreYouSureToReNew", lic.getPrice()))
                                                       .build()
                                                       .show();
                                            });
                                        });
                                        if(expiredLicenses.getItems().size() == 0) {
                                            player.sendMessage(Color.GREEN, localizedStringSet.get(player, "Licenses.AllLicensesAreGood"));
                                            expiredLicenses.showParentDialog();
                                        } else {
                                            expiredLicenses.show();
                                        }
                                    })
                                    .build()
                                    .show();
                        })
                        .item(localizedStringSet.get(player, "Dialog.PreviewModels"), (event) -> {
                            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Dialog.PreviewModels"))
                                    .buttonOk(localizedStringSet.get(player, "Main.Ok"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .parentDialog(event.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .item(localizedStringSet.get(player, "Dialog.ListPreviews"), (e) -> {
                                        ListDialog previewModelList = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Dialog.PreviewModels"))
                                                .buttonOk(localizedStringSet.get(player, "Main.Find"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .parentDialog(e.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        for (VehicleOffer offer : playerData.getProvider().getOfferList()) {
                                            previewModelList.addItem(localizedStringSet.get(player, "Common.VehicleName") + VehicleModel.getName(offer.getModelId()), (clickEvent) -> {
                                                player.setCheckpoint(Checkpoint.create(new Radius(offer.getPreview().getLocation(), 10), new Consumer<Player>() {
                                                    @Override
                                                    public void accept(Player player) {
                                                        player.disableCheckpoint();
                                                    }
                                                }, null));
                                                player.sendMessage(Color.GREEN, localizedStringSet.get(player, "Dialog.VehicleMarked"));
                                            });
                                        }
                                        if (previewModelList.getItems().size() > 0)
                                            previewModelList.show();
                                        else {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Dialog.NoPreviews"));
                                            e.getCurrentDialog().show();
                                        }
                                    })
                                    .item(localizedStringSet.get(player, "Dialog.AddPreviews"), (e) -> {
                                        Random rnd = new Random();
                                        ListDialog availableModels = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Dialog.AddPreviews"))
                                                .parentDialog(e.getCurrentDialog())
                                                .buttonOk(localizedStringSet.get(player, "Main.Add"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        for (BuyableVehicleLicense license : playerData.getProvider().getBoughtLicenses()) {
                                            availableModels.addItem(localizedStringSet.get(player, "Common.VehicleName") + VehicleModel.getName(license.getModelid()) + " - ID: " + license.getModelid(), (clickEvent) -> {
                                                InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                        .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Dialog.AddPreviews"))
                                                        .message(localizedStringSet.format(player, "Dialog.AddPreviewPrice"))
                                                        .buttonOk(localizedStringSet.get(player, "Main.Ok"))
                                                        .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                        .parentDialog(clickEvent.getCurrentDialog())
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .onClickOk((inputDialog, s) -> {
                                                            try {
                                                                int price = Integer.parseInt(s);
                                                                if (price < 1) {
                                                                    player.sendMessage(Color.RED, localizedStringSet.get(player, "Dialog.PriceTooLow"));
                                                                    clickEvent.getCurrentDialog().show();
                                                                } else {
                                                                    MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                                            .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Dialog.AddPreviews"))
                                                                            .buttonOk(localizedStringSet.get(player, "Dialog.Buy"))
                                                                            .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                                            .parentDialog(inputDialog)
                                                                            .message(localizedStringSet.format(player, "Dialog.PriceToPayForPreview", VehicleModel.getName(license.getModelid()), license.getPrice() / 4))
                                                                            .onClickCancel(AbstractDialog::showParentDialog)
                                                                            .onClickOk(msgboxDialog -> {
                                                                                if (playerData.getProvider().getCash() >= license.getPrice() / 4) {
                                                                                    if (playerData.getProvider().getParkingList().size() < 1)
                                                                                        player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NoParkingSpots"));
                                                                                    else {
                                                                                        VehicleParkingspot parkingSpot = playerData.getProvider().getParkingList().get(rnd.nextInt(playerData.getProvider().getParkingList().size()));
                                                                                        VehicleOffer offer = new VehicleOffer(license.getModelid(), price, parkingSpot.getLocation().x, parkingSpot.getLocation().y, parkingSpot.getLocation().z, parkingSpot.getLocation().angle, playerData.getProvider());
                                                                                        playerData.getProvider().getOfferList().add(offer);
                                                                                        offer.setDatabaseId(DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("INSERT INTO vehicleoffers (providerId, modelid, price, spawnX, spawnY, spawnZ, spawnA) VALUES (" +
                                                                                                "'" + playerData.getProvider().getDatabaseId() + "', '" + offer.getModelId() + "', '" + offer.getPrice() + "', '" + offer.getSpawnLocation().x + "', '" + offer.getSpawnLocation().y + "', '" + offer.getSpawnLocation().z + "', '" + offer.getSpawnLocation().angle + "')"));
                                                                                        player.setVehicle(offer.getPreview(), 0);
                                                                                        playerData.getProvider().setCash(playerData.getProvider().getCash() - (license.getPrice() / 4));
                                                                                        player.sendMessage(Color.GREEN, localizedStringSet.get(player, "Dialog.DriveToPreviewPositionNow"));
                                                                                        playerData.getProvider().update3DTextLabel();
                                                                                    }
                                                                                } else
                                                                                    player.sendMessage(Color.RED, localizedStringSet.format(player, "Dialog.NotEnoughMoneyInBank", VehicleModel.getName(license.getModelid())));
                                                                            })
                                                                            .build()
                                                                            .show();
                                                                }
                                                            } catch (Exception ex) {
                                                                inputDialog.show();
                                                                player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.OnlyInputNumbers"));
                                                            }
                                                        })
                                                        .build()
                                                        .show();
                                            });
                                        }
                                        if (availableModels.getItems().size() > 0)
                                            availableModels.show();
                                        else {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Dialog.CantGetPreviewNoLicenses"));
                                            e.getCurrentDialog().show();
                                        }
                                    })
                                    .item(localizedStringSet.get(player, "Dialog.DeletePreviews"), (e) -> {
                                        ListDialog previewModels = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Dialog.DeletePreviews"))
                                                .buttonOk(localizedStringSet.get(player, "Dialog.Delete"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .parentDialog(e.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        playerData.getProvider().getOfferList().sort((o1, o2) -> {
                                            Location loc = player.getLocation();
                                            return (int) (loc.distance(o1.getSpawnLocation()) - loc.distance(o2.getSpawnLocation()));
                                        });
                                        for (VehicleOffer offer : playerData.getProvider().getOfferList()) {
                                            float distance = offer.getSpawnLocation().distance(player.getLocation());
                                            previewModels.addItem(localizedStringSet.get(player, "Common.VehicleName") + VehicleModel.getName(offer.getModelId()) + " - ID: " + offer.getModelId() + " - " + localizedStringSet.get(player, "Main.Distance") + ": " + distance, (clickEvent) -> {
                                                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                        .caption(localizedStringSet.get(player, "Dialog.DeletePreviews"))
                                                        .buttonOk(localizedStringSet.get(player, "Dialog.Delete"))
                                                        .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                        .parentDialog(clickEvent.getCurrentDialog())
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .message(localizedStringSet.format(player, "Dialog.SureToDeletePreview", VehicleModel.getName(offer.getModelId())))
                                                        .onClickOk(msgboxDialog -> {
                                                            DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM vehicleoffers WHERE Id = '" + offer.getDatabaseId() + "'");
                                                            offer.destroy();
                                                            playerData.getProvider().getOfferList().remove(offer);
                                                            player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Dialog.PreviewDeleted", VehicleModel.getName(offer.getModelId())));
                                                            clickEvent.getCurrentDialog().showParentDialog();
                                                            playerData.getProvider().update3DTextLabel();
                                                        })
                                                        .build()
                                                        .show();
                                            });
                                        }
                                        if (previewModels.getItems().size() > 0)
                                            previewModels.show();
                                        else {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Dialog.NoPreviews"));
                                            previewModels.showParentDialog();
                                        }
                                    })
                                    .build()
                                    .show();
                        })
                        .item(localizedStringSet.get(player, "Name.Change"), (e) -> {
                            InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Name.Change"))
                                    .buttonOk(localizedStringSet.get(player, "Dialog.Change"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .message(localizedStringSet.format(player, "Name.InputNewName", playerData.getProvider().getName()))
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .onClickOk((inputDialog, s) -> {
                                        if (s.length() < 1 || s.length() > 24) {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Name.MaxLength"));
                                            inputDialog.show();
                                        } else {
                                            playerData.getProvider().setName(s);
                                            playerData.getProvider().update3DTextLabel();
                                            playerData.getProvider().getOfferList().forEach(VehicleOffer::updateLabel);
                                            player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Name.Changed", s));
                                            inputDialog.showParentDialog();
                                        }
                                    })
                                    .build()
                                    .show();
                        })
                        .item(localizedStringSet.get(player, "CashBox.CashBox"), (e) -> {
                            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "CashBox.CashBox"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .buttonOk(localizedStringSet.get(player, "Main.Ok"))
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .item(localizedStringSet.get(player, "CashBox.Status"), (event) -> {
                                        MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "CashBox.CashBox"))
                                                .parentDialog(event.getCurrentDialog())
                                                .buttonOk(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .onClickOk(AbstractDialog::showParentDialog)
                                                .message(localizedStringSet.format(player, "CashBox.StatusMessage", playerData.getProvider().getCash()))
                                                .build()
                                                .show();
                                    })
                                    .item(localizedStringSet.get(player, "CashBox.TakeMoney"), (event) -> {
                                        InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "CashBox.CashBox"))
                                                .parentDialog(event.getCurrentDialog())
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .buttonOk(localizedStringSet.get(player, "CashBox.Withdraw"))
                                                .message(localizedStringSet.format(player, "CashBox.HowMuchDeposit", playerData.getProvider().getCash()))
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .onClickOk((inputDialog, s) -> {
                                                    try {
                                                        int money = Integer.parseInt(s);
                                                        if (money < 1 || money > playerData.getProvider().getCash()) {
                                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "CashBox.WithdrawValueTooHigh"));
                                                            inputDialog.show();
                                                        } else {
                                                            DealershipPlugin.getInstance().getAddMoneyFunction().accept(player, money);
                                                            playerData.getProvider().setCash(playerData.getProvider().getCash() - money);
                                                            player.sendMessage(Color.GREEN, localizedStringSet.format(player, "CashBox.TookMoneyOut", money));
                                                            inputDialog.showParentDialog();
                                                        }
                                                    } catch (Exception ex) {
                                                        player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.OnlyInputNumbers"));
                                                        inputDialog.show();
                                                    }
                                                })
                                                .build()
                                                .show();
                                    })
                                    .item(localizedStringSet.get(player, "CashBox.DepositMoney"), (event) -> {
                                        InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "CashBox.DepositMoney"))
                                                .parentDialog(event.getCurrentDialog())
                                                .buttonOk(localizedStringSet.get(player, "CashBox.Deposit"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .message(localizedStringSet.format(player, "CashBox.HowMuchToDeposit", DealershipPlugin.getInstance().getMoneyGetter().apply(player), playerData.getProvider().getCash()))
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .onClickOk((inputDialog, s) -> {
                                                    try {
                                                        int money = Integer.parseInt(s);
                                                        if (money < 1 || money > DealershipPlugin.getInstance().getMoneyGetter().apply(player)) {
                                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "CashBox.DepositValueTooHigh"));
                                                            inputDialog.show();
                                                        } else {
                                                            DealershipPlugin.getInstance().getAddMoneyFunction().accept(player, -money);
                                                            playerData.getProvider().setCash(playerData.getProvider().getCash() + money);
                                                            player.sendMessage(Color.GREEN, localizedStringSet.format(player, "CashBox.DepositedMoney", money));
                                                            inputDialog.showParentDialog();
                                                        }
                                                    } catch (Exception ex) {
                                                        player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.OnlyInputNumbers"));
                                                        inputDialog.show();
                                                    }
                                                })
                                                .build()
                                                .show();
                                    })
                                    .build()
                                    .show();
                        })
                        .item(localizedStringSet.get(player, "SellLog.SellLog"), (e) -> {
                            PageListDialog logDialog = PageListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "SellLog.SellLog"))
                                    .buttonOk(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .itemsPerPage(9)
                                    .nextPage(localizedStringSet.get(player, "SellLog.NextPage"))
                                    .prevPage(localizedStringSet.get(player, "SellLog.PrevPage"))
                                    .build();
                            updateLogDialog(logDialog, playerData);
                        })
                        .item(localizedStringSet.get(player, "Testdrive.Testdrives"), (e) -> {
                            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Testdrive.Testdrives"))
                                    .parentDialog(e.getCurrentDialog())
                                    .buttonOk(localizedStringSet.get(player, "Main.Ok"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .item(ListDialogItemSwitch.create().statusSupplier(() -> playerData.getProvider().isTestDrives())
                                            .switchText(localizedStringSet.get(player, "Main.Enabled"), localizedStringSet.get(player, "Main.Disabled"))
                                            .switchColor(Color.GREEN, Color.RED)
                                            .itemText(localizedStringSet.get(player, "Testdrive.Allowed"))
                                            .onSelect((listDialogItem, o) -> {
                                                if (playerData.getProvider().isTestDrives())
                                                    playerData.getProvider().setTestDrives(false);
                                                else playerData.getProvider().setTestDrives(true);
                                                listDialogItem.getCurrentDialog().show();
                                            })
                                            .build())
                                    .item(localizedStringSet.get(player, "Testdrive.ChangeTimeTitle"), (d) -> {
                                        InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Testdrive.Testdrives"))
                                                .message(localizedStringSet.format(player, "Testdrive.ChangeTime", playerData.getProvider().getTestDriveTime()))
                                                .buttonOk(localizedStringSet.get(player, "Main.Ok"))
                                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                                .parentDialog(d.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .onClickOk((inputDialog, s) -> {
                                                    try {
                                                        int minutes = Integer.parseInt(s);
                                                        if (minutes < 1) {
                                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Testdrive.OnlyPositiveDigits"));
                                                            inputDialog.show();
                                                        } else if (minutes > 5) {
                                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Testdrive.TooLong"));
                                                            inputDialog.show();
                                                        } else {
                                                            playerData.getProvider().setTestDriveTime(minutes);
                                                            player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Testdrive.ChangedTime", minutes));
                                                            inputDialog.showParentDialog();
                                                        }
                                                    } catch (Exception ex) {
                                                        player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.OnlyInputNumbers"));
                                                        inputDialog.show();
                                                    }
                                                }).build()
                                                .show();
                                    })
                                    .item(localizedStringSet.get(player, "Testdrive.SetPositionTitle"), (d) -> {
                                        if (playerData.getProvider().isTestDrives()) {
                                            player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Testdrive.DriveToPos1"));
                                            player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Testdrive.DriveToPos2"));
                                        } else {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Testdrive.TestdrivesNotActivated"));
                                            d.getCurrentDialog().show();
                                        }
                                    })
                                    .build()
                                    .show();
                        })
                        .item(Color.CRIMSON.toEmbeddingString() + localizedStringSet.get(player, "Dialog.DeleteDealership"), (e) -> {
                            InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption(localizedStringSet.get(player, "Main.YourDealership") + " - " + localizedStringSet.get(player, "Dialog.DeleteDealership"))
                                    .buttonOk(localizedStringSet.get(player, "Dialog.Delete"))
                                    .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                    .message(localizedStringSet.format(player, "Dialog.ConfirmDeletion", playerData.getProvider().getName()))
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .onClickOk((inputDialog, s) -> {
                                        if (!s.equals(playerData.getProvider().getName())) {
                                            player.sendMessage(Color.RED, localizedStringSet.get(player, "Dialog.InputedNameNotCorrect"));
                                            inputDialog.showParentDialog();
                                        } else {
                                            DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM vehicleproviders WHERE Id = '" + playerData.getProvider().getDatabaseId() + "'");
                                            DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM vehicleoffers WHERE providerId = '" + playerData.getProvider().getDatabaseId() + "'");
                                            DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM messagelog WHERE providerId = '" + playerData.getProvider().getDatabaseId() + "'");
                                            DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM licenses WHERE providerId = '" + playerData.getProvider().getDatabaseId() + "'");
                                            playerData.getProvider().destroy();
                                            DealershipPlugin.getInstance().getVehicleProviderList().remove(playerData.getProvider());
                                            playerData.setProvider(null);
                                            player.sendMessage(Color.GREEN, localizedStringSet.get(player, "Dialog.DealershipDeleted"));
                                        }
                                    })
                                    .build()
                                    .show();
                        })
                        .build()
                        .show();
            }
        }
        return true;
    }

    private void updateLogDialog(ListDialog listDialog, PlayerData playerData) {
        listDialog.getItems().clear();
        listDialog.addItem(ListDialogItemRadio.create()
                .radioColor(Color.GREEN, Color.GRAY)
                .itemText(Color.GRAY.toEmbeddingString() + localizedStringSet.get(playerData.getPlayer(), "Dialog.SortBy"))
                .selectedIndex(playerData::getSelectedIndex)
                .onRadioItemSelect((listDialogItemRadio, radioItem, i) -> updateLogDialog(listDialogItemRadio.getCurrentDialog(), playerData))
                .item(new ListDialogItemRadio.RadioItem(localizedStringSet.get(playerData.getPlayer(), "Dialog.Date"), Color.GREEN, listDialogItemRadio -> {
                    playerData.getProvider().getBoughtLogEntryList().sort((o1, o2) -> o2.getBoughtDate().compareTo(o1.getBoughtDate()));
                    playerData.setSelectedIndex(0);
                }))
                .item(new ListDialogItemRadio.RadioItem(localizedStringSet.get(playerData.getPlayer(), "Dialog.PriceHigher"), Color.GREEN, listDialogItemRadio -> {
                    playerData.getProvider().getBoughtLogEntryList().sort((o1, o2) -> Integer.compare(o2.getPrice(), o1.getPrice()));
                    playerData.setSelectedIndex(1);
                }))
                .item(new ListDialogItemRadio.RadioItem(localizedStringSet.get(playerData.getPlayer(), "Dialog.PriceLower"), Color.GREEN, listDialogItemRadio -> {
                    playerData.getProvider().getBoughtLogEntryList().sort((o1, o2) -> Integer.compare(o1.getPrice(), o2.getPrice()));
                    playerData.setSelectedIndex(2);
                }))
                .build());
        for (VehicleBoughtLogEntry entry : playerData.getProvider().getBoughtLogEntryList())
            listDialog.addItem(localizedStringSet.format(playerData.getPlayer(), "Dialog.LogMessage", entry.getBoughtDate().toString(), VehicleModel.getName(entry.getBoughtModel())), listDialogItem -> {
                MsgboxDialog.create(playerData.getPlayer(), DealershipPlugin.getInstance().getEventManagerInstance())
                        .caption(localizedStringSet.format(playerData.getPlayer(), "Dialog.SoldAt", entry.getBoughtDate().toString()))
                        .buttonOk(localizedStringSet.get(playerData.getPlayer(), "Dialog.GoBack"))
                        .buttonCancel(localizedStringSet.get(playerData.getPlayer(), "Dialog.GoBack"))
                        .parentDialog(listDialog)
                        .onClickOk(AbstractDialog::showParentDialog)
                        .onClickCancel(AbstractDialog::showParentDialog)
                        .message(localizedStringSet.format(playerData.getPlayer(), "Dialog.FullLogMessage", entry.getBoughtDate().toString(), entry.getBuyer(), VehicleModel.getName(entry.getBoughtModel()), entry.getBoughtModel(), entry.getPrice()))
                        .build()
                        .show();
            });
        if (listDialog.getItems().size() > 0)
            listDialog.show();
        else {
            playerData.getPlayer().sendMessage(Color.RED, localizedStringSet.get(playerData.getPlayer(), "Dialog.LogIsEmpty"));
            listDialog.showParentDialog();
        }
    }

    @Command
    public boolean settestdrive(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if(playerData.getProvider() == null) player.sendMessage(Color.RED, localizedStringSet.get(player, "Main.YouAreNotAOwner"));
        else if(!playerData.getProvider().isTestDrives()) player.sendMessage(Color.RED, localizedStringSet.get(player, "Testdrive.NotActivated"));
        else if(player.getVehicle() == null) player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.YouNeedToSitInAVeh"));
        else {
            playerData.getProvider().setTestDriveLocation(player.getVehicle().getLocation());
            player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Testdrive.StartHere"));
        }
        return true;
    }

    @Command
    public boolean ahcreate(Player player) {
        if (!player.isAdmin()) player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.NotAllowedToUse"));
        else {
            ListDialog playerList = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                    .caption(localizedStringSet.get(player, "Dialog.SelectOwner"))
                    .buttonCancel(localizedStringSet.get(player, "Dialog.Cancel"))
                    .buttonOk(localizedStringSet.get(player, "Dialog.Next"))
                    .build();
            for (Player pl : Player.getHumans()) {
                PlayerData externPlayerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(pl, PlayerData.class);
                if (externPlayerData.getProvider() == null) {
                    playerList.addItem(pl.getName(), (e) -> {
                        InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                .caption(localizedStringSet.get(player, "Dialog.ChooseNameTitle"))
                                .message(localizedStringSet.format(player, "Dialog.ChooseName", pl.getName() + "'s Autohaus"))
                                .buttonCancel(localizedStringSet.get(player, "Dialog.GoBack"))
                                .buttonOk(localizedStringSet.get(player, "Dialog.Next"))
                                .parentDialog(e.getCurrentDialog())
                                .onClickCancel(AbstractDialog::showParentDialog)
                                .onClickOk((inputDialog, s) -> {
                                    if (externPlayerData.getProvider() != null) {
                                        player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.PlayerAlreadyHasDealership"));
                                        inputDialog.showParentDialog();
                                    } else {
                                        VehicleProvider provider = new VehicleProvider(pl.getName(), player.getLocation(), s);
                                        provider.setDatabaseId(DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("INSERT INTO vehicleproviders (owner) VALUES ('" + pl.getName() + "')"));
                                        provider.update3DTextLabel();
                                        externPlayerData.setProvider(provider);
                                        DealershipPlugin.getInstance().getVehicleProviderList().add(provider);
                                        pl.sendMessage(Color.GREEN, localizedStringSet.format(pl, "Main.YouGotDealership", s));
                                        pl.sendMessage(Color.GREEN, localizedStringSet.get(pl, "Main.DealershipLocated"));
                                        pl.setCheckpoint(Checkpoint.create(new Radius(provider.getPickupPosition(), 3), new Consumer<Player>() {
                                            @Override
                                            public void accept(Player player) {
                                                pl.disableCheckpoint();
                                                pl.sendMessage(Color.GREEN, localizedStringSet.get(pl, "Main.SettingsMessage"));
                                            }
                                        }, null));
                                        player.sendMessage(Color.GREEN, localizedStringSet.format(player, "Main.PlayerOwnDealershipNow", pl.getName(), s));
                                    }
                                })
                                .build()
                                .show();
                    });
                }
            }
            playerList.show();
        }
        return true;
    }

    @Command
    public boolean ahfind(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if (playerData.getProvider() == null)
            player.sendMessage(Color.RED, localizedStringSet.get(player, "Main.YouAreNotAOwner"));
        else {
            player.setCheckpoint(Checkpoint.create(new Radius(playerData.getProvider().getPickupPosition(), 3), new Consumer<Player>() {
                @Override
                public void accept(Player player) {
                    player.disableCheckpoint();
                }
            }, null));
            player.sendMessage(Color.ORANGE, localizedStringSet.format(player, "Main.PositionLocated", player.getLocation().distance(playerData.getProvider().getPickupPosition())));
        }
        return true;
    }

    @Command
    public boolean addparking(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if (playerData.getProvider() == null)
            player.sendMessage(Color.RED, localizedStringSet.get(player, "Main.YouAreNotAOwner"));
        else {
            if (player.getVehicle() == null) player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.YouNeedToSitInAVeh"));
            else {
                if (player.getLocation().distance(playerData.getProvider().getPickupPosition()) > 200)
                    player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.ParkingSpotTooFarAway"));
                else {
                    MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                            .caption(localizedStringSet.get(player, "Dialog.AddParkingSpotTitle"))
                            .message(localizedStringSet.get(player, "Dialog.AddParkingSpot"))
                            .buttonOk(localizedStringSet.get(player, "Main.OptionYes"))
                            .buttonCancel(localizedStringSet.get(player, "Main.OptionNo"))
                            .onClickOk(msgboxDialog -> {
                                if (player.getVehicle() == null)
                                    player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.YouNeedToSitInAVeh"));
                                else {
                                    if (player.getVehicle().getLocation().distance(playerData.getProvider().getPickupPosition()) > 200)
                                        player.sendMessage(Color.RED, localizedStringSet.get(player, "Errors.ParkingSpotTooFarAway"));
                                    else {
                                        AngledLocation location = player.getVehicle().getLocation();
                                        int id = DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("INSERT INTO parkingspots (providerId, spawnX, spawnY, spawnZ, spawnA) VALUES ('" + playerData.getProvider().getDatabaseId() + "', " +
                                                "'" + location.x + "', '" + location.y + "', '" + location.z + "', '" + location.angle + "')");
                                        playerData.getProvider().getParkingList().add(new VehicleParkingspot(location, id));
                                        player.sendMessage(Color.GREEN, localizedStringSet.get(player, "Dialog.ParkingSpotAdded"));
                                        if (playerData.getProvider().isLabelShown()) {
                                            playerData.getProvider().getParkingSpotLabels().forEach(PlayerLabel::destroy);
                                            playerData.getProvider().getParkingSpotLabels().clear();
                                            playerData.getProvider().getParkingList().forEach(ploc -> playerData.getProvider().getParkingSpotLabels().add(PlayerLabel.create(player, "|-- " + localizedStringSet.get(player, "Main.ParkingSpot") + " --|\nID: " + playerData.getProvider().getParkingList().indexOf(ploc), Color.GREEN, ploc.getLocation(), 20, false)));
                                        }
                                    }
                                }
                            })
                            .build()
                            .show();
                }
            }
        }
        return true;
    }

    @Command
    public boolean ahsave(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if (playerData.getProvider() == null)
            player.sendMessage(Color.RED, localizedStringSet.get(player, "Main.YouAreNotAOwner"));
        else {
            if (player.getVehicle() == null) player.sendMessage(Color.RED, "* Du sitzt in keinem Fahrzeug");
            else {
                VehicleOffer offer = playerData.getProvider().getOfferList().stream().filter(voffer -> voffer.getPreview() == player.getVehicle()).findAny().orElse(null);
                if (offer == null) player.sendMessage(Color.RED, localizedStringSet.get(player, "Main.CantLockThat"));
                else {
                    offer.setSpawnLocation(player.getVehicle().getLocation());
                    player.sendMessage(Color.GREEN, localizedStringSet.get(player, "Main.SavedNewPos"));
                }
            }
        }
        return true;
    }

    @Command
    public boolean phelp(Player player) {
        player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Help.Plock"));
        player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Help.Pfind"));
        player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Help.Psell"));
        player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Help.Ppark"));
        player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Help.Pvehs"));
        player.sendMessage(Color.ORANGE, localizedStringSet.get(player, "Help.Phelp"));
        return true;
    }
}
