package org.marvin.dealership;

import net.gtaun.shoebill.common.player.PlayerLifecycleObject;
import net.gtaun.shoebill.constant.TextDrawAlign;
import net.gtaun.shoebill.constant.TextDrawFont;
import net.gtaun.shoebill.data.Color;
import net.gtaun.shoebill.data.Time;
import net.gtaun.shoebill.data.Vector3D;
import net.gtaun.shoebill.object.*;
import net.gtaun.util.event.EventManager;

import java.util.ArrayList;
import java.util.List;

class PlayerData extends PlayerLifecycleObject {
    private Player player;
    private List<PlayerVehicle> playerVehicles;
    private int money;
    private VehicleProvider provider;
    private PlayerTextdraw offerVehicleName;
    private PlayerTextdraw offerVehiclePrice;
    private PlayerTextdraw offerVehicleModel;
    private PlayerTextdraw offerModelBox;
    private PlayerTextdraw offerDelete;
    private PlayerTextdraw offerCancel;
    private VehicleOffer lastOffer;
    private int selectedIndex;
    private TestDriver testDriver;
    private Timer testDriveRemover;

    public PlayerData(EventManager eventManager, Player player) {
        super(eventManager, player);
        this.player = player;
        this.playerVehicles = new ArrayList<>();
    }

    public Player getPlayer() {
        return player;
    }

    List<PlayerVehicle> getPlayerVehicles() {
        return playerVehicles;
    }

    int getMoney() {
        return money;
    }

    void setMoney(int money) {
        this.money = money;
    }

    VehicleProvider getProvider() {
        return provider;
    }

    PlayerTextdraw getOfferDelete() {
		return offerDelete;
	}
    
    PlayerTextdraw getOfferModelBox() {
		return offerModelBox;
	}
    
    PlayerTextdraw getOfferVehicleModel() {
		return offerVehicleModel;
	}
    PlayerTextdraw getOfferVehicleName() {
		return offerVehicleName;
	}
	
	PlayerTextdraw getOfferVehiclePrice() {
		return offerVehiclePrice;
	}

    Timer getTestDriveRemover() {
        return testDriveRemover;
    }

    void setTestDriveRemover(Timer testDriveRemover) {
        this.testDriveRemover = testDriveRemover;
    }

    TestDriver getTestDriver() {
        return testDriver;
    }

    void setTestDriver(TestDriver testDriver) {
        this.testDriver = testDriver;
    }

    int getSelectedIndex() {
        return selectedIndex;
    }

    void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    void setProvider(VehicleProvider provider) {
        this.provider = provider;
        if(provider == null) {
        	if(offerVehicleName != null)
        		offerVehicleName.destroy();
        	if(offerVehiclePrice != null)
        		offerVehiclePrice.destroy();
        	if(offerVehicleModel != null)
        		offerVehicleModel.destroy();
        	if(offerModelBox != null)
        		offerModelBox.destroy();
        	if(offerDelete != null)
        		offerDelete.destroy();
        } else {
        	if(offerVehicleName == null) {
        		offerVehicleName = PlayerTextdraw.create(player, 311.666625f, 164.681350f, DealershipPlugin.getInstance().getLocalizedStringSet().get(player, "Textdraws.VehicleName"));
        		offerVehicleName.setLetterSize(0.256666f, 1.828148f);
        		offerVehicleName.setAlignment(TextDrawAlign.CENTER);
        		offerVehicleName.setColor(new Color(-1));
        		offerVehicleName.setShadowSize(0);
        		offerVehicleName.setOutlineSize(1);
        		offerVehicleName.setBackgroundColor(new Color(51));
        		offerVehicleName.setFont(TextDrawFont.FONT2);
        		offerVehicleName.setProportional(true);
        	} if(offerVehiclePrice == null) {
        		offerVehiclePrice = PlayerTextdraw.create(player, 351.666809f, 201.599990f, DealershipPlugin.getInstance().getLocalizedStringSet().get(player, "Textdraws.Price"));
        		offerVehiclePrice.setLetterSize(0.298332f, 1.392591f);
        		offerVehiclePrice.setAlignment(TextDrawAlign.RIGHT);
        		offerVehiclePrice.setColor(new Color(-1));
        		offerVehiclePrice.setShadowSize(0);
        		offerVehiclePrice.setOutlineSize(1);
        		offerVehiclePrice.setBackgroundColor(new Color(51));
        		offerVehiclePrice.setFont(TextDrawFont.FONT2);
        		offerVehiclePrice.setProportional(true);
        		offerVehiclePrice.setSelectable(true);
        	} if(offerVehicleModel == null) {
        		offerVehicleModel = PlayerTextdraw.create(player, 310.999938f, 185.422302f, DealershipPlugin.getInstance().getLocalizedStringSet().get(player, "Textdraws.ModelId"));
        		offerVehicleModel.setLetterSize(0.284666f, 1.280591f);
        		offerVehicleModel.setAlignment(TextDrawAlign.CENTER);
        		offerVehicleModel.setColor(new Color(-1));
        		offerVehicleModel.setShadowSize(0);
        		offerVehicleModel.setOutlineSize(1);
        		offerVehicleModel.setBackgroundColor(new Color(51));
        		offerVehicleModel.setFont(TextDrawFont.FONT2);
        		offerVehicleModel.setProportional(true);
        	} if(offerModelBox == null) {
        		offerModelBox = PlayerTextdraw.create(player, 290.999908f, 223.011062f, "_");
        		offerModelBox.setTextSize(50, 50);
        		offerModelBox.setUseBox(true);
        		offerModelBox.setBoxColor(new Color(0x000000FF));
        		offerModelBox.setFont(TextDrawFont.MODEL_PREVIEW);
        		offerModelBox.setPreviewModelRotation(-12, 0, 0, 0.75f);
        	} if(offerDelete == null) {
        		offerDelete = PlayerTextdraw.create(player, 247.333389f, 284.148437f, DealershipPlugin.getInstance().getLocalizedStringSet().get(player, "Textdraws.Delete"));
        		offerDelete.setLetterSize(0.449999f, 1.600000f);
        		offerDelete.setAlignment(TextDrawAlign.LEFT);
        		offerDelete.setColor(new Color(-1));
        		offerDelete.setShadowSize(0);
        		offerDelete.setOutlineSize(1);
        		offerDelete.setBackgroundColor(new Color(51));
        		offerDelete.setFont(TextDrawFont.FONT2);
        		offerDelete.setProportional(true);
        		offerDelete.setSelectable(true);
        	} if(offerCancel == null) {
                offerCancel = PlayerTextdraw.create(player, 323.666687f, 284.563018f, DealershipPlugin.getInstance().getLocalizedStringSet().get(player, "Textdraws.Cancel"));
                offerCancel.setLetterSize(0.400000f, 1.467259f);
                offerCancel.setAlignment(TextDrawAlign.LEFT);
                offerCancel.setColor(new Color(-1));
                offerCancel.setShadowSize(0);
                offerCancel.setOutlineSize(1);
                offerCancel.setBackgroundColor(new Color(51));
                offerCancel.setFont(TextDrawFont.FONT2);
                offerCancel.setProportional(true);
                offerCancel.setSelectable(true);
            }
        }
    }

    PlayerTextdraw getOfferCancel() {
        return offerCancel;
    }

    void setLastOffer(VehicleOffer offer) {
		this.lastOffer = offer;
	}
	
	VehicleOffer getLastOffer() {
		return lastOffer;
	}


    @Override
    protected void onInit() {

    }

    @Override
    protected void onDestroy() {

    }
}
