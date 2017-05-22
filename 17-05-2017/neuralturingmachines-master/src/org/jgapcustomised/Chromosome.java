/*
 * Copyright 2001-2003 Neil Rotstan Copyright (C) 2004 Derek James and Philip Tucker
 * 
 * This file is part of JGAP.
 * 
 * JGAP is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser Public License as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * JGAP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along with JGAP; if not,
 * write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * 
 * Modified on Feb 3, 2003 by Philip Tucker
 */
package org.jgapcustomised;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import com.ojcoleman.ahni.evaluation.novelty.Behaviour;
import com.ojcoleman.ahni.util.ArrayUtil;

/**
 * Chromosomes represent potential solutions and consist of a fixed-length collection of genes. Each gene represents a
 * discrete part of the solution. Each gene in the Chromosome may be backed by a different concrete implementation of
 * the Gene interface, but all genes with the same innovation ID must share the same concrete implementation across
 * Chromosomes within a single population (genotype).
 */
public class Chromosome extends EvaluationData implements Comparable, Serializable, Cloneable {
	/**
	 * default ID
	 */
	public final static Long DEFAULT_ID = new Long(-1);

	private Long m_id = DEFAULT_ID;

	private String m_idString;

	/**
	 * Genetic material contained in this chromosome.
	 */
	private ChromosomeMaterial m_material = null;

	private SortedSet<Allele> m_alleles = null;

	/**
	 * Keeps track of whether or not this Chromosome has been selected by the natural selector to move on to the next
	 * generation.
	 */
	protected boolean m_isSelectedForNextGeneration = false;


	protected Species m_specie = null;

	public boolean isElite = false;


	/**
	 * ctor for hibernate
	 */
	public Chromosome() {
		this(1, 0);
	}

	/**
	 * ctor for hibernate
	 */
	public Chromosome(int objectiveCount, int behaviourCount) {
		super(objectiveCount, behaviourCount);
		m_material = new ChromosomeMaterial();
	}

	/**
	 * this should only be called when a chromosome is being created from persistence; otherwise, the ID should be
	 * generated by <code>a_activeConfiguration</code>.
	 * 
	 * @param a_material Genetic material to be contained within this Chromosome instance.
	 * @param an_id unique ID of new chromosome
	 */
	public Chromosome(ChromosomeMaterial a_material, Long an_id, int objectiveCount, int behaviourCount) {
		super(objectiveCount, behaviourCount);
		// Sanity checks: make sure the parameters are all valid.
		if (a_material == null)
			throw new IllegalArgumentException("Chromosome material can't be null.");

		setId(an_id);
		m_material = a_material;
		m_alleles = Collections.unmodifiableSortedSet(m_material.getAlleles());
		m_fitnessValue = new double[objectiveCount];
		Arrays.fill(m_fitnessValue, Double.NaN);
		if (behaviourCount > 0) {
			behaviours = new Behaviour[behaviourCount];
		}
		m_performanceValue = new TreeMap<String, Double>();
	}

	/**
	 * Calculates compatibility distance between this and <code>target</code> according to <a
	 * href="http://nn.cs.utexas.edu/downloads/papers/stanley.ec02.pdf">NEAT </a> speciation methodology. It is generic
	 * enough that the alleles do not have to be nodes and connections.
	 * 
	 * @param target
	 * @param parms
	 * @return distance between this object and <code>target</code>
	 * @see ChromosomeMaterial#distance(ChromosomeMaterial, SpeciationParms)
	 */
	public double distance(Chromosome target, SpeciationParms parms) {
		return m_material.distance(target.m_material, parms);
	}

	/**
	 * @return Long unique identifier for chromosome; useful for <code>hashCode()</code> and persistence
	 */
	public Long getId() {
		return m_id;
	}

	/**
	 * for hibernate
	 * 
	 * @param id
	 */
	private void setId(Long id) {
		m_id = id;
		m_idString = "Chromosome " + m_id;
	}

