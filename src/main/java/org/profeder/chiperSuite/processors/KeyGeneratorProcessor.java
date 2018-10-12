/**
 * 
 */
package org.profeder.chiperSuite.processors;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;

/**
 * @author fprofeti
 *
 */
@SideEffectFree
@Tags({"Key generator"})
@CapabilityDescription ("Generate random key for simmetric chiper")
public class KeyGeneratorProcessor extends AbstractProcessor{

	private static char [] alpha;
	private static final int startChar = 33;	// !
	private static final int endChar = 126;		// ~
	
	private static Set <String> availableLength;
	
	private List <PropertyDescriptor> properties;
	private Set <Relationship> relationship;
	
	public static final Relationship SUCCESS = new Relationship.Builder()
	        .name("SUCCESS")
	        .description("Succes relationship")
	        .build();
	
	protected void init(ProcessorInitializationContext context) {
		super.init(context);
		alpha = new char[endChar - startChar];
		for(int i = 0; i < endChar - startChar; i++) {
			alpha[i] = (char)(i + startChar);
		}
		properties = new ArrayList<PropertyDescriptor>();
		relationship = new HashSet<Relationship>();
		relationship.add(SUCCESS);
		
		availableLength = new HashSet<String>();
		
		availableLength.add("128");
		availableLength.add("256");
		
		PropertyDescriptor kLen = new PropertyDescriptor.Builder().name("key length")
				.description("Specify the output key length").dynamic(true).allowableValues(availableLength).build();
		properties.add(kLen);
	}
	
	public Set<Relationship> getRelationships(){
		return relationship;
	}
	
	public List<PropertyDescriptor> getSupportedPropertyDescriptors(){
		return properties;
	}
	
	private String calcolateKey() {
		int len = 128;
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < len; i++) {
			sb.append(alpha[(int)(Math.random()*alpha.length)]);
		}
		return sb.toString();
	}
	
	@Override
	public void onTrigger(ProcessContext arg0, ProcessSession session) throws ProcessException {
		FlowFile ff = session.create();
		final String key = calcolateKey();
		session.putAttribute(ff, "key", key);
		ff = session.write(ff, new OutputStreamCallback() {
			
			public void process(OutputStream out) throws IOException {
				out.write(key.getBytes());
				
			}
		});
		
		session.transfer(ff, SUCCESS);
	}
	
	

}
