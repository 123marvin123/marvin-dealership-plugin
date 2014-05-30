package org.marvin.dealership;

import net.gtaun.shoebill.common.command.Command;
import net.gtaun.shoebill.common.dialog.*;
import net.gtaun.shoebill.constant.VehicleModel;
import net.gtaun.shoebill.data.*;
import net.gtaun.shoebill.object.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.function.BooleanSupplier;

import static net.gtaun.shoebill.common.dialog.InputDialog.ClickOkHandler;

/**
 * Created by Marvin on 26.05.2014.
 */
public class Commands {
    @Command
    public boolean plock(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if (playerData.getPlayerVehicles().size() < 1)
            player.sendMessage(Color.RED, "* Du besitzt keine Privatfahrzeuge!");
        else {
            PlayerVehicle nearestVehicle = playerData.getPlayerVehicles().stream().filter((playerVehicle) -> playerVehicle.getVehicleLocation().distance(player.getLocation()) < 20).sorted((o1, o2) -> {
                Location loc = player.getLocation();
                return (int) (loc.distance(o1.getVehicleLocation()) - loc.distance(o2.getVehicleLocation()));
            }).findFirst().orElse(null);
            if (nearestVehicle == null)
                player.sendMessage(Color.RED, "* Es ist kein Privatfahrzeug in der Nähe, was du beeinflussen könntest.");
            else {
                if (nearestVehicle.getDoors()) {
                    player.sendMessage(Color.RED, ">> Dein/e " + nearestVehicle.getModelName() + " wurde aufgeschlossen.");
                    player.playSound(1057);
                    nearestVehicle.setDoors(false);
                } else {
                    player.sendMessage(Color.GREEN, ">> Dein/e " + nearestVehicle.getModelName() + " wurde abgeschlossen.");
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
        if(playerData.getPlayerVehicles().size() < 1) player.sendMessage(Color.RED, "* Du besitzt keine Privatfahrzeuge!");
        else {
            ListDialog vehicleDialog = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                    .caption("Deine Fahrzeuge")
                    .buttonOk("Details")
                    .buttonCancel("Abbrechen")
                    .build();
            playerData.getPlayerVehicles().forEach(playerVehicle -> {
                vehicleDialog.getItems().add(ListDialogItem.create()
                        .itemText(playerVehicle.getModelName() + " - " + ((playerVehicle.getDoors()) ? Color.GREEN.toEmbeddingString() + "Abgeschlossen" : Color.RED.toEmbeddingString() + "Geöffnet"))
                        .onSelect((listDialogItem, o) -> {
                            MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption("Deine Fahrzeuge: " + playerVehicle.getModelName())
                                    .buttonOk("Ok")
                                    .buttonCancel("Zurück")
                                    .parentDialog(vehicleDialog)
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .onClickOk(AbstractDialog::showParentDialog)
                                    .message(Color.ALICEBLUE.toEmbeddingString() + "Fahrzeugname: " + playerVehicle.getModelName() + "\n" +
                                            "Modelid: " + playerVehicle.getModel() + "\n" +
                                            "Kaufpreis: " + playerVehicle.getPrice() + "$\n" +
                                            "Kaufdatum: " + playerVehicle.getBoughtDate().toString() + "\n" +
                                            "Anbieter: " + playerVehicle.getSellersName() + "\n" +
                                            "Türen: " + ((playerVehicle.getDoors()) ? Color.GREEN.toEmbeddingString() + "Geschlossen" : Color.RED.toEmbeddingString() + " Geöffnet") + "\n" +
                                            Color.ALICEBLUE.toEmbeddingString())
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
        if(playerData.getPlayerVehicles().size() < 1) player.sendMessage(Color.RED, "* Du besitzt keine Privatfahrzeuge!");
        else if(player.getVehicle() == null) player.sendMessage(Color.RED, "* Du befindest dich in keinem Fahrzeuge!");
        else {
            PlayerVehicle currentVehicle = playerData.getPlayerVehicles().stream().filter(playerVehicle -> playerVehicle.getVehicle() != null && playerVehicle.getVehicle() == player.getVehicle()).findAny().orElse(null);
            if(currentVehicle == null) player.sendMessage(Color.RED, "* Du befindest dich nicht in einem deiner Privatfahrzeuge!");
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
                player.sendMessage(Color.CORAL, ">> Dein Fahrzeug wird in Zukunft hier erscheinen.");
            }
        }
        return true;
    }

    @Command
    public boolean pfind(Player player) {
        if(!DealershipPlugin.getInstance().isFindCarEnabled()) player.sendMessage(Color.RED, "* Diese Funktion ist leider nicht aktiviert!");
        else {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
            if (playerData.getPlayerVehicles().size() < 1)
                player.sendMessage(Color.RED, "* Du besitzt keine Privatfahrzeuge!");
            else {
                ListDialog findCarDialog = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                        .caption("Fahrzeug finden")
                        .buttonOk("Suchen")
                        .buttonCancel("Abbrechen")
                        .build();
                playerData.getPlayerVehicles().stream().filter(playerVehicle -> playerVehicle.getVehicle() != null).forEach(playerVehicle -> {
                    float distanceToPlayer = playerVehicle.getVehicleLocation().distance(player.getLocation());
                    findCarDialog.getItems().add(ListDialogItem.create()
                            .itemText(playerVehicle.getModelName() + " (Distanz: " + distanceToPlayer + ")")
                            .onSelect((listDialogItem, o) -> {
                                player.setCheckpoint(new Checkpoint() {
                                    @Override
                                    public Radius getLocation() {
                                        return new Radius(playerVehicle.getVehicleLocation(), 5);
                                    }

                                    @Override
                                    public void onEnter(Player player) {
                                        player.disableCheckpoint();
                                        player.playSound(1057);
                                    }
                                });
                                player.sendMessage(Color.CORAL, "* Dein/e " + playerVehicle.getModelName() + " wurde auf der Karte Rort gekennzeichnet.");
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
        if(player.getVehicle() == null) player.sendMessage(Color.RED, "* Du befindest dich in keinem Fahrzeug!");
        else if(playerData.getPlayerVehicles().size() < 1) player.sendMessage(Color.RED, "* Du besitzt keine Privatfahrzeuge!");
        else {
            PlayerVehicle currentVehicle = DealershipPlugin.getInstance().getPlayerVehicles().stream().filter(playerVehicle -> playerVehicle.getVehicle() != null && playerVehicle.getOwner().equals(player.getName()) && playerVehicle.getVehicle() == player.getVehicle()).findAny().orElse(null);
            if(currentVehicle == null) player.sendMessage(Color.RED, "* Du kannst dieses Fahrzeug nicht verkaufen!");
            else {
                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                        .message("Bist du dir sicher, dass du dein/e " + currentVehicle.getModelName() + "\nfür " + currentVehicle.getPrice() / 2 + "$ verkaufen möchdest?")
                        .buttonOk("Ja!")
                        .buttonCancel("Nein!")
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
        if(playerData.getProvider() == null) player.sendMessage(Color.RED, "* Du besitzt kein Autohaus!");
        else {
            if (playerData.getProvider().getPickupPosition().distance(player.getLocation()) > 3)
                player.sendMessage(Color.RED, "* Du bist nicht in der Nähe deines Autohauses!");
            else {
                ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                        .caption("Dein Autohaus - Einstellungen")
                        .buttonOk("OK")
                        .buttonCancel("Abbrechen")
                        .item("Modelle im Angebot", e -> {
                            ListDialog models = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption("Fahrzeuge im Angebot")
                                    .buttonOk("Auswählen")
                                    .buttonCancel("Zurück")
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .build();
                            playerData.getProvider().getOfferList().forEach(offer -> {
                                models.getItems().add(ListDialogItem.create()
                                        .itemText(VehicleModel.getName(offer.getModelId()) + " - Preis: " + offer.getPrice() + "$")
                                        .onSelect(event -> {
                                            DealershipPlugin.getInstance().getOfferBoxTextdraw().show(player);
                                            playerData.getOfferVehicleModel().setText("Modelid: " + offer.getModelId());
                                            playerData.getOfferModelBox().setPreviewModel(offer.getModelId());
                                            playerData.getOfferVehicleName().setText("Fahrzeugname: " + VehicleModel.getName(offer.getModelId()));
                                            playerData.getOfferVehiclePrice().setText("Preis: " + offer.getPrice() + "$");

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
                            if(models.getItems().size() > 0)
                                models.show();
                            else {
                                player.sendMessage(Color.RED, "* Dein Autohaus besitzt keine Angebote.");
                                e.getCurrentDialog().show();
                            }
                        })
                        .item("Parkplätze", (e) -> {
                            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption("Parkplätze")
                                    .buttonCancel("Zurück")
                                    .buttonOk("Ok")
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .item("Parkplätze auflisten", (event) -> {
                                        ListDialog parkList = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Parkplätze")
                                                .buttonOk("Finden")
                                                .buttonCancel("Zurück")
                                                .parentDialog(event.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        playerData.getProvider().getParkingList().sort((o1, o2) -> {
                                            Location loc = player.getLocation();
                                            return (int) (loc.distance(o1) - loc.distance(o2));
                                        });
                                        for (AngledLocation location : playerData.getProvider().getParkingList()) {
                                            parkList.addItem("Parkplatz - Distanz: " + location.distance(player.getLocation()) + " - ID: " + playerData.getProvider().getParkingList().indexOf(location), (clickEvent) -> player.setCheckpoint(new Checkpoint() {
                                                @Override
                                                public Radius getLocation() {
                                                    return new Radius(location, 5);
                                                }

                                                @Override
                                                public void onEnter(Player player) {
                                                    player.disableCheckpoint();
                                                }
                                            }));
                                        }
                                        if (parkList.getItems().size() < 1) {
                                            player.sendMessage(Color.RED, "* Dein Autohaus besitzt keine Parkplätze.");
                                            event.getCurrentDialog().show();
                                        } else
                                            parkList.show();
                                    })
                                    .item("Parkplätze löschen", (event) -> {
                                        ListDialog parkList = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Parkplatz löschen")
                                                .buttonOk("Löschen")
                                                .buttonCancel("Zurück")
                                                .parentDialog(event.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        playerData.getProvider().getParkingList().sort((o1, o2) -> {
                                            Location loc = player.getLocation();
                                            return (int) (loc.distance(o1) - loc.distance(o2));
                                        });
                                        for (AngledLocation location : playerData.getProvider().getParkingList()) {
                                            float distance = location.distance(player.getLocation());
                                            parkList.addItem("Parkplatz - Distanz: " + distance + " - ID: " + playerData.getProvider().getParkingList().indexOf(location), (parkplatzEvent) -> {
                                                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                        .caption("Parkplatz wirklich löschen?")
                                                        .message("Bist du dir sicher, dass du den Parkplatz mit der aktuellen Distanz von\n" + distance + " löschen möchdest?")
                                                        .buttonOk("Ja")
                                                        .buttonCancel("Nein")
                                                        .parentDialog(parkplatzEvent.getCurrentDialog())
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .onClickOk(msgboxDialog -> {
                                                            playerData.getProvider().getParkingList().remove(location);
                                                            player.sendMessage(Color.GREEN, "* Der Parkplatz wurde erfolgreich gelöscht.");
                                                            event.getCurrentDialog().show();
                                                            if(playerData.getProvider().isLabelShown()) {
                                                                playerData.getProvider().getParkingSpotLabels().forEach(PlayerLabel::destroy);
                                                                playerData.getProvider().getParkingSpotLabels().clear();
                                                                playerData.getProvider().getParkingList().forEach(ploc -> {
                                                                    playerData.getProvider().getParkingSpotLabels().add(PlayerLabel.create(player, "|-- Parkplatz --|\nID: " + playerData.getProvider().getParkingList().indexOf(ploc), Color.GREEN, ploc, 20, false));
                                                                });
                                                            }
                                                        })
                                                        .build()
                                                        .show();
                                            });
                                        }
                                        if (parkList.getItems().size() < 1) {
                                            player.sendMessage(Color.RED, "* Dein Autohaus besitzt keine Parkplätze.");
                                            event.getCurrentDialog().show();
                                        } else
                                            parkList.show();
                                    })
                                    .item("Partkplatz hinzufügen", (event) -> {
                                        player.sendMessage(Color.ORANGE, "* Fahre nun bitte zu der Position, wo der Parkplatz erstellt werden soll.");
                                        player.sendMessage(Color.ORANGE, "* Wenn du an der richtigen Stelle stehst, gebe bitte /addparkplatz ein.");
                                    })
                                    .item(ListDialogItemSwitch.create()
                                            .statusSupplier(() -> playerData.getProvider().isLabelShown())
                                            .switchColor(Color.GREEN, Color.RED)
                                            .switchText("AN", "AUS")
                                            .itemText("Parkplatzmarkierugen")
                                            .onSelect((listDialogItem, o) -> {
                                                if(playerData.getProvider().isLabelShown()) {
                                                    playerData.getProvider().setLabelShown(false);
                                                    playerData.getProvider().getParkingSpotLabels().forEach(PlayerLabel::destroy);
                                                    listDialogItem.getCurrentDialog().show();
                                                } else {
                                                    playerData.getProvider().setLabelShown(true);
                                                    playerData.getProvider().getParkingList().forEach(location -> {
                                                        playerData.getProvider().getParkingSpotLabels().add(PlayerLabel.create(player, "|-- Parkplatz --|\nID: " + playerData.getProvider().getParkingList().indexOf(location), Color.GREEN, location, 20, false));
                                                    });
                                                    listDialogItem.getCurrentDialog().show();
                                                }
                                            })
                                            .build())
                                    .build()
                                    .show();
                        })
                        .item("Lizenzen", (event) -> {
                            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption("Lizenzen")
                                    .buttonOk("Ok")
                                    .buttonCancel("Zurück")
                                    .parentDialog(event.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .item("Lizenzen löschen", (e) -> {
                                        ListDialog licenseDialog = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Lizenzen löschen")
                                                .parentDialog(e.getCurrentDialog())
                                                .buttonCancel("Zurück")
                                                .buttonOk("Löschen")
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        for (BuyableVehicleLicense license : playerData.getProvider().getBoughtLicenses()) {
                                            licenseDialog.addItem("Lizenz für Fahrzeug '" + VehicleModel.getName(license.getModelid()) + "'", (clickEvent) -> {
                                                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManager())
                                                        .caption("Lizenz löschen")
                                                        .message("Bist du dir sicher das du die Lizenz für das Fahrzeug '" + VehicleModel.getName(license.getModelid()) + "' löschen möchtest?\nDiese Lizenz hat dich " + license.getPrice() + "$ gekostet. Das Geld bekommst du NICHT wieder!\n" +
                                                                "Alle Fahrzeuge die diese Lizenz verwenden, werden aus deinem Sortiment entfernt.")
                                                        .buttonOk("Löschen")
                                                        .buttonCancel("Zurück")
                                                        .parentDialog(clickEvent.getCurrentDialog())
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .onClickOk(msgboxDialog -> {
                                                            playerData.getProvider().getBoughtLicenses().remove(license);
                                                            player.sendMessage(Color.GREEN, "* Die Lizenz für das Fahrzeug '" + VehicleModel.getName(license.getModelid()) + " wurde erfolgreich gelöcht.");
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
                                                            player.sendMessage(Color.GREEN, "* Es wurde/n " + removedVehicles + " Fahrzeug/e mit dieser Lizenz gelöscht.");
                                                        })
                                                        .build()
                                                        .show();
                                            });
                                        }
                                        if (licenseDialog.getItems().size() > 0)
                                            licenseDialog.show();
                                        else {
                                            player.sendMessage(Color.RED, "* Dein Autohaus besitzt keine Lizenzen!");
                                            licenseDialog.showParentDialog();
                                        }
                                    })
                                    .item("Lizenzen erwerben", (e) -> {
                                        ListDialog licenseDialog = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Lizenzen erwerben")
                                                .buttonOk("Details")
                                                .buttonCancel("Zurück")
                                                .parentDialog(e.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        DealershipPlugin.getInstance().getBuyableLicenses().stream().filter(lic -> !playerData.getProvider().hasLicense(lic.getModelid())).forEach(lic -> {
                                            licenseDialog.addItem("Lizenz für Fahrzeug '" + VehicleModel.getName(lic.getModelid()) + "'", (licenseEvent) -> {
                                                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                        .message("Möchdest du die Lizenz für das Fahrzeug '" + VehicleModel.getName(lic.getModelid()) + "' für " + lic.getPrice() + "$ erwerben?")
                                                        .parentDialog(licenseEvent.getCurrentDialog())
                                                        .buttonCancel("Nein")
                                                        .buttonOk("Ja")
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .onClickOk(msgboxDialog -> {
                                                            if (DealershipPlugin.getInstance().getMoneyGetter().apply(player) >= lic.getPrice()) {
                                                                playerData.getProvider().getBoughtLicenses().add(lic);
                                                                DealershipPlugin.getInstance().getAddMoneyFunction().accept(player, -lic.getPrice());
                                                                player.sendMessage(Color.GREEN, "* Dir wurde erfolgreich eine Lizenz ausgestellt. Du musstest dafür " + lic.getPrice() + "$ zahlen.");
                                                                msgboxDialog.getParentDialog().showParentDialog();
                                                            } else {
                                                                player.sendMessage(Color.RED, "* Du hast nicht genügend Geld. Du brauchst " + lic.getPrice() + "$");
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
                                            player.sendMessage(Color.RED, "* Du besitzt bereits alle verfügbaren Lizenzen.");
                                            e.getCurrentDialog().show();
                                        }
                                    })
                                    .build()
                                    .show();
                        })
                        .item("Vorschaumodelle", (event) -> {
                            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption("Vorschaumodelle")
                                    .buttonOk("Ok")
                                    .buttonCancel("Zurück")
                                    .parentDialog(event.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .item("Vorschaumodelle auflisten", (e) -> {
                                        ListDialog previewModelList = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Vorschaumodelle")
                                                .buttonOk("Finden")
                                                .buttonCancel("Zurück")
                                                .parentDialog(e.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        for (VehicleOffer offer : playerData.getProvider().getOfferList()) {
                                            previewModelList.addItem("Fahrzeugname: " + VehicleModel.getName(offer.getModelId()), (clickEvent) -> {
                                                player.setCheckpoint(new Checkpoint() {
                                                    @Override
                                                    public Radius getLocation() {
                                                        return new Radius(offer.getPreview().getLocation(), 10);
                                                    }

                                                    @Override
                                                    public void onEnter(Player player) {
                                                        player.disableCheckpoint();
                                                    }
                                                });
                                                player.sendMessage(Color.GREEN, "* Das Fahrzeug wurde auf deiner Karte Rot gekennzeichnet.");
                                            });
                                        }
                                        if (previewModelList.getItems().size() > 0)
                                            previewModelList.show();
                                        else {
                                            player.sendMessage(Color.RED, "* Dein Autohaus hat momentan keine Fahrzeuge in der Austellung.");
                                            e.getCurrentDialog().show();
                                        }
                                    })
                                    .item("Vorschaumodell hinzufügen", (e) -> {
                                        Random rnd = new Random();
                                        ListDialog availableModels = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Vorschaumodell aussuchen")
                                                .parentDialog(e.getCurrentDialog())
                                                .buttonOk("Hinzufügen")
                                                .buttonCancel("Zurück")
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        for(BuyableVehicleLicense license : playerData.getProvider().getBoughtLicenses()) {
                                            availableModels.addItem("Fahrzeugname: " + VehicleModel.getName(license.getModelid()) + " - ID: " + license.getModelid(), (clickEvent) -> {
                                                InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                        .caption("Preis eingeben")
                                                        .message("Gebe nun den Preis ein, den das Fahrzeug bekommen soll (nur Zahlen):")
                                                        .buttonOk("Ok")
                                                        .buttonCancel("Zurück")
                                                        .parentDialog(clickEvent.getCurrentDialog())
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .onClickOk((inputDialog, s) -> {
                                                            try {
                                                                int price = Integer.parseInt(s);
                                                                if(price < 1) {
                                                                    player.sendMessage(Color.RED, "* Der Preis darf nicht unter 0$ liegen!");
                                                                    clickEvent.getCurrentDialog().show();
                                                                } else {
                                                                    MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                                            .caption("Bestellung bestätigen")
                                                                            .buttonOk("Kaufen")
                                                                            .buttonCancel("Zurück")
                                                                            .parentDialog(inputDialog)
                                                                            .message("Der Kauf eines Vorschaumodells des Fahrzeug '" + VehicleModel.getName(license.getModelid()) + "' wird dich " + license.getPrice()/4 + "$ kosten.\nBist du dir sicher, dass du diesen Preis bezahlen möchdest?")
                                                                            .onClickCancel(AbstractDialog::showParentDialog)
                                                                            .onClickOk(msgboxDialog -> {
                                                                                if(DealershipPlugin.getInstance().getMoneyGetter().apply(player) >=  license.getPrice()/4) {
                                                                                    if(playerData.getProvider().getParkingList().size() < 1) player.sendMessage(Color.RED, "* Dein Autohaus besitzt leider noch keine Parkplätze.");
                                                                                    else {
                                                                                        AngledLocation parkingSpot = playerData.getProvider().getParkingList().get(rnd.nextInt(playerData.getProvider().getParkingList().size()));
                                                                                        VehicleOffer offer = new VehicleOffer(license.getModelid(), price, parkingSpot.x, parkingSpot.y, parkingSpot.z, parkingSpot.angle, playerData.getProvider());
                                                                                        playerData.getProvider().getOfferList().add(offer);
                                                                                        player.setVehicle(offer.getPreview(), 0);
                                                                                        DealershipPlugin.getInstance().getAddMoneyFunction().accept(player, -(license.getPrice() / 4));
                                                                                        player.sendMessage(Color.GREEN, "* Fahre nun zu der Position, an der das Fahrzeug sein soll und benutze /ahsave");
                                                                                    }
                                                                                } else
                                                                                    player.sendMessage(Color.RED, "* Du hast leider nicht genug Geld um dir ein Vorstellungsmodell von dem Fahrzeug '" + VehicleModel.getName(license.getModelid()) + "' zu erwerben.");
                                                                            })
                                                                            .build()
                                                                            .show();
                                                                }
                                                            } catch (Exception ex) {
                                                                inputDialog.show();
                                                                player.sendMessage(Color.RED, "* Bitte nur Zahlen eingeben!");
                                                            }
                                                        })
                                                        .build()
                                                        .show();
                                            });
                                        }
                                        if(availableModels.getItems().size() > 0)
                                            availableModels.show();
                                        else {
                                            player.sendMessage(Color.RED, "* Du hast keine Fahrzeuge zu Auswahl, da du keine Lizezen besitzt!");
                                            e.getCurrentDialog().show();
                                        }
                                    })
                                    .item("Vorschaumodelle löschen", (e) -> {
                                        ListDialog previewModels = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Vorschaumodell zum löschen wählen")
                                                .buttonOk("Löschen")
                                                .buttonCancel("Zurück")
                                                .parentDialog(e.getCurrentDialog())
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .build();
                                        playerData.getProvider().getOfferList().sort((o1, o2) -> {
                                            Location loc = player.getLocation();
                                            return (int) (loc.distance(o1.getSpawnLocation()) - loc.distance(o2.getSpawnLocation()));
                                        });
                                        for(VehicleOffer offer : playerData.getProvider().getOfferList()) {
                                            float distance = offer.getSpawnLocation().distance(player.getLocation());
                                            previewModels.addItem("Fahrzeugname: " + VehicleModel.getName(offer.getModelId()) + " - ID: " + offer.getModelId() + " - Distanz: " + distance, (clickEvent) -> {
                                                MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                        .caption("Fahrzeug löschen")
                                                        .buttonOk("Löschen")
                                                        .buttonCancel("Zurück")
                                                        .parentDialog(clickEvent.getCurrentDialog())
                                                        .onClickCancel(AbstractDialog::showParentDialog)
                                                        .message("Bist du dir sicher, dass du das Vorschaumodell des Fahrzeuges '" + VehicleModel.getName(offer.getModelId()) + " löschen möchdest?")
                                                        .onClickOk(msgboxDialog -> {
                                                            offer.destroy();
                                                            playerData.getProvider().getOfferList().remove(offer);
                                                            player.sendMessage(Color.GREEN, "* Das Fahrzeug " + VehicleModel.getName(offer.getModelId()) + " wurde erfolgreich entfernt.");
                                                            clickEvent.getCurrentDialog().showParentDialog();
                                                        })
                                                        .build()
                                                        .show();
                                            });
                                        }
                                        if(previewModels.getItems().size() > 0)
                                            previewModels.show();
                                        else {
                                            player.sendMessage(Color.RED, "* Dein Autohaus hat keine Vorschaumodelle");
                                            previewModels.showParentDialog();
                                        }
                                    })
                                    .build()
                                    .show();
                        })
                        .item("Name ändern", (e) -> {
                            InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption("Name ändern")
                                    .buttonOk("Ändern")
                                    .buttonCancel("Zurück")
                                    .message("Bitte gebe nun den neuen Namen für dein Autohaus ein.\nAktueller Name: " + playerData.getProvider().getName())
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .onClickOk((inputDialog, s) -> {
                                        if(s.length() < 1 || s.length() > 24) {
                                            player.sendMessage(Color.RED, "* Bitte max. 24 Zeichen eingeben. (Mind. 1)");
                                            inputDialog.show();
                                        } else {
                                            playerData.getProvider().setName(s);
                                            playerData.getProvider().update3DTextLabel();
                                            player.sendMessage(Color.GREEN, "* Dein Autohaus heißt nun: " + s);
                                            inputDialog.showParentDialog();
                                        }
                                    })
                                    .build()
                                    .show();
                        })
                        .item("Kasse", (e) -> {
                            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption("Kasse")
                                    .buttonCancel("Zurück")
                                    .buttonOk("Ok")
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .item("Status einsehen", (event) -> {
                                        MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Aktueller Status")
                                                .parentDialog(event.getCurrentDialog())
                                                .buttonOk("Zurück")
                                                .buttonCancel("Zurück")
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .onClickOk(AbstractDialog::showParentDialog)
                                                .message("Es befinden sich aktuell:\n\n" + playerData.getProvider().getCash() + "$\n\nin der Kasse.")
                                                .build()
                                                .show();
                                    })
                                    .item("Geld abheben", (event) -> {
                                        InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Geld abheben")
                                                .parentDialog(event.getCurrentDialog())
                                                .buttonCancel("Zurück")
                                                .buttonOk("Abheben")
                                                .message("Wie viel Geld möchtest du aus der Kasse nehmen?\nEs befinden sich " + playerData.getProvider().getCash() + "$ in der Kasse:")
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .onClickOk((inputDialog, s) -> {
                                                    try {
                                                        int money = Integer.parseInt(s);
                                                        if(money < 1 || money > playerData.getProvider().getCash()) {
                                                            player.sendMessage(Color.RED, "* Der Betrag darf nicht kleiner als 1 sein, und nicht größer als in der Kasse verfügbar ist.");
                                                            inputDialog.show();
                                                        } else {
                                                            DealershipPlugin.getInstance().getAddMoneyFunction().accept(player, money);
                                                            playerData.getProvider().setCash(playerData.getProvider().getCash()-money);
                                                            player.sendMessage(Color.GREEN, "* Du hast " + money + "$ aus der Kasse genommen.");
                                                            inputDialog.showParentDialog();
                                                        }
                                                    } catch (Exception ex) {
                                                        player.sendMessage(Color.RED, "* Bitte nur Zahlen eingeben!");
                                                        inputDialog.show();
                                                    }
                                                })
                                                .build()
                                                .show();
                                    })
                                    .item("Geld einzahlen", (event) -> {
                                        InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                                .caption("Geld einzahlen")
                                                .parentDialog(event.getCurrentDialog())
                                                .buttonOk("Einzahlen")
                                                .buttonCancel("Zurück")
                                                .message("Wie viel Geld möchtest du in die Kasse einzahlen?\n\nDu kannst maximal " + DealershipPlugin.getInstance().getMoneyGetter().apply(player) + "$ einzahlen." +
                                                        "\nAktuell befinden sich " + playerData.getProvider().getCash() + "$ in der Kasse:")
                                                .onClickCancel(AbstractDialog::showParentDialog)
                                                .onClickOk((inputDialog, s) -> {
                                                    try {
                                                        int money = Integer.parseInt(s);
                                                        if(money < 1 || money > DealershipPlugin.getInstance().getMoneyGetter().apply(player)) {
                                                            player.sendMessage(Color.RED, "* Der Betrag darf nicht unter 1, und nicht höher als dein aktuelles Geld sein.");
                                                            inputDialog.show();
                                                        } else {
                                                            DealershipPlugin.getInstance().getAddMoneyFunction().accept(player, -money);
                                                            playerData.getProvider().setCash(playerData.getProvider().getCash() + money);
                                                            player.sendMessage(Color.GREEN, "* Du hast soeben " + money + "$ in die Kasse eingezahlt.");
                                                            inputDialog.showParentDialog();
                                                        }
                                                    } catch (Exception ex) {
                                                        player.sendMessage(Color.RED, "* Bitte nur Zahlen eingeben!");
                                                        inputDialog.show();
                                                    }
                                                })
                                                .build()
                                                .show();
                                    })
                                    .build()
                                    .show();
                        })
                        .item(Color.CRIMSON.toEmbeddingString() + "Autohaus löschen", (e) -> {
                            InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                    .caption("Autohaus löschen")
                                    .buttonOk("Löschen")
                                    .buttonCancel("Zurück")
                                    .message("Gebe nun bitte den Namen deines Autohauses ein, um zu bestätigen,\ndass du dein Autohaus löschen möchtest.\nName: " + playerData.getProvider().getName())
                                    .parentDialog(e.getCurrentDialog())
                                    .onClickCancel(AbstractDialog::showParentDialog)
                                    .onClickOk((inputDialog, s) -> {
                                        if (!s.equals(playerData.getProvider().getName())) {
                                            player.sendMessage(Color.RED, "* Der eingegebene Name stimmt nicht mit dem aktuellen überein.");
                                            inputDialog.showParentDialog();
                                        } else {
                                            DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("DELETE FROM vehicleproviders WHERE Id = '" + playerData.getProvider().getDatabaseId() + "'");
                                            playerData.getProvider().destroy();
                                            DealershipPlugin.getInstance().getVehicleProviderList().remove(playerData.getProvider());
                                            playerData.setProvider(null);
                                            player.sendMessage(Color.GREEN, "* Dein Autohaus wurde erfolgreich gelöscht.");
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

    @Command
    public boolean ahcreate(Player player) {
        if(!player.isAdmin()) player.sendMessage(Color.RED, "* Du bist nicht berechtigt diesen Befehl zu verwenden!");
        else {
            ListDialog playerList = ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                    .caption("Besitzer auswählen")
                    .buttonCancel("Abbrechen")
                    .buttonOk("Weiter")
                    .build();
            for(Player pl : Player.getHumans()) {
                PlayerData externPlayerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(pl, PlayerData.class);
                if(externPlayerData.getProvider() == null) {
                    playerList.addItem(pl.getName(), (e) -> {
                        InputDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                                .caption("Autohausname eingeben")
                                .message("Gebe bitte nun den Namen des Autohaus ein.\n(z.B. " + pl.getName() + "'s Autohaus)")
                                .buttonCancel("Zurück")
                                .buttonOk("Weiter")
                                .parentDialog(e.getCurrentDialog())
                                .onClickCancel(AbstractDialog::showParentDialog)
                                .onClickOk((inputDialog, s) -> {
                                    if(s.contains("|") || s.contains(",")) {
                                        player.sendMessage(Color.RED, "* Der Name darf keine '|' oder ',' enthalten.");
                                        inputDialog.show();
                                    } else {
                                        if(externPlayerData.getProvider() != null) {
                                            player.sendMessage(Color.RED, "* Der ausgewählte Spieler hat in der Zeit schon ein Autohaus zugewiesen bekommen.");
                                            inputDialog.showParentDialog();
                                        } else {
                                            VehicleProvider provider = new VehicleProvider(pl.getName(), player.getLocation());
                                            provider.setName(s);
                                            provider.setDatabaseId(DealershipPlugin.getInstance().getMysqlConnection().executeUpdate("INSERT INTO vehicleproviders (owner) VALUES ('" + pl.getName() + "')"));
                                            provider.update3DTextLabel();
                                            externPlayerData.setProvider(provider);
                                            DealershipPlugin.getInstance().getVehicleProviderList().add(provider);
                                            pl.sendMessage(Color.GREEN, "* Dir wurde soeben ein Autohaus mit dem Namen '" + s + "' zugewiesen.");
                                            pl.sendMessage(Color.GREEN, "* Die Position wurde dir Rot auf der Karte markiert.");
                                            pl.setCheckpoint(new Checkpoint() {
                                                @Override
                                                public Radius getLocation() {
                                                    return new Radius(provider.getPickupPosition(), 3);
                                                }

                                                @Override
                                                public void onEnter(Player player) {
                                                    pl.disableCheckpoint();
                                                    pl.sendMessage(Color.GREEN, "* Du kannst dein Autohaus mit /ahsettings konfigurieren.");
                                                }
                                            });
                                            player.sendMessage(Color.GREEN, "* Der Spieler " + pl.getName() + " besitzt nun ein Autohaus mit dem Namen " + s + ".");
                                        }
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
        if(playerData.getProvider() == null) player.sendMessage(Color.RED, "* Du besitzt kein Autohaus!");
        else {
            player.setCheckpoint(new Checkpoint() {
                @Override
                public Radius getLocation() {
                    return new Radius(playerData.getProvider().getPickupPosition(), 3);
                }

                @Override
                public void onEnter(Player player) {
                    player.disableCheckpoint();
                }
            });
            player.sendMessage(Color.ORANGE, "* Dein Autohaus wurde auf der Karte Rot markiert. (Distanz: " + player.getLocation().distance(playerData.getProvider().getPickupPosition()) + ")");
        }
        return true;
    }

    @Command
    public boolean addparkplatz(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if(playerData.getProvider() == null) player.sendMessage(Color.RED, "* Du besitzt kein Autohaus!");
        else {
            if(player.getVehicle() == null) player.sendMessage(Color.RED, "* Du musst in einem Fahrzeug sitzen!");
            else {
                if(player.getLocation().distance(playerData.getProvider().getPickupPosition()) > 100) player.sendMessage(Color.RED, "* Der Parkplatz muss im Umkreis von 100 Metern liegen!");
                else {
                    MsgboxDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                            .caption("Parkplatz hinzufügen")
                            .message("Bitte gehe sicher, dass du dich an der richtigen Position befindest.\nParkplatz hinzufügen?")
                            .buttonOk("Ja")
                            .buttonCancel("Nein")
                            .onClickOk(msgboxDialog -> {
                                if (player.getVehicle() == null)
                                    player.sendMessage(Color.RED, "* Du musst dich in einem Fahrzeug befinden!");
                                else {
                                    playerData.getProvider().getParkingList().add(player.getVehicle().getLocation());
                                    player.sendMessage(Color.GREEN, "* Der Parkplatz wurde erfolgreich registriert.");
                                    if(playerData.getProvider().isLabelShown()) {
                                        playerData.getProvider().getParkingSpotLabels().forEach(PlayerLabel::destroy);
                                        playerData.getProvider().getParkingSpotLabels().clear();
                                        playerData.getProvider().getParkingList().forEach(ploc -> playerData.getProvider().getParkingSpotLabels().add(PlayerLabel.create(player, "|-- Parkplatz --|\nID: " + playerData.getProvider().getParkingList().indexOf(ploc), Color.GREEN, ploc, 20, false)));
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
        if(playerData.getProvider() == null) player.sendMessage(Color.RED, "* Du besitzt kein Autohaus!");
        else {
            if(player.getVehicle() == null) player.sendMessage(Color.RED, "* Du sitzt in keinem Fahrzeug");
            else {
                VehicleOffer offer = playerData.getProvider().getOfferList().stream().filter(voffer -> voffer.getPreview() == player.getVehicle()).findAny().orElse(null);
                if(offer == null) player.sendMessage(Color.RED, "* Dieses Fahrzeug kannst du nicht beieinflussen!");
                else {
                    offer.setSpawnLocation(player.getVehicle().getLocation());
                    player.sendMessage(Color.GREEN, "* Die Position wurde erfolgreich gepeichert. Das Fahrzeug wird in Zukunuft an dieser Stelle sein.");
                }
            }
        }
        return true;
    }

    @Command
    public boolean phelp(Player player) {
        player.sendMessage(Color.ORANGE, " /plock - Schließt das am nahsten gelegene Privatfahrzeug von dir ab");
        player.sendMessage(Color.ORANGE, " /pfind - Findet dein Privatfahrzeug und markiert es auf der Karte");
        player.sendMessage(Color.ORANGE, " /psell - Verkauft dein aktuelles Privatfahrzeug");
        player.sendMessage(Color.ORANGE, " /ppark - Parkt dein aktuelles Fahrzeug an einer Position");
        player.sendMessage(Color.ORANGE, " /pvehs - Zeigt deine Privatfahrzeuge an");
        player.sendMessage(Color.ORANGE, " /phelp - Zeigt diese Meldungen");
        return true;
    }
}
