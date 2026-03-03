package de.einwesen.heimklangwelle.test.manual;

import java.util.ArrayList;
import java.util.Arrays;

import de.einwesen.heimklangwelle.HeimklangNetworkAdressFactoryImpl;

public class ListNetworkInterfaces implements ManualTestCase {

	
	
	@Override
	public void runTest() throws Exception {
		
		// Collect output to nopt mix with logging
		ArrayList<String> outputLines = new ArrayList<>(); 
		
		HeimklangNetworkAdressFactoryImpl naf = new HeimklangNetworkAdressFactoryImpl();
		naf.getNetworkInterfacesByDefault().forEachRemaining((ni) -> {
			
			try {
				final boolean[] usableInterface = naf.isUsableNetworkInterface2(ni);				
				outputLines.add(Arrays.toString(usableInterface) +  "|" + ni.getDisplayName() + "|" + ni.getName());
			} catch (Exception e) {
				outputLines.add("ERROR|" + ni.getDisplayName() + "|" + ni.getName());
			}
			
			ni.getInetAddresses().asIterator().forEachRemaining((addr) -> {
				final boolean usableAdress = naf.isUsableAddressByDefault(ni, addr);
				outputLines.add("     |" + usableAdress + "|" + addr);
			});			
		});
		
		System.out.println("Devices returned by default implementation:");
		for (String output : outputLines) {
			System.out.println(output);
		}
	}

	public static void main(String[] args) throws Exception {
		ListNetworkInterfaces test = new ListNetworkInterfaces();
		test.runTest();		
	}	
	
}
