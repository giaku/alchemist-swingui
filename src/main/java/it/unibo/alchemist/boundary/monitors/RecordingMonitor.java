/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.monitors;


import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.utils.L;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import org.jfree.graphics2d.svg.SVGGraphics2D;
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
	private Abstract2DDisplay<T> source = null;
	private final Semaphore mutex = new Semaphore(1);
	private String fpCache;
	private final String defaultFilePath = System.getProperty("user.home") + System.getProperty("file.separator") + sdf.format(new Date()) + "-alchemist_screenshot.svg";
	private PrintStream writer;
	private SVGGraphics2D svgGraphicator;
	
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

	@Override
	public void finished(final IEnvironment<T> env, final ITime time, final long step) {
		if (endingScreenshot) {
			writeData(env, null, time, step);
		}
	}

	@Override
	public void initialized(final IEnvironment<T> env) {
		
		//TODO aggiungere caricamento via reflection di source
		env.getPreferredMonitor();
		
		if (startingScreenshot) {
			super.initialized(env);
		}
	}

	/**
	 * Save in a svg file a screenshot of the current source.
	 * @param env unused
	 * @param r unused
	 * @param time the current time of the simulation that could be added to the file name
	 * @param step the current step of the simulation that could be added to the file name
	 */
	private void writeData(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		mutex.acquireUninterruptibly();
		if (System.identityHashCode(fpCache) != System.identityHashCode(getFilePath())) {
			fpCache = getFilePath();
			String currentStep = /*addingStepNumber*/isLoggingStep() ? getSeparator() + step : "";
			String currentTime = (/*addingTime*/isLoggingTime() ? getSeparator() + time : "");
			if (writer != null) {
				writer.close();
			}
			try {
				writer = new PrintStream(new File(fpCache + currentStep + currentTime + ".svg"), StandardCharsets.UTF_8.name());
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				L.error(e);
			}
		}
	
		svgGraphicator = new SVGGraphics2D(source.getWidth(), source.getHeight());
		source.paint(svgGraphicator);
		writer.print(svgGraphicator.getSVGDocument());
		writer.close();
		mutex.release();
	}
	
	/**
	 * Unused.
	 */
	@Override
	protected double[] extractValues(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		return new double[0];
	}
	
}