	/**
	 * Returns the size of this Chromosome (the number of alleles it contains). A Chromosome's size is constant and will
	 * never change.
	 * 
	 * @return The number of alleles contained within this Chromosome instance.
	 */
	public int size() {
		return m_alleles.size();
	}

	/**
	 * @return clone with primary parent ID of this chromosome and the same genetic material.
	 */
	public ChromosomeMaterial cloneMaterial() {
		return m_material.clone(getId());
	}

	/**
	 * @return SortedSet alleles, sorted by innovation ID
	 */
	public SortedSet<Allele> getAlleles() {
		return m_alleles;
	}

	/**
	 * @param alleleToMatch
	 * @return Gene gene with same innovation ID as <code>geneToMatch</code, or <code>null</code> if none match
	 */
	public Allele findMatchingGene(Allele alleleToMatch) {
		Iterator iter = m_alleles.iterator();
		while (iter.hasNext()) {
			Allele allele = (Allele) iter.next();
			if (allele.equals(alleleToMatch))
				return allele;
		}
		return null;
	}

	/**
	 * @return int fitness value adjusted for fitness sharing according to <a
	 *         href="http://nn.cs.utexas.edu/downloads/papers/stanley.ec02.pdf>NEAT </a> paradigm.
	 */
	public double getSpeciatedFitnessValue() {
		if (m_specie == null)
			return getFitnessValue();
		return m_specie.getChromosomeSpeciatedFitnessValue(this);
	}


	/**
	 * Returns a string representation of this Chromosome, useful for some display purposes.
	 * 
	 * @return A string representation of this Chromosome.
	 */
	public String toString() {
		return m_idString;
	}

	/**
	 * Compares this Chromosome against the specified object. The result is true if and the argument is an instance of
	 * the Chromosome class and has the same ID.
	 * 
	 * @param other The object to compare against.
	 * @return true if the objects are the same, false otherwise.
	 */
	public boolean equals(Object other) {
		return compareTo(other) == 0;
	}

	/**
	 * Retrieve a hash code for this Chromosome.
	 * 
	 * @return the hash code of this Chromosome.
	 */
	public int hashCode() {
		return m_id.hashCode();
	}

	/**
	 * Compares the given Chromosome to this Chromosome by their ID.
	 * 
	 * @param o The Chromosome against which to compare this chromosome.
	 * @return a negative number if this chromosome is "less than" the given chromosome, zero if they are equal to each
	 *         other, and a positive number if this chromosome is "greater than" the given chromosome.
	 */
	public int compareTo(Object o) {
		Chromosome other = (Chromosome) o;
		return m_id.compareTo(other.m_id);
		// return m_material.compareTo(other.m_material);
	}

	/**
	 * Compares this Chromosome against the specified object. The result is true if and the argument is an instance of
	 * the Chromosome class and has a set of genes with equal values (eg connection weight, activation type) to this
	 * one.
	 * 
	 * @param c2 The object to compare against.
	 * @return true if the objects are the same, false otherwise.
	 */
	public boolean isEquivalent(Chromosome c2) {
		return m_material.isEquivalent(c2.m_material);
	}

	/**
	 * Sets whether this Chromosome has been selected by the natural selector to continue to the next generation.
	 * 
	 * @param a_isSelected true if this Chromosome has been selected, false otherwise.
	 */
	public void setIsSelectedForNextGeneration(boolean a_isSelected) {
		m_isSelectedForNextGeneration = a_isSelected;
	}

	/**
	 * Retrieves whether this Chromosome has been selected by the natural selector to continue to the next generation.
	 * 
	 * @return true if this Chromosome has been selected, false otherwise.
	 */
	public boolean isSelectedForNextGeneration() {
		return m_isSelectedForNextGeneration;
	}

