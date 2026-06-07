package com.paymentswitch.persistence.mapper;

import com.paymentswitch.routing.BinTableEntry;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * MyBatis mapper for the {@code bin_table} routing table.
 */
public interface BinTableMapper {

    @Select("SELECT bin_range_start, bin_range_end, issuer_id, issuer_name, active " +
            "FROM bin_table " +
            "WHERE #{bin} BETWEEN bin_range_start AND bin_range_end " +
            "AND active = TRUE " +
            "LIMIT 1")
    @Results({
        @Result(column = "bin_range_start", property = "binRangeStart"),
        @Result(column = "bin_range_end",   property = "binRangeEnd"),
        @Result(column = "issuer_id",       property = "issuerId"),
        @Result(column = "issuer_name",     property = "issuerName"),
        @Result(column = "active",          property = "active")
    })
    BinTableEntry findByBin(String bin);

    @Select("SELECT bin_range_start, bin_range_end, issuer_id, issuer_name, active " +
            "FROM bin_table WHERE active = TRUE ORDER BY bin_range_start")
    List<BinTableEntry> findAllActive();
}