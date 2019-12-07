package com.softwire.training.shipit.model.truck;

import com.softwire.training.shipit.exception.NotEnoughSpaceInTruckException;
import com.softwire.training.shipit.model.RenderableAsXML;

import java.util.ArrayList;
import java.util.List;

public class Truck implements RenderableAsXML {

    private static final int KG = 1000;
    private double totalWeightInGrams;
    private double availableSpaceInGrams;
    private List<OutboundOrderLine> outboundOrderLineList;

    public Truck() {
        availableSpaceInGrams = 2000000;
        totalWeightInGrams = 0;
        outboundOrderLineList = new ArrayList<OutboundOrderLine>();
    }

    public void addOrder(OutboundOrderLine currentOrder) {
        double totalWeightOfOrderInKg = currentOrder.getTotalWeightOfOrder();
        if ((availableSpaceInGrams - totalWeightOfOrderInKg) < 0) {
            throw new NotEnoughSpaceInTruckException("Available space: " + availableSpaceInGrams +
                    " | Total weight of Order: " + totalWeightOfOrderInKg);
        }

        totalWeightInGrams += totalWeightOfOrderInKg;
        availableSpaceInGrams -= totalWeightOfOrderInKg;
        outboundOrderLineList.add(currentOrder);
    }

    public double getAvailableSpaceInGrams() {
        return availableSpaceInGrams;
    }

    public double getTotalWeightInKG() {
        return totalWeightInGrams / KG;
    }

    public String renderXML() {
        StringBuilder renderedOrderLines = new StringBuilder();
        for (OutboundOrderLine orderLine : outboundOrderLineList) {
            renderedOrderLines.append(orderLine.renderXML());
        }

        return "<Truck>" +
                "<TotalWeight>" + getTotalWeightInKG() + " kg" + "</TotalWeight>" +
                "<OutboundOrderLines>" + renderedOrderLines.toString() + "</OutboundOrderLine>" +
                "</Truck>";
    }
}
