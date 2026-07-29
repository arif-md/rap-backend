package x.y.z.backend.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kie.server.api.KieServerConstants;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import x.y.z.backend.utils.KieClient;

@Component
public class ApplicationAttributes implements InitializingBean{

    private final Log log = LogFactory.getLog(getClass());
		
    @Value("${env.kieServerURL:}")
    private String kieServerURL;

    @Value("${env.kieServerUser:}")
    private String kieServerUser;

    @Value("${env.kieServerPwd:}")
    private String kieServerPwd;
	
    @Value("${latestPidVer.rec.newApp:}")
    private String latestPidVerRecNewApp;

	public String getKieServerURL() {
		return kieServerURL;
	}
	public String getKieServerUser() {
		return kieServerUser;
	}

	public void afterPropertiesSet() throws Exception {
       KieClient.setKieServerURL(kieServerURL);
       KieClient.setKieServerUser(kieServerUser);
       KieClient.setKieServerPassword(kieServerPwd);
       KieClient.setLatestPidVerRecNewApp(latestPidVerRecNewApp);

       // kie-server-client only sends the "user=" query param (letting task queries filter
       // by an arbitrary business user instead of the shared kieServerUser REST principal)
       // when this system property is set - the processes service already sets it server-side
       // (see processes/application.properties), but the client (this app) also needs it, and
       // must be set before the first KieClient.get*ServicesClient() call, since
       // AbstractKieServicesClientImpl reads it once into a static final field at class-load time.
       System.setProperty(KieServerConstants.CFG_BYPASS_AUTH_USER, "true");
	}
	
}
