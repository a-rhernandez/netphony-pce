package tid.pce.tedb;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

//import sun.org.mozilla.javascript.internal.ast.ForInLoop;
import org.eclipse.jetty.util.log.Log;

import tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.DefaultTEMetricLinkAttribTLV;
import tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.IPv4RouterIDLocalNodeLinkAttribTLV;
import tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.IPv4RouterIDRemoteNodeLinkAttribTLV;
import tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.LinkProtectionTypeLinkAttribTLV;
import tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.MetricLinkAttribTLV;
import tid.ospf.ospfv2.lsa.tlv.subtlv.AdministrativeGroup;
import tid.ospf.ospfv2.lsa.tlv.subtlv.AvailableLabels;
import tid.ospf.ospfv2.lsa.tlv.subtlv.IPv4RemoteASBRID;
import tid.ospf.ospfv2.lsa.tlv.subtlv.InterfaceSwitchingCapabilityDescriptor;
import tid.ospf.ospfv2.lsa.tlv.subtlv.LinkLocalRemoteIdentifiers;
import tid.ospf.ospfv2.lsa.tlv.subtlv.LinkProtectionType;
import tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumBandwidth;
import tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumReservableBandwidth;
import tid.ospf.ospfv2.lsa.tlv.subtlv.RemoteASNumber;
import tid.ospf.ospfv2.lsa.tlv.subtlv.SharedRiskLinkGroup;
import tid.ospf.ospfv2.lsa.tlv.subtlv.TrafficEngineeringMetric;
import tid.ospf.ospfv2.lsa.tlv.subtlv.UnreservedBandwidth;
import tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.server.PCEServer;
import tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;

public class TE_Information {
	
	private TrafficEngineeringMetric trafficEngineeringMetric;
	
	private DefaultTEMetricLinkAttribTLV defaultTEMetric;

	private MaximumBandwidth maximumBandwidth; 

	private MaximumReservableBandwidth maximumReservableBandwidth;

	private UnreservedBandwidth unreservedBandwidth; 

	private AdministrativeGroup administrativeGroup;
	
	private LinkLocalRemoteIdentifiers linkLocalRemoteIdentifiers;
	
	private LinkProtectionType linkProtectionType;
	
	private LinkProtectionTypeLinkAttribTLV linkProtectionBGPLS;
	
	private InterfaceSwitchingCapabilityDescriptor interfaceSwitchingCapabilityDescriptor;
	
	private SharedRiskLinkGroup sharedRiskLinkGroup;	
	
	private RemoteASNumber remoteASNumber;
	
	private IPv4RemoteASBRID iPv4RemoteASBRID;
	
	private IPv4RouterIDLocalNodeLinkAttribTLV iPv4LocalNode;
	
	private IPv4RouterIDRemoteNodeLinkAttribTLV iPv4RemoteNode;
	
	private MetricLinkAttribTLV metric;
	
	private AvailableLabels availableLabels;
	
	private int NumberWLANs;
	
	private boolean withWLANs = false;
	
	private boolean[] occupiedWLANs;
	private boolean[] reservedWLANs;
	
	private boolean vlanLink = false;
	
	private int vlan;
	
	/**
	 * TEDB logger
	 */
	public TE_Information(){			
	}

	public AvailableLabels getAvailableLabels() {
		return availableLabels;
	}

	public void setAvailableLabels(AvailableLabels availableLabels) {
		this.availableLabels = availableLabels;
	}

	public TrafficEngineeringMetric getTrafficEngineeringMetric() {
		return trafficEngineeringMetric;
	}

	public void setTrafficEngineeringMetric(
			TrafficEngineeringMetric trafficEngineeringMetric) {
		this.trafficEngineeringMetric = trafficEngineeringMetric;
	}

	public MaximumBandwidth getMaximumBandwidth() {
		return maximumBandwidth;
	}

	public void setMaximumBandwidth(MaximumBandwidth maximumBandwidth) {
		this.maximumBandwidth = maximumBandwidth;
	}

	public MaximumReservableBandwidth getMaximumReservableBandwidth() {
		return maximumReservableBandwidth;
	}

	public IPv4RouterIDRemoteNodeLinkAttribTLV getiPv4RemoteNode() {
		return iPv4RemoteNode;
	}

	public void setiPv4RemoteNode(IPv4RouterIDRemoteNodeLinkAttribTLV iPv4RemoteNode) {
		this.iPv4RemoteNode = iPv4RemoteNode;
	}

