package x.y.z.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationProperty;
import org.kie.server.client.ProcessServicesClient;

import x.y.z.backend.domain.model.CorrelationKeyImpl;
import x.y.z.backend.domain.model.CorrelationPropertyImpl;
import x.y.z.backend.domain.model.ProcessInfo;
import x.y.z.backend.domain.model.Task;
import x.y.z.backend.domain.model.TaskForm;
import x.y.z.backend.utils.KieClient;
import x.y.z.backend.domain.model.Constants;

public abstract class BaseController {

	protected CorrelationKey getCorrelationKey(String correlationKeyProp) {
		CorrelationKeyImpl key = new CorrelationKeyImpl(Constants.CORR_KEY_RAPTOR);			
		CorrelationPropertyImpl keyProp = new CorrelationPropertyImpl(Constants.CORR_KEY_PROP_APPNO,"java.lang.String",correlationKeyProp);
		List<CorrelationProperty<String>> keyProps = new ArrayList<CorrelationProperty<String>>();
		keyProps.add(keyProp);
		key.setProperties(keyProps);	
		return key;
	}

	protected Long startProcess(ProcessInfo processInfo, Boolean generateCorrelationKey,
			TaskForm form, String applicantId, Map<String, Object> processVars) {
		
		Map<String, Object> params = new HashMap<String, Object>();

		params.put(Constants.UNIVERSITY_ID, form.getUniversityId());
		params.put(Constants.APPLICANT_ID, applicantId.toString());
		params.put(Constants.APPLICATION_ID, form.getId());
		if(processVars !=null && processVars.size() > 0) {
			params.putAll(processVars);
		}
		ProcessServicesClient processServices = KieClient.getProcessServicesClient();
		Long processInsanceId = null;
		
		if(generateCorrelationKey) {
			CorrelationKey key = getCorrelationKey(form.getApplicationCode());		
			processInsanceId = processServices.startProcess(processInfo.getContainerId(), 
					processInfo.getProcessId(), key, params);
		} else {
			processInsanceId = processServices.startProcess(processInfo.getContainerId(), 
					processInfo.getProcessId(), params);
		}
		form.setProcessInstanceId(processInsanceId);
		return processInsanceId;
	}

}
