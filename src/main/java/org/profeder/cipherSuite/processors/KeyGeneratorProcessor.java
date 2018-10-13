/**
 * 
 */
package org.profeder.cipherSuite.processors;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Provider.Service;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

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
	
	private List <PropertyDescriptor> properties;
	private Set <Relationship> relationship;
	
	public static final Relationship KEYOUT = new Relationship.Builder()
	        .name("KEYOUT")
	        .description("Generated key")
	        .build();
	
	private static PropertyDescriptor CIPHER;
	private KeyGenerator gen;
	
	public static void initAlgos() {
		if(CIPHER == null) {
			Set <String> algos = new HashSet<String>();
			Provider[] providers = Security.getProviders();
			for(Provider provider : providers) {
				Set <Service> services = provider.getServices();
				for(Service service : services) {
					if(service.getType().equalsIgnoreCase(KeyGenerator.class.getSimpleName())) {
						algos.add(service.getAlgorithm());
					}
				}
			}
			CIPHER = new PropertyDescriptor.Builder().name("Key generator").allowableValues(algos).build();
		}
	}
	
	protected void init(ProcessorInitializationContext context) {
		super.init(context);

		initAlgos();
		properties = new ArrayList<PropertyDescriptor>();
		relationship = new HashSet<Relationship>();
		relationship.add(KEYOUT);
		
		properties.add(CIPHER);
	}
	
	public Set<Relationship> getRelationships(){
		return relationship;
	}
	
	public List<PropertyDescriptor> getSupportedPropertyDescriptors(){
		return properties;
	}
	
	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
		final AtomicReference<byte []> key = new AtomicReference<byte []>();
		FlowFile ff = session.create();
		try {
			if(gen == null)
				gen = KeyGenerator.getInstance(context.getProperty(CIPHER).getValue());
		} catch (NoSuchAlgorithmException e) {
			throw new ProcessException(e);
		}
		
		SecretKey k = gen.generateKey();
		key.set(k.getEncoded());
		session.putAttribute(ff, "isKey", "");
		
		ff = session.write(ff, new OutputStreamCallback() {
			
			@Override
			public void process(OutputStream out) throws IOException {
				out.write(key.get());
			}
		});
		
		session.transfer(ff, KEYOUT);
	}
	
	

}
