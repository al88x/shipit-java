package com.softwire.training.shipit.model;

public class InboundOrderData {
    private String gtinCode;
    private String gtinName;
    private int inboundOrderLineQuantity;
    private Company company;


    public InboundOrderData(String gtinCode, String gtinName, int inboundOrderLineQuantity, Company company) {

        this.gtinCode = gtinCode;
        this.gtinName = gtinName;
        this.inboundOrderLineQuantity = inboundOrderLineQuantity;
        this.company = company;
    }

    public String getGtinCode() {
        return gtinCode;
    }

    public String getGtinName() {
        return gtinName;
    }

    public int getInboundOrderLineQuantity() {
        return inboundOrderLineQuantity;
    }

    public Company getCompany() {
        return company;
    }
}
