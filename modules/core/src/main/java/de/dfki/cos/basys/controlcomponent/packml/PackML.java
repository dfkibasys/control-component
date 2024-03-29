package de.dfki.cos.basys.controlcomponent.packml;

import de.dfki.cos.basys.controlcomponent.ExecutionMode;
import de.dfki.cos.basys.controlcomponent.ExecutionState;
import org.apache.commons.scxml2.EventBuilder;
import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.env.SimpleDispatcher;
import org.apache.commons.scxml2.env.SimpleSCXMLListener;
import org.apache.commons.scxml2.env.jexl.JexlEvaluator;
import org.apache.commons.scxml2.io.SCXMLReader;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.SCXML;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URL;

public class PackML {

	//protected final Logger LOGGER = LoggerFactory.getLogger(PackML.class.getName());
		
	private PackMLUnit unit = null;

	private boolean initialized = false;

	private SCXML scxml = null;
	private SCXMLExecutor exec = null;

	public PackML(PackMLUnit unit) {
		this.unit = unit;		
	}

	public void initialize() {
		if (!initialized) {
			URL scxmlResource = this.getClass().getResource("/de/dfki/cos/basys/controlcomponent/packml/packml.scxml");			
			// initialize states and state machine
			try {
				scxml = SCXMLReader.read(scxmlResource);

				// configure state machine and set loaded scxml
				exec = new SCXMLExecutor();
				exec.setEvaluator(new JexlEvaluator());
				exec.setStateMachine(scxml);
				exec.setEventdispatcher(new SimpleDispatcher());

				// add script variables to scope
				exec.addListener(scxml, new SimpleSCXMLListener());
				exec.getRootContext().set("unit", unit);
				//exec.getRootContext().set("packml", this);
				exec.getRootContext().set("Mode", ExecutionMode.class);
				exec.getRootContext().set("State", ExecutionState.class);

				//exec.go();
				exec.run();

				initialized = true;
			} catch (IOException | ModelException | XMLStreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void dispose() {
		if (initialized) {
			raiseLifecycleEvent("dispose");
			initialized = false;
		}
	}

	public void raiseLifecycleEvent(String event) {
		unit.LOGGER.trace("raiseLifecycleEvent: " + event);
		if (!initialized) {
			unit.LOGGER.warn("PackML automaton not yet initialized, skipping event: " + event);
			return;
		}
		exec.addEvent(new EventBuilder("lifecycle.events." + event, TriggerEvent.SIGNAL_EVENT).build());
//		try {
//			exec.triggerEvent(new EventBuilder("lifecycle.events." + event, TriggerEvent.SIGNAL_EVENT).build());			
//		} catch (ModelException e) {
//			e.printStackTrace();
//		}
	}

}
