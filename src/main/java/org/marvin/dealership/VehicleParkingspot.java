package org.marvin.dealership;

import net.gtaun.shoebill.data.AngledLocation;

/**
 * Created by Marvin on 01.06.2014.
 */
class VehicleParkingspot {
    private AngledLocation location;
    private int databaseId;

    public VehicleParkingspot(AngledLocation location, int databaseId) {
        this.location = location;
        this.databaseId = databaseId;
    }

    AngledLocation getLocation() {
        return location;
    }

    int getDatabaseId() {
        return databaseId;
    }

}
