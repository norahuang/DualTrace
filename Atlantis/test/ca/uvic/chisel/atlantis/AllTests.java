package ca.uvic.chisel.atlantis;

import ca.uvic.chisel.bfv.annotations.AllAnnotationTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestColours.class, AllAnnotationTests.class })
public class AllTests {}
