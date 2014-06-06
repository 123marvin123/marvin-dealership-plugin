package org.marvin.dealership;

import net.gtaun.shoebill.object.Vehicle;

/**
 * Created by Marvin on 03.06.2014.
 */
public class TestDriver {
    private Vehicle vehicle;
    private VehicleProvider provider;

    TestDriver(Vehicle vehicle, VehicleProvider provider) {
        this.vehicle = vehicle;
        this.provider = provider;
    }

    Vehicle getVehicle() {
        return vehicle;
    }

    VehicleProvider getProvider() {
        return provider;
    }
}
