package es.tid.pce.computingEngine.algorithms;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.of.DataPathID;
import es.tid.pce.computingEngine.ComputingRequest;
import es.tid.pce.computingEngine.ComputingResponse;
import es.tid.pce.computingEngine.algorithms.mpls.EncodeEroMPLS;
import es.tid.pce.computingEngine.algorithms.mpls.EncodeEroMPLS2;
import es.tid.pce.pcep.constructs.EndPoint;
import es.tid.pce.pcep.constructs.EndPointAndRestrictions;
import es.tid.pce.pcep.constructs.IPv4AddressEndPoint;
import es.tid.pce.pcep.constructs.NAIIPv4Adjacency;
import es.tid.pce.pcep.constructs.NAIIPv4NodeID;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.constructs.Response;
import es.tid.pce.pcep.constructs.UnnumIfEndPoint;
import es.tid.pce.pcep.objects.Bandwidth;
import es.tid.pce.pcep.objects.BandwidthExistingLSP;
import es.tid.pce.pcep.objects.BandwidthRequested;
import es.tid.pce.pcep.objects.EndPoints;
import es.tid.pce.pcep.objects.EndPointsIPv4;
import es.tid.pce.pcep.objects.ExplicitRouteObject;
import es.tid.pce.pcep.objects.GeneralizedEndPoints;
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.Monitoring;
import es.tid.pce.pcep.objects.NoPath;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.pce.pcep.objects.P2MPGeneralizedEndPoints;
import es.tid.pce.pcep.objects.P2PGeneralizedEndPoints;
import es.tid.pce.pcep.objects.RequestParameters;
import es.tid.pce.pcep.objects.subobjects.SREROSubobject;
import es.tid.pce.pcep.objects.tlvs.NoPathTLV;
import es.tid.pce.pcep.objects.tlvs.PathSetupTLV;
import es.tid.rsvp.objects.subobjects.DataPathIDEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import es.tid.rsvp.objects.subobjects.UnnumberedDataPathIDEROSubobject;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.MDTEDB;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;

public class DefaultSinglePathComputing implements ComputingAlgorithm {

	private SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph;
	private Logger log=LoggerFactory.getLogger("PCEServer");
	private ComputingRequest pathReq;
	private TEDB ted;

	public DefaultSinglePathComputing(ComputingRequest pathReq,TEDB ted ){
		this.ted=ted;
		try {
			if (ted.getClass().equals(SimpleTEDB.class)){
				this.networkGraph= ((SimpleTEDB)ted).getDuplicatedNetworkGraph();
			} else if (ted.getClass().equals(MDTEDB.class) ){
				//this.networkGraph= ((MDTEDB)ted).getDuplicatedNetworkGraph();
				this.networkGraph=null;
			} 

			this.pathReq=pathReq;	
		}catch (Exception e){
			this.pathReq=pathReq;
			this.networkGraph=null;
		}

	}

