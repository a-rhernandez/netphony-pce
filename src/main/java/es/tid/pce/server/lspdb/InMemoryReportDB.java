package es.tid.pce.server.lspdb;


import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import es.tid.pce.pcep.constructs.StateReport;

public class InMemoryReportDB {
    private final Map<Inet4Address, List<StateReport>> reportStorage;

    public InMemoryReportDB() {
        this.reportStorage = new ConcurrentHashMap<>();
    }

    public synchronized void addReport(Inet4Address senderIP, StateReport report) {
        reportStorage.computeIfAbsent(senderIP, k -> new ArrayList<>()).add(report);
    }

    public synchronized void removeReport(Inet4Address senderIP, StateReport report) {
        List<StateReport> reports = reportStorage.get(senderIP);
        if (reports != null) {
            reports.remove(report);
            if (reports.isEmpty()) {
                reportStorage.remove(senderIP);
            }
        }
    }

    public List<StateReport> getReports(Inet4Address senderIP) {
        return reportStorage.getOrDefault(senderIP, Collections.emptyList());
    }

    public synchronized void clear() {
        reportStorage.clear();
    }
}