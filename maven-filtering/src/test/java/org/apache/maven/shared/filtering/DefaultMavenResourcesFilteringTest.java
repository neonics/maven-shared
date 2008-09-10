package org.apache.maven.shared.filtering;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Resource;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/*
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * 
 * @since 28 janv. 08
 * 
 * @version $Id$
 */
public class DefaultMavenResourcesFilteringTest
    extends PlexusTestCase
{

    File outputDirectory = new File( getBasedir(), "target/DefaultMavenResourcesFilteringTest" );

    protected void setUp()
        throws Exception
    {
        super.setUp();
        if ( outputDirectory.exists() )
        {
            FileUtils.forceDelete( outputDirectory );
        }
        outputDirectory.mkdirs();
        LoggerManager loggerManager = (LoggerManager) lookup( LoggerManager.ROLE );
        loggerManager.setThreshold( 0 );
    }

    public void testSimpleFiltering()
        throws Exception
    {
        File baseDir = new File( "c:\\foo\\bar" );
        StubMavenProject mavenProject = new StubMavenProject( baseDir );
        mavenProject.setVersion( "1.0" );
        mavenProject.setGroupId( "org.apache" );
        mavenProject.setName( "test project" );

        Properties projectProperties = new Properties();
        projectProperties.put( "foo", "bar" );
        projectProperties.put( "java.version", "zloug" );
        mavenProject.setProperties( projectProperties );
        MavenResourcesFiltering mavenResourcesFiltering = (MavenResourcesFiltering) lookup( MavenResourcesFiltering.class
            .getName() );

        String unitFilesDir = getBasedir() + "/src/test/units-files/maven-resources-filtering";
        File initialImageFile = new File( unitFilesDir, "happy_duke.gif" );

        Resource resource = new Resource();
        List resources = new ArrayList();
        resources.add( resource );
        resource.setDirectory( unitFilesDir );
        resource.setFiltering( true );

        List filtersFile = new ArrayList();
        filtersFile.add( getBasedir()
            + "/src/test/units-files/maven-resources-filtering/empty-maven-resources-filtering.txt" );

        List nonFilteredFileExtensions = Collections.singletonList( "gif" );

        mavenResourcesFiltering.filterResources( resources, outputDirectory, mavenProject, null, filtersFile,
                                                 nonFilteredFileExtensions, new StubMavenSession() );

        assertFiltering( baseDir, initialImageFile, false );
    }

    public void testWithMavenResourcesExecution()
        throws Exception
    {
        File baseDir = new File( "c:\\foo\\bar" );
        StubMavenProject mavenProject = new StubMavenProject( baseDir );
        mavenProject.setVersion( "1.0" );
        mavenProject.setGroupId( "org.apache" );
        mavenProject.setName( "test project" );

        Properties projectProperties = new Properties();
        projectProperties.put( "foo", "bar" );
        projectProperties.put( "java.version", "zloug" );
        mavenProject.setProperties( projectProperties );
        MavenResourcesFiltering mavenResourcesFiltering = (MavenResourcesFiltering) lookup( MavenResourcesFiltering.class
            .getName() );

        String unitFilesDir = getBasedir() + "/src/test/units-files/maven-resources-filtering";
        File initialImageFile = new File( unitFilesDir, "happy_duke.gif" );

        Resource resource = new Resource();
        List resources = new ArrayList();
        resources.add( resource );
        resource.setDirectory( unitFilesDir );
        resource.setFiltering( true );

        List filtersFile = new ArrayList();
        filtersFile.add( getBasedir()
            + "/src/test/units-files/maven-resources-filtering/empty-maven-resources-filtering.txt" );

        List nonFilteredFileExtensions = Collections.singletonList( "gif" );
        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution( resources, outputDirectory,
                                                                                       mavenProject, null, filtersFile,
                                                                                       nonFilteredFileExtensions,
                                                                                       new StubMavenSession() );
        mavenResourcesExecution.setEscapeString( "\\" );
        mavenResourcesFiltering.filterResources( mavenResourcesExecution );
        assertFiltering( baseDir, initialImageFile, true );
    }

    private void assertFiltering( File baseDir, File initialImageFile, boolean escapeTest )
        throws Exception
    {
        assertEquals( 7, outputDirectory.listFiles().length );
        Properties result = new Properties();
        result.load( new FileInputStream( new File( outputDirectory, "empty-maven-resources-filtering.txt" ) ) );

        assertTrue( result.isEmpty() );

        result = new Properties();
        result.load( new FileInputStream( new File( outputDirectory, "maven-resources-filtering.txt" ) ) );
        assertFalse( result.isEmpty() );

        assertEquals( "1.0", result.get( "version" ) );
        assertEquals( "org.apache", result.get( "groupId" ) );
        assertEquals( "bar", result.get( "foo" ) );
        assertEquals( "${foo.version}", result.get( "fooVersion" ) );

        assertEquals( "@@", result.getProperty( "emptyexpression" ) );
        assertEquals( "${}", result.getProperty( "emptyexpression2" ) );
        assertEquals( System.getProperty( "user.dir" ), result.getProperty( "userDir" ) );
        assertEquals( System.getProperty( "java.version" ), result.getProperty( "javaVersion" ) );

        if ( escapeTest )
        {
            assertEquals( "${java.version}", result.getProperty( "escapeJavaVersion" ) );
            assertEquals( "@user.dir@", result.getProperty( "escapeuserDir" ) );
        }
        assertEquals( baseDir.toString(), result.get( "base" ) );
        assertEquals( new File( baseDir.toString() ).getPath(), new File( result.getProperty( "base" ) ).getPath() );

        File imageFile = new File( outputDirectory, "happy_duke.gif" );
        assertTrue( imageFile.exists() );
        //assertEquals( initialImageFile.length(), imageFile.length() );
        assertTrue( filesAreIdentical( initialImageFile, imageFile ) );
    }

    public void testaddingTokens()
        throws Exception
    {
        File baseDir = new File( "c:\\foo\\bar" );
        final StubMavenProject mavenProject = new StubMavenProject( baseDir );
        mavenProject.setVersion( "1.0" );
        mavenProject.setGroupId( "org.apache" );
        mavenProject.setName( "test project" );

        Properties projectProperties = new Properties();
        projectProperties.put( "foo", "bar" );
        projectProperties.put( "java.version", "zloug" );
        mavenProject.setProperties( projectProperties );
        MavenResourcesFiltering mavenResourcesFiltering = (MavenResourcesFiltering) lookup( MavenResourcesFiltering.class
            .getName() );

        String unitFilesDir = getBasedir() + "/src/test/units-files/maven-resources-filtering";
        File initialImageFile = new File( unitFilesDir, "happy_duke.gif" );

        Resource resource = new Resource();
        List resources = new ArrayList();
        resources.add( resource );
        resource.setDirectory( unitFilesDir );
        resource.setFiltering( true );

        List filtersFile = new ArrayList();
        filtersFile.add( getBasedir()
            + "/src/test/units-files/maven-resources-filtering/empty-maven-resources-filtering.txt" );

        List nonFilteredFileExtensions = Collections.singletonList( "gif" );

        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution( resources, outputDirectory,
                                                                                       mavenProject, null, null,
                                                                                       nonFilteredFileExtensions,
                                                                                       new StubMavenSession() );

        mavenResourcesExecution.addFilerWrapperWithEscaping( new MavenProjectValueSource( mavenProject, true ), "@",
                                                             "@", null );
        mavenResourcesFiltering.filterResources( mavenResourcesExecution );
        Properties result = PropertyUtils
            .loadPropertyFile( new File( outputDirectory, "maven-resources-filtering.txt" ), null );
        assertFalse( result.isEmpty() );
        assertEquals( mavenProject.getName(), result.get( "pomName" ) );
        assertFiltering( baseDir, initialImageFile, false );
    }

    public void testNoFiltering()
        throws Exception
    {
        StubMavenProject mavenProject = new StubMavenProject( new File( getBasedir() ) );
        mavenProject.setVersion( "1.0" );
        mavenProject.setGroupId( "org.apache" );

        MavenResourcesFiltering mavenResourcesFiltering = (MavenResourcesFiltering) lookup( MavenResourcesFiltering.class
            .getName() );

        String unitFilesDir = getBasedir() + "/src/test/units-files/maven-resources-filtering";
        File initialImageFile = new File( unitFilesDir, "happy_duke.gif" );

        Resource resource = new Resource();
        List resources = new ArrayList();
        resources.add( resource );

        resource.setDirectory( unitFilesDir );
        resource.setFiltering( false );
        mavenResourcesFiltering.filterResources( resources, outputDirectory, mavenProject, null, null,
                                                 Collections.EMPTY_LIST, new StubMavenSession() );

        assertEquals( 7, outputDirectory.listFiles().length );
        Properties result = PropertyUtils.loadPropertyFile( new File( outputDirectory,
                                                                      "empty-maven-resources-filtering.txt" ), null );
        assertTrue( result.isEmpty() );

        result = PropertyUtils.loadPropertyFile( new File( outputDirectory, "maven-resources-filtering.txt" ), null );
        assertFalse( result.isEmpty() );

        assertEquals( "${pom.version}", result.get( "version" ) );
        assertEquals( "${pom.groupId}", result.get( "groupId" ) );
        assertEquals( "${foo}", result.get( "foo" ) );
        assertEquals( "@@", result.getProperty( "emptyexpression" ) );
        assertEquals( "${}", result.getProperty( "emptyexpression2" ) );
        File imageFile = new File( outputDirectory, "happy_duke.gif" );
        assertTrue( filesAreIdentical( initialImageFile, imageFile ) );
    }

    public static boolean filesAreIdentical( File expected, File current )
        throws IOException
    {
        if ( expected.length() != current.length() )
        {
            return false;
        }
        FileInputStream expectedIn = new FileInputStream( expected );
        FileInputStream currentIn = new FileInputStream( current );
        try
        {
            byte[] expectedBuffer = IOUtil.toByteArray( expectedIn );

            byte[] currentBuffer = IOUtil.toByteArray( currentIn );
            if ( expectedBuffer.length != currentBuffer.length )
            {
                return false;
            }
            for ( int i = 0, size = expectedBuffer.length; i < size; i++ )
            {
                if ( expectedBuffer[i] != currentBuffer[i] )
                {
                    return false;
                }
            }
        }
        finally
        {
            expectedIn.close();
            currentIn.close();
        }
        return true;
    }

    public void testIncludeOneFile()
        throws Exception
    {
        File baseDir = new File( "c:\\foo\\bar" );
        StubMavenProject mavenProject = new StubMavenProject( baseDir );
        mavenProject.setVersion( "1.0" );
        mavenProject.setGroupId( "org.apache" );
        mavenProject.setName( "test project" );

        MavenResourcesFiltering mavenResourcesFiltering = (MavenResourcesFiltering) lookup( MavenResourcesFiltering.class
            .getName() );

        String unitFilesDir = getBasedir() + "/src/test/units-files/maven-resources-filtering";

        Resource resource = new Resource();
        List resources = new ArrayList();
        resources.add( resource );
        resource.setDirectory( unitFilesDir );
        resource.setFiltering( true );
        resource.addInclude( "includ*" );

        List filtersFile = new ArrayList();
        filtersFile.add( getBasedir()
            + "/src/test/units-files/maven-resources-filtering/empty-maven-resources-filtering.txt" );

        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution( resources, outputDirectory,
                                                                                       mavenProject, null, filtersFile,
                                                                                       Collections.EMPTY_LIST,
                                                                                       new StubMavenSession() );
        mavenResourcesFiltering.filterResources( mavenResourcesExecution );

        File[] files = outputDirectory.listFiles();
        assertEquals( 1, files.length );
        assertEquals( "includefile.txt", files[0].getName() );

    }

    public void testIncludeOneFileAndDirectory()
        throws Exception
    {
        File baseDir = new File( "c:\\foo\\bar" );
        StubMavenProject mavenProject = new StubMavenProject( baseDir );
        mavenProject.setVersion( "1.0" );
        mavenProject.setGroupId( "org.apache" );
        mavenProject.setName( "test project" );

        MavenResourcesFiltering mavenResourcesFiltering = (MavenResourcesFiltering) lookup( MavenResourcesFiltering.class
            .getName() );

        String unitFilesDir = getBasedir() + "/src/test/units-files/maven-resources-filtering";

        Resource resource = new Resource();
        List resources = new ArrayList();
        resources.add( resource );
        resource.setDirectory( unitFilesDir );
        resource.setFiltering( true );
        resource.addInclude( "includ*" );
        resource.addInclude( "**/includ*" );

        List filtersFile = new ArrayList();
        filtersFile.add( getBasedir()
            + "/src/test/units-files/maven-resources-filtering/empty-maven-resources-filtering.txt" );

        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution( resources, outputDirectory,
                                                                                       mavenProject, null, filtersFile,
                                                                                       Collections.EMPTY_LIST,
                                                                                       new StubMavenSession() );
        mavenResourcesFiltering.filterResources( mavenResourcesExecution );

        File[] files = outputDirectory.listFiles();
        assertEquals( 2, files.length );
        File includeFile = new File( outputDirectory, "includefile.txt" );
        assertTrue( includeFile.exists() );

        includeFile = new File( new File( outputDirectory, "includedir" ), "include.txt" );
        assertTrue( includeFile.exists() );

    }

    public void testExcludeOneFile()
        throws Exception
    {
        File baseDir = new File( "c:\\foo\\bar" );
        StubMavenProject mavenProject = new StubMavenProject( baseDir );
        mavenProject.setVersion( "1.0" );
        mavenProject.setGroupId( "org.apache" );
        mavenProject.setName( "test project" );

        MavenResourcesFiltering mavenResourcesFiltering = (MavenResourcesFiltering) lookup( MavenResourcesFiltering.class
            .getName() );

        String unitFilesDir = getBasedir() + "/src/test/units-files/maven-resources-filtering";

        Resource resource = new Resource();
        List resources = new ArrayList();
        resources.add( resource );
        resource.setDirectory( unitFilesDir );
        resource.setFiltering( true );
        resource.addExclude( "*.gif" );
        resource.addExclude( "**/excludedir/**" );

        List filtersFile = new ArrayList();
        filtersFile.add( getBasedir()
            + "/src/test/units-files/maven-resources-filtering/empty-maven-resources-filtering.txt" );

        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution( resources, outputDirectory,
                                                                                       mavenProject, null, filtersFile,
                                                                                       Collections.EMPTY_LIST,
                                                                                       new StubMavenSession() );
        mavenResourcesFiltering.filterResources( mavenResourcesExecution );

        File[] files = outputDirectory.listFiles();
        assertEquals( 5, files.length );
        File includeFile = new File( outputDirectory, "includefile.txt" );
        assertTrue( includeFile.exists() );

        includeFile = new File( new File( outputDirectory, "includedir" ), "include.txt" );
        assertTrue( includeFile.exists() );

        File imageFile = new File( outputDirectory, "happy_duke.gif" );
        assertFalse( imageFile.exists() );

        File excludeDir = new File( outputDirectory, "excludedir" );
        assertFalse( excludeDir.exists() );
    }

}