package tid.pce.computingEngine.algorithms.sson;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.computingEngine.ComputingRequest;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.computingEngine.algorithms.AlgorithmReservation;
import tid.pce.computingEngine.algorithms.ComputingAlgorithm;
import tid.pce.computingEngine.algorithms.PCEPUtils;
import tid.pce.computingEngine.algorithms.utilities.bandwidthToSlotConversion;
import tid.pce.computingEngine.algorithms.utilities.channel_generator;
import tid.pce.pcep.constructs.EndPoint;
import tid.pce.pcep.constructs.EndPointAndRestrictions;
import tid.pce.pcep.constructs.P2MPEndpoints;
import tid.pce.pcep.constructs.P2PEndpoints;
import tid.pce.pcep.constructs.Path;
import tid.pce.pcep.constructs.Request;
import tid.pce.pcep.constructs.Response;
import tid.pce.pcep.objects.Bandwidth;
import tid.pce.pcep.objects.EndPoints;
import tid.pce.pcep.objects.EndPointsIPv4;
import tid.pce.pcep.objects.ExplicitRouteObject;
import tid.pce.pcep.objects.GeneralizedBandwidthSSON;
import tid.pce.pcep.objects.GeneralizedEndPoints;
import tid.pce.pcep.objects.Metric;
import tid.pce.pcep.objects.Monitoring;
import tid.pce.pcep.objects.NoPath;
import tid.pce.pcep.objects.ObjectParameters;
import tid.pce.pcep.objects.RequestParameters;
import tid.pce.pcep.objects.tlvs.NoPathTLV;
import tid.pce.server.wson.ReservationManager;
import tid.pce.tedb.DomainTEDB;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.SSONInformation;
import tid.pce.tedb.TEDB;
import tid.rsvp.RSVPProtocolViolationException;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;
import tid.util.UtilsFunctions;
/**
 * Implementation of the algorithm "Dynamic Routing and Spectrum Allocation".
 * 
 * <p>Reference: A. Castro et al., Dynamic routing and spectrum (re)allocation in future flexgrid optical networks, Comput.
Netw. (2012), http://dx.doi.org/10.1016/j.comnet.2012.05.001
</p>
 * @author Arturo mayoral
 *
 */
public class Dynamic_RSA implements ComputingAlgorithm {

	/**
	 * The Logger.
	 */
	private Logger log=Logger.getLogger("PCEServer");
	
	/**
	 * The Path Computing Request to calculate.
	 */
	private ComputingRequest pathReq;

	/**
	 * Access to the Precomputation part of the algorithm.
	 */
	private Dynamic_RSAPreComputation preComp;
	
	private SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph;
	
	/**
	 * Access to the Reservation Manager to make reservations of Wavalengths/labels.
	 */
	private ReservationManager reservationManager;
	
	
	private SSONInformation SSONInfo;
	
//	/**
//	 * Number of wavelenghts (labels).
//	 */
	//private int num_lambdas;
	
	private channel_generator genChannels;
	private ArrayList<BitmapLabelSet> setChannels;
	/**
	 * The traffic engineering database
	 */
	private DomainTEDB ted;
	/**
	 * Required number of consecutive frequency slots
	 */
	private int num_slots = 0;
	
	private GenericLambdaReservation  reserv;
	/**
	 * Constructor
	 * @param pathReq
	 * @param ted
	 * @param reservationManager
	 * @param mf (modulation format)
	 */
	public Dynamic_RSA(ComputingRequest pathReq,TEDB ted, ReservationManager reservationManager, int mf ){
		//this.num_lambdas=((DomainTEDB)ted).getWSONinfo().getNumLambdas();
		this.pathReq=pathReq;
		this.reservationManager=reservationManager;
		this.ted=(DomainTEDB)ted;
	}