	public void setMaximumReservableBandwidth(
			MaximumReservableBandwidth maximumReservableBandwidth) {
		this.maximumReservableBandwidth = maximumReservableBandwidth;
	}

	public UnreservedBandwidth getUnreservedBandwidth() {
		return unreservedBandwidth;
	}

	public void setUnreservedBandwidth(UnreservedBandwidth unreservedBandwidth) {
		this.unreservedBandwidth = unreservedBandwidth;
	}

	public AdministrativeGroup getAdministrativeGroup() {
		return administrativeGroup;
	}

	public void setAdministrativeGroup(AdministrativeGroup administrativeGroup) {
		this.administrativeGroup = administrativeGroup;
	}

	public LinkLocalRemoteIdentifiers getLinkLocalRemoteIdentifiers() {
		return linkLocalRemoteIdentifiers;
	}

	public void setLinkLocalRemoteIdentifiers(
			LinkLocalRemoteIdentifiers linkLocalRemoteIdentifiers) {
		this.linkLocalRemoteIdentifiers = linkLocalRemoteIdentifiers;
	}

	public LinkProtectionType getLinkProtectionType() {
		return linkProtectionType;
	}

	public void setLinkProtectionType(LinkProtectionType linkProtectionType) {
		this.linkProtectionType = linkProtectionType;
	}

	public InterfaceSwitchingCapabilityDescriptor getInterfaceSwitchingCapabilityDescriptor() {
		return interfaceSwitchingCapabilityDescriptor;
	}

	public void setInterfaceSwitchingCapabilityDescriptor(
			InterfaceSwitchingCapabilityDescriptor interfaceSwitchingCapabilityDescriptor) {
		this.interfaceSwitchingCapabilityDescriptor = interfaceSwitchingCapabilityDescriptor;
	}

	public SharedRiskLinkGroup getSharedRiskLinkGroup() {
		return sharedRiskLinkGroup;
	}

	public void setSharedRiskLinkGroup(SharedRiskLinkGroup sharedRiskLinkGroup) {
		this.sharedRiskLinkGroup = sharedRiskLinkGroup;
	}

	public RemoteASNumber getRemoteASNumber() {
		return remoteASNumber;
	}

	public void setRemoteASNumber(RemoteASNumber remoteASNumber) {
		this.remoteASNumber = remoteASNumber;
	}

	public IPv4RemoteASBRID getiPv4RemoteASBRID() {
		return iPv4RemoteASBRID;
	}

	public void setiPv4RemoteASBRID(IPv4RemoteASBRID iPv4RemoteASBRID) {
		this.iPv4RemoteASBRID = iPv4RemoteASBRID;
	}

	public int getNumberWLANs() {
		return NumberWLANs;
	}

	public void setNumberWLANs(int numberWLANs) {
		NumberWLANs = numberWLANs;
	}
	
	public boolean isWLANFree()
	{
		/*
		for (int i = 0; i < reservedWLANs.length; i++) 
		{
			if (reservedWLANs[i] == false)
			{
				return true;
			}
		}
		return false;
		*/
		return true;
	}
	
	public Integer getFreeWLAN()
	{
		for (int i = 0; i < reservedWLANs.length; i++) 
		{
			if (reservedWLANs[i] == false)
			{
				return i;
			}
		}
		return null;
	}
	
	public void initWLANs()
	{
		withWLANs = true;
		occupiedWLANs = new boolean[NumberWLANs];
		reservedWLANs = new boolean[NumberWLANs];
		for (int i = 0 ; i < NumberWLANs ; i++)
		{
			occupiedWLANs[i] = false;
			reservedWLANs[i] = false;
		}
	}

