/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui.asmc;

import it.unibo.alchemist.modelchecker.AlchemistASMC;
import it.unibo.alchemist.utils.L;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Given an ascendant ordered array of double values representing time
 * measurements, this class produces a sampling of the probability v.s. time
 * function (see Manual), returning both mean value and confidence interval.
 * 
 * @author Davide Ensini
 * 
 */
public class SimpleSampler implements ASMCSampler {
	private static final int QUATTRO = 4;
	private static final double DEFAULT_GRAIN = 0.1;
	private static final double DEFAULT_ALPHA = 0.01;
	private final Semaphore sem = new Semaphore(0);
	private double grain;
	private double alpha;
	private static final int UNSET = 1;
	private double lb = UNSET;
	private double ub = UNSET;
	private ASMCPlot plot;
	private Double[] data;

	/**
	 * Default constructor.
	 */
	public SimpleSampler() {
		this(DEFAULT_GRAIN, DEFAULT_ALPHA);
	}

	/**
	 * Constructs a Sampler with given sampling step and confidence.
	 * 
	 * @param samplingGrain
	 *            Sampling step
	 * @param confidence
	 *            Confidence
	 */
	public SimpleSampler(final double samplingGrain, final double confidence) {
		grain = samplingGrain;
		alpha = confidence;
		sem.release();
	}

	@Override
	public void batchDone(final Double[] values) {
		try {
			sem.acquire();
			this.data = values.clone();
			draw();
			sem.release();
		} catch (final InterruptedException e) {
			L.error(e);
		}
	}

	private void draw() {
		final ExecutorService ex = Executors.newSingleThreadExecutor();
		ex.execute(new Runnable() {
			@Override
			public void run() {
				try {
					sem.acquire();
				} catch (final InterruptedException e) {
					L.error(e);
					sem.release();
				} finally {
					sem.release();
				}
				final int sampleSize = data == null ? 0 : data.length;
				if (sampleSize == 0) {
					sem.release();
					return;
				}
				final double min = data[0] - grain;
				final double max = data[sampleSize - 1] + grain;
				final int adjust = (max - min) / grain == 0.0 ? 1 : 2;
				final int xValues = (int) (Math.floor((max - min) / grain) + adjust);
				int xIndex = 0;
				final double[][] v = new double[xValues][QUATTRO];
				int gone = 0;
				for (; gone < sampleSize && xIndex < xValues; xIndex++) {
					final double base = min + (xIndex - 1) * grain;
					final double limit = base + grain;
					while (gone < sampleSize && data[gone] <= limit) {
						gone++;
					}
					final double sPar = Math.sqrt(((double) gone - gone * gone / sampleSize) / (sampleSize - 1));
					final double d = Math.min(AlchemistASMC.computeDeltaDynamic(sPar, sampleSize, alpha), AlchemistASMC.computeDeltaStatic(sampleSize, alpha));
					final double val = (double) gone / sampleSize;
					v[xIndex] = new double[] { limit, val, val - d / 2, val + d / 2 };
				}
				plot.batchDone(v, lb == UNSET ? min : lb, ub == UNSET ? max : ub, sampleSize);
				sem.release();
			}
		});
	}

	@Override
	public void redraw() {
		draw();
	}

	@Override
	public void setAutoScale() {
		try {
			sem.acquire();
			this.lb = UNSET;
			this.ub = UNSET;
			draw();
			sem.release();
		} catch (final InterruptedException e) {
			L.error(e);
		}
	}

	@Override
	public void setBounds(final double lowerBound, final double upperBound) {
		if (lowerBound > upperBound && upperBound != UNSET || lowerBound < 0 && lowerBound != UNSET) {
			return;
		}
		try {
			sem.acquire();
			this.lb = lowerBound;
			this.ub = upperBound;
			draw();
			sem.release();
		} catch (final InterruptedException e) {
			L.error(e);
		}
	}

	@Override
	public void setConfidence(final double newAlpha) {
		try {
			sem.acquire();
			if (newAlpha > 0 & newAlpha < 1) {
				this.alpha = newAlpha;
				draw();
			}
			sem.release();
		} catch (final InterruptedException e) {
			L.error(e);
		}
	}

	@Override
	public void setGranularity(final double newGrain) {
		try {
			sem.acquire();
			this.grain = newGrain;
			draw();
			sem.release();
		} catch (final InterruptedException e) {
			L.error(e);
		}
	}

	@Override
	public void setLowerBound(final double lowerBound) {
		setBounds(lowerBound, this.ub);
	}

	@Override
	public void setPlotter(final ASMCPlot sp) {
		plot = sp;
	}

	@Override
	public void setUpperBound(final double upperBound) {
		setBounds(this.lb, upperBound);
	}

}
