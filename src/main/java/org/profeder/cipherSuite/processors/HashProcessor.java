/**
 * 
 */
package org.profeder.cipherSuite.processors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.nifi.annotation.behavior.EventDriven;
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
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;

/**
 * @author fprofeti
 *
 */
@SideEffectFree
@Tags({"Hash"})
@CapabilityDescription("Perform hashing algorith on input data")
@EventDriven
public class HashProcessor extends AbstractProcessor{
	
	private List <PropertyDescriptor> properties;
	private Set <Relationship> relationship;
	
	private static PropertyDescriptor ALGORITHM; 
	public static final Relationship HASH = new Relationship.Builder()
	        .name("HASH")
	        .description("Succes relationship")
	        .build();
	
	private MessageDigest algo;
	
	public static void initAlgos() {
		if(ALGORITHM == null) {
			Set <String> algos = new HashSet<String>();
			Provider[] providers = Security.getProviders();
			for(Provider provider : providers) {
				Set <Service> services = provider.getServices();
				for(Service service : services) {
					if(service.getType().equalsIgnoreCase(MessageDigest.class.getSimpleName())) {
						algos.add(service.getAlgorithm());
					}
				}
			}
			ALGORITHM = new PropertyDescriptor.Builder().name("Message digest algorithm").allowableValues(algos).build();
		}
	}
	
	@Override
	protected void init(ProcessorInitializationContext context) {
		super.init(context);
		
		properties = new ArrayList<PropertyDescriptor>();
		initAlgos();
		properties.add(ALGORITHM);
		
		relationship = new HashSet<Relationship>();
		relationship.add(HASH);
	}
	
	public Set<Relationship> getRelationships(){
		return relationship;
	}
	
	public List<PropertyDescriptor> getSupportedPropertyDescriptors(){
		return properties;
	}
	
	private byte [] performHash (byte [] input){
		algo.reset();
		algo.update(input);
		return algo.digest();
	}

	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
		final AtomicReference<byte []> hash = new AtomicReference<byte []>();
		FlowFile ff = session.get();
		if(ff == null)
			return;
		
		if(algo == null) {
			try {
				algo = MessageDigest.getInstance(context.getProperty(ALGORITHM).getValue());
			} catch (NoSuchAlgorithmException e) {
				throw new ProcessException(e);
			}
		}
		session.read(ff, new InputStreamCallback() {
			
			public void process(InputStream in) throws IOException {
				byte [] buffer = new byte[1024];
				ByteArrayOutputStream bais = new ByteArrayOutputStream();
				int len = -1;
				while((len = in.read(buffer)) > 1) {
					bais.write(buffer, 0, len);
				}
				hash.set(bais.toByteArray());
			}
		});
		
		byte [] tmp = performHash(hash.get());
		hash.set(tmp);
		
		ff = session.write(ff, new OutputStreamCallback() {
			
			public void process(OutputStream out) throws IOException {
				out.write(hash.get());
			}
		});
		
		session.transfer(ff, HASH);
	}

}
