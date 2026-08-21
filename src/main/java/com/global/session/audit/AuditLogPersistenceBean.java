package com.global.session.audit;

import com.global.entity.AuditLog;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class AuditLogPersistenceBean {

    @PersistenceContext(unitName = "globalTradePU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void persist(AuditLog log) {
        em.persist(log);
    }
}