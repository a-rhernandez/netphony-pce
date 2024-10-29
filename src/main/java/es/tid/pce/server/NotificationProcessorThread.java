package es.tid.pce.server;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.pce.pcep.constructs.Notify;
import es.tid.pce.pcep.objects.Notification;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.rsvp.constructs.gmpls.DWDMWavelengthLabel;
import es.tid.rsvp.objects.subobjects.EROSubobject;
import es.tid.rsvp.objects.subobjects.GeneralizedLabelEROSubobject;
import es.tid.rsvp.objects.subobjects.IPv4prefixEROSubobject;
import es.tid.rsvp.objects.subobjects.SubObjectValues;
import es.tid.rsvp.objects.subobjects.UnnumberIfIDEROSubobject;

public class NotificationProcessorThread extends Thread {

	private boolean running;

	private LinkedBlockingQueue<Notify> notificationList;

	//private ReservationManager reservationManager;

	private Logger log;

	public NotificationProcessorThread(LinkedBlockingQueue<Notify> notificationList) {
		running = true;
		this.notificationList = notificationList;
		//this.reservationManager = reservationManager;

		log = LoggerFactory.getLogger("PCEServer");

	}

	public void run() {
		// Notify notify;
		Notify notify;
		while (running) {
			try {
				notify = notificationList.take();
				LinkedList<Notification> notificationList = notify.getNotificationList();
				for (int i = 0; i < notificationList.size(); i++) {
				    Notification notif = notificationList.get(i);
				    // LOG DEL TIPO DE NOTIFICACIÓN
				    switch (notif.getNotificationType()){
				        case ObjectParameters.PCEP_NOTIFICATION_TYPE_REACHABILITY:
				            log.info("PCEP NOTIFICATION TYPE: REACHABILITY");
				            break;
				        case ObjectParameters.PCEP_NOTIFICATION_TYPE_TOPOLOGY:
				            log.info("PCEP NOTIFICATION TYPE: TOPOLOGY");
				            break;
				        case ObjectParameters.PCEP_NOTIFICATION_TYPE_IT_RESOURCE_INFORMATION:
				            log.info("PCEP NOTIFICATION TYPE: IT RESOURCE INFORMATION");
				            break;
				        case ObjectParameters.PCEP_NOTIFICATION_TYPE_CANCEL_RESERVATION:
				            log.info("PCEP NOTIFICATION TYPE: CANCEL RESERVATION");
				            break;
				        case ObjectParameters.PCEP_NOTIFICATION_TYPE_PRERESERVE:
				            log.info("PCEP NOTIFICATION TYPE: PRERESERVE");
				            break;
				        case ObjectParameters.PCEP_NOTIFICATION_VALUE_QUERY:
				            log.info("PCEP NOTIFICATION VALUE: QUERY");
				            break;
				        case ObjectParameters.PCEP_NOTIFICATION_VALUE_CANCEL_ALL_RESERVATIONS:
				            log.info("PCEP NOTIFICATION VALUE: CANCEL ALL RESERVATIONS");
				            break;
				        case ObjectParameters.PCEP_NOTIFICATION_VALUE_PATH_RESERVATION:
				            log.info("PCEP NOTIFICATION VALUE: PATH RESERVATION");
				            break;
				        default:
				            log.error("Error: Unexpected Message");
				            break;
				    }
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private boolean initializeVariables(LinkedList<EROSubobject> erolist, LinkedList<Object> src,
			LinkedList<Object> dst, DWDMWavelengthLabel dwdmWavelengthLabel) {
		int number_lambdas = 0;
		boolean labelFound = false;
		for (int i = 0; i < erolist.size() - 1; ++i) {
			if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_LABEL) {
				if (!labelFound) {
					labelFound = true;
					dwdmWavelengthLabel = ((GeneralizedLabelEROSubobject) erolist.get(i)).getDwdmWavelengthLabel();
				}
				number_lambdas++;
			}
			if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				src.add(i - number_lambdas, ((IPv4prefixEROSubobject) erolist.get(i)).getIpv4address());
			} else if (erolist.get(i).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				src.add(i - number_lambdas, ((UnnumberIfIDEROSubobject) erolist.get(i)).getRouterID());
			}
			if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
				dst.add(i - number_lambdas, ((IPv4prefixEROSubobject) erolist.get(i + 1)).getIpv4address());

			} else if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
				dst.add(i - number_lambdas, ((UnnumberIfIDEROSubobject) erolist.get(i + 1)).getRouterID());
			} else if (erolist.get(i + 1).getType() == SubObjectValues.ERO_SUBOBJECT_LABEL) {
				if (erolist.get(i + 2).getType() == SubObjectValues.ERO_SUBOBJECT_IPV4PREFIX) {
					dst.add(i - number_lambdas, ((IPv4prefixEROSubobject) erolist.get(i + 2)).getIpv4address());
				} else if (erolist.get(i + 2).getType() == SubObjectValues.ERO_SUBOBJECT_UNNUMBERED_IF_ID) {
					dst.add(i - number_lambdas, ((UnnumberIfIDEROSubobject) erolist.get(i + 2)).getRouterID());
				}

				return true;
			}
		}
		return false;

	}

}
