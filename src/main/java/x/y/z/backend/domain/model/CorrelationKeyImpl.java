package x.y.z.backend.domain.model;

import java.util.ArrayList;
import java.util.List;

import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationProperty;


public class CorrelationKeyImpl implements CorrelationKey {

	private String name;
	private List<CorrelationProperty<String>> properties;
	
	public CorrelationKeyImpl(){
		
	}
	public CorrelationKeyImpl(String name){
		this.name = name;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public List<CorrelationProperty<?>> getProperties() {
		// TODO Auto-generated method stub
		return new ArrayList<CorrelationProperty<?>>(this.properties);
	}
	public void setProperties(List<CorrelationProperty<String>> properties) {
		this.properties = properties;
	}
	
	@Override	
	public String toExternalForm(){
		StringBuilder builder = new StringBuilder();
		//builder.append(name).append(" --> ");
		for(CorrelationProperty<String> prop : properties){
			builder.append(prop.getValue());
		}
		return builder.toString();		
	}
	
	
}
