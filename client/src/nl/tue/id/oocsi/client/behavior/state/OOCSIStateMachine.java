package nl.tue.id.oocsi.client.behavior.state;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nl.tue.id.oocsi.client.protocol.Handler;

/**
 * A simple finite state machine for use with OOCSI
 * 
 * API and logic was inspired by the Processing StateMachine library: https://github.com/atduskgreg/Processing-FSM and
 * the AlphaBeta FSM library for Arduino: http://www.arduino.cc/playground/Code/FiniteStateMachine
 * 
 * @author matsfunk
 */
public class OOCSIStateMachine {

	private String currentState;
	private Map<String, State> states = new HashMap<String, State>();
	private List<Transition> transitions = new LinkedList<Transition>();

	/**
	 * add a new state to the state machine by specifying its name, and optional handlers for this new state's enter and
	 * exit events, and the execute handler that will be used while state is on
	 * 
	 * @param name
	 * @param enter
	 * @param execute
	 * @param exit
	 * @return the new state
	 */
	public State addState(String name, Handler enter, Handler execute, Handler exit) {
		State state = new State(enter, execute, exit);
		states.put(name, state);
		return state;
	}

	/**
	 * triggers the execute handler of the current state of this state machine
	 * 
	 */
	public void execute() {
		// trigger execute action in current transition
		if (currentState != null && states.containsKey(currentState)) {
			states.get(currentState).execute();
		}
	}

	/**
	 * check if this state machine is currently in the given state
	 * 
	 * @param state
	 * @return
	 */
	public boolean isInState(String state) {
		return state != null && state.equals(currentState);
	}

	/**
	 * get the current state of this state machine; might be null
	 * 
	 * @return
	 */
	public String get() {
		return currentState;
	}

	/**
	 * get the state with the given name
	 * 
	 * @param name
	 * @return
	 */
	public State get(String name) {
		return states.get(name);
	}

	/**
	 * set the state with the given name as the new current state; will trigger enter and exit handlers respectively
	 * 
	 * @param newState
	 */
	public void set(String newState) {
		if (checkTransition(newState, newState)) {
			if (currentState != null && states.containsKey(currentState)) {
				// trigger exit action
				states.get(currentState).exit();
			}

			// set the new state
			currentState = newState;

			if (currentState != null && states.containsKey(currentState)) {
				// trigger enter action
				states.get(currentState).enter();
			}
		}
	}

	/**
	 * add a new transition between the source and the destination state
	 * 
	 * @param source
	 * @param destination
	 */
	public void addTransition(State source, State destination) {
		transitions.add(new Transition(source, destination));
	}

	/**
	 * check whether the desired transition from source to destination is valid for this state machine; if no
	 * transitions are given, then every transition is valid.
	 * 
	 * @param source
	 * @param destination
	 * @return
	 */
	private boolean checkTransition(String source, String destination) {

		State currentState = states.get(source);
		State newState = states.get(destination);

		if (!transitions.isEmpty()) {
			for (Transition transition : transitions) {
				if (transition.accept(currentState, newState)) {
					return true;
				}
			}
			return false;
		} else {
			// if no transitions are defined, all transitions are possible
			return true;
		}
	}

	/**
	 * internal state representation
	 * 
	 */
	public class State {

		private Handler enterAction;
		private Handler executeAction;
		private Handler exitAction;

		/**
		 * constructor that sets the given handlers for enter, execute, and exit
		 * 
		 * @param enter
		 * @param execute
		 * @param exit
		 */
		public State(Handler enter, Handler execute, Handler exit) {
			this.enterAction = enter;
			this.executeAction = execute;
			this.exitAction = exit;
		}

		/**
		 * trigger the enter handler, if it is given and not null
		 * 
		 */
		public void enter() {
			if (enterAction != null) {
				enterAction.receive("StateMachine", Collections.<String, Object> emptyMap(),
						System.currentTimeMillis(), "", "enter state");
			}
		}

		/**
		 * set the enter handler (any time after creation of this state)
		 * 
		 * @param handler
		 */
		public void setEnter(Handler handler) {
			enterAction = handler;
		}

		/**
		 * trigger the execute handler, if it is given and not null
		 * 
		 */
		public void execute() {
			if (executeAction != null) {
				executeAction.receive("StateMachine", Collections.<String, Object> emptyMap(),
						System.currentTimeMillis(), "", "execute state");
			}
		}

		/**
		 * set the execute handler (any time after creation of this state)
		 * 
		 * @param handler
		 */
		public void setExecute(Handler handler) {
			executeAction = handler;
		}

		/**
		 * trigger the exit handler, if it is given and not null
		 * 
		 */
		public void exit() {
			if (exitAction != null) {
				exitAction.receive("StateMachine", Collections.<String, Object> emptyMap(), System.currentTimeMillis(),
						"", "exit state");
			}
		}

		/**
		 * set the exit handler (any time after creation of this state)
		 * 
		 * @param handler
		 */
		public void setExit(Handler handler) {
			exitAction = handler;
		}
	}

	/**
	 * internal transition representation
	 *
	 */
	public class Transition {

		private State source;
		private State destination;

		/**
		 * constructor that sets the given source and destination state of this new transition
		 * 
		 * @param from
		 * @param to
		 */
		public Transition(State from, State to) {
			this.source = from;
			this.destination = to;
		}

		/**
		 * check if the given source and destination state match this transition
		 * 
		 * @param from
		 * @param to
		 * @return
		 */
		public boolean accept(State from, State to) {
			return from == source && to == destination;
		}
	}
}
