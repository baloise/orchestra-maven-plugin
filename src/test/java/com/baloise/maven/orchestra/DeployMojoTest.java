package com.baloise.maven.orchestra;


import static org.junit.Assert.assertTrue;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
//TODO can we write a good test case?
public class DeployMojoTest
{
    @Rule
    public MojoRule rule = new MojoRule()
    {
        @Override
        protected void before() throws Throwable 
        {
        }

        @Override
        protected void after()
        {
        }
    };

    /**
     * @throws Exception if any
     */
    @Test
    public void testSomething()
            throws Exception
    {
//        File pom = new File( "target/test-classes/project-to-test/" );
//        assertNotNull( pom );
//        assertTrue( pom.exists() );
//
//        Mojo myMojo = rule.lookupConfiguredMojo( pom, "scenario-package" );
//        assertNotNull( myMojo );
//        myMojo.execute();
//
//        File outputDirectory = ( File ) rule.getVariableValueFromObject( myMojo, "outputDirectory" );
//        assertNotNull( outputDirectory );
//        assertTrue( outputDirectory.exists() );


    }

    /** Do not need the MojoRule. */
    @WithoutMojo
    @Test
    public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn()
    {
        assertTrue( true );
    }

}

