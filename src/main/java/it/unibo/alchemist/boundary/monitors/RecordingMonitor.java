/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.monitors;


import it.unibo.alchemist.boundary.interfaces.GraphicalOutputMonitor;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.utils.L;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.apache.commons.math3.util.FastMath;
import org.danilopianini.view.ExportForGUI;



/**
 * @author Gianluca Turin
 * 
 * @param <T>
 */
@ExportInspector
public class RecordingMonitor<T> extends EnvironmentInspector<T> {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
	private Generic2DDisplay<T> source;
	private final Semaphore mutex = new Semaphore(1);
	private String fpCache;
	private final String defaultFilePath = System.getProperty("user.home") + System.getProperty("file.separator") + sdf.format(new Date()) + "-alchemist_screenshot";
	private PrintStream writer;
	private SVGGraphics2D svgGraphicator;
	private long lastStep = Long.MIN_VALUE;
	private double lastUpdate = Long.MIN_VALUE;
	
	@SuppressWarnings("rawtypes")
	private static final Class< ? extends GraphicalOutputMonitor> DEFAULT_MONITOR_CLASS = Generic2DDisplay.class;
	private static final String DEFAULT_MONITOR_PACKAGE = "it.unibo.alchemist.boundary.monitors.";
	
	@ExportForGUI(nameToExport = "Capture after initialization")
	private boolean startingScreenshot = true;
	
	@ExportForGUI(nameToExport = "Capture when finished")
	private boolean endingScreenshot = true;
	
	/**
	 * RecordingMonitor<T> empty constructor.
	 */
	public RecordingMonitor() {
		super();
		setFilePath(defaultFilePath);
	}
	
	@SuppressWarnings("unchecked")
	private void createMonitor(final IEnvironment<T> env) {
		String monitorClassName = env.getPreferredMonitor();
		Class< ? extends GraphicalOutputMonitor<T>> monitorClass;
		if (monitorClassName == null) {
			monitorClass = (Class< ? extends GraphicalOutputMonitor<T>>) DEFAULT_MONITOR_CLASS;
		} else {
			if (!monitorClassName.contains(".")) {
				monitorClassName = DEFAULT_MONITOR_PACKAGE + monitorClassName;
			}
			try {
				monitorClass = (Class<GraphicalOutputMonitor<T>>) Class.forName(monitorClassName);
			} catch (final ClassNotFoundException e) {
				L.warn(e);
				monitorClass = (Class< ? extends GraphicalOutputMonitor<T>>) DEFAULT_MONITOR_CLASS;
			}
		}
		try {
			source = (Generic2DDisplay<T>) monitorClass.getConstructor().newInstance();
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			L.error(e);
		}
	}

	@Override
	public void finished(final IEnvironment<T> env, final ITime time, final long step) {
		if (endingScreenshot) {
			saveScreenshot(env, null, time, step);
		}
		
		source.finished(env, time, step);
	}

	@Override
	public void initialized(final IEnvironment<T> env) {
		
		//TODO aggiungere caricamento via reflection di source
				
		createMonitor(env);
		
		source.initialized(env);
			
		if (startingScreenshot) {
			saveScreenshot(env, null, new DoubleTime(), 0);
		}
	}
	
	@Override
	public void stepDone(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		source.stepDone(env, r, time, step);
		mutex.acquireUninterruptibly();
		final double sample = getInterval().getVal() * FastMath.pow(10, getIntervalOrderOfMagnitude().getVal());
		final boolean log = getMode().equals(Mode.TIME) ?  time.toDouble() - lastUpdate >= sample : step - lastStep >= sample;
		
		if (log) {
			lastUpdate = time.toDouble();
			lastStep = step;
			saveScreenshot(env, r, time, step);
		}
		mutex.release();
	}

	/**
	 * Save in a svg file a screenshot of the current source.
	 * @param env unused
	 * @param r unused
	 * @param time the current time of the simulation that could be added to the file name
	 * @param step the current step of the simulation that could be added to the file name
	 */
	private void saveScreenshot(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		
		if (System.identityHashCode(fpCache) != System.identityHashCode(getFilePath())) {
			fpCache = getFilePath();
		}
		lastStep = step;
		lastUpdate = time.toDouble();
		String currentStep = isLoggingStep() ? getSeparator() + step : "";
		String currentTime = isLoggingTime() ? getSeparator() + time : "";			
			
		try {
			writer = new PrintStream(new File(fpCache + currentStep + currentTime + ".svg"), StandardCharsets.UTF_8.name());
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			L.error(e);
		}
		
		svgGraphicator = new SVGGraphics2D(source.getWidth(), source.getHeight());
		source.paintAll(svgGraphicator);
		writer.print(svgGraphicator.getSVGDocument());
		writer.close();
		
	}
	
	@Override
	protected double[] extractValues(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		/**
		 * Unused.
		 */
		return new double[0];
	}
	
}
