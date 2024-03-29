/*
 *  Copyright (c) 2011 Antonio Nastasi
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documhentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */
package org.clever.administration.commands;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.administration.ClusterManagerAdministrationTools;
import org.clever.Common.XMLTools.MessageFormatter;


public class ListVMsCommand extends CleverCommand
{

  @Override
  public Options getOptions()
  {
    Options options = new Options();
    options.addOption( "h", true, "The name of the host manager." );
   // options.addOption( "getOS", false, "getOSType" );
    options.addOption( "hypvr", false, "Force the request to the hypervisor plugin of the the host manager \'h\'" );
    options.addOption( "doexception", false, "Generate an Exception in order to test the Exception managing \'h\'" );
    options.addOption( "onlyrunning", false, "Show only VM in running state \'h\'" );
    options.addOption( "xml", false, "Displays the XML request/response Messages." );
    options.addOption( "debug", false, "Displays debug information." );
    return options;
  }



  @Override
  public void exec( CommandLine commandLine )
  {
    try
    {
          String target = commandLine.getOptionValue( "h" );
          String listing = "listVms";
          //invoke HvVirtualbox Plugin method for testing exception management
          if(commandLine.hasOption("doexception"))
          {
              boolean ris = (Boolean) ClusterManagerAdministrationTools.instance().execSyncAdminCommand( this, target, "HyperVisorAgent", "testException", new ArrayList(), commandLine.hasOption( "xml" ) );

              return;
          }
//          if(commandLine.hasOption("getOS"))
//          {
//                List returnResponse = ( List )ClusterManagerAdministrationTools.instance().execSyncAdminCommand( this, target, "HyperVisorAgent", "getOSTypes", new ArrayList(), commandLine.hasOption( "xml" ) );
//                System.out.println( "\n---------VMs----------(sync)" );
//                  for( int i = 0; i < returnResponse.size(); i++ )
//                   {
//                    System.out.println( returnResponse.get( i ) );
//                  }
//                 System.out.println( "\n----------------------(sync)" );
//                   return;
//          }

          if( commandLine.hasOption( "hypvr" ) )
          {
            if( commandLine.hasOption( "onlyrunning" ) )
                listing = "listRunningHVms";
            else
                listing = "listHVms";
          } else
          {
              if( commandLine.hasOption( "onlyrunning" ) )
                listing = "listRunningVms";
            else
                listing = "listVms";
          }
          //ClusterManagerAdministrationTools.instance().execAdminCommand( this, target, "HyperVisorAgent", listing, new ArrayList(), commandLine.hasOption( "xml" ) );
          List returnResponse = ( List<String> )ClusterManagerAdministrationTools.instance().execSyncAdminCommand( this, target, "HyperVisorAgent", listing, new ArrayList(), commandLine.hasOption( "xml" ) );
          if(commandLine.hasOption( "xml" ))
          {
              System.out.println( MessageFormatter.messageFromObject(returnResponse));
          }
          System.err.println( "\n---------VMs----------(sync)" );
          for( int i = 0; i < returnResponse.size(); i++ )
          {
            System.out.println( returnResponse.get( i ) );
          }
          System.err.println( "\n----------------------(sync)" );
    }
    catch( CleverException ex )
    {
      logger.error(ex);
            if(commandLine.hasOption("debug"))
                 ex.printStackTrace();
            else
                System.err.println(ex);
    }

  }



  @Override
  public void handleMessage( Object response )
  {
    
  }
  
 public void handleMessageError(CleverException e) {
        System.out.println(e);
    }
 
}
