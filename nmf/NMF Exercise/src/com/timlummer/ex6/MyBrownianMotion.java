package com.timlummer.ex6;

import com.timlummer.ex4.*;
import net.finmath.time.*;

public class MyBrownianMotion {

	private TimeDiscretization time;
	private int dimension;
	private double initialValue,
	private int numberofPaths;
	private int numberofFactors;
	
	private double [][][]brownianIncrements;
	private double [][][]brownianPath;
	
	
		
	MyBrownianMotion (int initialTime,
			int numberOfTimeSteps,
			double deltaT, 
			int numberofPaths, 
			int numberofFactors,
			int dimension
			){
	
		this.numberofPaths = numberofPaths;
		this.numberofFactors = numberofFactors;
		
		time = new TimeDiscretization(initialTime,numberOfTimeSteps,deltaT);
	}
	
	public MyBrownianMotion(    
			TimeDiscretization timeDiscretization,
			int numberOfFactors,
			int numberOfPaths) {
		this.time = timeDiscretization;
		this.numberofFactors	= numberOfFactors;
		this.numberofPaths		= numberOfPaths;
	}
	
	
	public void generateBrownianMotion(Generator generator){
		
	int numberOfTimeSteps = time.getNumberOfTimeSteps();
	double [] STD = new double[numberOfTimeSteps];
	
	
	for (int i=0; i<numberOfTimeSteps;i++){
		STD[i] = Math.sqrt(time.getTimeStep(i));		
	}
	
	brownianIncrements = new double[numberOfTimeSteps][numberofFactors][numberofPaths];
	
	NormalRandomVariable NV = new NormalRandomVariable(0.0 , 1.0);
		
	for(int f = 0; f<numberofFactors; f++){
		for(int p = 0; p < numberofPaths;p++){
			
			brownianPath[0][f][p] = initialValue;
			
			for(int t = 0; t<numberOfTimeSteps;t++){
				
				brownianIncrements[t][f][p]= NV.generate()*STD[t];
				
				brownianPath [t+1][f][p] = 	brownianPath [t][f][p] + brownianIncrements[t][f][p];
				
			}
		}
	}
	
	}
	
}
