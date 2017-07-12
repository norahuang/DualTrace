package ca.uvic.chisel.bfv.annotations;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestRegions.class, TestTags.class, TestComments.class })
public class AllAnnotationTests {}
