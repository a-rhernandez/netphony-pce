package tid.pce.computingEngine.algorithms.wson;

import java.net.Inet4Address;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.ComputingAlgorithmPreComputation;
import tid.pce.pcep.constructs.EndPoint;
import tid.pce.pcep.constructs.EndPointAndRestrictions;
import tid.pce.pcep.constructs.P2MPEndpoints;
import tid.pce.pcep.constructs.P2PEndpoints;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.messages.PCEPResponse;
import tid.pce.pcep.objects.EndPoints;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.GeneralizedEndPoints;
import tid.pce.pcep.objects.Metric;
import tid.pce.pcep.objects.Monitoring;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.TEDB;
import tid.rsvp.RSVPProtocolViolationException;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabelValues;
import tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;

/**
 * Shortest Path Routing + First Fit Wavelength Assignement WSON Algorithm.
 * 
 * First performs Dijskstra Shortest Path for the routing
 * and then it does the Wavelegth assigment
 * 
 * If there is no free wavelegth in this shortest path, 
 * noPath is returned
 *  
 * @author ogondio
 *
 */
public class SP_FF_RWA_Algorithm implements ComputingAlgorithm{
	private SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph;
	private Logger log=Logger.getLogger("PCEServer");
	private ComputingRequest pathReq;
	
	private ReservationManager reservationManager;
	
	private SP_FF_RWA_AlgorithmPreComputation preComp;
	

	public SP_FF_RWA_Algorithm(ComputingRequest pathReq,DomainTEDB ted ,ReservationManager reservationManager){
		this.networkGraph= ((SimpleTEDB)ted).getDuplicatedNetworkGraph();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
	}

	public PCEPResponse call(){
		long tiempoini =System.nanoTime();
		PCEPResponse m_resp=new PCEPResponse();
		Request req=pathReq.getRequestList().get(0);
		long reqId=req.getRequestParameters().getRequestID();
		log.info("SP_FF_RWA: "+reqId);

		//Start creating the response
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);


		//esto hay que cambiarlo para poder leer del GENERALIZED END POINTS
		//if (getObjectType(req.getEndPoints()))
		EndPoints  EP= req.getEndPoints();	
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PEndpoints p2pep= gep.getP2PEndpoints();
				EndPoint sourceep=p2pep.getSourceEndPoint();
				EndPoint destep=p2pep.getDestinationEndPoint();
				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
				dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;
			}
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
				P2MPEndpoints p2mpep= gep.getP2MPEndpoints();
				EndPointAndRestrictions epandrest=p2mpep.getEndPointAndRestrictions();
				EndPoint sourceep=epandrest.getEndPoint();
				source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
				int cont=0;
				while (cont<=p2mpep.getEndPointAndRestrictionsList().size()){ //esto est� mal
					epandrest=p2mpep.getEndPointAndRestrictionsList().get(cont);
					EndPoint destep=epandrest.getEndPoint();
					source_router_id_addr=sourceep.getEndPointIPv4TLV().IPv4address;
					dest_router_id_addr=destep.getEndPointIPv4TLV().IPv4address;

				}
			}
		}
		//aqu� acaba lo que he a�adido

		//Source node
		log.info("Source: "+source_router_id_addr);
		//Destination node
		log.info("Destination: "+dest_router_id_addr);
		//Check if we have source and destination in our TED
		if (!((networkGraph.containsVertex(source_router_id_addr))&&(networkGraph.containsVertex(dest_router_id_addr)))){
			log.warning("Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((networkGraph.containsVertex(source_router_id_addr)))){
				log.finest("Unknown source");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((networkGraph.containsVertex(dest_router_id_addr)))){
				log.finest("Unknown destination");
				noPathTLV.setUnknownDestination(true);	
			}
			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		}
		//Computing path
		log.info("Computing SP Routing");		
		DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (networkGraph, source_router_id_addr, dest_router_id_addr);
		GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();

		log.info("Performing FF WA");

		List<IntraDomainEdge> edge_list=gp.getEdgeList();
		
		int lambda=preComp.getFirstFitBBDD().queryLambdaFromNodes(edge_list);
		


	int i;
