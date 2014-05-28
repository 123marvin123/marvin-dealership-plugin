package org.marvin.dealership;

import net.gtaun.shoebill.common.player.PlayerLifecycleObject;
import net.gtaun.shoebill.data.Vector3D;
import net.gtaun.shoebill.object.Player;
import net.gtaun.shoebill.object.PlayerObject;
import net.gtaun.shoebill.object.SampObject;
import net.gtaun.util.event.EventManager;

import java.util.ArrayList;
import java.util.List;

public class PlayerData extends PlayerLifecycleObject {
    private Player player;
    private List<PlayerVehicle> playerVehicles;
    private int money;
    private VehicleProvider provider;
    private boolean editingCamera;
    private PlayerObject cameraObject;
    private long lastCameraMove;
    private MoveMode currentMoveMode;
    private int udold;
    private int lrold;

    public PlayerData(EventManager eventManager, Player player) {
        super(eventManager, player);
        this.player = player;
        this.playerVehicles = new ArrayList<>();
    }

    public Player getPlayer() {
        return player;
    }

    public List<PlayerVehicle> getPlayerVehicles() {
        return playerVehicles;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public VehicleProvider getProvider() {
        return provider;
    }

    public void setProvider(VehicleProvider provider) {
        this.provider = provider;
    }

    public PlayerObject getCameraObject() {
        return cameraObject;
    }

    public boolean isEditingCamera() {
        return editingCamera;
    }

    public void setEditingCamera(boolean editingCamera) {
        this.editingCamera = editingCamera;
    }

    public void setCameraObject(PlayerObject cameraObject) {
        this.cameraObject = cameraObject;
    }

    public long getLastCameraMove() {
        return lastCameraMove;
    }

    public void setLastCameraMove(long lastCameraMove) {
        this.lastCameraMove = lastCameraMove;
    }

    public MoveMode getCurrentMoveMode() {
        return currentMoveMode;
    }

    public int getLrold() {
        return lrold;
    }

    public int getUdold() {
        return udold;
    }

    public void setCurrentMoveMode(MoveMode currentMoveMode) {
        this.currentMoveMode = currentMoveMode;
    }

    public void setLrold(int lrold) {
        this.lrold = lrold;
    }

    public void setUdold(int udold) {
        this.udold = udold;
    }

    @Override
    protected void onInit() {

    }

    @Override
    protected void onDestroy() {

    }

    public void moveCamera() {
        lastCameraMove = System.currentTimeMillis();
        Vector3D cameraPosition = player.getCameraPosition();
        Vector3D cameraVector = player.getCameraFrontVector();
        float offsetX = cameraVector.x*6000.0f;
        float offsetY = cameraVector.y*6000.0f;
        float offsetZ = cameraVector.z*6000.0f;
        switch (currentMoveMode) {
            case MOVE_FORWARD:
                cameraObject.move(new Vector3D(cameraPosition.x+offsetX, cameraPosition.y+offsetY, cameraPosition.z+offsetZ), 10);
            case MOVE_BACK:
                cameraObject.move(new Vector3D(cameraPosition.x-offsetX, cameraPosition.y-offsetY, cameraPosition.z-offsetZ), 10);
            case MOVE_LEFT:
                cameraObject.move(new Vector3D(cameraPosition.x-offsetX, cameraPosition.y+offsetY, cameraPosition.z), 10);
            case MOVE_RIGHT:
                cameraObject.move(new Vector3D(cameraPosition.x+offsetX, cameraPosition.y-offsetY, cameraPosition.z), 10);
            case MOVE_BACK_LEFT:
                cameraObject.move(new Vector3D(cameraPosition.x+(-offsetX - offsetY), cameraPosition.y+(-offsetY + offsetX), cameraPosition.z-offsetZ), 10);
            case MOVE_BACK_RIGHT:
                cameraObject.move(new Vector3D(cameraPosition.x+(-offsetX + offsetY), cameraPosition.y+(-offsetY - offsetX), cameraPosition.z-offsetZ), 10);
            case MOVE_FORWARD_LEFT:
                cameraObject.move(new Vector3D(cameraPosition.x+(-offsetX - offsetY), cameraPosition.y+(-offsetY + offsetX), cameraPosition.z+offsetZ), 10);
            case MOVE_FORWARD_RIGHT:
                cameraObject.move(new Vector3D(cameraPosition.x+(-offsetX + offsetY), cameraPosition.y+(-offsetY - offsetX), cameraPosition.z+offsetZ), 10);
        }
    }

    public void setMoveDirectionFromKeys() {
        int ud, lr;
        ud = player.getKeyState().getUpdownValue();
        lr = player.getKeyState().getLeftrightValue();
        if(lr < 0) {
            if(ud < 0) currentMoveMode = MoveMode.MOVE_FORWARD_LEFT;
            else if(ud > 0) currentMoveMode = MoveMode.MOVE_BACK_LEFT;
            else currentMoveMode = MoveMode.MOVE_LEFT;
        } else if (lr > 0) {
            if(ud < 0) currentMoveMode = MoveMode.MOVE_FORWARD_RIGHT;
            else if(ud > 0) currentMoveMode = MoveMode.MOVE_BACK_RIGHT;
            else currentMoveMode = MoveMode.MOVE_RIGHT;
        } else if(ud < 0) currentMoveMode = MoveMode.MOVE_FORWARD;
        else if(ud > 0) currentMoveMode = MoveMode.MOVE_BACK;
        System.out.println("New MoveMode: " + currentMoveMode.toString());
        System.out.println("ud: " + ud);
        System.out.println("lr: " + lr);
    }

}
