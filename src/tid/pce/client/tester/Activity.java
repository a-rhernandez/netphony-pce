package tid.pce.client.tester;

import tid.netManager.NetworkLSPManager;
import tid.pce.client.PCCPCEPSession;
import tid.pce.client.emulator.AutomaticTesterStatistics;
import tid.pce.computingEngine.ComputingResponse;
import tid.pce.pcep.messages.PCEPRequest;
import tid.vntm.LigthPathManagement;
import tid.vntm.VNTMSession;

/**
 * Interface to describe an activity you can simulate in the client. The posible activities are:
 * - NetworkEmulator
 * - VNTM
 * @author mcs
 *
 */
public interface Activity extends Runnable{
	
	public void addVNTMSession(PCCPCEPSession VNTMSession);
	public void addNetworkEmulator(NetworkLSPManager networkLSPManager);
	public void addStatistics(AutomaticTesterStatistics stats);
	public void addRequest(PCEPRequest request);
	public void addResponse(ComputingResponse response);
	public void addPCEsessionVNTM(PCCPCEPSession vNTMSession);
	public void addLigthPathManagement(LigthPathManagement ligthPathManagement);
	

}
