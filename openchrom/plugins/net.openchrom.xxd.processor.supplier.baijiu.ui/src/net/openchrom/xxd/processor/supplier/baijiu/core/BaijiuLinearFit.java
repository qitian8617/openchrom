/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

/**
 * Ordinary linear regression of response vs concentration, plus the effective
 * single-number RF written into {@link BaijiuMethodSettings#getResponseFactors()}.
 * <p>
 * Axes (see {@code docs/GCWS-MULTIPOINT.md}):
 * <ul>
 * <li>x = analyte concentration (g/L) at that mix needle</li>
 * <li>y = area ratio A_analyte / A_ISTD</li>
 * </ul>
 * Fit: {@code y = intercept + slope · x}. R² is the OLS coefficient of
 * determination. There is no hard regulatory cutoff; {@link #R2_SOFT_WARN} is a
 * display hint only.
 * <p>
 * Quantify still uses {@code C = RF × C_ISTD × (A_a / A_ISTD)}. The effective RF
 * is evaluated at the method 1× mix concentration (working range):
 * {@code RF = C_work / (C_ISTD × ŷ(C_work))}. When the intercept is 0 this equals
 * {@code 1 / (slope × C_ISTD)}.
 */
public final class BaijiuLinearFit {

	public static final int MIN_POINTS = 3;
	public static final double R2_SOFT_WARN = 0.99d;

	private final boolean valid;
	private final String message;
	private final int n;
	private final double slope;
	private final double intercept;
	private final double rSquared;
	private final double effectiveRf;

	private BaijiuLinearFit(boolean valid, String message, int n, double slope, double intercept, double rSquared, double effectiveRf) {

		this.valid = valid;
		this.message = message == null ? "" : message;
		this.n = n;
		this.slope = slope;
		this.intercept = intercept;
		this.rSquared = rSquared;
		this.effectiveRf = effectiveRf;
	}

	public static BaijiuLinearFit invalid(String message) {

		return new BaijiuLinearFit(false, message, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
	}

	public static BaijiuLinearFit stored(int n, double slope, double intercept, double rSquared, double effectiveRf) {

		boolean ok = n >= MIN_POINTS && Double.isFinite(slope) && Double.isFinite(intercept) && Double.isFinite(rSquared) && BaijiuCalibrationGate.isValidResponseFactor(effectiveRf);
		return new BaijiuLinearFit(ok, ok ? "" : "stored fit is incomplete", n, slope, intercept, rSquared, effectiveRf);
	}

	/**
	 * OLS of {@code y} on {@code x}. Points with non-finite or non-positive x/y are skipped.
	 */
	public static BaijiuLinearFit ordinaryLeastSquares(double[] x, double[] y, double workingConcentrationGL, double istdInjectedGL) {

		if(x == null || y == null || x.length != y.length) {
			return invalid("x/y length mismatch");
		}
		int raw = x.length;
		double[] xs = new double[raw];
		double[] ys = new double[raw];
		int n = 0;
		for(int i = 0; i < raw; i++) {
			if(Double.isFinite(x[i]) && x[i] > 0.0d && Double.isFinite(y[i]) && y[i] > 0.0d) {
				xs[n] = x[i];
				ys[n] = y[i];
				n++;
			}
		}
		if(n < MIN_POINTS) {
			return invalid("need at least " + MIN_POINTS + " points");
		}
		double sumX = 0.0d;
		double sumY = 0.0d;
		for(int i = 0; i < n; i++) {
			sumX += xs[i];
			sumY += ys[i];
		}
		double meanX = sumX / n;
		double meanY = sumY / n;
		double sxx = 0.0d;
		double syy = 0.0d;
		double sxy = 0.0d;
		for(int i = 0; i < n; i++) {
			double dx = xs[i] - meanX;
			double dy = ys[i] - meanY;
			sxx += dx * dx;
			syy += dy * dy;
			sxy += dx * dy;
		}
		if(!(sxx > 0.0d)) {
			return invalid("concentrations are not distinct");
		}
		double slope = sxy / sxx;
		double intercept = meanY - slope * meanX;
		if(!Double.isFinite(slope) || !Double.isFinite(intercept)) {
			return invalid("slope/intercept not finite");
		}
		if(!(slope > 0.0d)) {
			return invalid("slope must be positive");
		}
		double rSquared;
		if(!(syy > 0.0d)) {
			rSquared = 0.0d;
		} else {
			rSquared = (sxy * sxy) / (sxx * syy);
			if(rSquared > 1.0d) {
				rSquared = 1.0d;
			} else if(rSquared < 0.0d) {
				rSquared = 0.0d;
			}
		}
		if(!(workingConcentrationGL > 0.0d) || !(istdInjectedGL > 0.0d)) {
			return invalid("working concentration or ISTD g/L is invalid");
		}
		double yWork = intercept + slope * workingConcentrationGL;
		if(!(yWork > 0.0d) || !Double.isFinite(yWork)) {
			return invalid("fitted response at working range is not positive");
		}
		double rf = workingConcentrationGL / (istdInjectedGL * yWork);
		if(!BaijiuCalibrationGate.isValidResponseFactor(rf)) {
			return invalid("effective RF is invalid");
		}
		return new BaijiuLinearFit(true, "", n, slope, intercept, rSquared, rf);
	}

	public boolean isValid() {

		return valid;
	}

	public String getMessage() {

		return message;
	}

	public int getN() {

		return n;
	}

	public double getSlope() {

		return slope;
	}

	public double getIntercept() {

		return intercept;
	}

	public double getRSquared() {

		return rSquared;
	}

	public double getEffectiveRf() {

		return effectiveRf;
	}

	public boolean isR2SoftWarn() {

		return valid && Double.isFinite(rSquared) && rSquared < R2_SOFT_WARN;
	}
}
