package org.marvin.dealership;

import net.gtaun.shoebill.common.command.Command;
import net.gtaun.shoebill.common.dialog.AbstractDialog;
import net.gtaun.shoebill.common.dialog.ListDialog;
import net.gtaun.shoebill.common.dialog.ListDialogItem;
import net.gtaun.shoebill.common.dialog.MsgboxDialog;
import net.gtaun.shoebill.data.*;
import net.gtaun.shoebill.object.Checkpoint;
import net.gtaun.shoebill.object.Player;
import net.gtaun.shoebill.object.PlayerObject;

/**
 * Created by Marvin on 26.05.2014.
 */
public class Commands {
    @Command
    public boolean plock(Player player) {
        PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(player, PlayerData.class);
        if(playerData.getPlayerVehicles().size() < 1) player.sendMessage(Color.RED, "* Du besitzt keine Privatfahrzeuge!");
        else {
            PlayerVehicle nearestVehicle = playerData.getPlayerVehicles().stream().filter((playerVehicle) -> playerVehicle.getVehicleLocation().distance(player.getLocation()) < 20).sorted((o1, o2) -> {
                Location loc = player.getLocation();
                return (int) (loc.distance(o1.getVehicleLocation()) - loc.distance(o2.getVehicleLocation()));
            }).findFirst().orElse(null);
            if(nearestVehicle == null) player.sendMessage(Color.RED, "* Es ist kein Privatfahrzeug in der Nähe, was du beeinflussen könntest.");
            else {
                if(nearestVehicle.getDoors()) {
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
    public boolean pengine(Player player) {
        if(player.getVehicle() == null) player.sendMessage(Color.RED, "* Du bist in keinem Fahrzeug!");
        else {
            PlayerVehicle playerVehicle = DealershipPlugin.getInstance().getPlayerVehicles().stream().filter(veh -> veh.getVehicle() == player.getVehicle()).findAny().orElse(null);
            toggleVehicleEngine(player, playerVehicle, true);
        }
        return true;
    }

    public static void toggleVehicleEngine(Player player, PlayerVehicle vehicle, boolean showMessage) {
        if(vehicle == null || !vehicle.getOwner().equalsIgnoreCase(player.getName())) {
            if(showMessage) player.sendMessage(Color.RED, "* Du kannst diesen Motor nicht ohne Schlüssel starten!");
        }
        else {
            if(vehicle.getEngine())
                player.sendMessage(Color.CORAL, "* Der Motor deines Fahrzeuges wurde abgeschaltet.");
            else
                player.sendMessage(Color.CORAL, "* Der Motor deines Fahrzeuges wurde eingeschaltet.");
            vehicle.toggleEngine();
        }
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
                                            Color.ALICEBLUE.toEmbeddingString() + "Motor: " + ((playerVehicle.getEngine()) ? Color.GREEN.toEmbeddingString() + "Gestartet" : Color.RED.toEmbeddingString() + "Gestoppt"))
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
                currentVehicle.spawnVehicle();
                player.setVehicle(currentVehicle.getVehicle(), 0);
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
        else if(playerData.getProvider().getPickupPosition().distance(player.getLocation()) > 3) player.sendMessage(Color.RED, "* Du bist nicht in der Nähe deines Autohauses!");
        else {
            ListDialog.create(player, DealershipPlugin.getInstance().getEventManagerInstance())
                    .caption("Dein Autohaus - Einstellungen")
                    .buttonOk("OK")
                    .buttonCancel("Abbrechen")
                    .item("Kamera Positon setzen", listDialogItem -> {
                        if (playerData.isEditingCamera()) {
                            playerData.setEditingCamera(false);
                            playerData.getCameraObject().destroy();
                            player.setCameraBehind();
                        } else {
                            playerData.setEditingCamera(true);
                            playerData.setCameraObject(PlayerObject.create(player, 19300, player.getLocation(), new Vector3D()));
                            player.attachCameraTo(playerData.getCameraObject());
                        }
                    })
                    .build()
                    .show();
        }
        return true;
    }
}