//	,j;
//		int num_bytes=edge_list.get(0).getNum_bytes();
//		byte[] wavelengths=new byte[num_bytes];
//		int lambda=-1;
//		System.arraycopy(edge_list.get(0).getWavelengths(), 0, wavelengths, 0, num_bytes); 
//		for (i=1;i<edge_list.size();i++){
//			for (j=0;j<num_bytes;++j){
//				wavelengths[j]=(byte) (wavelengths[j]&edge_list.get(i).getWavelengths()[j]);	
//			}			
//		}
//		for (i=0;i<edge_list.get(0).getNum_wavelengths();++i){
//			if (((wavelengths[i/8]<<i)&0x80)==0x00){
//				//If 0--> lambda free
//				lambda=i;
//				log.info("First free lambda= "+lambda);
//				break;
//			}else {
//				log.info("Lambda "+lambda+" ocuupied");
//			}
//		}
		
		
		log.finest("Creating response");

		m_resp.addResponse(response);
		Path path=new Path();
		ExplicitRouteObject ero= new ExplicitRouteObject();
		
		
		for (i=0;i<edge_list.size();i++){
			//Version UnnumberIfIDEROSubobject
			//UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
			//Version Numbered 
			//Version IPv4 Prefix 
			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			//eroso.setRouterID(edge_list.get(i).getSource());
			//eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
			eroso.setIpv4address((Inet4Address)edge_list.get(i).getSource());
			eroso.setPrefix(32); 
			eroso.setLoosehop(false);
			ero.addEROSubobject(eroso);
			GeneralizedLabelEROSubobject genLabel= new GeneralizedLabelEROSubobject();
			ero.addEROSubobject(genLabel);
			//ITU-T Format
//			DWDMWavelengthLabel WDMlabel=new DWDMWavelengthLabel();
//			WDMlabel.setGrid(DWDMWavelengthLabelValues.ITU_T_DWDM_GRID);
//			WDMlabel.setChannelSpacing(DWDMWavelengthLabelValues.DWDM_CHANNEL_SPACING_50_GHZ);
//			//FIXME: CUIDADO, HEMOS PUESTO LA LAMBDA DIRECTAMENTE
//			WDMlabel.setN(lambda);
//			WDMlabel.setIdentifier(0);
//			try {
//				WDMlabel.encode();
//			} catch (RSVPProtocolViolationException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			genLabel.setLabel(WDMlabel.getBytes());

		//Lambda tipo Victor
			byte[] WDMlabel=new byte[4];
			WDMlabel[0]=0;
			WDMlabel[1]=0;
			WDMlabel[2]=0;
			WDMlabel[3]=(byte)lambda;
			log.info("Lambda= "+lambda);
			genLabel.setLabel(WDMlabel);
		}
		IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
		eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
		eroso.setPrefix(32);
		ero.addEROSubobject(eroso);
		path.seteRO(ero);
		if (req.getMetricList().size()!=0){
			Metric metric=new Metric();
			metric.setMetricType(req.getMetricList().get(0).getMetricType() );
			log.fine("Number of hops "+edge_list.size());
			float metricValue=(float)edge_list.size();
			metric.setMetricValue(metricValue);
			path.getMetricList().add(metric);
		}
		response.addPath(path);
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		Monitoring monitoring=pathReq.getMonitoring();
		if (monitoring!=null){
			if (monitoring.isProcessingTimeBit()){

			}
		}
		//RESERVA AHORA A CAPONAZO
		LinkedList<Object> sourceVertexList=new LinkedList<Object>();
		LinkedList<Object> targetVertexList=new LinkedList<Object>();
		for (i=0;i<edge_list.size();i++){
			sourceVertexList.add(edge_list.get(i).getSource());
			targetVertexList.add(edge_list.get(i).getTarget());
		}	
		sourceVertexList.add(edge_list.get(i-1).getSource());
		targetVertexList.add(edge_list.get(i-1).getTarget());
		
		
		reservationManager.reserve(sourceVertexList, targetVertexList, lambda, 10000, req.getRequestParameters().isBidirect());
		return m_resp;
	}

	public void setPreComp(ComputingAlgorithmPreComputation preComp) {
		this.preComp = (SP_FF_RWA_AlgorithmPreComputation)preComp;
	}

	@Override
	public AlgorithmReservation getReserv() {
		// TODO Auto-generated method stub
		return null;
	}

}
