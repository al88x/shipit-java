package com.softwire.training.shipit.dao.impl;

import com.softwire.training.shipit.dao.InboundOrderDataDAO;
import com.softwire.training.shipit.model.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class InboundOrderDataDAOImpl implements InboundOrderDataDAO {

    private static final ParameterizedRowMapper<InboundOrderData> MAPPER = new ParameterizedRowMapper<InboundOrderData>() {

        public InboundOrderData mapRow(ResultSet rs, int rowNum) throws SQLException {
            int inboundOrderQuantity = NumberUtils.max(
                    rs.getInt("l_th") * 3 - rs.getInt("hld"), rs.getInt("min_qt"));

            return new InboundOrderData(
                    rs.getString("gtin_cd"),
                    rs.getString("gtin_nm"),
                    inboundOrderQuantity,
                    new Company(
                    rs.getString("gcp_cd"),
                    rs.getString("gln_nm"),
                    rs.getString("gln_addr_02"),
                    rs.getString("gln_addr_03"),
                    rs.getString("gln_addr_04"),
                    rs.getString("gln_addr_postalcode"),
                    rs.getString("gln_addr_city"),
                    rs.getString("contact_tel"),
                    rs.getString("contact_mail")));
        }
    };

    private SimpleJdbcTemplate simpleJdbcTemplate;
    private JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) {
        simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<InboundOrderData> getInboundOrderByWarehouseId(Integer warehouseId) {
        String sqlQuery = "SELECT stock.p_id, stock.hld, gtin.gtin_cd, gtin_nm, gtin.l_th, gtin.ds, gtin.min_qt, gcp.GCP_CD, gcp.GLN_NM, gcp.GLN_ADDR_02, gcp.GLN_ADDR_03, gcp.GLN_ADDR_04, gcp.GLN_ADDR_POSTALCODE, gcp.GLN_ADDR_CITY, gcp.CONTACT_TEL, gcp.CONTACT_MAIL\n" +
                "FROM stock\n" +
                "INNER JOIN gtin\n" +
                "ON stock.p_id = gtin.p_id\n" +
                "INNER JOIN gcp \n" +
                "ON gtin.GCP_CD = gcp.GCP_CD\n" +
                "where stock.w_id = ?\n" +
                "and gtin.ds=0\n" +
                "and stock.hld < gtin.l_th;\n";
        return simpleJdbcTemplate.query(sqlQuery, MAPPER, warehouseId);
    }
}
