package nl.tue.id.oocsi.server.model;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.config.FunctionDictionaryIfc;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionIfc;
import com.ezylang.evalex.functions.FunctionParameter;
import com.ezylang.evalex.parser.ParseException;
import com.ezylang.evalex.parser.Token;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Message;

public class FunctionClient extends Client {

	private String functionString;
	private Client delegate;

	// reference: https://github.com/uklimaschewski/EvalEx
	private List<PreparedExpression> filterExpressions = new LinkedList<>();
	private List<PreparedTransform> transformExpressions = new LinkedList<>();

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

		this.configuration = initConfiguration();
		initExpressions(functionString);
	}

	private ExpressionConfiguration initConfiguration() {
		FunctionDictionaryIfc delegateDict = ExpressionConfiguration.defaultConfiguration().getFunctionDictionary();
		FunctionDictionaryIfc safeDict = new FunctionDictionaryIfc() {
			private final Set<String> blockedFunctions = Set.of("FACT", "STR_MATCHES", "STR_FORMAT", "DT_DATE_NEW",
					"DT_DURATION_NEW");

			@Override
			public FunctionIfc getFunction(String functionName) {
				if (functionName != null && blockedFunctions.contains(functionName.toUpperCase())) {
					return null;
				}
				return delegateDict.getFunction(functionName);
			}

			@Override
			public void addFunction(String functionName, FunctionIfc function) {
				delegateDict.addFunction(functionName, function);
			}

			@Override
			public boolean hasFunction(String functionName) {
				if (functionName != null && blockedFunctions.contains(functionName.toUpperCase())) {
					return false;
				}
				return delegateDict.hasFunction(functionName);
			}
		};

		safeDict.addFunction("sum", sumFct);
		safeDict.addFunction("mean", meanFct);
		safeDict.addFunction("stdev", stdevFct);
		safeDict.addFunction("emin", minFct);
		safeDict.addFunction("emax", maxFct);

		return ExpressionConfiguration.defaultConfiguration().toBuilder().functionDictionary(safeDict).build();
	}

	private void initExpressions(String functionString) {
		final Pattern filterPattern = Pattern.compile("filter\\((.*)\\)");
		final Pattern transformPattern = Pattern.compile("transform\\(([^,]+),(.*)\\)");

		// functions are separated by ';'
		String[] functions = functionString.split(";");
		for (String fct : functions) {
			Matcher filterMatcher = filterPattern.matcher(fct);
			if (filterMatcher.find()) {
				// init filter expression
				filterExpressions.add(new PreparedExpression(filterMatcher.group(1), configuration));
				continue;
			}

			Matcher transformMatcher = transformPattern.matcher(fct);
			if (transformMatcher.find()) {
				// init transform expression
				transformExpressions.add(
						new PreparedTransform(transformMatcher.group(1), transformMatcher.group(2), configuration));
				continue;
			}
		}
	}

	@Override
	public synchronized boolean send(Message message) {

		// filtering checks
		for (PreparedExpression pe : filterExpressions) {
			// apply expression
			try {
				final Expression e = pe.instantiate(message, true);
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
		for (PreparedTransform pt : transformExpressions) {
			try {
				Expression e = pt.expression.instantiate(message, false);
				EvaluationValue result = e.evaluate();
				transformedMessage.addData(pt.key, result.getNumberValue().floatValue());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		// send message with a function client specific recipient
		delegate.send(transformedMessage);

		// log this if recipient is this client exactly and not private
		if (!isPrivate() && message.getRecipient().equals(getName())) {
			OOCSIServer.logEvent(message.getSender(), "", message.getRecipient(), message.data, message.getTimestamp());
		}

		return true;
	}

	private static class PreparedExpression {
		final Expression template;
		final Set<String> usedVariables;
		final ParseException parseException;

		PreparedExpression(String exprString, ExpressionConfiguration config) {
			Expression t = null;
			Set<String> vars = Set.of();
			ParseException pe = null;
			try {
				t = new Expression(exprString, config);
				t.validate();
				vars = t.getUsedVariables();
			} catch (ParseException e) {
				pe = e;
			} catch (Exception e) {
				pe = new ParseException(0, 0, exprString, e.getMessage());
			}
			this.template = t;
			this.usedVariables = vars;
			this.parseException = pe;
		}

		Expression instantiate(Message message, boolean abortOnMissing) throws ParseException {
			if (parseException != null) {
				throw parseException;
			}
			Expression e = template.copy();
			for (String key : usedVariables) {
				Object value = message.data.get(key);
				if (value != null) {
					e.and(key, BigDecimal.valueOf(Float.parseFloat(value.toString())));
				} else if (!abortOnMissing) {
					e.and(key, BigDecimal.ZERO);
				}
			}
			return e;
		}
	}

	private static class PreparedTransform {
		final String key;
		final PreparedExpression expression;

		PreparedTransform(String key, String exprString, ExpressionConfiguration config) {
			this.key = key;
			this.expression = new PreparedExpression(exprString, config);
		}
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

		protected Queue<BigDecimal> queue = new ConcurrentLinkedQueue<BigDecimal>();
		protected int queueLength = -1;
		protected double runningSum = 0.0;

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
				BigDecimal polled = queue.poll();
				if (polled != null) {
					runningSum -= polled.doubleValue();
				}
			}

			// insert element
			BigDecimal num = value.getNumberValue();
			queue.offer(num);
			runningSum += num.doubleValue();

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
			return EvaluationValue.numberValue(BigDecimal.valueOf(runningSum));
		}
	}

	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	class MeanOverWindowFunction extends WindowFunction {

		@Override
		public synchronized EvaluationValue evalQueue(Queue<BigDecimal> queue) {
			return EvaluationValue.numberValue(BigDecimal.valueOf(runningSum / queueLength));
		}
	}

	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	class StandardDeviationOverWindowFunction extends WindowFunction {

		@Override
		public synchronized EvaluationValue evalQueue(Queue<BigDecimal> queue) {
			double result = 0;
			double mean = runningSum / queueLength;
			for (BigDecimal number : queue) {
				result += Math.pow(number.doubleValue() - mean, 2);
			}
			return EvaluationValue.numberValue(BigDecimal.valueOf(Math.sqrt(result / queueLength)));
		}
	}

	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	class MinOverWindowFunction extends WindowFunction {

		@Override
		public synchronized EvaluationValue evalQueue(Queue<BigDecimal> queue) {
			return EvaluationValue.numberValue(queue.stream().min((a, b) -> a.compareTo(b)).orElse(BigDecimal.ZERO));
		}
	}

	@FunctionParameter(name = "value")
	@FunctionParameter(name = "windowLength")
	class MaxOverWindowFunction extends WindowFunction {

		@Override
		public synchronized EvaluationValue evalQueue(Queue<BigDecimal> queue) {
			return EvaluationValue.numberValue(queue.stream().max((a, b) -> a.compareTo(b)).orElse(BigDecimal.ZERO));
		}
	}

}
