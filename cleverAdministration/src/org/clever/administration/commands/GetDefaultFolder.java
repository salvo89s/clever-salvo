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



public class GetDefaultFolder extends CleverCommand
{

  @Override
  public Options getOptions()
  {
    Options options = new Options();
    options.addOption( "h", true, "The name of the host manager." );
    options.addOption( "xml", false, "Dispalys the XML request/response Messages." );
    options.addOption( "debug", false, "Displays debug information." );
    return options;
  }



  @Override
  public void exec( CommandLine commandLine )
  {
    try
    {
          String target = commandLine.getOptionValue( "h" );

          String ris = (String) ClusterManagerAdministrationTools.instance().execSyncAdminCommand( this, target, "HyperVisorAgent", "getDefaultMachineFolder", new ArrayList(), commandLine.hasOption( "xml" ) );
          System.out.println(ris);

    }
    catch( CleverException ex )
    {
      logger.error(ex);
            if(commandLine.hasOption("debug"))
                 ex.printStackTrace();
            else
                System.out.println(ex);
    }

  }



  @Override
  public void handleMessage( Object response )
  {
    List returnResponse = ( List<String> ) response;
    System.out.println( "\n---------VMs----------" );
    for( int i = 0; i < returnResponse.size(); i++ )
    {
      System.out.println( returnResponse.get( i ) );
    }
    System.out.println( "\n----------------------" );
  }
  
   public void handleMessageError(CleverException e) {
        System.out.println(e);
    }
}
