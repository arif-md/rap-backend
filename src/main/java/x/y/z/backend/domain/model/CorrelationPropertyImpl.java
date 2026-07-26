package x.y.z.backend.domain.model;

import org.kie.internal.process.CorrelationProperty;


public class CorrelationPropertyImpl implements CorrelationProperty<String>{

	private String name;
	private String type;
	private String value;
	
	public CorrelationPropertyImpl(){
		
	}
	public CorrelationPropertyImpl(String name, String type, String value){
	   this.name = name;
	   this.type = type;
	   this.value = value;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}
	
	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return type;
	}
	
	@Override
	public String getValue() {
		// TODO Auto-generated method stub
		return value;
	}
}
