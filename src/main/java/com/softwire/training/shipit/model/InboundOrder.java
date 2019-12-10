package com.softwire.training.shipit.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

public class InboundOrder implements RenderableAsXML
{
    private int warehouseId;
    private List<OrderSegment> segments;
    private Employee operationsManager;

    public InboundOrder() {
        segments = new ArrayList<OrderSegment>();
    }

    public InboundOrder(int warehouseId, List<OrderSegment> segments, Employee operationsManager)
    {
        this.warehouseId = warehouseId;
        this.segments = segments;
        this.operationsManager = operationsManager;
    }

    public void addOrderSegment(OrderSegment orderSegment){
        segments.add(orderSegment);
    }

    public int getWarehouseId()
    {
        return warehouseId;
    }

    public Employee getOperationsManager()
    {
        return operationsManager;
    }

    public List<OrderSegment> getSegments()
    {
        return segments;
    }

    public String renderXML()
    {
        StringBuilder renderedSegments = new StringBuilder();
        for (OrderSegment segment : segments)
        {
            renderedSegments.append(segment.renderXML());
        }
        return "<order>" +
                "<warehouseId>" + warehouseId + "</warehouseId>" +
                "<segments>" + renderedSegments.toString() + "</segments>" +
                "<operationsManager>" + operationsManager.renderXML() + "</operationsManager>" +
                "</order>";
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        InboundOrder inboundOrder = (InboundOrder) o;

        return new EqualsBuilder()
                .append(warehouseId, inboundOrder.warehouseId)
                .append(segments, inboundOrder.segments)
                .append(operationsManager, inboundOrder.operationsManager)
                .isEquals();
    }

    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                .append(warehouseId)
                .append(segments)
                .append(operationsManager)
                .toHashCode();
    }

    public String toString()
    {
        return new ToStringBuilder(this)
                .append("warehouseId", warehouseId)
                .append("segments", segments)
                .append("operationsManager", operationsManager)
                .toString();
    }
}
