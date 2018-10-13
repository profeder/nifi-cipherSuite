package chiperSuite;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.profeder.cipherSuite.processors.KeyGeneratorProcessor;


public class KeyGeneratorProcessorTest {
	
	public void testOnTrigger()throws IOException{
		TestRunner runner = TestRunners.newTestRunner(new KeyGeneratorProcessor());
		runner.setProperty(KeyGeneratorProcessor.kLen, "128");
		runner.assertQueueEmpty();
		List<MockFlowFile> results = runner.getFlowFilesForRelationship(KeyGeneratorProcessor.KEYOUT);
		MockFlowFile result = results.get(0);
        String resultValue = new String(runner.getContentAsByteArray(result));
        System.out.println("Result: " + IOUtils.toString(runner.getContentAsByteArray(result)));
	}

}
