package es.tid.pce.computingEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.messages.PCEPReport;
import es.tid.pce.server.PCEServerParameters;
import es.tid.pce.server.delegation.DelegationManager;
import es.tid.pce.server.lspdb.ReportDB_Handler;
// import es.tid.pce.server.lspdb.ReportDB_Handler;
import es.tid.pce.server.lspdb.ReportDB_Simple;
import es.tid.pce.server.lspdb.SingleDomainLSPDB;

/**
 * The ReportDispatcher receives PCEPReport messages and decides what to do with:
 * Add information to the database, generate an error,...
 * @author jaume, the farruk
 *
 */

public class ReportDispatcher 
{
	/**
	 * Number of processing threads
	 */
	 private final int nThreads;
	 
	 /**
	  * Array of RequestProcessor Threads
	  */
	 private final ReportProcessorThread[] threads;
	 
	 private DelegationManager dm;
	 
	 /**
	  * Queue to add path computing requests.
	  * This queue is read by the request processor threads. 
	  */
	private LinkedBlockingQueue<ReportProcessTask> reportMessageQueue;

	private List<PCEPReport> reportsWithZeroLspId;  // Lista para almacenar los reportes con lspId == 0
	 

	
	/**
	  * The logger
	  */
	private Logger log;
	
			
	public ReportDispatcher(ReportDB_Handler lspDB, int nThreads,SingleDomainLSPDB singleDomainLSPDB)
	{
		log=LoggerFactory.getLogger("PCEServer");
		dm=new DelegationManager(singleDomainLSPDB);
		
		this.nThreads = nThreads;
	    reportMessageQueue = new LinkedBlockingQueue<ReportProcessTask>();
	    threads = new ReportProcessorThread[nThreads];
        for (int i=0; i<this.nThreads; i++) {
        	log.info("Starting Request Processor Thread");	        	
            threads[i] = new ReportProcessorThread(reportMessageQueue, lspDB,dm);
            threads[i].setPriority(Thread.MAX_PRIORITY);
            threads[i].start();
            
        }
	}
	
	public void dispatchReport(ReportProcessTask pcepReport)
	{	    
		reportMessageQueue.add(pcepReport);
		// reportsWithZeroLspId = new ArrayList<>();
		// reportsWithZeroLspId.add(pcepReport.getReportMessage());
    }

	// Método para obtener los reportes almacenados con lspId == 0
    public List<PCEPReport> getReportsWithZeroLspId() {
        return reportsWithZeroLspId;
    }

	public DelegationManager getDm() {
		return dm;
	}

	public void setDm(DelegationManager dm) {
		this.dm = dm;
	}
	

}
