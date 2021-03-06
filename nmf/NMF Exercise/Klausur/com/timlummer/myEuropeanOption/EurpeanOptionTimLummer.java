/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 21.01.2004
 */
package com.timlummer.myEuropeanOption;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements the valuation of a European option on a single asset.
 * 
 * Tim Lummer based on Fries
 * 
 * Given a model for an asset <i>S</i>, the European option with strike <i>K</i>, maturity <i>T</i>
 * pays
 * <br>
 * 	<i>V(T) = max(S(T) - K , 0)</i> in <i>T</i>.
 * <br>
 * 
 * The <code>getValue</code> method of this class will return the random variable <i>N(t) * V(T) / N(T)</i>,
 * where <i>N</i> is the numerarie provided by the model. If <i>N(t)</i> is deterministic,
 * calling <code>getAverage</code> on this random variable will result in the value. Otherwise a
 * conditional expectation has to be applied.
 * 
 * @author Tim Lummer based on Fries
 * @version 1.3
 */
public class EurpeanOptionTimLummer extends AbstractAssetMonteCarloProduct {

	private final double maturity;
	private final double strike;
	private final double barrier;
	private final Integer underlyingIndex;
	private final String nameOfUnderliyng;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param underlyingIndex The index of the underlying to be fetched from the model.
	 */
	public EurpeanOptionTimLummer(double maturity, double strike, double barrier,int underlyingIndex) {
		super();
		this.maturity			= maturity;
		this.strike				= strike;
		this.barrier			= barrier;
		this.underlyingIndex	= underlyingIndex;
		this.nameOfUnderliyng	= null;		// Use underlyingIndex
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 */
	public EurpeanOptionTimLummer(double maturity, double strike, double barrier) {
		this(maturity, strike, barrier ,0);
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 * 
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException {
		// Get underlying and numeraire

		
		// Get S(T)
		RandomVariableInterface underlyingAtMaturity	= model.getAssetValue(maturity, underlyingIndex);

		// The payoff: values = max(underlying - strike, 0) = V(T) = max(S(T)-K,0)
		RandomVariableInterface values = underlyingAtMaturity.sub(strike).floor(0.0);
		
		//The payoff: Check the Barrier
		values = values.apply(x ->  x < barrier ? x : 0.0);
		
		// Discounting...
		RandomVariableInterface numeraireAtMaturity		= model.getNumeraire(maturity);
		RandomVariableInterface monteCarloWeights		= model.getMonteCarloWeights(maturity);
		values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
		RandomVariableInterface	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
		RandomVariableInterface	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		
		values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);
		
		
		return values;
}	

	public static void main(String[] args) throws Exception {
		
		// Model properties
		double	initialValue   = 100;
		double	riskFreeRate   = 0.04;
		double	volatility     = 0.25;

		// Process discretization properties
		int		numberOfPaths		= 10000;
		int		numberOfTimeSteps	= 10;
		double	deltaT				= 0.1;
		
		int		seed				= 31415;

		// Product properties
		double	optionMaturity = 1.0;
		double	optionStrike = 90.0;
		
		double Barrier = Double.MAX_VALUE;
				
		
		// Create a model
		AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Create a time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process 	// net.finmath.montecarlo.process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		/*
		 * Value a call option (using the product implementation)
		 */
		EurpeanOptionTimLummer europeanOption = new EurpeanOptionTimLummer(optionMaturity, optionStrike,Barrier);
		EuropeanOption europeanOptionNormal = new EuropeanOption(optionMaturity, optionStrike);
		
		double value = europeanOption.getValue(monteCarloBlackScholesModel);
		
		double valueNormal = europeanOptionNormal.getValue(monteCarloBlackScholesModel);
		
		double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

		System.out.println("Tim Lummers value using Monte-Carlo and Barrier Option.......: " + value);
		System.out.println("Tim Lummers value using Monte-Carlo without Barrier Option.......: " + valueNormal);
		System.out.println("Tim Lummers value Analytic without Barrier Option.......: " + valueAnalytic);
	}
}
