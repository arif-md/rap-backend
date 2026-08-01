package x.y.z.backend.utils;

import org.kie.server.api.marshalling.MarshallingFormat;
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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import x.y.z.backend.domain.model.CustomKieTask;

import java.util.Arrays;
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
	private static KieServicesClient queryKieServicesClient;
	private static UserTaskServicesClient userTaskServicesClient;
	private static QueryServicesClient queryServicesClient;
	private static ProcessServicesClient processServicesClient;
	private static DocumentServicesClient documentServicesClient;
	private static ProcessAdminServicesClient processAdminServicesClient;
	private static UserTaskAdminServicesClient userTaskAdminServicesClient;

	private static final Object mutex_kieSvcClient = new Object();
	private static final Object mutex_qryKieSvcClient = new Object();
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
		jaxbClasses.add(CustomKieTask.class);	
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

	/**
	 * Separate KieServicesClient used only by getQueryServicesClient(), configured for JSON
	 * instead of the default (XML/JAXB) format the shared client above uses. Custom queries
	 * (e.g. findTasksAssignedAsPotentialOwnerByGroup) return CustomKieTask, a class that isn't
	 * known to the KIE Server's JAXBContext on the processes side (see CustomQueryConfig in
	 * that module) - JAXB requires every marshalled class to be registered ahead of time,
	 * so it throws "class ... nor any of its super class is known to this context" for any
	 * custom result type. Jackson (used for JSON) serializes POJOs via reflection without that
	 * closed-world requirement, so routing query calls through JSON sidesteps the problem
	 * entirely. Scoped to its own client (not applied to getKieServicesClient()) so task/
	 * process/document/admin calls, which already work under the default format, are untouched.
	 */
	private static KieServicesClient getQueryKieServicesClient(){
		if(queryKieServicesClient == null){
			synchronized (mutex_qryKieSvcClient){
				if(queryKieServicesClient == null){
					KieServicesConfiguration kieConfig =  KieServicesFactory.newRestConfiguration(
							kieServerURL, kieServerUser, kieServerPassword, 60000);
					kieConfig.setMarshallingFormat(MarshallingFormat.JSON);
					kieConfig.setExtraClasses(jaxbClasses);
					queryKieServicesClient = KieServicesFactory.newKieServicesClient(kieConfig);
					((AbstractKieServicesClientImpl)queryKieServicesClient).getLoadBalancer().setCheckFailedEndpoint(true);
				}
			}
		}
		return queryKieServicesClient;
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
					KieServicesClient client = getQueryKieServicesClient();
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
			
	/**
	 * Fetch the process instance diagram (SVG, active nodes highlighted) from the KIE
	 * Server REST API. Not exposed by the typed kie-server-client interfaces
	 * (ProcessServicesClient etc.), so this calls the "Process images" resource
	 * (org.kie.server.remote.rest.jbpm.ui.ImageResource, kie-server-rest-jbpm-ui) directly:
	 * GET {kieServerURL}/containers/{containerId}/images/processes/instances/{processInstanceId}
	 * No processId path segment - the running instance already identifies its process.
	 */
	public static byte[] getProcessInstanceImageSvg(String containerId, Long processInstanceId) {
		String url = String.format("%s/containers/%s/images/processes/instances/%s",
				kieServerURL, containerId, processInstanceId);

		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(kieServerUser, kieServerPassword);
		headers.setAccept(Arrays.asList(MediaType.valueOf("image/svg+xml"), MediaType.ALL));

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<byte[]> response = restTemplate.exchange(
				url, HttpMethod.GET, new HttpEntity<Void>(headers), byte[].class);
		return response.getBody();
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