	public ComputingResponse call() {
		long tiempoini =System.nanoTime();
		ComputingResponse m_resp=new ComputingResponse();
		Request req=pathReq.getRequestList().get(0);
		long reqId=req.getRequestParameters().getRequestID();
		log.info("Processing Single Path Computing Request id: "+reqId);
		
		
		//Start creating the response
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
//		PathSetupTLV pathi = new PathSetupTLV();
//		pathi.setPST(1);
//		pathi.setTLVType(28);
//		rp.setPathSetupTLV(pathi);
		
		
		/***************ELIMINAR************/
//		PathSetupTLV patheo = new PathSetupTLV();
//		patheo.setTLVType(28);
//		patheo.setPST(1);
//		rp.setPathSetupTLV(patheo);
		/////////////////////////////////////////
//		rp.setPbit(true);
//		rp.setRequestID(reqId);
//		response.setRequestParameters(rp);
		
		
		if (req.getRequestParameters().getPathSetupTLV()!=null ) {
			PathSetupTLV pst =new PathSetupTLV();
			rp.setPathSetupTLV(pst);
		}
		 if (this.networkGraph==null){
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();	
			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		} 


		EndPoints  EP= req.getEndPoints();	
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;
		Object router_xro = null;
		long source_port = 0;
		long destination_port = 0;
		DataPathID xro = new DataPathID();
		

		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){
			log.info("ENDPOINTS IPv6 not supported");
		}
		else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
			GeneralizedEndPoints  gep=(GeneralizedEndPoints) req.getEndPoints();
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2P){
				P2PGeneralizedEndPoints p2pep= (P2PGeneralizedEndPoints)gep;	
				EndPoint sep= p2pep.getSourceEndpoint().getEndPoint();
				
				if (sep instanceof IPv4AddressEndPoint) {
				  source_router_id_addr = ((IPv4AddressEndPoint)p2pep.getSourceEndpoint().getEndPoint()).getEndPointIPv4().getIPv4address();
				  dest_router_id_addr = ((IPv4AddressEndPoint)p2pep.getDestinationEndpoint().getEndPoint()).getEndPointIPv4().getIPv4address();
				}else if (sep instanceof UnnumIfEndPoint) {
					source_router_id_addr = ((UnnumIfEndPoint)p2pep.getSourceEndpoint().getEndPoint()).getUnnumberedEndpoint().getIPv4address();
					dest_router_id_addr = ((UnnumIfEndPoint)p2pep.getDestinationEndpoint().getEndPoint()).getUnnumberedEndpoint().getIPv4address();
					source_port=((UnnumIfEndPoint)p2pep.getSourceEndpoint().getEndPoint()).getUnnumberedEndpoint().getIfID();
					destination_port=((UnnumIfEndPoint)p2pep.getSourceEndpoint().getEndPoint()).getUnnumberedEndpoint().getIfID();
				}
			}
			if(gep.getGeneralizedEndPointsType()==ObjectParameters.PCEP_GENERALIZED_END_POINTS_TYPE_P2MP_NEW_LEAVES){
				P2MPGeneralizedEndPoints p2mpep= (P2MPGeneralizedEndPoints)gep;
				EndPointAndRestrictions epandrest=p2mpep.getEndpointAndRestrictions();
				EndPoint sourceep=epandrest.getEndPoint();
				source_router_id_addr=((IPv4AddressEndPoint)sourceep).getEndPointIPv4().IPv4address;
				int cont=0;
				while (cont<=p2mpep.getEndpointAndRestrictionsList().size()){ //esto est� mal
					epandrest=p2mpep.getEndpointAndRestrictionsList().get(cont);
					EndPoint destep=epandrest.getEndPoint();
					dest_router_id_addr=((IPv4AddressEndPoint)destep).getEndPointIPv4().IPv4address;

				}
			}
		}
		
		log.info("Algorithm->  Source:: "+source_router_id_addr+" Destination:: "+dest_router_id_addr);
		log.info("Check if we have source and destination in our TED");
		
		
		//Case XRO is not null
		if(req.getXro()!=null){
			xro.setDataPathID(req.getXro().getXROSubobjectList().getFirst().toString());
			log.info("Algorithm.getXro ::"+xro);
			if (networkGraph.containsVertex(xro)){
				log.info("Delete node in graph:: "+xro);
				networkGraph.removeVertex(xro);
			}
		}

		if (!((networkGraph.containsVertex(source_router_id_addr))&&(networkGraph.containsVertex(dest_router_id_addr)))){
			log.warn("DefaultSinglePathComputing:: Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((networkGraph.containsVertex(source_router_id_addr)))){
				log.warn("Unknown source");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((networkGraph.containsVertex(dest_router_id_addr)))){
				log.warn("Unknown destination");
				noPathTLV.setUnknownDestination(true);	
			}

			noPath.setNoPathTLV(noPathTLV);				
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		} 

		log.debug("Computing path");
