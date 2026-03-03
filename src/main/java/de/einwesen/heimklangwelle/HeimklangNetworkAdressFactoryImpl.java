package de.einwesen.heimklangwelle;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Iterator;
import java.util.Locale;

import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.spi.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeimklangNetworkAdressFactoryImpl extends NetworkAddressFactoryImpl {
	private final static Logger LOGGER = LoggerFactory.getLogger(HeimklangNetworkAdressFactoryImpl.class);
	
	public HeimklangNetworkAdressFactoryImpl() throws InitializationException {
		super();
	}

	/**
	 * @param streamListenPort
	 * @param multicastResponsePort
	 * @throws InitializationException
	 */
	public HeimklangNetworkAdressFactoryImpl(int streamListenPort, int multicastResponsePort)
			throws InitializationException {
		super(streamListenPort, multicastResponsePort);
	}

	@Override
	protected boolean isUsableNetworkInterface(NetworkInterface iface) throws Exception {

		if (super.isUsableNetworkInterface(iface)) {
			return _isUsableNetworkInterfaceByHeimklang(iface);
		} else {
			return false;
		}

	}
	
	private boolean _isUsableNetworkInterfaceByHeimklang(NetworkInterface iface) throws Exception {
		// I don't know if this would be correct inside vm servers, but for now i assume this should be like this by default 
		if (iface.getDisplayName().toLowerCase(Locale.ENGLISH).startsWith("hyper-v virtual")) {
			LOGGER.trace("Skipping network interface (Virtual Box): {}", iface.getDisplayName());
			return false;
		}
		return true;
	}

	/**
	 * Return the default and and HHeimklang answer to that question
	 * 
	 * @param iface
	 * @return
	 * @throws Exception
	 */
	public boolean[] isUsableNetworkInterface2(NetworkInterface iface) throws Exception {
		final boolean byDefault = super.isUsableNetworkInterface(iface);
		return new boolean[] {
			byDefault,
			byDefault && this._isUsableNetworkInterfaceByHeimklang(iface)
		};
	}

	public boolean isUsableAddressByDefault(NetworkInterface networkInterface, InetAddress address) {
		return super.isUsableAddress(networkInterface, address);
	}

	public Iterator<NetworkInterface> getNetworkInterfacesByDefault() {
		// the list of my own instance would be fitered already
		return new NetworkAddressFactoryImpl().getNetworkInterfaces();
	}
	
}
