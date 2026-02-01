package de.einwesen.heimklangwelle.test.manual;

import de.einwesen.heimklangwelle.HeimklangStation;

public class OsDetection implements ManualTestCase {

	@Override
	public void runTest() throws Exception {
		System.out.println("os.name  : " +  System.getProperty("os.name"));
		System.out.println("IsWindows: " + HeimklangStation.isOnWindows());
	}
	
	public static void main(String[] args) throws Exception {
		new OsDetection().runTest();
	}

}
