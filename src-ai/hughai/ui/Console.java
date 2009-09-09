// Copyright Hugh Perkins 2009
// hughperkins@gmail.com http://manageddreams.com
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
// or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
//  more details.
//
// You should have received a copy of the GNU General Public License along
// with this program in the file licence.txt; if not, write to the
// Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-
// 1307 USA
// You can find the licence also on the web at:
// http://www.opensource.org/licenses/gpl-license.php
//
// ======================================================================================
//

package hughai.ui;

import java.util.*;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.activation.FileTypeMap;
import javax.swing.*;
import javax.script.*;
import javax.xml.transform.stream.StreamResult;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;

import hughai.*;
import hughai.loader.utils.Loader;
import hughai.utils.LogFile;

// provides a console tab to the gui that lets us execute code
// on the fly!
// is that wicked or wot! :-D
public class Console {
   final String classdir = "console-classes";
   final String jarfilename = "Console.jar";
   final String consoletemplatefilename = "ConsoleTemplate.txt";
   
//   final String classpath="$aidir/SkirmishAI.jar:$aidir/UnderlyingAI.jar:$aidir/../../Interfaces/Java/0.1/AIInterface.jar"; 

//   JFrame frame;
   JPanel panel;
   GridLayout gridLayout;
   JTextArea textarea;
   JButton gobutton;
   JButton quitbutton;

   JTextArea outputTextarea;
   
   PlayerObjects playerObjects;
   
   public Console( PlayerObjects playerObjects ) {
      this.playerObjects = playerObjects;
      
      init();
   }
   
   void init () {
      try {
//      frame = new JFrame("Console");
//      frame.setSize( 400, 400 );

      gridLayout = new GridLayout( 4, 1 );
      panel = new JPanel( gridLayout );
//      frame.add( panel );

      textarea = new JTextArea();
      JScrollPane scrollPane = new JScrollPane( textarea );

      outputTextarea = new JTextArea();
      JScrollPane outputscrollpane = new JScrollPane( outputTextarea );

      gobutton = new JButton( "Go" );
      quitbutton = new JButton( "Quit" );

      String templatepath = playerObjects.getCSAI().getAIDirectoryPath() + consoletemplatefilename;
      String initialFile = readFile( templatepath );
      if( initialFile != null ) {
         textarea.setText( initialFile );
      } else {
         textarea.setText("<Missing file " + templatepath + ">" );
      }

      gobutton.addActionListener( new GoButton() );
      quitbutton.addActionListener( new QuitButton() );

      panel.add( scrollPane );
      panel.add( outputscrollpane );
      panel.add( gobutton );
      panel.add( quitbutton );

      playerObjects.getMainUI().addPanelToTabbedPanel( "Console", panel );
//      frame.validate();
//      frame.setVisible( true );
      } catch( Exception e ) {
         e.printStackTrace();
         throw new RuntimeException( e );
      }
   }

   // should be moved to some generic utility class...
   // returns the contents of file filename
   // or null if not found
   String readFile( String filename ) {
      try {
         FileReader fileReader = new FileReader( filename );
         BufferedReader bufferedReader = new BufferedReader( fileReader );
         StringBuilder stringBuilder = new StringBuilder();
         String line = bufferedReader.readLine();
         while( line != null ) {
            stringBuilder.append( line );
            stringBuilder.append( "\n" );
            line = bufferedReader.readLine();
         }
         bufferedReader.close();
         fileReader.close();
         return stringBuilder.toString();
      } catch( Exception e ) {
         e.printStackTrace();
         return null;
      }
   }

