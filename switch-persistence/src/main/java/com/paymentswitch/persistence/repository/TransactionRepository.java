package com.paymentswitch.persistence.repository;

import com.paymentswitch.common.model.SwitchMessage;
import com.paymentswitch.persistence.entity.TransactionRecord;
import com.paymentswitch.persistence.mapper.TransactionMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository facade for transaction persistence.
 * Opens and closes MyBatis sessions internally.
 */
public class TransactionRepository {

    private static final Logger log = LoggerFactory.getLogger(TransactionRepository.class);

    private final SqlSessionFactory sqlSessionFactory;

    public TransactionRepository(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * Persist an inbound switch message as a PENDING transaction.
     */
    public void savePending(SwitchMessage message) {
        TransactionRecord record = toRecord(message, "PENDING");
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getMapper(TransactionMapper.class).insert(record);
            log.debug("Saved PENDING transaction: {}", message.getMessageId());
        }
    }

    /**
     * Update transaction status after issuer response.
     */
    public void updateStatus(String messageId, String responseCode, String status) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getMapper(TransactionMapper.class).updateStatus(messageId, responseCode, status);
            log.debug("Updated transaction {} -> status={}, rc={}", messageId, status, responseCode);
        }
    }

    public TransactionRecord findByMessageId(String messageId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(TransactionMapper.class).findByMessageId(messageId);
        }
    }

    private TransactionRecord toRecord(SwitchMessage msg, String status) {
        TransactionRecord r = new TransactionRecord();
        r.setMessageId(msg.getMessageId());
        r.setMti(msg.getMessageType() != null ? msg.getMessageType().getMti() : null);
        r.setProcessingCode(msg.getProcessingCode() != null ? msg.getProcessingCode().getCode() : null);
        r.setPan(msg.getMaskedPan());
        r.setAmount(msg.getTransactionAmount());
        r.setCurrencyCode(msg.getCurrencyCode());
        r.setStan(msg.getSystemTraceAuditNumber());
        r.setRrn(msg.getRetrievalReferenceNumber());
        r.setAcquirerId(msg.getAcquirerId());
        r.setIssuerId(msg.getIssuerId());
        r.setStatus(status);
        return r;
    }
}
