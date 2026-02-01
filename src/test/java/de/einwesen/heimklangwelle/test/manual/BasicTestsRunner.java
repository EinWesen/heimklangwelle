package de.einwesen.heimklangwelle.test.manual;

import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;

public class BasicTestsRunner {
	
	public final static Class<?>[] TESTCASES = new Class<?>[] {
		OsDetection.class,
		MpvBasics.class
	};
		
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		System.out.println("List of testcases\n");
		for (int i=0;i< TESTCASES.length;i++) {			
			System.out.println("[%d] %s".formatted(Integer.valueOf(i), TESTCASES[i].getSimpleName()));
		}
		
		System.out.println("\nChoose a testcase:");
		
        try (final Scanner s = new Scanner(System.in)) {
        	
        	int index = s.nextInt();
        	s.close();
        	
        	ManualTestCase testCase = (ManualTestCase)TESTCASES[index].getConstructor(new Class[0]).newInstance(new Object[0]);
        	
        	final String header = "\nRunning %s".formatted(testCase.getClass().getName());
        	System.out.println(header);
        	System.out.println("-".repeat(header.length()));
        	try {
        		testCase.runTest();
        		System.out.println("-".repeat(header.length()));
        	} catch (Throwable t) {
        		System.out.println("-".repeat(header.length()));
        		System.out.println("Testcase throw an exception!");
        		t.printStackTrace(System.out);
        	}        	
        };

	}
}
