package com.softwire.training.shipit.dao;

import com.softwire.training.shipit.model.InboundOrderData;

import java.util.List;

public interface InboundOrderDataDAO {

    List<InboundOrderData> getInboundOrderByWarehouseId(Integer warehouseId);
}
