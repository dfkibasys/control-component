package de.dfki.cos.basys.controlcomponent.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.api.nodes.ObjectNode;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.model.nodes.objects.FolderNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dfki.cos.basys.common.component.Component;
import de.dfki.cos.basys.common.component.ServiceProvider;
import de.dfki.cos.basys.common.component.StringConstants;
import de.dfki.cos.basys.common.component.ComponentContext;
import de.dfki.cos.basys.common.component.ComponentException;
import de.dfki.cos.basys.controlcomponent.CommandInterface;
import de.dfki.cos.basys.controlcomponent.ComponentOrderStatus;
import de.dfki.cos.basys.controlcomponent.ControlComponentInfo;
import de.dfki.cos.basys.controlcomponent.ErrorStatus;
import de.dfki.cos.basys.controlcomponent.ExecutionCommand;
import de.dfki.cos.basys.controlcomponent.ExecutionMode;
import de.dfki.cos.basys.controlcomponent.ExecutionState;
import de.dfki.cos.basys.controlcomponent.OccupationCommand;
import de.dfki.cos.basys.controlcomponent.OccupationState;
import de.dfki.cos.basys.controlcomponent.OccupationStatus;
import de.dfki.cos.basys.controlcomponent.OperationMode;
import de.dfki.cos.basys.controlcomponent.OperationModeInfo;
import de.dfki.cos.basys.controlcomponent.OrderStatus;
import de.dfki.cos.basys.controlcomponent.ParameterDirection;
import de.dfki.cos.basys.controlcomponent.ParameterInfo;
import de.dfki.cos.basys.controlcomponent.SharedParameterSpace;
import de.dfki.cos.basys.controlcomponent.StatusInterface;
import de.dfki.cos.basys.controlcomponent.client.opcua.nodes.ControlComponentNode;
import de.dfki.cos.basys.controlcomponent.client.opcua.nodes.ControlComponentOperationsNode;
import de.dfki.cos.basys.controlcomponent.client.opcua.nodes.ControlComponentStatusNode;
import de.dfki.cos.basys.controlcomponent.client.opcua.util.NodeIds;
import de.dfki.cos.basys.controlcomponent.client.opcua.util.OpcUaChannel;
import de.dfki.cos.basys.controlcomponent.client.opcua.util.OpcUaException;
import de.dfki.cos.basys.controlcomponent.packml.PackMLWaitStatesHandler;
import de.dfki.cos.basys.controlcomponent.util.Strings;

public class ControlComponentClientImpl implements ControlComponentClient, ServiceProvider<ControlComponentClient> {

	public final Logger LOGGER = LoggerFactory.getLogger(this.getClass().getSimpleName());

	private boolean connected = false;
	Properties config;
	OpcUaChannel channel;

	PackMLWaitStatesHandler executionStateChangedHandler = null;

	private ControlComponentNode cc = null;
	private ControlComponentStatusNode status = null;
	private ControlComponentOperationsNode operations = null;
	private NodeId operationsNodeId = null;
	private FolderNode variables = null;

	public ControlComponentClientImpl(Properties config, PackMLWaitStatesHandler handler) {
		this.config = config;
		this.channel = new OpcUaChannel();
		this.executionStateChangedHandler = handler;
	}

