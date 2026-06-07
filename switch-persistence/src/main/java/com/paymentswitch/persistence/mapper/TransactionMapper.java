package com.paymentswitch.persistence.mapper;

import com.paymentswitch.persistence.entity.TransactionRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * MyBatis mapper for {@code transactions} table.
 */
public interface TransactionMapper {

    @Insert("INSERT INTO transactions " +
            "(message_id, mti, processing_code, pan, amount, currency_code, " +
            "stan, rrn, acquirer_id, issuer_id, response_code, status, created_at, updated_at) " +
            "VALUES " +
            "(#{messageId}, #{mti}, #{processingCode}, #{pan}, #{amount}, #{currencyCode}, " +
            "#{stan}, #{rrn}, #{acquirerId}, #{issuerId}, #{responseCode}, #{status}, " +
            "NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TransactionRecord record);

    @Select("SELECT * FROM transactions WHERE message_id = #{messageId}")
    @ResultMap("transactionResultMap")
    TransactionRecord findByMessageId(String messageId);

    @Select("SELECT * FROM transactions WHERE rrn = #{rrn} ORDER BY created_at DESC LIMIT 1")
    @ResultMap("transactionResultMap")
    TransactionRecord findByRrn(String rrn);

    @Update("UPDATE transactions " +
            "SET response_code = #{responseCode}, status = #{status}, updated_at = NOW() " +
            "WHERE message_id = #{messageId}")
    int updateStatus(@Param("messageId") String messageId,
                     @Param("responseCode") String responseCode,
                     @Param("status") String status);
}