	public void createBitmapLabelSet(int numLabels,int grid, int cs,int n){
		
		 createBitmapLabelSet(numLabels,grid,cs,n,0,numLabels);
	}
	public void createBitmapLabelSet(int numLabels,int grid, int cs,int n,int lambdaIni, int lambdaEnd){
			//FIXME: no hay problema de que se salga el ancho de banda
	//log.info("Creamos bit map");
		BitmapLabelSet bitmapLabelSet = new BitmapLabelSet();
			DWDMWavelengthLabel dwdmWavelengthLabel = new DWDMWavelengthLabel();
			dwdmWavelengthLabel.setGrid(grid);
			dwdmWavelengthLabel.setChannelSpacing(cs);
			dwdmWavelengthLabel.setN(n);
			bitmapLabelSet.setDwdmWavelengthLabel(dwdmWavelengthLabel);
								
			int numberBytes = 	getNumberBytes(numLabels);
		
			byte[] bytesBitMap =  new byte[numberBytes];
			for (int i=0;i<numberBytes;i++)
					bytesBitMap[i]=0x00;	
			
			bitmapLabelSet.setBytesBitmap(bytesBitMap);
			byte[] bytesBitMapRes =  new byte[numberBytes];
			for (int i=0;i<numberBytes;i++)
				bytesBitMapRes[i]=0x00;	
			/*----Opcion: LAMBDA SUBSET----*/
			/*Ponemos a 1 los bytes del BitMap que no maneje el pce. Esto es como poner tiempo de reserva infinito*/
			/*Traducir lambdaIni a numero de bytes*/
			int numberBytesLambdaIni = getNumberBytes(lambdaIni);
			
			/*Traducir lambdaEnd a numero bytes*/
			int numberBytesLambdaEnd =getNumberBytes( lambdaEnd);
			
			for (int i=0;i<numberBytesLambdaIni;i++){
				bytesBitMapRes[i]= (byte) 0xff;
				bytesBitMap[i]= (byte) 0xff;
				
			}
			
			for (int i=numberBytesLambdaEnd;i<numberBytes;i++){
				bytesBitMapRes[i]= (byte) 0xff;	
				bytesBitMap[i]= (byte) 0xff;
				
			}
			//FuncionesUtiles.printByte(bytesBitMap, "bytesBitMap",log);
			bitmapLabelSet.setBytesBitmapReserved(bytesBitMapRes);
			bitmapLabelSet.setNumLabels(numLabels);
			availableLabels = new AvailableLabels();
			availableLabels.setLabelSet(bitmapLabelSet);
			
		
	}
	/**
	 * Funcion que transforma una cantidad de bits en el numero de bytes que necesita 
	 * @param numBit
	 * @return
	 */
	private int getNumberBytes(int numBits){
		int numberBytes = numBits/8;
		if ((numberBytes*8)<numBits){
			numberBytes++;
		}
		return numberBytes;
	}
	
	public boolean[] getCopyUnreservedWLANs()
	{
		boolean[] ret = new boolean[NumberWLANs];

		System.arraycopy( reservedWLANs, 0, ret, 0, ret.length );
		return ret;
	}
	
	public void setFreeWLANS (boolean[] orig)
	{
		reservedWLANs = orig;
	}
	
