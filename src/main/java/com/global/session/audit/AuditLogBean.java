package com.global.session.audit;

import com.global.entity.AuditLog;
import com.global.entity.TimerLog;
import com.global.interceptor.PerformanceMonitored;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;

@Stateless
@PerformanceMonitored
public class AuditLogBean {

    @PersistenceContext(unitName = "globalTradePU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<AuditLog> findRecentAuditLogs(int hours) {

        LocalDateTime since =
                LocalDateTime.now().minusHours(hours);

        return em.createNamedQuery(
                        "AuditLog.findRecent",
                        AuditLog.class
                )
                .setParameter("since", since)
                .setMaxResults(100)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<AuditLog> findAllAuditLogs() {

        return em.createNamedQuery(
                        "AuditLog.findAll",
                        AuditLog.class
                )
                .setMaxResults(200)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<TimerLog> findRecentTimerLogs(int hours) {

        LocalDateTime since =
                LocalDateTime.now().minusHours(hours);

        return em.createNamedQuery(
                        "TimerLog.findRecent",
                        TimerLog.class
                )
                .setParameter("since", since)
                .setMaxResults(100)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<TimerLog> findAllTimerLogs() {

        return em.createNamedQuery(
                        "TimerLog.findAll",
                        TimerLog.class
                )
                .setMaxResults(200)
                .getResultList();
    }
}