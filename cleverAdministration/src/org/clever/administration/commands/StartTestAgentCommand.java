package org.clever.administration.commands;

/**
 *
 * @author Maurizio Paone
 */


/**Command for testing purposes
*/

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


import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.administration.ClusterManagerAdministrationTools;



public class StartTestAgentCommand  extends CleverCommand
{

  @Override
  public Options getOptions()
  {
    Options options = new Options();
    options.addOption( "h", true, "The name of the Host Target" );
    options.addOption( "a", true, "Agent target for the method invocation " );
    options.addOption( "t", true, "Time period " );
    options.addOption( "m", true, "Method to be invoked" );
    options.addOption( "xml", false, "Displays the XML request/response Messages." );
    options.addOption( "debug", false, "Displays debug information." );
    return options;
  }



  @Override
  public void exec( CommandLine commandLine )
  {
    try
    {
          String cmTarget = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
          String HMtarget = commandLine.getOptionValue( "h" );
          String agentTarget = commandLine.getOptionValue( "a" );
          String methodTarget = commandLine.getOptionValue( "m" );
          ArrayList params = new ArrayList();
          params.add(HMtarget);
          params.add(agentTarget);
          params.add(methodTarget);
          params.add(new Long(5000));
          System.out.println( "start test; targets: "+methodTarget+" of the "+agentTarget+" agent of Host:"+HMtarget);
          Object returnResponse = ClusterManagerAdministrationTools.instance().execSyncAdminCommand( this, cmTarget, "TestAgent","startTest" ,params, commandLine.hasOption( "xml" ) );
          System.out.println( "Response: "+returnResponse.toString());
          //Object returnResponse = (  )ClusterManagerAdministrationTools.instance().execSyncAdminCommand( this, target, "MonitorAgent", command, new ArrayList(), commandLine.hasOption( "xml" ) );

    }
    catch( CleverException ex )
    {
     if(commandLine.hasOption("debug"))
     {
                 ex.printStackTrace();

     }
            else
                System.out.println(ex);
      logger.error( ex );
    }

  }



  @Override
  public void handleMessage( Object response )
  {
    if(response instanceof Exception)
    {
        System.out.println( "\nException received: " );
        ((Exception)response).printStackTrace() ;
        return;
    }
    System.out.println( "\nResponse received: " );
    System.out.println( response.toString() );

  }
  
   public void handleMessageError(CleverException e) {
        System.out.println(e);
    }
}
