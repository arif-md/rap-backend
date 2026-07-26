package x.y.z.backend.utils;

import org.kie.server.client.DocumentServicesClient;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.kie.server.client.admin.ProcessAdminServicesClient;
import org.kie.server.client.admin.UserTaskAdminServicesClient;
import org.kie.server.client.impl.AbstractKieServicesClientImpl;

import java.util.HashSet;
import java.util.Set;

public class KieClient {
	
    private static String kieServerURL;
    private static String kieServerUser;
    private static String kieServerPassword;
    public static String latestPidVerRecNewApp;
	public static String latestPidVerRecInsRenewal;
	public static String latestPidVerRecLetter;
    public static String latestPidVerPalNewApp;
    public static String latestPidVerPalModifyPermit;    
    public static String latestPidVerSciNewApp;
	public static String latestPidVerSciModifyPermit;   
	
    private static KieServicesClient kieServicesClient;
	private static UserTaskServicesClient userTaskServicesClient;
	private static QueryServicesClient queryServicesClient;
	private static ProcessServicesClient processServicesClient;
	private static DocumentServicesClient documentServicesClient;
	private static ProcessAdminServicesClient processAdminServicesClient;
	private static UserTaskAdminServicesClient userTaskAdminServicesClient;
	
	private static final Object mutex_kieSvcClient = new Object();
	private static final Object mutex_usrSvcClient = new Object();
	private static final Object mutex_qrySvcClient = new Object();
	private static final Object mutex_prcSvcClient = new Object();
	private static final Object mutex_docSvcClient = new Object();
	private static final Object mutex_prcAdmSvcClient = new Object();
	private static final Object mutex_usrAdmSvcClient = new Object();
	
	//private static final MarshallingFormat FORMAT = MarshallingFormat.XSTREAM;

	private static final Set<Class<?>> jaxbClasses = new HashSet<Class<?>>();
	static {
		//jaxbClasses.add(Domain.class);		
		jaxbClasses.add(org.jbpm.document.service.impl.DocumentImpl.class);	
		//jaxbClasses.add(CustomKieTask.class);	
	}
	

	public static KieServicesClient getKieServicesClient(){
		if(kieServicesClient == null){
			synchronized (mutex_kieSvcClient){
				if(kieServicesClient == null){
					KieServicesConfiguration kieConfig =  KieServicesFactory.newRestConfiguration(
							kieServerURL, kieServerUser, kieServerPassword, 60000);
					//kieConfig.setMarshallingFormat(MarshallingFormat.JSON);
					kieConfig.setExtraClasses(jaxbClasses);
					kieServicesClient = KieServicesFactory.newKieServicesClient(kieConfig);
					((AbstractKieServicesClientImpl)kieServicesClient).getLoadBalancer().setCheckFailedEndpoint(true);
				}
			}
		}
		return kieServicesClient;
	}
	
	public static ProcessAdminServicesClient getProcessAdminServicesClient(){
		//double-checked locking pattern.
		if(processAdminServicesClient == null){
			synchronized (mutex_prcAdmSvcClient){
				if(processAdminServicesClient == null){
					KieServicesClient client = getKieServicesClient();
					processAdminServicesClient = client.getServicesClient(ProcessAdminServicesClient.class);					
				}
			}
		}
		return processAdminServicesClient;
	}

	public static UserTaskAdminServicesClient getUserTaskAdminServicesClient(){
		//double-checked locking pattern.
		if(userTaskAdminServicesClient == null){
			synchronized (mutex_usrAdmSvcClient){
				if(userTaskAdminServicesClient == null){
					KieServicesClient client = getKieServicesClient();
					userTaskAdminServicesClient = client.getServicesClient(UserTaskAdminServicesClient.class);					
				}
			}
		}
		return userTaskAdminServicesClient;
	}
	
	public static ProcessServicesClient getProcessServicesClient(){
		//double-checked locking pattern.
		if(processServicesClient == null){
			synchronized (mutex_prcSvcClient){
				if(processServicesClient == null){
					KieServicesClient client = getKieServicesClient();
					processServicesClient = client.getServicesClient(ProcessServicesClient.class);					
				}
			}
		}
		return processServicesClient;
	}
	
	public static UserTaskServicesClient getUserTaskServicesClient(){
		//double-checked locking pattern.
		if(userTaskServicesClient == null){
			synchronized (mutex_usrSvcClient){
				if(userTaskServicesClient == null){
					KieServicesClient client = getKieServicesClient();
					userTaskServicesClient = client.getServicesClient(UserTaskServicesClient.class);					
				}
			}
		}
		return userTaskServicesClient;
	}
	
	public static QueryServicesClient getQueryServicesClient(){
		//double-checked locking pattern.
		if(queryServicesClient == null){
			synchronized (mutex_qrySvcClient){
				if(queryServicesClient == null){
					KieServicesClient client = getKieServicesClient();
					queryServicesClient = client.getServicesClient(QueryServicesClient.class);					
				}
			}
		}
		return queryServicesClient;
	}
	
	public static DocumentServicesClient getDocumentServicesClient(){
		//double-checked locking pattern.
		if(documentServicesClient == null){
			synchronized (mutex_docSvcClient){
				if(documentServicesClient == null){
					KieServicesClient client = getKieServicesClient();
					documentServicesClient = client.getServicesClient(DocumentServicesClient.class);					
				}
			}
		}	
		return documentServicesClient;
	}
			
	public static void setKieServerURL(String kieServerURL) {
		KieClient.kieServerURL = kieServerURL;
	}
	public static void setKieServerUser(String kieServerUser) {
		KieClient.kieServerUser = kieServerUser;
	}
	public static void setKieServerPassword(String kieServerPassword) {
		KieClient.kieServerPassword = kieServerPassword;
	}
	
	public static void setLatestPidVerRecNewApp(String latestPidVerRecNewApp) {
		KieClient.latestPidVerRecNewApp = latestPidVerRecNewApp;
	}
	public static void setLatestPidVerRecInsRenewal(String latestPidVerRecInsRenewal) {
		KieClient.latestPidVerRecInsRenewal = latestPidVerRecInsRenewal;
	}
	public static void setLatestPidVerPalModifyPermit(String latestPidVerPalModifyPermit) {
		KieClient.latestPidVerPalModifyPermit = latestPidVerPalModifyPermit;
	}
	public static void setLatestPidVerPalNewApp(String latestPidVerPalNewApp) {
		KieClient.latestPidVerPalNewApp = latestPidVerPalNewApp;
	}
	public static void setLatestPidVerSciNewApp(String latestPidVerSciNewApp) {
		KieClient.latestPidVerSciNewApp = latestPidVerSciNewApp;
	}
	public static void setLatestPidVerSciModifyPermit(String latestPidVerSciModifyPermit) {
		KieClient.latestPidVerSciModifyPermit = latestPidVerSciModifyPermit;
	}
	public static void setLatestPidVerRecLetter(String latestPidVerRecLetter) {
		KieClient.latestPidVerRecLetter = latestPidVerRecLetter;
	}
}