	public void setWavelengthOccupied(int num_wavelength)
	{
		if (withWLANs)
		{
			occupiedWLANs[num_wavelength] = true;
		}
		else
		{
			int num_byte=num_wavelength/8;
			((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitMap()[num_byte]=(byte)((((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitMap()[num_byte])|(0x80>>>(num_wavelength%8)));
		}
	}
	public void setWavelengthFree(int num_wavelength)
	{
		if (withWLANs)
		{
			occupiedWLANs[num_wavelength] = false;
			reservedWLANs[num_wavelength] = false;
		}
		else
		{
		int num_byte=num_wavelength/8;
		((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitMap()[num_byte]=(byte)(((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitMap()[num_byte]&(0xFFFF7F>>>(num_wavelength%8)));
		}
	}
	
	public void setWavelengthReserved(int num_wavelength){
		if (withWLANs)
		{
			reservedWLANs[num_wavelength] = true;
		}
		else
		{
			int num_byte=num_wavelength/8;
			if ( this.getAvailableLabels()==null){
				PCEServer.Log.info("AvailableLabels ES NULL");
				
			}
			if ( this.getAvailableLabels().getLabelSet()==null){
				PCEServer.Log.info("AvailableLabels LABEL SET ES NULL");
				
			}
			if (((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitmapReserved()==null){
				PCEServer.Log.info("BytesBitmapReserved ES NULL");
				
			}
	
			((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[num_byte]=(byte)((((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[num_byte])|(0x80>>>(num_wavelength%8)));
		}
	}
	
	public void setWavelengthUnReserved(int num_wavelength){
		if (withWLANs)
		{
			reservedWLANs[num_wavelength] = false;
		}
		else
		{
			int num_byte=num_wavelength/8;
			((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[num_byte]=(byte)(((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[num_byte]&(0xFFFF7F>>>(num_wavelength%8)));
		}
	}
	public void setAllWavelengtshUnReserved(){
		if (withWLANs)
		{
			for (int i = 0; i < reservedWLANs.length; i++) 
			{
				reservedWLANs[i] = false;
			}
		}
		else
		{
			int num_bytes=((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitmapReserved().length;
			for (int i=0;i<num_bytes;++i){
				((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[i]=0x00;	
			}
		}
	}
	
	public boolean isWavelengthFree(int num_wavelength){
		if (withWLANs)
		{
			return (!occupiedWLANs[num_wavelength]);
		}
		else
		{
			int num_byte=num_wavelength/8;
			return ((((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitMap()[num_byte]&(0x80>>>(num_wavelength%8)))==0);				
		}
		
	}
	public boolean isWavelengthUnreserved(int num_wavelength){//si es true esta unreserved
		if (withWLANs)
		{
			return (!reservedWLANs[num_wavelength]);
		}
		else
		{
			int num_byte=num_wavelength/8;
			if (((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitmapReserved()==null){
				return false;
			}
			else{
				return ((((BitmapLabelSet)this.getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[num_byte]&(0x80>>>(num_wavelength%8)))==0);				
			}
		}	
	}
	
	
	public String toString(){
		String ret="";
//		if (linkType!=null){
//			ret=ret+linkType.toString()+"\t";			
//		}
//		if (linkID!=null){
//			ret=ret+linkID.toString()+"\t";
//		}
//		if (localInterfaceIPAddress!=null){
//			ret=ret+localInterfaceIPAddress.toString()+"\r\n";
//		}
//		if (remoteInterfaceIPAddress!=null){
//			ret=ret+remoteInterfaceIPAddress.toString()+"\r\n";
//		}

		if (maximumBandwidth!=null){
			ret=ret+maximumBandwidth.toStringShort()+"\t";
		}
		if (maximumReservableBandwidth!=null){
			ret=ret+maximumReservableBandwidth.toString()+"\t";
		}
		
		if (unreservedBandwidth!=null){
			ret=ret+unreservedBandwidth.toStringShort()+"\t";
		}
		
		if (administrativeGroup!=null){
			ret=ret+administrativeGroup.toString()+"\t";
		}

		if (remoteASNumber!=null){
			ret=ret+remoteASNumber.toString()+"\t";
		}
		
		if (iPv4RemoteASBRID!=null){
			ret=ret+iPv4RemoteASBRID.toString()+"\t";
		}
		
		if (availableLabels!= null){
			ret=ret+availableLabels.toString()+"\r\n";
		}
		
		if (iPv4LocalNode!=null){
			ret=ret+iPv4LocalNode.toString()+"\r\n";
		}
		
		if (iPv4RemoteNode!=null){
			ret=ret+iPv4RemoteNode.toString()+"\r\n";
		}
		
		if(linkProtectionBGPLS!=null){
			ret=ret+linkProtectionBGPLS.toString()+"\r\n";
		}
		
		if(trafficEngineeringMetric!=null){
			ret=ret+trafficEngineeringMetric.toString()+"\r\n";
		}
		
		if(metric!=null){
			ret=ret+metric.toString()+"\r\n";
		}
		
		if(defaultTEMetric!=null){
			ret=ret+defaultTEMetric.toString()+"\r\n";
		}
		return ret;
	}

	public boolean isVlanLink() {
		return vlanLink;
	}

	public void setVlanLink(boolean vlanLink) {
		this.vlanLink = vlanLink;
	}

	public int getVlan() {
		return vlan;
	}

	public void setVlan(int vlan) {
		this.vlan = vlan;
	}

	public void setiPv4LocalNode(IPv4RouterIDLocalNodeLinkAttribTLV iPv4RouterIDLocalNode) {	
		this.iPv4LocalNode = iPv4RouterIDLocalNode;
	}
	public IPv4RouterIDLocalNodeLinkAttribTLV getiPv4LocalNode() {	
		return iPv4LocalNode;
	}

	public MetricLinkAttribTLV getMetric() {
		return metric;
	}

	public void setMetric(MetricLinkAttribTLV metric) {
		this.metric = metric;
	}

	public LinkProtectionTypeLinkAttribTLV getLinkProtectionBGPLS() {
		return linkProtectionBGPLS;
	}

	public void setLinkProtectionBGPLS(LinkProtectionTypeLinkAttribTLV linkProtectionBGPLS) {
		this.linkProtectionBGPLS = linkProtectionBGPLS;
	}

	public DefaultTEMetricLinkAttribTLV getDefaultTEMetric() {
		return defaultTEMetric;
	}

	public void setDefaultTEMetric(DefaultTEMetricLinkAttribTLV defaultTEMetric) {
		this.defaultTEMetric = defaultTEMetric;
	}
}