	/**
	 * Exectutes the path computation and returns the PCEP Response
	 */
	public ComputingResponse call(){ 
		//Timestamp of the start of the algorithm;
		long tiempoini =System.nanoTime();
		log.info("Starting DynamicRSA Algorithm");
		//Create the response message
		//It will contain either the path or noPath
		ComputingResponse m_resp=new ComputingResponse();
		m_resp.setEncodingType(pathReq.getEcodingType());
		//The request that needs to be solved
		Request req=pathReq.getRequestList().get(0);
		
		//Request Id, needed for the response
		long reqId=req.getRequestParameters().getRequestID();
		log.info("Request id: "+reqId+", getting endpoints");
		//Start creating the response
		Response response=new Response();
		RequestParameters rp = new RequestParameters();
		rp.setRequestID(reqId);
		response.setRequestParameters(rp);
		m_resp.addResponse(response);
		SSONInfo= new SSONInformation();
		SSONInfo=((DomainTEDB)ted).getSSONinfo();
		
		//esto hay que cambiarlo para poder leer del GENERALIZED END POINTS
		//if (getObjectType(req.getEndPoints()))
		EndPoints  EP= req.getEndPoints();
		Bandwidth  Bw= req.getBandwidth(); // Objeto bandwidth para saber la demanda de la peticion.
		Object source_router_id_addr = null;
		Object dest_router_id_addr = null;

		GraphPath<Object,IntraDomainEdge> gp_chosen=null;
	
		int cs;
		boolean end=false;//The search has not ended yet
		int num_labels=0;
		int m=0;
		int Bmod=0; // Spectrum efficiency b/s/Hz (Modulation formats: 16-QAM, QPSK)
		
		num_labels = SSONInfo.getNumLambdas();
		log.info("Num_lambdas "+num_labels);
		

		bandwidthToSlotConversion conversion= new bandwidthToSlotConversion();
		
		// Conversión Bw a numero de slots en función de la grid.	
		if (Bw.getBw()!=0){
			SSONInfo=((DomainTEDB)ted).getSSONinfo();
			cs = SSONInfo.getCs();
			num_slots=conversion.getNumSlots(Bw.getBw(), cs);
		}
			
		/* The set of request-channels will be bounded by the total number of frequency slots
		*  less the number of contiguous frequency slot required for serve the demand plus one.
		*/
		log.info("Request num_slots: "+num_slots);
		setChannels = preComp.getTotalSetChannels().get(num_slots-1);

		
		if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV4){
			EndPointsIPv4  ep=(EndPointsIPv4) req.getEndPoints();
			source_router_id_addr=ep.getSourceIP();
			dest_router_id_addr=ep.getDestIP();
		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_ENDPOINTS_IPV6){

		}else if (EP.getOT()==ObjectParameters.PCEP_OBJECT_TYPE_GENERALIZED_ENDPOINTS){
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

		//Now, check if the source and destination are in the TED.
		log.info("Source: "+source_router_id_addr+"; Destination:"+dest_router_id_addr);
		if (!(((ted.containsVertex(source_router_id_addr))&&(ted.containsVertex(dest_router_id_addr))))){
			log.warning("Source or destination are NOT in the TED");	
			NoPath noPath= new NoPath();
			noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
			NoPathTLV noPathTLV=new NoPathTLV();
			if (!((ted.containsVertex(source_router_id_addr)))){
				log.finest("Unknown source");	
				noPathTLV.setUnknownSource(true);	
			}
			if (!((ted.containsVertex(dest_router_id_addr)))){
				log.finest("Unknown destination");
				noPathTLV.setUnknownDestination(true);	
			}

			noPath.setNoPathTLV(noPathTLV);
			response.setNoPath(noPath);
			return m_resp;
		}
		// check if src and dst are the same 
		if (source_router_id_addr.equals(dest_router_id_addr)){
			log.info("Source and destination are the same!");
			Path path=new Path();
			ExplicitRouteObject ero= new ExplicitRouteObject();
			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			eroso.setIpv4address((Inet4Address)source_router_id_addr);
			eroso.setPrefix(32);
			ero.addEROSubobject(eroso);
			path.seteRO(ero);
			
			if (req.getMetricList().size()!=0){
				Metric metric=new Metric();
				metric.setMetricType(req.getMetricList().get(0).getMetricType() );
				log.fine("Number of hops "+0);
				float metricValue=0;
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
			m_resp.addResponse(response);
			return m_resp;
			
		}


		boolean nopath=true;//Initially, we still have no path

		int central_freq=0; // It represents the central frequency slot n.
		
		double max_metric=Integer.MAX_VALUE;	
		
		preComp.getGraphLock().lock();
		try{
			while (!end){
				//SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda=preComp.getNetworkGraphs().get(0);
				SimpleDirectedWeightedGraph<Object,IntraDomainEdge> graphLambda=preComp.getbaseSimplegraph();
				
				//networkGraph=PCEPUtils.duplicateTEDDB(((SimpleTEDB)ted).getNetworkGraph());
//				Set<IntraDomainEdge> fiberEdges= preComp.getNetworkGraphs().get(0).edgeSet();
//				Iterator<IntraDomainEdge> Iterator = fiberEdges.iterator();
//				while(Iterator.hasNext()){
//				IntraDomainEdge fiberEdge =Iterator.next();
//				FuncionesUtiles.printByte(((BitmapLabelSet)fiberEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap(), "Bitmap edge "+fiberEdge.toString()+".", log);
//				}
				log.info("Antes de DSP"+preComp.printTopology(0));
				ModifiedDijkstraSP dsp=new ModifiedDijkstraSP(graphLambda, source_router_id_addr, dest_router_id_addr, Double.POSITIVE_INFINITY, setChannels, num_slots);
				log.info("Fin MDSP");
				GraphPath<Object,IntraDomainEdge> gp=dsp.getPath();
				
				
				if (gp==null){
					log.fine("No path found");
					NoPath noPath= new NoPath();
					noPath.setNatureOfIssue(ObjectParameters.NOPATH_NOPATH_SAT_CONSTRAINTS);
					NoPathTLV noPathTLV=new NoPathTLV();
					noPath.setNoPathTLV(noPathTLV);				
					response.setNoPath(noPath);
					nopath=true;
					return m_resp;
				}
				else{
					BitmapChannelState valid_channels = new BitmapChannelState(dsp.getVertexSpectrumState().get(dest_router_id_addr).getLength());
					valid_channels.setBytesBitmap(dsp.getVertexSpectrumState().get(dest_router_id_addr).getBytesBitmap());
					
					for (int i=0; i<valid_channels.getLength(); i++){
						if ((valid_channels.getBytesBitmap()[i/8]&(0x80>>(i%8))) == (0x80>>i%8)){
							BitmapLabelSet chosen_channel=setChannels.get(i);
							UtilsFunctions.printByte(chosen_channel.getBytesBitMap(),"Bitmap Channel "+i+">>", log);
							central_freq=i+(num_slots+1)/2;
							break;
						}
					}
					if (central_freq!=0){
						gp_chosen=gp;
						max_metric=gp.getWeight();
						m=(num_slots)/2;
						log.info("Central Frequency"+central_freq);
						log.info("Frequency width"+m);
						log.info("Path"+gp_chosen);
						nopath=false;
						end=true;
					}else{
						throw new IllegalArgumentException("Invalid central frequency for this request");
					}
				}
			}
		}
		finally{
			preComp.getGraphLock().unlock();	
		}

		if (nopath==false){

			Path path=new Path();
			ExplicitRouteObject ero= new ExplicitRouteObject();
			List<IntraDomainEdge> edge_list= gp_chosen.getEdgeList();
			int i;
			
			for (i=0;i<edge_list.size();i++){
				UnnumberIfIDEROSubobject eroso= new UnnumberIfIDEROSubobject();
				eroso.setRouterID((Inet4Address)edge_list.get(i).getSource());
				eroso.setInterfaceID(edge_list.get(i).getSrc_if_id());
				eroso.setLoosehop(false);
				ero.addEROSubobject(eroso);
				
				//FIXME: Temp, only for HPCE algorithms
				preComp.setReservation(m, (central_freq)+preComp.getWSONInfo().getnMin(), edge_list.get(i).getSource(), edge_list.get(i).getTarget());
				
				
				//ITU-T Format
				DWDMWavelengthLabel WDMlabel=new DWDMWavelengthLabel();
				WDMlabel.setGrid(preComp.getWSONInfo().getGrid());
				WDMlabel.setChannelSpacing(preComp.getWSONInfo().getCs());
				WDMlabel.setN((central_freq)+preComp.getWSONInfo().getnMin());
				WDMlabel.setIdentifier(0);
				WDMlabel.setM(m);
				try {
					WDMlabel.encode();
				} catch (RSVPProtocolViolationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				GeneralizedLabelEROSubobject genLabel= new GeneralizedLabelEROSubobject();
				ero.addEROSubobject(genLabel);
				genLabel.setLabel(WDMlabel.getBytes());	
			}
			
			//log.info("Label bit map: "+ted.getWSONinfo().getCommonAvailableLabels().getLabelSet().toString());
			GeneralizedBandwidthSSON GB_SSON = new GeneralizedBandwidthSSON ();
			preComp.setTotalBandwidth((m*2*6.25)+preComp.getTotalBandwidth());
			
//			SSONSenderTSpec mLabel = new SSONSenderTSpec();
//          mLabel.setM(m);
//          mLabel.encode();
          
	        GB_SSON.setM(m);
	        GB_SSON.setRreverse(false);
	        GB_SSON.setOreopt(false);
			
			IPv4prefixEROSubobject eroso= new IPv4prefixEROSubobject();
			eroso.setIpv4address((Inet4Address)edge_list.get(edge_list.size()-1).getTarget());
			eroso.setPrefix(32);
			ero.addEROSubobject(eroso);
			path.seteRO(ero);
			path.setGeneralizedbandwidth(GB_SSON);
			PCEPUtils.completeMetric(path, req, edge_list);
			response.addPath(path);
			
			//FIXME: RESERVATION NEEDS TO BE IMPROVED!!!
			LinkedList<Object> sourceVertexList=new LinkedList<Object>();
			LinkedList<Object> targetVertexList=new LinkedList<Object>();
			
			for (i=0;i<edge_list.size();i++){
				sourceVertexList.add(edge_list.get(i).getSource());
				targetVertexList.add(edge_list.get(i).getTarget());
			}	
			sourceVertexList.add(edge_list.get(i-1).getSource());
			targetVertexList.add(edge_list.get(i-1).getTarget());
			
			if (req.getReservation()!=null){
			  reserv= new GenericLambdaReservation();
			  reserv.setResp(m_resp);
			  reserv.setLambda_chosen(central_freq);
			  reserv.setBidirectional(req.getRequestParameters().isBidirect());
			  reserv.setReservation(req.getReservation());
			  reserv.setSourceVertexList(sourceVertexList);
			  reserv.setTargetVertexList(targetVertexList);
			  //log.info("Bidirect = " +req.getRequestParameters().isBidirect());
			  reserv.setReservationManager(reservationManager);
			}
		
		}
		long tiempofin =System.nanoTime();
		long tiempotot=tiempofin-tiempoini;
		log.info("Ha tardado "+tiempotot+" nanosegundos");
		log.info("RESPONSE: "+m_resp.toString());
		return m_resp;
	}

	public AlgorithmReservation getReserv() {
		return reserv;
	}

	
	public void setPreComp(Dynamic_RSAPreComputation preComp) {
		this.preComp = preComp;
	}
	
	
	
	public static String toHexString(byte [] packetBytes){
		 StringBuffer sb=new StringBuffer(packetBytes.length*2);
		 for (int i=0; i<packetBytes.length;++i){
		  if ((packetBytes[i]&0xFF)<=0x0F){
		   sb.append('0');
		   sb.append(Integer.toHexString((packetBytes[i]&0xFF))); 
		  }
		  else {
		   sb.append(Integer.toHexString((packetBytes[i]&0xFF)));
		  }
		 }
		 return sb.toString();
		 
		 }
}