	@Override
	public boolean connect(ComponentContext context, String connectionString) {
		LOGGER.info("connect");
		try {
			channel.open(connectionString);
			// channel.subscribeToValue(nodeIds.statusExecutionMode,
			// this::onExecutionModeChanged);
			channel.subscribeToValue(status.getExecutionStateNode().get().getNodeId().get(), this::onExecutionStateChanged);
			// channel.subscribeToValue(nodeIds.statusOccupationState,
			// this::onOccupationLevelChanged);
			connected = true;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return connected;
	}

	@Override
	public void disconnect() {
		LOGGER.info("disconnect");
		try {
			channel.close();
			connected = false;
		} catch (OpcUaException e) {
			LOGGER.error(e.getMessage());
		}
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public ControlComponentClient getService() {
		return this;
	}

	@Override
	public int getErrorCode() {
		LOGGER.info("getErrorCode");
		int result = Integer.MIN_VALUE;
		try {
			result = status.getErrorCode().get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getErrorMessage() {
		LOGGER.info("getErrorMessage");
		String result = null;
		try {
			result = status.getErrorMessage().get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public ErrorStatus getErrorStatus() {
		LOGGER.info("getErrorStatus");
		int errorCode = getErrorCode();
		String errorMessage = getErrorMessage();
		return new ErrorStatus.Builder().errorCode(errorCode).errorMessage(errorMessage).build();
	}

	@Override
	public OccupationState getOccupationState() {
		LOGGER.info("getOccupationLevel");
		OccupationState result = null;
		try {
			result = OccupationState.get(status.getOccupationState().get());
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
		}
		return result;
	}

	@Override
	public String getOccupierId() {
		LOGGER.info("getOccupierId");
		String result = null;
		try {
			result = status.getOccupierId().get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
		}
		return result;
	}

	@Override
	public OccupationStatus getOccupationStatus() {
		LOGGER.info("getOccupationStatus");
		OccupationState level = getOccupationState();
		String occupierId = getOccupierId();
		return new OccupationStatus.Builder().level(level).occupierId(occupierId).build();
	}

	@Override
	public OperationModeInfo getOperationMode() {
		LOGGER.info("getOperationMode");
		OperationModeInfo result = null;
		try {
			String opmode = status.getOperationMode().get();
			// TODO Get data from AAS
			result = new OperationModeInfo.Builder().name(opmode).build();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
		}
		return result;
	}

	@Override
	public List<OperationModeInfo> getOperationModes() {
		LOGGER.info("getOperationModes");
		// TODO Get data from AAS
		return null;
	}

	@Override
	public String getWorkState() {
		LOGGER.info("getWorkState");
		String result = null;
		try {
			result = status.getWorkState().get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
		}
		return result;
	}

	@Override
	public ExecutionMode getExecutionMode() {
		LOGGER.info("getExecutionMode");
		ExecutionMode result = null;
		try {
			result = ExecutionMode.get(status.getExecutionMode().get());
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
		}
		return result;
	}

	@Override
	public ExecutionState getExecutionState() {
		LOGGER.info("getExecutionState");
		ExecutionState result = null;
		try {
			result = ExecutionState.get(status.getExecutionState().get());
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
		}
		return result;
	}

	@Override
	public ComponentOrderStatus occupy(OccupationCommand command, String senderId) {
		LOGGER.info("OccupationCommand [" + command + "] (senderId=" + senderId + ")");
		
		CompletableFuture<NodeId> cf = null;
		switch (command) {
		case FREE:
			cf = operations.getFreeMethodNodeId();
			break;
		case OCCUPY:
			cf = operations.getOccupyMethodNodeId();			
			break;
		case PRIO:
			cf = operations.getPrioMethodNodeId();			
			break;
		default:
			return new ComponentOrderStatus.Builder().status(OrderStatus.REJECTED).message("unknown occupation command").build();			
		}
		
		try {
			return channel.callMethod(operationsNodeId, cf.get(), senderId).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
			return new ComponentOrderStatus.Builder().status(OrderStatus.REJECTED).message(e.getMessage()).build();
		}
	}

	@Override
	public ComponentOrderStatus free(String senderId) {
		return occupy(OccupationCommand.FREE, senderId);
	}

	@Override
	public ComponentOrderStatus occupy(String senderId) {
		return occupy(OccupationCommand.OCCUPY, senderId);
	}

	@Override
	public ComponentOrderStatus occupyPriority(String senderId) {
		return occupy(OccupationCommand.PRIO, senderId);
	}

//	@Override
//	public ComponentOrderStatus occupyLocal(String senderId) {
//		return occupy(OccupationState.LOCAL, senderId);
//	}

	@Override
	public ComponentOrderStatus registerOperationMode(OperationMode opmode, String senderId) {
		return new ComponentOrderStatus.Builder().status(OrderStatus.REJECTED).message("not (yet) possible via remote")
				.build();
	}

	@Override
	public ComponentOrderStatus unregisterOperationMode(String opmode, String senderId) {
		return new ComponentOrderStatus.Builder().status(OrderStatus.REJECTED).message("not (yet) possible via remote")
				.build();
	}

	@Override
	public ComponentOrderStatus setOperationMode(String mode, String senderId) {
		LOGGER.info("setOperationMode [" + mode + "] (senderId=" + senderId + ")");
				
		CompletableFuture<NodeId> cf = operations.getComponent(new QualifiedName(channel.getNsIndex(), mode)).thenApply(UaNode.class::cast).thenCompose(UaNode::getNodeId);;
		
		try {
			return channel.callMethod(operationsNodeId, cf.get(), senderId).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
			return new ComponentOrderStatus.Builder().status(OrderStatus.REJECTED).message(e.getMessage()).build();
		}
	}

	@Override
	public ComponentOrderStatus setExecutionMode(ExecutionMode mode, String senderId) {
		LOGGER.info("ExecutionMode [" + mode + "] (senderId=" + senderId + ")");
		
		CompletableFuture<NodeId> cf = null;
		switch (mode) {
		case AUTO:
			cf = operations.getAutoMethodNodeId();
			break;
		case SEMIAUTO:
			cf = operations.getSemiAutoMethodNodeId();			
			break;
		case MANUAL:
			cf = operations.getManualMethodNodeId();			
			break;
		case SIMULATION:
			cf = operations.getSimulateMethodNodeId();			
			break;
		default:
			return new ComponentOrderStatus.Builder().status(OrderStatus.REJECTED).message("unknown execution mode").build();			
		}
		
		try {
			return channel.callMethod(operationsNodeId, cf.get(), senderId).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
			return new ComponentOrderStatus.Builder().status(OrderStatus.REJECTED).message(e.getMessage()).build();
		}
	}

	@Override
	public ComponentOrderStatus raiseExecutionCommand(ExecutionCommand command, String senderId) {
		LOGGER.info("ExecutionCommand [" + command + "] (senderId=" + senderId + ")");
		
		CompletableFuture<NodeId> cf = null;
		switch (command) {
		case RESET:
			cf = operations.getResetMethodNodeId();
			break;
		case START:
			cf = operations.getStartMethodNodeId();			
			break;
		case STOP:
			cf = operations.getStopMethodNodeId();			
			break;
		case HOLD:
			cf = operations.getHoldMethodNodeId();			
			break;
		case UNHOLD:
			cf = operations.getUnholdMethodNodeId();			
			break;
		case SUSPEND:
			cf = operations.getSuspendMethodNodeId();			
			break;
		case UNSUSPEND:
			cf = operations.getUnsuspendMethodNodeId();			
			break;
		case ABORT:
			cf = operations.getAbortMethodNodeId();			
			break;
		case CLEAR:
			cf = operations.getClearMethodNodeId();			
			break;
		default:
			return new ComponentOrderStatus.Builder().status(OrderStatus.REJECTED).message("unknown execution command").build();			
		}
		
		try {
			return channel.callMethod(operationsNodeId, cf.get(), senderId).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage());
			return new ComponentOrderStatus.Builder().status(OrderStatus.REJECTED).message(e.getMessage()).build();
		}
		

	}

	@Override
	public ComponentOrderStatus reset(String senderId) {
		return raiseExecutionCommand(ExecutionCommand.RESET, senderId);
	}

	@Override
	public ComponentOrderStatus start(String senderId) {
		return raiseExecutionCommand(ExecutionCommand.START, senderId);
	}

	@Override
	public ComponentOrderStatus stop(String senderId) {
		return raiseExecutionCommand(ExecutionCommand.STOP, senderId);
	}

	@Override
	public ComponentOrderStatus hold(String senderId) {
		return raiseExecutionCommand(ExecutionCommand.HOLD, senderId);
	}

	@Override
	public ComponentOrderStatus unhold(String senderId) {
		return raiseExecutionCommand(ExecutionCommand.UNHOLD, senderId);
	}

	@Override
	public ComponentOrderStatus suspend(String senderId) {
		return raiseExecutionCommand(ExecutionCommand.SUSPEND, senderId);
	}

	@Override
	public ComponentOrderStatus unsuspend(String senderId) {
		return raiseExecutionCommand(ExecutionCommand.UNSUSPEND, senderId);
	}

	@Override
	public ComponentOrderStatus abort(String senderId) {
		return raiseExecutionCommand(ExecutionCommand.ABORT, senderId);
	}

	@Override
	public ComponentOrderStatus clear(String senderId) {
		return raiseExecutionCommand(ExecutionCommand.CLEAR, senderId);
	}

	protected void onExecutionModeChanged(UaMonitoredItem item, DataValue value) {
		LOGGER.info("onExecutionModeChanged: item={}, value={}", item.getReadValueId().getNodeId(), value.getValue());

		// System.out.println("subscription value received: item=" +
		// item.getReadValueId().getNodeId() + ", value="
		// + value.getValue());

		ExecutionMode val = ExecutionMode.get((String) value.getValue().getValue());
		LOGGER.info("NEW ExecutionMode: {}", val.toString());

	}

	protected void onExecutionStateChanged(UaMonitoredItem item, DataValue value) {
		LOGGER.info("onExecutionStateChanged: item={}, value={}", item.getReadValueId().getNodeId(), value.getValue());

		// System.out.println("subscription value received: item=" +
		// item.getReadValueId().getNodeId() + ", value="
		// + value.getValue());

		ExecutionState val = ExecutionState.get((String) value.getValue().getValue());
		switch (val) {
		case IDLE:
			executionStateChangedHandler.onIdle();
			break;
		case COMPLETE:
			executionStateChangedHandler.onComplete();
			break;
		case STOPPED:
			executionStateChangedHandler.onStopped();
			break;
		case HELD:
			executionStateChangedHandler.onHeld();
			break;
		case SUSPENDED:
			executionStateChangedHandler.onSuspended();
			break;
		case ABORTED:
			executionStateChangedHandler.onAborted();
			break;

		default:
			break;
		}

		LOGGER.info("NEW ExecutionState: {}", val.toString());

	}

	protected void onOccupationLevelChanged(UaMonitoredItem item, DataValue value) {
		LOGGER.info("onOccupationLevelChanged: item={}, value={}", item.getReadValueId().getNodeId(), value.getValue());

		// System.out.println("subscription value received: item=" +
		// item.getReadValueId().getNodeId() + ", value="
		// + value.getValue());

		OccupationState val = OccupationState.get((String) value.getValue().getValue());
		LOGGER.info("NEW OccupationLevel: {}", val.toString());

	}

	// SharedParameterSpace

	@Override
	public List<ParameterInfo> getParameters() throws ComponentException {
		List<Node> nodes = channel.browseNode(nodeIds.folderVariables);
		List<ParameterInfo> result = new ArrayList<>(nodes.size());

		for (Node node : nodes) {

			ParameterInfo p = null;
			try {
				p = getParameter(node.getBrowseName().get().getName());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (p != null)
				result.add(p);
		}

		return result;
	}

	@Override
	public ParameterInfo getParameter(String name) throws ComponentException {
		try {
			NodeId nodeId = nodeIds.newHierarchicalNodeId(nodeIds.folderVariables, name);
			VariableNode var = channel.getClient().getAddressSpace().getVariableNode(nodeId).get();
			Object value = var.getValue().get();
			EnumSet<AccessLevel> accessLevel = AccessLevel.fromMask(var.getUserAccessLevel().get());
			ParameterDirection direction = ParameterDirection.OUT;
			if (accessLevel.contains(AccessLevel.CurrentWrite))
				direction = ParameterDirection.IN;

			ReadValueId rvid = new ReadValueId(nodeId, AttributeId.DataType.uid(), null, QualifiedName.NULL_VALUE);
			ReadResponse resp = channel.getClient().read(0, TimestampsToReturn.Both, Collections.singletonList(rvid))
					.get();
			UaNode datatypeNode = channel.getClient().getAddressSpace()
					.getNodeInstance((NodeId) resp.getResults()[0].getValue().getValue()).get();
			String typename = datatypeNode.getBrowseName().get().getName();

			ParameterInfo parameter = new ParameterInfo.Builder().name(name).value(value).type(typename)
					.access(direction).build();
			return parameter;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object getParameterValue(String name) throws ComponentException {
		NodeId nodeId = nodeIds.newHierarchicalNodeId(nodeIds.folderVariables, name);
		try {
			return channel.readValue(nodeId);
		} catch (OpcUaException e) {
			e.printStackTrace();
			throw new ComponentException(e);
		}
	}

	@Override
	public void setParameterValue(String name, Object value) throws ComponentException {
		NodeId nodeId = nodeIds.newHierarchicalNodeId(nodeIds.folderVariables, name);
		try {
			StatusCode status = channel.writeValue(nodeId, value);
		} catch (OpcUaException e) {
			e.printStackTrace();
			throw new ComponentException(e);
		}
	}

}