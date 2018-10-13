/**
 * 
 */
package org.profeder.cipherSuite.processors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Provider.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.nifi.annotation.behavior.EventDriven;
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
@EventDriven
public class SymmetricCipherProcessor extends AbstractProcessor {
	
	private List <PropertyDescriptor> properties;
	private Set <Relationship> relationship;
	
	private static PropertyDescriptor ALGORITHM; 
	private static final PropertyDescriptor OPERATION = new PropertyDescriptor.Builder().name("Mode")
			.description("Specify the operation to be perform")
			.allowableValues(new HashSet<String>(Arrays.asList("Encript", "Decript")))
			.build();
	
	public static final Relationship OUTPUT = new Relationship.Builder()
	        .name("OUTPUT")
	        .description("Cipher output")
	        .build();
	
	private Cipher algo;
	private SecretKeySpec key;
	
	public static void initAlgos() {
		if(ALGORITHM == null) {
			Set <String> algos = new HashSet<String>();
			Provider[] providers = Security.getProviders();
			for(Provider provider : providers) {
				Set <Service> services = provider.getServices();
				for(Service service : services) {
					if(service.getType().equalsIgnoreCase(Cipher.class.getSimpleName())) {
						algos.add(service.getAlgorithm());
					}
				}
			}
			ALGORITHM = new PropertyDescriptor.Builder().name("Cipher algorithms").allowableValues(algos).build();
		}
	}
	
	@Override
	protected void init(ProcessorInitializationContext context) {
		super.init(context);
		
		properties = new ArrayList<PropertyDescriptor>();
		initAlgos();
		properties.add(ALGORITHM);
		properties.add(OPERATION);
		
		relationship = new HashSet<Relationship>();
		relationship.add(OUTPUT);
	}
	
	public Set<Relationship> getRelationships(){
		return relationship;
	}
	
	public List<PropertyDescriptor> getSupportedPropertyDescriptors(){
		return properties;
	}

	/* (non-Javadoc)
	 * @see org.apache.nifi.processor.AbstractProcessor#onTrigger(org.apache.nifi.processor.ProcessContext, org.apache.nifi.processor.ProcessSession)
	 */
	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
		final AtomicReference<byte []> msg = new AtomicReference<byte []>();
		FlowFile ff = session.get();
		String k;
		int mode;
		if(ff == null)
			return;
		session.read(ff, new InputStreamCallback() {
			
			@Override
			public void process(InputStream in) throws IOException {
				byte [] buffer = new byte[1024];
				ByteArrayOutputStream bais = new ByteArrayOutputStream();
				int len = -1;
				while((len = in.read(buffer)) > 1) {
					bais.write(buffer, 0, len);
				}
				msg.set(bais.toByteArray());
			}
		});
		if(ff.getAttribute("isKey") != null) {
			key = new SecretKeySpec(msg.get(), context.getProperty(ALGORITHM).getValue());
			session.remove(ff);
			return;
		}
		try {
			if(algo == null)
				algo = Cipher.getInstance(context.getProperty(ALGORITHM).getValue());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new ProcessException(e);
		}
		
		if("Encript".equals(context.getProperty(OPERATION).getValue())) 
			mode = Cipher.ENCRYPT_MODE;
		else
			if("Decript".equals(context.getProperty(OPERATION).getValue()))
				mode = Cipher.DECRYPT_MODE;
			else
				throw new ProcessException("Mode not defined");
		
		try {
			algo.init(mode, key);
			algo.update(msg.get());
			msg.set(algo.doFinal());
		} catch (InvalidKeyException e) {
			throw new ProcessException(e);
		} catch (IllegalBlockSizeException e) {
			throw new ProcessException(e);
		} catch (BadPaddingException e) {
			throw new ProcessException(e);
		}
		
		ff = session.write(ff, new OutputStreamCallback() {
			
			@Override
			public void process(OutputStream out) throws IOException {
				out.write(msg.get());
			}
		});

		session.transfer(ff, OUTPUT);
	}

}