//		long tiempoini =System.nanoTime();
		DijkstraShortestPath<Object,IntraDomainEdge>  dsp=new DijkstraShortestPath<Object,IntraDomainEdge> (networkGraph, source_router_id_addr, dest_router_id_addr);
		GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();

		log.debug("Creating response");
		if (gp==null){
			log.warn("DefaultSinglePathComputing:: No Path Found");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);				
			response.setNoPath(noPath);
			m_resp.addResponse(response);
			return m_resp;
		} 
		
		// Code ERO Object
		m_resp.addResponse(response);
		Path path=new Path();
		
		ExplicitRouteObject ero= new ExplicitRouteObject();
		List<IntraDomainEdge> edge_list=gp.getEdgeList();
		EncodeEroMPLS2.createEroMpls(ero, edge_list);
		
		/**Inet4Address eru = null;
		
		try {
			eru = (Inet4Address) Inet4Address.getByName("192.168.1.12");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IPv4prefixEROSubobject eroso = new IPv4prefixEROSubobject();
		eroso.setIpv4address(eru);
		eroso.setPrefix(32);
		eroso.setLoosehop(false);
		ero.getEROSubobjectList().add(eroso);**/
		
		
		/********ELIMINAR*******************/
//		SREROSubobject ero2 = new SREROSubobject();
//		SREROSubobject ero3 = new SREROSubobject();
//		NAIIPv4Adjacency naia = new NAIIPv4Adjacency();
//		NAIIPv4Adjacency naia2 = new NAIIPv4Adjacency();
//		NAIIPv4NodeID nai= new NAIIPv4NodeID();
//		Inet4Address eru;
//		
//		try {
//			eru = (Inet4Address) Inet4Address.getByName("172.16.0.2");
//			naia.setLocalNodeAddress(eru);
//			eru = (Inet4Address) Inet4Address.getByName("172.16.0.1");
//			naia.setRemoteNodeAddress(eru);
//			naia.setNaiType(3);
//		}catch(Exception e) {
//			
//		}
//		try {
//			eru = (Inet4Address) Inet4Address.getByName("172.16.0.1/30");
//			nai.setNodeID(eru);
//			nai.setNaiType(1);
//		}catch(Exception e) {
//			
//		}
//		
//		ero2.setNai(nai);
//		ero2.setNT(0);
//		ero2.setMflag(true);
//		ero2.setFflag(false);
//		ero2.setSID(253956096);
//		ero2.setLoosehop(false);
//		ero.getEROSubobjectList().add(ero2); 
//		
//		try {
//			eru = (Inet4Address) Inet4Address.getByName("192.168.1.11");
//			naia2.setLocalNodeAddress(eru);
//			eru = (Inet4Address) Inet4Address.getByName("192.168.1.12");
//			naia2.setRemoteNodeAddress(eru);
//			naia2.setNaiType(3);
//		}catch(Exception e) {
//			
//		}
//		try {
//			eru = (Inet4Address) Inet4Address.getByName("1.1.1.2");
//			nai.setNodeID(eru);
//		}catch(Exception e) {
//			
//		}
		
//		ero3.setFflag(false);
//
//		ero3.setNai(naia2);
//		ero3.setNT(3);
//		ero3.setMflag(false);
//		ero3.setSID(200);
//		ero3.setLoosehop(false);
//		ero.getEROSubobjectList().add(ero3);
		
		/***************************/
		
