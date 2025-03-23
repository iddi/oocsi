package nl.tue.id.oocsi.server.model;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;
import com.ezylang.evalex.parser.ParseException;
import com.ezylang.evalex.parser.Token;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Message;

public class FunctionClient extends Client {

	private String functionString;
	private Client delegate;

	// reference: https://github.com/uklimaschewski/EvalEx
	private List<String> filterExpression = new LinkedList<>();
	private List<Map.Entry<String, String>> transformExpression = new LinkedList<>();

	private final ExpressionConfiguration configuration;

	private WindowFunction sumFct = new SumOverWindowFunction();
	private WindowFunction meanFct = new MeanOverWindowFunction();
	private WindowFunction stdevFct = new StandardDeviationOverWindowFunction();
	private WindowFunction minFct = new MinOverWindowFunction();
	private WindowFunction maxFct = new MaxOverWindowFunction();

	public FunctionClient(Client delegateClient, String token, String functionString, ChangeListener presence) {
		super(token, presence);
		this.functionString = functionString;
		this.delegate = delegateClient;

		configuration = initFunctions(functionString);
	}

	private ExpressionConfiguration initFunctions(String functionString) {
		final Pattern filterPattern = Pattern.compile("filter\\((.*)\\)");
		final Pattern transformPattern = Pattern.compile("transform\\(([^,]+),(.*)\\)");

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
				transformExpression.add(Map.entry(transformMatcher.group(1), transformMatcher.group(2)));
				continue;
			}
		}

		return ExpressionConfiguration.defaultConfiguration().withAdditionalFunctions(Map.entry("sum", sumFct),
		        Map.entry("mean", meanFct), Map.entry("stdev", stdevFct), Map.entry("emin", minFct),
		        Map.entry("emax", maxFct));
	}

	@Override
	public synchronized boolean send(Message message) {

		// filtering checks
		for (String expression : filterExpression) {
			// apply expression
			try {
				final Expression e = loadExpression(expression, message, true);
				EvaluationValue result = e.evaluate();
				if (!result.getBooleanValue()) {
					return false;
				}
			} catch (Exception ex) {
				// default behavior: filter out on error
				return false;
			}
		}

		// transformation
		Message transformedMessage = message.cloneForRecipient(message.getRecipient() + ("[" + functionString + "]"));
		for (Map.Entry<String, String> entry : transformExpression) {
			try {
				String key = entry.getKey();
				String expression = entry.getValue();
				Expression e = loadExpression(expression, message, false);
				EvaluationValue result = e.evaluate();
				transformedMessage.addData(key, result.getNumberValue().floatValue());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		// send message with a function client specific recipient
		delegate.send(transformedMessage);

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
	 * @throws ParseException
	 */
	private Expression loadExpression(final String expression, Message message, boolean abortOnMissing)
	        throws ParseException {
		final Expression e = new Expression(expression, configuration);
		Set<String> vars = e.getUsedVariables();
		for (String key : vars) {
			Object value = message.data.get(key);
			if (value != null) {
				e.and(key, BigDecimal.valueOf(Float.parseFloat(value.toString())));
			} else if (!abortOnMissing) {
				e.and(key, BigDecimal.valueOf(0));
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

	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	abstract class WindowFunction extends AbstractFunction {

		private Queue<BigDecimal> queue = new ConcurrentLinkedQueue<BigDecimal>();
		protected int queueLength = -1;

		@Override
		public synchronized EvaluationValue evaluate(Expression expression, Token functionToken,
		        EvaluationValue... parameterValues) {

			EvaluationValue value = parameterValues[0];
			EvaluationValue windowLength = parameterValues[1];
			// not initialized?
			if (queueLength == -1) {
				// initialize!
				queueLength = Math.min(windowLength.getNumberValue().intValue(), 50);
			}

			// make space
			while (queue.size() >= queueLength) {
				queue.poll();
			}

			// insert element
			queue.offer(value.getNumberValue());

			return evalQueue(queue);
		}

		abstract public EvaluationValue evalQueue(Queue<BigDecimal> queue);

	}

	// summary statistics over windows
	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	class SumOverWindowFunction extends WindowFunction {

		@Override
		public synchronized EvaluationValue evalQueue(Queue<BigDecimal> queue) {
			BigDecimal result = new BigDecimal(0);
			for (BigDecimal number : queue) {
				result = result.add(number);
			}
			return EvaluationValue.numberValue(result);
		}
	}

	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	class MeanOverWindowFunction extends WindowFunction {

		@Override
		public synchronized EvaluationValue evalQueue(Queue<BigDecimal> queue) {
			return EvaluationValue.numberValue(new BigDecimal(mean(queue)));
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

	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	class StandardDeviationOverWindowFunction extends WindowFunction {

		@Override
		public synchronized EvaluationValue evalQueue(Queue<BigDecimal> queue) {
			double result = 0;
			double mean = mean(queue);
			for (BigDecimal number : queue) {
				result += Math.pow(number.doubleValue() - mean, 2);
			}
			return EvaluationValue.numberValue(new BigDecimal(Math.sqrt(result / queueLength)));
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

	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	class MinOverWindowFunction extends WindowFunction {

		@Override
		public synchronized EvaluationValue evalQueue(Queue<BigDecimal> queue) {
			return EvaluationValue.numberValue(queue.stream().min((a, b) -> a.compareTo(b)).orElse(new BigDecimal(0)));
		}
	}

	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	class MaxOverWindowFunction extends WindowFunction {

		@Override
		public synchronized EvaluationValue evalQueue(Queue<BigDecimal> queue) {
			return EvaluationValue.numberValue(queue.stream().max((a, b) -> a.compareTo(b)).orElse(new BigDecimal(0)));
		}
	}

}
