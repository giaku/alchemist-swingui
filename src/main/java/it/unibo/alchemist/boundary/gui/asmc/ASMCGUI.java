/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui.asmc;

import it.unibo.alchemist.boundary.gui.asmc.ASMCMenu.Commands;
import it.unibo.alchemist.modelchecker.interfaces.ASMCListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * An user interface that acts as a listener for ASMC and plots results as they
 * are generated.
 * 
 * @author Davide Ensini
 * 
 */
public class ASMCGUI extends JFrame implements ActionListener, ASMCListener {

	private static final long serialVersionUID = -4695122438166355966L;

	private static final Dimension DIMENSION = new Dimension(1500, 1000);

	private final ASMCSampler ss;
	private final ASMCPlot sp;
	private final ASMCMenu menu;
	private final JLabel status;
	private int nPerformed;

	/**
	 * Creates a GUI with given sampler and plotter.
	 * 
	 * @param sampler
	 *            The component responsible for data sampling
	 * @param plot
	 *            The plotting panel
	 */
	public ASMCGUI(final ASMCSampler sampler, final ASMCPlot plot) {
		super("ASMC GUI");
		this.setSize(DIMENSION);
		this.sp = plot;
		this.ss = sampler;
		ss.setPlotter(sp);

		this.menu = new ASMCMenu();
		menu.addActionListener(this);

		status = new JLabel("No available data. Please wait.");

		this.add(sp, BorderLayout.CENTER);
		this.add(menu, BorderLayout.NORTH);
		this.add(status, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (Commands.ALPHA.equals(e.getActionCommand())) {
			ss.setConfidence(Double.parseDouble(menu.getAlphaField()));
		} else if (Commands.LB.equals(e.getActionCommand())) {
			ss.setLowerBound(Double.parseDouble(menu.getLBField()));
		} else if (Commands.UB.equals(e.getActionCommand())) {
			ss.setUpperBound(Double.parseDouble(menu.getUBField()));
		} else if (Commands.GRAIN.equals(e.getActionCommand())) {
			ss.setGranularity(Double.parseDouble(menu.getGrainField()));
		} else if (Commands.SWITCH_VIEW.equals(e.getActionCommand())) {
			((SimplePlot) sp).switchView();
			ss.redraw();
		} else if (Commands.REDRAW.equals(e.getActionCommand())) {
			ss.redraw();
		} else if (Commands.AUTO_SCALE.equals(e.getActionCommand())) {
			ss.setAutoScale();
			menu.resetBoundsFields();
		}
	}

	@Override
	public void batchDone(final Double[] values) {
		nPerformed = values.length;
		updateStatusLine();
		ss.batchDone(values);
	}

	/**
	 * @return the sampler in use by this GUI
	 */
	public ASMCSampler getSampler() {
		return ss;
	}

	private void updateStatusLine() {
		status.setText("Effettuate " + nPerformed + " simulazioni.");
		status.repaint();
	}

}
