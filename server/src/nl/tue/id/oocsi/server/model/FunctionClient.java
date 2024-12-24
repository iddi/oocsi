package nl.tue.id.oocsi.server.model;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.udojava.evalex.AbstractFunction;
import com.udojava.evalex.AbstractLazyFunction;
import com.udojava.evalex.Expression;
import com.udojava.evalex.Expression.LazyNumber;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Message;

public class FunctionClient extends Client {

	private String functionString;
	private Client delegate;

	// reference: https://github.com/uklimaschewski/EvalEx
	private List<String> filterExpression = new LinkedList<String>();
	private List<String> transformExpression = new LinkedList<String>();

	private WindowFunction sumFct = new SumOverWindowFunction();
	private WindowFunction meanFct = new MeanOverWindowFunction();
	private WindowFunction stdevFct = new StandardDeviationOverWindowFunction();
	private WindowFunction minFct = new MinOverWindowFunction();
	private WindowFunction maxFct = new MaxOverWindowFunction();

	public FunctionClient(Client delegateClient, String token, String functionString, ChangeListener presence) {
		super(token, presence);
		this.functionString = functionString;
		this.delegate = delegateClient;

		initFunctions(functionString);
	}

	private void initFunctions(String functionString) {
		final Pattern filterPattern = Pattern.compile("filter\\((.*)\\)");
		final Pattern transformPattern = Pattern.compile("transform\\((.*)\\)");

		// functions are separated by ';'
		String[] functions = functionString.split(";");
		for (String fct : functions) {
			Matcher filterMatcher = filterPattern.matcher(fct);
			if (filterMatcher.find()) {
				// init filter expression
				filterExpression.add(filterMatcher.group(1));
				continue;
			}

			Matcher transformMatcher = transformPattern.matcher(fct);
			if (transformMatcher.find()) {
				// init transform expression
				transformExpression.add(transformMatcher.group());
				continue;
			}
		}
	}

	@Override
	public synchronized boolean send(Message message) {

		// filtering checks
		for (String expression : filterExpression) {
			// apply expression
			try {
				final Expression e = loadExpression(expression, message, true);
				if (expression.contains("sum(") || expression.contains("SUM("))
					e.addFunction(sumFct);
				if (expression.contains("mean(") || expression.contains("MEAN("))
					e.addFunction(meanFct);
				if (expression.contains("stdev(") || expression.contains("STDEV("))
					e.addFunction(stdevFct);
				if (expression.contains("emin(") || expression.contains("EMIN("))
					e.addFunction(minFct);
				if (expression.contains("emax(") || expression.contains("EMAX("))
					e.addFunction(maxFct);
				BigDecimal bd = e.eval();
				if (bd.intValue() == 0) {
					return false;
				}
			} catch (Exception ex) {
				// default behavior: filter out on error
				return false;
			}
		}

		// transformation
		for (String expression : transformExpression) {
			try {
				Expression e = loadExpression(expression, message, false);
				if (expression.contains("sum(") || expression.contains("SUM("))
					e.addFunction(sumFct);
				if (expression.contains("mean(") || expression.contains("MEAN("))
					e.addFunction(meanFct);
				if (expression.contains("stdev(") || expression.contains("STDEV("))
					e.addFunction(stdevFct);
				if (expression.contains("emin(") || expression.contains("EMIN("))
					e.addFunction(minFct);
				if (expression.contains("emax(") || expression.contains("EMAX("))
					e.addFunction(maxFct);
				e.addLazyFunction(new AbstractTransform(message));
				e.eval();
			} catch (Exception ex) {
				// System.out.println("problem");
			}
		}

		// send message with a function client specific recipient
		delegate.send(message.cloneForRecipient(message.getRecipient() + ("[" + functionString + "]")));

		// log this if recipient is this client exactly
		if (message.getRecipient().equals(getName())) {
			OOCSIServer.logEvent(message.getSender(), "", message.getRecipient(), message.data, message.getTimestamp());
		}

		return true;
	}

	/**
	 * create an expression based on <code>message</code> parameters and a String <code>expression</code>
	 * 
	 * @param expression
	 * @param message
	 * @param abortOnMissing
	 * @return
	 */
	private Expression loadExpression(final String expression, Message message, boolean abortOnMissing) {
		final Expression e = new Expression(expression);
		List<String> vars = e.getUsedVariables();
		for (String key : vars) {
			Object value = message.data.get(key);
			if (value != null) {
				e.and(key, value.toString());
			} else if (!abortOnMissing) {
				e.and(key, new BigDecimal(0));
			}
		}
		return e;
	}