   // executes commandstring, and returns a string with output
   // from errorstream and inversely named "input"stream
   // commandstring includes arguments
   // this should be moved to a different utility class really...
   String exec( String commandstring, String dir ) {
      try {
         StringBuilder output = new StringBuilder();
         output.append( "Executing " + commandstring + "\nin " + dir + "\n" );
         Process process = 
            Runtime.getRuntime().exec( commandstring, null,
                  new File( dir ) );
         output.append("executed" + "\n");
         process.waitFor();
         output.append("exit value: " + process.exitValue() + "\n" );
         InputStreamReader errorStreamReader = new InputStreamReader( process.getErrorStream() ); 
         BufferedReader errorReader = new BufferedReader(
               errorStreamReader );
         String line;
         //      if( errorReader.ready() ) {
         line = errorReader.readLine();
         while( line != null ) {
            output.append( "error stream: " + line + "\n" );
            line = null;
            //            if( errorReader.ready() ) {
            line = errorReader.readLine();
            //            }
         }
         //      }
         output.append("errorstream read" + "\n");
         InputStreamReader outStreamReader = new InputStreamReader( process.getInputStream() ); 
         BufferedReader outReader = new BufferedReader(
               outStreamReader );
         line = outReader.readLine();
         while( line != null ) {
            output.append( "out stream: " + line + "\n" );
            line = errorReader.readLine();
         }
         output.append("outputstream read" + "\n");   
         return output.toString();
      } catch( Exception e ) {
         e.printStackTrace();
         throw new RuntimeException( e );
      }
   }
   
   // kind of hacky quick fix for linux vs Windows
   // there must be a better way of doing this...
   String localizeClassPath( String classpath ) {
      if( File.separator.equals("/") ) { // linux (and mac?)
         return classpath.replace( ";", ":" ).replace( "\\", "/" );
      } else {  // windows
         return classpath.replace( ":", ";" ).replace( "/", "\\" );         
      }
   }
   
   void debug( Object message ) {
      playerObjects.getLogFile().WriteLine( "" + message );
   }

   class GoButton implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent event ) {
         try {
//            String ourdir = "/home/user/persist/workspace/Test/";
            String ourdir = playerObjects.getCSAI().getAIDirectoryPath();

            System.out.println( textarea.getText() );
            new File( ourdir + "src-console" + File.separator + "console").mkdirs();
            PrintWriter printWriter = new PrintWriter( 
                  ourdir
                  + "src-console" + File.separator
                  + "console" + File.separator
                  + "ConsoleText.java" );
            printWriter.write( textarea.getText() );
            printWriter.write( "\n" );
            printWriter.close();

            new File( ourdir + classdir ).mkdirs();
            //            Process process = 
            //               Runtime.getRuntime().exec( "bash -c pwd", null,
            //                     null );
            
            String classpath = localizeClassPath( 
                  playerObjects.getConfig().getConsoleclasspath() );
            classpath = classpath.replace( "$aidir/", ourdir );
            
            debug( exec( "javac -classpath " + classpath
                        + " -d " + ourdir + classdir + 
                        " console" + File.separator + "ConsoleText.java", 
                        ourdir + "src-console" ) );

            debug( exec( "jar -cf " + ourdir + jarfilename 
                        + " console", 
                        ourdir + classdir ) );

            // this should be moved to some generic class really
            URL[] locations = new URL[] { new File( ourdir + jarfilename )
            .toURI().toURL()};
            ClassLoader baseClassLoader = Console.class.getClassLoader();
            if( baseClassLoader == null ) {
               System.out.println("using system classloader as base");
               baseClassLoader = ClassLoader.getSystemClassLoader();
            } else {
               System.out.println("using our classloader as base");         
            }
            URLClassLoader classloader = new URLClassLoader(
                  locations, baseClassLoader );
            Class<?> cls = classloader.loadClass("console.ConsoleText");
            if (!ConsoleEntryPoint.class.isAssignableFrom(cls)) {
               throw new RuntimeException("Invalid class");
            }
            Object newInstance = cls.newInstance(); 
            ConsoleEntryPoint subjar = (ConsoleEntryPoint)newInstance;
            String result = subjar.go( playerObjects );
            outputTextarea.setText( result );
         } catch( Exception e ) {
            e.printStackTrace();
         }
      }      
   }

   class QuitButton implements ActionListener {
      @Override
      public void actionPerformed( ActionEvent event ) {
         System.exit(0);
      }      
   }
}
