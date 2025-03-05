package es.tid.pce.computingEngine;

/**
 * Class made for managing report messages from PCCs. It only adds them to
 * the database
 * 
 * @author jimbo
 */

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.server.delegation.DelegationManager;
import es.tid.pce.server.lspdb.ReportDB_Handler;


public class ReportProcessorThread extends Thread {
	boolean running;

	LinkedBlockingQueue<ReportProcessTask> reportMessageQueue;

	ReportDB_Handler lspDB;
	// ReportDB_Simple lspDB;

	DelegationManager dm;

	Logger log;
	
	List<PCEPReport> reportsWithZeroLspId = new ArrayList<>();  // Lista para almacenar los reportes con lspId == 0


	public ReportProcessorThread(LinkedBlockingQueue<ReportProcessTask> reportMessageQueue, ReportDB_Handler lspDB,
			DelegationManager dm) {
		log = LoggerFactory.getLogger("PCEServer");
		running = true;
		this.lspDB = lspDB;
		this.reportMessageQueue = reportMessageQueue;
		this.dm = dm;
	}

	public void run() {
		ReportProcessTask reportTask;

		while (running) {
			try {
				reportTask = reportMessageQueue.take();
				effectivelyDispatch(reportTask);
			} catch (InterruptedException e) {
				log.warn("Interrupted Exception Captured in ReportProcessorThread");
				e.printStackTrace();
				break;
			}

		}
	}

	public void effectivelyDispatch(ReportProcessTask reportTask) {
		// FIXME: The sync might fail if is sent in multiple messages
		PCEPReport pcepReport = reportTask.getReportMessage();
		log.info("Received new report message: " + pcepReport.toString());
		int lspId = pcepReport.getStateReportList().get(0).getLsp().getLspId();
		log.info("whith ID :" + lspId);
		if (lspId != 0) {
			Inet4Address addres = pcepReport.getStateReportList().get(0).getLsp().getLspIdentifiers_tlv()
					.getTunnelSenderIPAddress();

			Boolean isSyncOver = false;

			log.info("Size LSP:" + pcepReport.getStateReportList().size());
			// isSyncOver =
			// lspDB.isPCCSyncOver(pcepReport.getStateReportList().get(0).getLSP().getLspIdentifiers_tlv().getTunnelSenderIPAddress());

			log.info("Package received from adress: " + pcepReport.getStateReportList().get(0).getLsp()
					.getLspIdentifiers_tlv().getTunnelSenderIPAddress());

			// FIXME: When the sync is over, delegation is permitted... NOT before
			if (!isSyncOver) {
				boolean syncFlag;
				int numLSPs = pcepReport.getStateReportList().size();
				for (int j = 0; j < numLSPs; j++) {
					syncFlag = pcepReport.getStateReportList().get(j).getLsp().isSyncFlag();
					if ((!syncFlag) && (pcepReport.getStateReportList().get(j).getLsp().getLspId() == 0)) {
						isSyncOver = true;
						log.info("Sync is over");
						// lspDB.setPCCSyncOver(addres);
					}
				}
			}

			lspDB.processReport(pcepReport);
			for (int i = 0; i < pcepReport.getStateReportList().size(); i++) {
				log.info("PROCESSING REPORT");
				dm.processReport(pcepReport.getStateReportList().get(i), reportTask.getOut());
			}

		} 
		else if (lspId == 0) {
		// Si el lspId es 0, procesamos igualmente  el reporte
		lspDB.processReport(pcepReport);
		
		
		}
	}
	// Método para obtener los reportes almacenados con lspId == 0
    public List<PCEPReport> getReportsWithZeroLspId() {
        return reportsWithZeroLspId;
    }
	
}