	/**
	 * should only be called from Species; assigns this chromosome to <code>aSpecie</code>; throws exception if
	 * chromosome is added to a specie twice
	 * 
	 * @param aSpecie
	 */
	public void setSpecie(Species aSpecie) {
		if (m_specie != null)
			throw new IllegalStateException("chromosome can't be added to " + aSpecie + ", already a member of specie " + m_specie);
		m_specie = aSpecie;
	}

	/**
	 * Resets (clears) the specie this chromose belongs to.
	 */
	public void resetSpecie() {
		m_specie = null;
	}

	/**
	 * for hibernate
	 * 
	 * @param id
	 */
	private void setPrimaryParentId(Long id) {
		m_material.setPrimaryParentId(id);
	}

	/**
	 * for hibernate
	 * 
	 * @param id
	 */
	private void setSecondaryParentId(Long id) {
		m_material.setSecondaryParentId(id);
	}

	/**
	 * @return this chromosome's specie
	 */
	public Species getSpecie() {
		return m_specie;
	}

	/**
	 * @return primary parent ID; this is the dominant parent for chromosomes spawned by crossover, and the only parent
	 *         for chromosomes spawned by cloning
	 */
	public Long getPrimaryParentId() {
		return m_material.getPrimaryParentId();
	}

	/**
	 * @return secondary parent ID; this is the recessive parent for chromosomes spawned by crossover, and null for
	 *         chromosomes spawned by cloning
	 */
	public Long getSecondaryParentId() {
		return m_material.getSecondaryParentId();
	}

	/**
	 * for hibernate
	 * 
	 * @param aAlleles
	 */
	private void setAlleles(SortedSet aAlleles) {
		m_material.setAlleles(aAlleles);
		m_alleles = Collections.unmodifiableSortedSet(aAlleles);
	}

	public int getObjectiveCount() {
		return m_fitnessValue.length;
	}

	public int getNoveltyObjectiveCount() {
		return behaviours == null ? 0 : behaviours.length;
	}

	/**
	 * Checks whether this individual dominates the specified other individual, i.e. it is at least as good as the other
	 * one in all objectives and for at least one objective it is better (i.e. higher fitness value).
	 * 
	 * This code is based on JNSGA2 by Joachim Melcher, Institut AIFB, Universitaet Karlsruhe (TH), Germany
	 * http://sourceforge.net/projects/jnsga2
	 * 
	 * @param otherChromosome other individual
	 * @return <code>true</code> iff this individual dominates the specified one
	 */
	public boolean dominates(Chromosome otherChromosome) {
		/* check special cases: at least one fitness value is 'NaN' (not a number) */
		boolean hasThisNaN = false;
		boolean hasOtherNaN = false;

		for (int i = 0; i < getObjectiveCount(); i++) {
			if (new Double(getFitnessValue(i)).equals(Double.NaN)) {
				hasThisNaN = true;
			}
			if (new Double(otherChromosome.getFitnessValue(i)).equals(Double.NaN)) {
				hasOtherNaN = true;
			}
		}

		// If it looks like we might not be using multi-objective, try using overall/old fitness values.
		if (getObjectiveCount() == 1 && hasThisNaN && hasOtherNaN) {
			return getFitnessValue() > otherChromosome.getFitnessValue();
		}

		if (hasThisNaN) {
			return false;
		}

		if (!hasThisNaN && hasOtherNaN) {
			return true;
		}

		// Both individuals have no 'NaN'
		boolean atLeastOneObjectiveBetter = false;

		for (int i = 0; i < getObjectiveCount(); i++) {
			if (getFitnessValue(i) < otherChromosome.getFitnessValue(i)) {
				return false;
			}
			if (getFitnessValue(i) > otherChromosome.getFitnessValue(i)) {
				atLeastOneObjectiveBetter = true;
			}
		}

		return atLeastOneObjectiveBetter;
	}

	public ChromosomeMaterial getMaterial() {
		return m_material;
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
