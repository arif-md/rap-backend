package x.y.z.backend.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import x.y.z.backend.utils.KieClient;

@Component
@Scope("application")
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
	}
	
}
