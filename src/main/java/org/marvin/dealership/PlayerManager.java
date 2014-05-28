package org.marvin.dealership;

import net.gtaun.shoebill.common.command.Command;
import net.gtaun.shoebill.common.command.PlayerCommandManager;
import net.gtaun.shoebill.constant.PlayerKey;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.event.player.PlayerConnectEvent;
import net.gtaun.shoebill.event.player.PlayerDisconnectEvent;
import net.gtaun.shoebill.event.player.PlayerKeyStateChangeEvent;
import net.gtaun.shoebill.event.player.PlayerUpdateEvent;
import net.gtaun.shoebill.object.PlayerKeyState;
import net.gtaun.util.event.EventHandler;
import net.gtaun.util.event.HandlerPriority;

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
                }
            });
            playerData.setProvider(DealershipPlugin.getInstance().getVehicleProviderList().stream().filter(provider -> provider.getOwner().equals(playerData.getPlayer().getName())).findAny().orElse(null));
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerDisconnectEvent.class, event -> {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(event.getPlayer(), PlayerData.class);
            playerData.getPlayerVehicles().forEach(PlayerVehicle::destoryVehicle);
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerKeyStateChangeEvent.class, event -> {
            if(DealershipPlugin.getInstance().getEngineKey() != null && event.getPlayer().getKeyState().isKeyPressed(DealershipPlugin.getInstance().getEngineKey())) {
                if (event.getPlayer().getVehicle() != null) {
                    PlayerVehicle playerVehicle = DealershipPlugin.getInstance().getPlayerVehicles().stream().filter(veh -> veh.getVehicle() == event.getPlayer().getVehicle()).findAny().orElse(null);
                    Commands.toggleVehicleEngine(event.getPlayer(), playerVehicle, false);
                }
            }
        });

        DealershipPlugin.getInstance().getEventManagerInstance().registerHandler(PlayerUpdateEvent.class, playerUpdateEvent -> {
            PlayerData playerData = DealershipPlugin.getInstance().getPlayerLifecycleHolder().getObject(playerUpdateEvent.getPlayer(), PlayerData.class);
            if(playerData.isEditingCamera()) {
                int ud, lr;
                ud = playerUpdateEvent.getPlayer().getKeyState().getUpdownValue();
                lr = playerUpdateEvent.getPlayer().getKeyState().getLeftrightValue();
                System.out.println("New ud: " + ud + " old ud: " + playerData.getUdold());
                System.out.println("New lr: " + lr + " old lr: " + playerData.getLrold());
                if ((System.currentTimeMillis() - playerData.getLastCameraMove()) > 100 && playerData.getCurrentMoveMode() == MoveMode.MOVE_FORWARD) {
                    playerData.moveCamera();
                }
                if (playerData.getUdold() != ud || playerData.getLrold() != lr) {
                    if ((playerData.getUdold() != 0 || playerData.getLrold() != 0) && ud == 0 && lr == 0) {
                        playerData.getCameraObject().stop();
                        playerData.setCurrentMoveMode(MoveMode.NONE);
                    } else {
                        playerData.setMoveDirectionFromKeys();
                        playerData.moveCamera();
                    }
                }
                playerData.setLrold(lr);
                playerData.setUdold(ud);
            }
        });
    }
}
