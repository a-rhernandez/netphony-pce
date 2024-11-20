package es.tid.pce.server;

import java.io.DataOutputStream;

/** 
 * PCE Domain Server.
 * 
 * It is the main class of a PCE that is responsible of a domain.
 * 
 * By default listens on port 4189
 * 
 * @author Oscar, Eduardo
 */

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.computingEngine.ReportDispatcher;
import es.tid.pce.computingEngine.RequestDispatcher;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.subobjects.SREROSubobject;
import es.tid.pce.pcepsession.PCEPSessionsInformation;

import es.tid.pce.server.lspdb.ReportDB_Handler;
import es.tid.pce.server.lspdb.SingleDomainLSPDB;
import es.tid.pce.server.management.PCEManagementSever;
import es.tid.pce.utils.StringToPCEP;
import io.grpc.stub.StreamObserver;

import src.main.proto.*;
import src.main.proto.GrpcService.LSPdb_Request;
import src.main.proto.GrpcService.LSPdb_Response;
import src.main.proto.GrpcService.Session_Request;
import src.main.proto.GrpcService.Session_Response;
import src.main.proto.GrpcService.commandRequest;
import src.main.proto.GrpcService.commandResponse;
import src.main.proto.pceServiceGrpc.pceServiceImplBase;

public class DomainPCEServer extends pceServiceImplBase implements Runnable{

	/**
	 * log: main logger for the PCE
	 */
	public static final Logger log =LoggerFactory.getLogger("PCEServer");
	
	/**
	 * PCEPSessionsInformation contains information about all the sessions of the PCE 
	 */
	private PCEPSessionsInformation pcepSessionsInformation;
	

	

	private static ReportDB_Handler rptdb;
	
	/**
	 * True if the PCE is listening. False otherwise.
	 */
	private static boolean listening=false;

	/**
	 * params: Parameters of the PCE read from the configuration File.
	 */
	PCEServerParameters params;

	/**
	 * Socket where the PCE is listening;
	 */
	ServerSocket serverSocket;
	/**
	 * Management server of PCE
	 */
	PCEManagementSever pms;

	// LSP Database FIXME: Merge later with Report_DB. Better a SINGLE LSP Database.
	SingleDomainLSPDB singleDomainLSPDB;

	RequestDispatcher PCCRequestDispatcher;
	
	
			
	//Used to initiate paths from NBI or locally. To merge later with the previous one.
	
	//Manager to initiate Paths
	IniPCCManager iniManager=null;
	
	ReportDispatcher PCCReportDispatcher = null;

	private commandRequest request;
	private commandRequest response;
	private LSPdb_Request lsp_request;
	private LSPdb_Response lsp_response;
	private Session_Request session_request;
	private Session_Response sessions_response;

	
	/**
	 * First of all, it is needed to configure the PCE
	 * @param configFile
	 */
	public void configure (String configFile){
		if (configFile!=null){

			params=new PCEServerParameters(configFile);
		}else {
			params=new PCEServerParameters();
		}
		params.initialize();

		//Initialize loggers
		//FIXME: check loggers

		//		try {
		//			
		//			
		//			Logger log2=LoggerFactory.getLogger("PCEPParser");
		//			Logger log3=LoggerFactory.getLogger("OSPFParser");
		//			Logger log4=LoggerFactory.getLogger("TEDBParser");
		//			//Logger log6=LoggerFactory.getLogger("BGPParser");
		//			log5=LoggerFactory.getLogger("OpMultiLayer");
		////			log.addHandler(fh);
		////			log2.addHandler(fh2);
		////			log3.addHandler(fh3);
		////			log4.addHandler(fh4);
		//			//logTimePCE.addHandler(fh5);
		////			fh4.setFormatter(new SimpleFormatter());
		////			log5.addHandler(fh5);
		////			log5.setLevel(Level.ALL);
		////			if (params.isSetTraces() == false){		    	
		////				log.setLevel(Level.SEVERE);
		////				log2.setLevel(Level.SEVERE);	
		////				log3.setLevel(Level.SEVERE);
		////				log4.setLevel(Level.SEVERE);
		////				//log6.setLevel(Level.SEVERE);
		////				//log5.setLevel(Level.SEVERE);
		////			}
		////
		////			else{
		////				log.setLevel(Level.ALL);
		////				log2.setLevel(Level.ALL);
		////				log3.setLevel(Level.ALL);
		////				log4.setLevel(Level.ALL);
		////				//log6.setLevel(Level.ALL);
		////			}
		//
		//		} catch (Exception e1) {
		//			e1.printStackTrace();
		//			System.exit(1);
		//		}

		log.info("Configuration file: " + configFile);
		log.info("Inizializing Netphony Domain PCE Server!!");

	}