//		// Add first hop in the ERO Object in there is an interface
//		if (source_port!=0){
//			if (edge_list.get(0).getSource() instanceof Inet4Address){
//				UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
//				eroso.setRouterID((Inet4Address)edge_list.get(0).getSource());
//				eroso.setInterfaceID(source_port);
//				eroso.setLoosehop(false);
//				ero.addEROSubobject(eroso);
//			}else if (edge_list.get(0).getSource() instanceof DataPathID){
//				UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
//				eroso.setDataPath((DataPathID)edge_list.get(0).getSource());
//				eroso.setInterfaceID(source_port);
//				eroso.setLoosehop(false);
//				ero.addEROSubobject(eroso);
//			}else{
//				log.info("Edge instance error");
//			}
//		} 
//		
//		// Add intermediate hops
//		int i;
//		for (i=0;i<edge_list.size();i++){
//			if (edge_list.get(i).getSource() instanceof Inet4Address){
//				UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
//				eroso.setRouterID((Inet4Address)edge_list.get(i).getSource());
//				eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
//				eroso.setLoosehop(false);
//				ero.addEROSubobject(eroso);
//			}else if (edge_list.get(i).getSource() instanceof DataPathID){
//				UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
//				eroso.setDataPath((DataPathID)edge_list.get(i).getSource());
//				eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
//				eroso.setLoosehop(false);
//				ero.addEROSubobject(eroso);
//			}else{
//				log.info("Edge instance error");
//			}
//		}
//		// Add last hop in the ERO Object
//		log.info("jm dspc destination_port: "+ destination_port);
//		if (destination_port!=0){
//			if (edge_list.get(edge_list.size()-1).getTarget() instanceof Inet4Address){
//				log.info("jm defoultsingle ultima interfaz ip"+ edge_list.get(edge_list.size()-1));
//				UnnumberIfIDEROSubobject eroso = new UnnumberIfIDEROSubobject();
//				eroso.setRouterID((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
//				eroso.setInterfaceID(destination_port);
//				eroso.setLoosehop(false);
//				ero.addEROSubobject(eroso);
//			}else if (edge_list.get(edge_list.size()-1).getTarget() instanceof DataPathID){
//				log.info("jm defoultsingle ultima interfaz dpid"+ edge_list.get(edge_list.size()-1));
//				UnnumberedDataPathIDEROSubobject eroso = new UnnumberedDataPathIDEROSubobject();
//				eroso.setDataPath((DataPathID)edge_list.get(edge_list.size()-1).getTarget());
//				eroso.setInterfaceID(destination_port);
//				eroso.setLoosehop(false);
//				ero.addEROSubobject(eroso);
//			}			
//		} else {
//			if (edge_list.get(edge_list.size()-1).getTarget() instanceof Inet4Address){
//				IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
//				eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
//				eroso.setPrefix(32);
//				ero.addEROSubobject(eroso);
//			} else if (edge_list.get(edge_list.size()-1).getTarget() instanceof DataPathID){
//				DataPathIDEROSubobject eroso = new DataPathIDEROSubobject();
//				eroso.setDataPath((DataPathID)edge_list.get(edge_list.size()-1).getTarget());
//				ero.addEROSubobject(eroso);
//			}else{
//				log.info("Edge instance error");
//			}
//		}

		log.info("Algorithm.ero :: "+ero.toString());
		path.setEro(ero);
		log.info("Algorithm.path:: "+path.toString());
		/*Metric metric=new Metric();
		metric.setMetricType(12);
		metric.setMetricValue(10);
		path.getMetricList().add(metric);*/
		
//		BandwidthRequested bw = new BandwidthRequested();
//		bw.setBw(100);
//		path.setBandwidth(bw);
		
//		log.debug("Number of hops "+edge_list.size());
//		//FIXME
//		float metricValue=(float)edge_list.size()*10;
//		metric.setMetricValue(metricValue);
//		path.getMetricList().add(metric);
//		if (req.getMetricList().size()!=0){
//			Metric metric=new Metric();
//			metric.setMetricType(req.getMetricList().get(0).getMetricType() );
//			log.debug("Number of hops "+edge_list.size());
//			//FIXME
//			float metricValue=(float)edge_list.size()*10;
//			metric.setMetricValue(metricValue);
//			path.getMetricList().add(metric);
//		}
		
//		Metric metric = new Metric();
//		
//		metric.setBoundBit(true);
//		metric.setMetricType(12);
//		metric.setMetricValue(5);
//		path.getMetricList().add(metric);
//		Metric metric2 = new Metric();
//		metric2.setBoundBit(true);
//		metric2.setMetricType(2);
//		metric2.setMetricValue(50);
//		path.getMetricList().add(metric2);
		
		BandwidthExistingLSP bw = new BandwidthExistingLSP();
		
		bw.setOT(1);
		bw.setBw(100);
		
		path.setBandwidth(bw);
		response.addPath(path);
		
		
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		Monitoring monitoring=pathReq.getMonitoring();
		if (monitoring!=null){
			if (monitoring.isProcessingTimeBit()){

			}
		}
		return m_resp;
	}

	@Override
	public AlgorithmReservation getReserv() {
		// TODO Auto-generated method stub
		return null;
	}


}
