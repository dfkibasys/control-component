package de.dfki.cos.basys.controlcomponent.impl;

import de.dfki.cos.basys.controlcomponent.SimulationConfiguration;
import de.dfki.cos.basys.controlcomponent.packml.PackMLActiveStatesHandler;

import java.util.concurrent.TimeUnit;

public class SimulatedStatesHandler implements PackMLActiveStatesHandler {

	private BaseControlComponent component;
	private SimulationConfiguration config;
	private double speedFactor = 1.0;
	
	public SimulatedStatesHandler(BaseControlComponent component) {
		this.component = component;
		//this.config = component.getConfig().getSimulationConfiguration();
	}

	private void sleep(long millis) {
		try {
			long duration = (long) (speedFactor * millis);
			TimeUnit.MILLISECONDS.sleep(duration);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void onResetting() {
		sleep(config.getOnResettingDuration());
	}

	@Override
	public void onStarting() {
		sleep(config.getOnStartingDuration());
	}

	@Override
	public void onExecute() {
		if (config.getOnExecuteDuration() > 0) {
			sleep(config.getOnExecuteDuration());
		} else {
			//TODO: respect initialSuspendOnExecute flag
			//TODO: we simulate a sensor device: busywait4ever and generate predefined sensor events with a certain degree of freedom
		}
	}

	@Override
	public void onCompleting() {
		sleep(config.getOnCompletingDuration());
		if (config.getOnCompletingVariables().size() > 0) {
			//component.handleCapabilityResponse(ResponseStatus.OK, config.getOnCompletingStatusCode(), config.getOnCompletingVariables());					
		} else {
			//component.handleCapabilityResponse(ResponseStatus.OK, config.getOnCompletingStatusCode());
		}
	}

	@Override
	public void onHolding() {
		sleep(config.getOnHoldingDuration());
	}

	@Override
	public void onUnholding() {
		sleep(config.getOnUnholdingDuration());
	}

	@Override
	public void onSuspending() {
		sleep(config.getOnSuspendingDuration());
	}

	@Override
	public void onUnsuspending() {
		sleep(config.getOnUnsuspendingDuration());
	}

	@Override
	public void onAborting() {
		sleep(config.getOnAbortingDuration());
	}

	@Override
	public void onClearing() {
		sleep(config.getOnClearingDuration());
	}

	@Override
	public void onStopping() {
		sleep(config.getOnStoppingDuration());
		//component.handleCapabilityResponse(ResponseStatus.NOT_OK, config.getOnStoppingStatusCode());
	}

}
