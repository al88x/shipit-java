package com.softwire.training.shipit.model.truck;

import com.softwire.training.shipit.model.RenderableAsXML;

public class OutboundOrderLine implements RenderableAsXML {

    private String gtin;
    private String name;
    private int quantity;
    private double weightOfItem;

    public OutboundOrderLine(String gtin, String name, int quantity, double weightOfItem) {
        this.gtin = gtin;
        this.name = name;
        this.quantity = quantity;
        this.weightOfItem = weightOfItem;
    }


    public String getGtin() {
        return gtin;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getWeightOfItem() {
        return weightOfItem;
    }

    public double getTotalWeightOfOrder() {
        return quantity * weightOfItem;
    }

    public double getTotalWeightOfOrderInKG(){
        return (quantity*weightOfItem)/1000;
    }

    public OutboundOrderLine splitOrder(double availableSpaceInTruck) {
        int quantityToHoldForThisTruck = (int) (availableSpaceInTruck / weightOfItem);
        OutboundOrderLine partOrderForNextTruck = new OutboundOrderLine(gtin, name, quantity - quantityToHoldForThisTruck, weightOfItem);
        this.quantity = quantityToHoldForThisTruck;
        return partOrderForNextTruck;
    }

    public String renderXML() {
        return "<OrderLine>" +
                "<gtin>" + gtin + "</gtin>" +
                "<name>" + name + "</name>" +
                "<quantity>" + quantity + "</quantity>" +
                "<totalWeightOfOrder>" + getTotalWeightOfOrderInKG() + " kg" + "</totalWeightOfOrder>" +
                "</OrderLine>";
    }
}