	@Override
	public void update(commandRequest request, StreamObserver<commandResponse> responseObserver) {
	    this.request = request;
	    String command = request.getCommand();
	    System.out.println("Comando recibido: " + command);

	    commandResponse response;

	    try {
	        if (command.startsWith("update lsp")) {
	            update_command(command.substring(11));
	        } else if (command.startsWith("terminate lsp")) {
	            terminate_command(command.substring(14));
	        } else if (command.startsWith("initiate lsp")) {
	            initiate_command(command.substring(13));
	        } else {
	            String errorMessage = "Invalid request: Unexistent method";
	            System.out.println(errorMessage);
	            throw new IllegalArgumentException(errorMessage);
	        }

	        // Build a success response
	        response = buildSuccessResponse();
	    } catch (Exception e) {
	        e.printStackTrace();
	        // Build an error response with the exception message
	        response = buildErrorResponse(e.getMessage());
	    }

	    // Use responseObserver to send a single response back
	    responseObserver.onNext(response);

	    // When you are done, you must call onCompleted.
	    try {
	        responseObserver.onCompleted();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	private commandResponse buildSuccessResponse() {
	    return commandResponse.newBuilder()
	            .setSuccess(true)
	            .build();
	}

	private commandResponse buildErrorResponse(String errorMessage) {
	    return commandResponse.newBuilder()
	            .setSuccess(false)
	            .setErrorMessage(errorMessage)
	            .build();
	}
	
	@Override
	public void getLSPdb(LSPdb_Request lspreq , StreamObserver<LSPdb_Response>responseObserver) {
		this.lsp_request = lspreq;

		System.out.println("Comando GETLSPDB recibido: "+lspreq);
		String lspdb_data = null;
		
		try {
			lspdb_data = this.getSingleDomainLSPDB().toString();
		}catch (Exception e) {
			// Handle exceptions or errors and send an appropriate response
            responseObserver.onError(e);
		}
			
		LSPdb_Response lspres = LSPdb_Response.newBuilder()
			      .setLSPdbData(lspdb_data)
			      .build();
		
		// Use responseObserver to send a single response back
	    responseObserver.onNext(lspres);
	    
		// When you are done, you must call onCompleted.
	    try {
	        responseObserver.onCompleted();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	@Override
	public void getSessionsInfo(Session_Request sessreq , StreamObserver<Session_Response>responseObserver) {
		this.session_request = sessreq;

		System.out.println("Comando SessionRequest recibido: "+sessreq);
		String session_data = null;
		
		try {
			session_data = this.getPcepSessionsInformation().toString();
		}catch (Exception e) {
			// Handle exceptions or errors and send an appropriate response
            responseObserver.onError(e);
		}
		
		Session_Response sessresp = Session_Response.newBuilder()
				.setSessionData(session_data)
				.build();

		// Use responseObserver to send a single response back	    
	    responseObserver.onNext(sessresp);
	    
		// When you are done, you must call onCompleted.
	    try {
	        responseObserver.onCompleted();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	private void initiate_command(String command) {
		int offset=0;
		
		StringTokenizer st = new StringTokenizer(command," ");
		String name=st.nextToken();
		offset+=name.length()+1;
		String pcc= st.nextToken();

		//Next 2 Items are the source and destination
		Inet4Address ip_pcc=null;
		try {
			ip_pcc = (Inet4Address)Inet4Address.getByName(pcc);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		offset+=pcc.length();
		EndPointsIPv4 ep=new EndPointsIPv4();
		String src_ip= st.nextToken();
		
		Inet4Address ipp;
		try {
			ipp = (Inet4Address)Inet4Address.getByName(src_ip);
			((EndPointsIPv4) ep).setSourceIP(ipp);								
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		String dst_ip= st.nextToken();
		try {
			ipp = (Inet4Address)Inet4Address.getByName(dst_ip);
			((EndPointsIPv4) ep).setDestIP(ipp);								
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		offset+=src_ip.length()+1+dst_ip.length()+1;
		ExplicitRouteObject ero=StringToPCEP.stringToExplicitRouteObject(command.substring(offset));
		
		int signalingType = 0;
		if(ero.getEROSubobjectList().getFirst() instanceof SREROSubobject) {
			//SR Up
			signalingType = 1;
		}
		this.getIniManager().initiateLSP(ep,ero,ip_pcc,signalingType,name);
	}

	private void terminate_command(String command) {
		StringTokenizer st = new StringTokenizer(command," ");
		String pcc= st.nextToken();
		
		//Next 2 Items are the source and destination
		Inet4Address ip_pcc=null;
		try {
			ip_pcc = (Inet4Address)Inet4Address.getByName(pcc);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		String number= st.nextToken();
		int int_lsp_number = Integer.parseInt(number);
		this.getIniManager().terminateLSP(int_lsp_number,ip_pcc);
	}
	
	private void update_command(String command) throws UnknownHostException {
		int offset=0;
		StringTokenizer st = new StringTokenizer(command," ");
		String id_lsp_s= st.nextToken();
		offset+=id_lsp_s.length()+1;
		int id_lsp=Integer.parseInt(id_lsp_s);
		// oneSession.sendPCEPMessage(m_update);
		
		//DataOutputStream out= oneSession.get(0).getOut();
		DataOutputStream out =null;
		Set<Long> keys = this.getPcepSessionsInformation().sessionList.keySet();
        for(Long key: keys){
            System.out.println("Value of "+key+" is: "+ this.getPcepSessionsInformation().sessionList.get(key));
            out=this.getPcepSessionsInformation().sessionList.get(key).getOut();
        }
		
		if (st.hasMoreTokens()) {
			ExplicitRouteObject ero=StringToPCEP.stringToExplicitRouteObject(command.substring(offset));
			this.getPCCReportDispatcher().getDm().updateDelegatedPath(id_lsp,false, ero, out);
		}else {
			this.getPCCReportDispatcher().getDm().updateDelegatedPath(id_lsp,true, null, out);
		}
	}

	public void run(){

		//Create all the Elements of the PCE Server

		// 1) PCEPSessionsInformation contains information about all the sessions of the PCE (dead or alive)
		pcepSessionsInformation = new PCEPSessionsInformation();
		// set all information about THIS PCE (not the peer)
		pcepSessionsInformation.setStateful(params.isStateful()); // If the PCE is stateful (it can receive delegations) 
		pcepSessionsInformation.setStatefulDFlag(params.isStatefulDFlag()); // If the PCE can delete
		pcepSessionsInformation.setStatefulSFlag(params.isStatefulSFlag());
		pcepSessionsInformation.setStatefulTFlag(params.isStatefulTFlag());
		pcepSessionsInformation.setActive(params.isActive()); // If the PCE is active (it CAN instantiate)
		pcepSessionsInformation.setSRCapable(params.isSRCapable());
		pcepSessionsInformation.setMSD(params.getMSD());

		// Used to initiate paths from Parent PCE requests
		SingleDomainInitiateDispatcher iniDispatcher=null;
		
		
		//Log if the PCE is SR capable or NOT
		if (params.isSRCapable()) {
			log.info("PCEServer: PCE is SR capable with MSD="+pcepSessionsInformation.getMSD());
		}

		if (params.isStateful())
		{
			//FIXME: By now, U flag is ALWAYS TRUE.
			log.info("Stateful PCE with T="+params.isStatefulTFlag()+" D="+params.isStatefulDFlag()+" S="+params.isStatefulSFlag()+" U=true "+"I="+params.isActive());
			singleDomainLSPDB=new SingleDomainLSPDB();
			if(params.getDbType().equals("_"))
			{
				singleDomainLSPDB.setExportDb(false);
			}
			iniManager= new IniPCCManager();
			iniDispatcher = new SingleDomainInitiateDispatcher(singleDomainLSPDB,iniManager);
		}


		/***/

		
///==		RequestDispatcher PCCRequestDispatcherChild = null;
		
		//The Request Dispatcher, needed to dispatch the requests coming from the PCCs
		log.info("Initializing Request Dispatcher");
		
//==		CollaborationPCESessionManager	collaborationPCESessionManager=null;
		
//		if ((params.getParentPCEAddress()!=null)){			
//			if (params.isCollaborativePCEs()){//STRONGEST: Collaborative PCEs						
//				collaborationPCESessionManager = new CollaborationPCESessionManager();			
//				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,pcm.getChildPCERequestManager(),params.isAnalyzeRequestTime(),collaborationPCESessionManager);
//			}
//			else
//				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,pcm.getChildPCERequestManager(),params.isAnalyzeRequestTime());
//		}else {
//			if (params.isMultilayer()== true){
//				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,null,params.isAnalyzeRequestTime(),params.isUseMaxReqTime(), reservationManager, OPcounter, params.isMultilayer());
//			}
//			else if (params.isCollaborativePCEs()){//STRONGEST: Collabotarive PCEs	
//				collaborationPCESessionManager = new CollaborationPCESessionManager();	
//				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,null,params.isAnalyzeRequestTime(),params.isUseMaxReqTime(), reservationManager,collaborationPCESessionManager);
//			}else
//				PCCRequestDispatcher=new  RequestDispatcher(params.getPCCRequestsProcessors(),ted,null,params.isAnalyzeRequestTime(),params.isUseMaxReqTime(), reservationManager);
//		}

		//Notification dispatcher
		NotificationDispatcher nd=new NotificationDispatcher(); // Nueva version sin reservattion Manager

		if(params.algorithmRuleList.size()==0){

			log.warn("There are no registered algorithms besides the default");
		}

		// When there is a parent PCE, connect to the parent PCE
		
		//Start the management server
		pms= new PCEManagementSever(this);	
		pms.start(); 

		//STRONGEST: Collaborative PCEs
		
		listening = true;
		try {
			
			log.info("Listening on port: "+params.getPCEServerPort());

			// Local PCE address for multiple network interfaces in a single computer

			log.info("Listening on address: "+params.getLocalPceAddress());
			serverSocket = new ServerSocket(params.getPCEServerPort(),0,(Inet4Address) InetAddress.getByName(params.getLocalPceAddress()));
		} catch (IOException e) {
			System.err.println("Could not listen on port: "+params.getPCEServerPort());
			System.exit(-1);
		}



			// This parameter tells the dispatcher that sync will be avoided.
			// In better future times sync should be implemented
		try{

			
			if (pcepSessionsInformation.isStateful())
			{
				log.info("redis: "+params.getDbType() + " "+params.getDbName());
				if (params.getDbType().equals("redis") && params.getDbName().length() > 0)
				{
					log.info("redis: redis db with id: "+ params.getDbName());
					rptdb = new ReportDB_Handler(params.getDbName(),"localhost");	
					rptdb.fillFromDB();
				}
				else
				{
					rptdb = new ReportDB_Handler();
				}
				params.setLspDB(rptdb);	
				log.info("Creando dispatchers para el LSP DB");
				//FIXME: Por ahora, evito concurrencia hasta solventar el tema de los state sync
				PCCReportDispatcher = new ReportDispatcher( rptdb, 1,singleDomainLSPDB);
			}


			
			while (listening) {
				new DomainPCESession(serverSocket.accept(),params,PCCRequestDispatcher,nd,pcepSessionsInformation,PCCReportDispatcher,iniDispatcher).start();
//				null,ted,pcm.getChildPCERequestManager()).start(
//				if (params.isCollaborativePCEs())
//					//SIN TED NI RESERVATION MANAGER
//					new DomainPCESession(serverSocket.accept(),params,PCCRequestDispatcher,nd,collaborationPCESessionManager,pcepSessionsInformation,PCCReportDispatcher).start();
//				else {
					//new DomainPCESession(serverSocket.accept(),params,PCCRequestDispatcher,nd,pcepSessionsInformation,PCCReportDispatcher,iniDispatcher).start();
				//}
			}
			serverSocket.close();

		} catch (SocketException e) {
			if (listening==false){
				log.info("Socket closed due to controlled close");
			}else {
				log.error("Problem with the socket, exiting");
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	



	public void stopServer(){
		pms.stopServer();
		listening=false;
		if (serverSocket!=null){
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public PCEPSessionsInformation getPcepSessionsInformation() {
		return pcepSessionsInformation;
	}

	public void setPcepSessionsInformation(PCEPSessionsInformation pcepSessionsInformation) {
		this.pcepSessionsInformation = pcepSessionsInformation;
	}

	

	public static ReportDB_Handler getRptdb() {
		return rptdb;
	}

	public static void setRptdb(ReportDB_Handler rptdb) {
		DomainPCEServer.rptdb = rptdb;
	}

	public static boolean isListening() {
		return listening;
	}

	public static void setListening(boolean listening) {
		DomainPCEServer.listening = listening;
	}

	public PCEServerParameters getParams() {
		return params;
	}

	public void setParams(PCEServerParameters params) {
		this.params = params;
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public PCEManagementSever getPms() {
		return pms;
	}

	public void setPms(PCEManagementSever pms) {
		this.pms = pms;
	}

	public SingleDomainLSPDB getSingleDomainLSPDB() {
		return singleDomainLSPDB;
	}

	public void setSingleDomainLSPDB(SingleDomainLSPDB singleDomainLSPDB) {
		this.singleDomainLSPDB = singleDomainLSPDB;
	}

	public static Logger getLog() {
		return log;
	}

	public RequestDispatcher getPCCRequestDispatcher() {
		return PCCRequestDispatcher;
	}

	public void setPCCRequestDispatcher(RequestDispatcher pCCRequestDispatcher) {
		PCCRequestDispatcher = pCCRequestDispatcher;
	}

	

	public IniPCCManager getIniManager() {
		return iniManager;
	}

	public void setIniManager(IniPCCManager iniManager) {
		this.iniManager = iniManager;
	}

	public ReportDispatcher getPCCReportDispatcher() {
		return PCCReportDispatcher;
	}

	public void setPCCReportDispatcher(ReportDispatcher pCCReportDispatcher) {
		PCCReportDispatcher = pCCReportDispatcher;
	}

	
}
