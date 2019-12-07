package com.softwire.training.shipit.model.truck;

import com.softwire.training.shipit.exception.NotEnoughTrucks;
import com.softwire.training.shipit.model.RenderableAsXML;

import java.util.ArrayList;
import java.util.List;

public class TruckManifest implements RenderableAsXML {

    private int warehouseId;
    private List<Truck> truckList;
    private List<OutboundOrderLine> outboundOrderList;

    public TruckManifest() {
        this.truckList = new ArrayList<Truck>();
        this.outboundOrderList = new ArrayList<OutboundOrderLine>();
    }

    public void buildManifest() {
        if (outboundOrderList.size() < 1) {
            return;
        }
        List<OutboundOrderLine> tempOrderList = outboundOrderList;
        createEmptyTrucksForOrder();
        while (tempOrderList.size() > 0) {
            OutboundOrderLine currentOrder = getLargestOrder(tempOrderList);
            boolean isAddedToManifest = addOrderToTrucks(currentOrder);
            if (isAddedToManifest) {
                tempOrderList.remove(currentOrder);
            }
        }
    }

    private OutboundOrderLine getLargestOrder(List<OutboundOrderLine> tempOrderList) {
        OutboundOrderLine largestOrder = null;
        for(OutboundOrderLine order : tempOrderList){
            if(largestOrder == null){
                largestOrder = order;
                continue;
            }
            if(order.getTotalWeightOfOrder() > largestOrder.getTotalWeightOfOrder()){
                largestOrder = order;
            }
        }
        return largestOrder;
    }

    private void createEmptyTrucksForOrder() {
        for (int i = 0; i < calculateNumberOfTrucksToFulfillOrder(outboundOrderList); i++) {
            truckList.add(new Truck());
        }
    }

    private boolean addOrderToTrucks(OutboundOrderLine currentOrder) {
        Truck truck = getTruckWithMostSpaceAvailable(truckList);
        if(truck == null){
            throw new NotEnoughTrucks("Not enough trucks to fulfill order");
        }
        if (truckHasEnoughSpaceForFullOrder(truck, currentOrder)) {
            truck.addOrder(currentOrder);
            return true;
        } else {
            OutboundOrderLine partOrderForAnotherTruck = currentOrder.splitOrder(truck.getAvailableSpaceInGrams());
            truck.addOrder(currentOrder);
            return addOrderToTrucks(partOrderForAnotherTruck);
        }
    }

    private boolean truckHasEnoughSpaceForFullOrder(Truck truck, OutboundOrderLine currentOrder) {
        return truck.getAvailableSpaceInGrams() > currentOrder.getTotalWeightOfOrder();
    }

    private Truck getTruckWithMostSpaceAvailable(List<Truck> truckList) {
        Truck truckWithMostSpace = null;
        for (Truck truck : truckList){
            if(truckWithMostSpace == null){
                truckWithMostSpace = truck;
                continue;
            }
            if(truck.getAvailableSpaceInGrams() > truckWithMostSpace.getAvailableSpaceInGrams()){
                truckWithMostSpace = truck;
            }
        }
        return truckWithMostSpace;
    }


    private double calculateNumberOfTrucksToFulfillOrder(List<OutboundOrderLine> outboundOrderList) {
        double totalWeight = 0;
        for (OutboundOrderLine order : outboundOrderList) {
            totalWeight += (order.getTotalWeightOfOrder());
        }
        return Math.ceil(totalWeight / 2000000);
    }

    public void addOrder(OutboundOrderLine outboundOrderLine) {
        this.outboundOrderList.add(outboundOrderLine);
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(int warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String renderXML() {
        StringBuilder renderedTruckList = new StringBuilder();
        for(Truck truck: truckList){
            renderedTruckList.append(truck.renderXML());
        }

        return "<TruckManifest>" +
                "<warehouseId>" + warehouseId + "</warehouseId>" +
                "<NumberOfTrucks>" + truckList.size() + "</NumberOfTrucks>" +
                "<Trucks>" + renderedTruckList.toString() + "</Trucks>" +
                "</TruckManifest>";
    }
}