	@Override
	public String getName() {
		return delegate != null ? delegate.getName() : super.getName();
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public void disconnect() {
		delegate.disconnect();
	}

	@Override
	public boolean isConnected() {
		return delegate.isConnected();
	}

	@Override
	public void ping() {
		delegate.ping();
	}

	@Override
	public void pong() {
		delegate.pong();
	}

	abstract class WindowFunction extends AbstractFunction {

		private Queue<BigDecimal> queue = new ConcurrentLinkedQueue<BigDecimal>();
		protected int queueLength = -1;

		protected WindowFunction(String name, int numParams) {
			super(name, numParams);
		}

		@Override
		public synchronized BigDecimal eval(List<BigDecimal> parameters) {

			// not initialized?
			if (queueLength == -1) {
				// initialize!
				queueLength = Math.min(parameters.get(1).intValue(), 50);
			}

			// make space
			while (queue.size() >= queueLength) {
				queue.poll();
			}

			// insert element
			BigDecimal value = parameters.get(0);
			queue.offer(value);

			// return the added element
			return evalQueue(queue);
		}

		abstract public BigDecimal evalQueue(Queue<BigDecimal> queue);

	}

	// summary statistics over windows

	class SumOverWindowFunction extends WindowFunction {

		protected SumOverWindowFunction() {
			super("sum", 2);
		}

		@Override
		public synchronized BigDecimal evalQueue(Queue<BigDecimal> queue) {
			BigDecimal result = new BigDecimal(0);
			for (BigDecimal number : queue) {
				result = result.add(number);
			}
			return result;
		}
	}

	class MeanOverWindowFunction extends WindowFunction {

		protected MeanOverWindowFunction() {
			super("mean", 2);
		}

		@Override
		public synchronized BigDecimal evalQueue(Queue<BigDecimal> queue) {
			return new BigDecimal(mean(queue));
		}

		/**
		 * calculate the mean of all values in queue
		 * 
		 * @param queue
		 * @return
		 */
		private double mean(Queue<BigDecimal> queue) {
			double result = 0;
			for (BigDecimal number : queue) {
				result += number.doubleValue() / queueLength;
			}
			return result;
		}
	}

	class StandardDeviationOverWindowFunction extends WindowFunction {

		protected StandardDeviationOverWindowFunction() {
			super("stdev", 2);
		}

		@Override
		public synchronized BigDecimal evalQueue(Queue<BigDecimal> queue) {
			double result = 0;
			double mean = mean(queue);
			for (BigDecimal number : queue) {
				result += Math.pow(number.doubleValue() - mean, 2);
			}
			return new BigDecimal(Math.sqrt(result / queueLength));
		}

		/**
		 * calculate the mean of all values in queue
		 * 
		 * @param queue
		 * @return
		 */
		private double mean(Queue<BigDecimal> queue) {
			double result = 0;
			for (BigDecimal number : queue) {
				result += number.doubleValue() / queueLength;
			}
			return result;
		}
	}

	class MinOverWindowFunction extends WindowFunction {

		protected MinOverWindowFunction() {
			super("emin", 2);
		}

		@Override
		public synchronized BigDecimal evalQueue(Queue<BigDecimal> queue) {
			return queue.stream().min((a, b) -> a.compareTo(b)).orElse(new BigDecimal(0));
		}
	}

	class MaxOverWindowFunction extends WindowFunction {

		protected MaxOverWindowFunction() {
			super("emax", 2);
		}

		@Override
		public synchronized BigDecimal evalQueue(Queue<BigDecimal> queue) {
			return queue.stream().max((a, b) -> a.compareTo(b)).orElse(new BigDecimal(0));
		}
	}

	class AbstractTransform extends AbstractLazyFunction {

		private Message message;

		protected AbstractTransform(Message message) {
			super("transform", 2);
			this.message = message;
		}

		@Override
		public synchronized LazyNumber lazyEval(List<LazyNumber> parameters) {

			String key = parameters.get(0).getString();
			String expression = parameters.get(1).getString();

			if (key == null || key.length() == 0 || expression == null || expression.length() == 0) {
				return parameters.get(0);
			}

			// compute transformation
			try {
				final Expression e = loadExpression(expression, message, true);
				BigDecimal bd = e.eval();

				// store in message
				message.data.put(key, bd.floatValue());
			} catch (Exception ex) {
				// do nothing
			}

			return parameters.get(0);
		}
	}
}
