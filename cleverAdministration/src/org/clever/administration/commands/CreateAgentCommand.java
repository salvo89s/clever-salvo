/*
 *  Copyright (c) 2011 Alessio Di Pietro
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
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
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.Common.Shared.HostEntityInfo;
import org.clever.administration.ClusterManagerAdministrationTools;

/**
 *
 * @author alessiodipietro
 */
public class CreateAgentCommand extends CleverCommand{

    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption( "xml", false, "Displays the XML request/response Messages." );
         options.addOption( "debug", false, "Displays debug information." );
         options.addOption("class",true,"The class name of the agent");
         options.addOption("h",true,"The name of the host manager/cluster manager");
        return options;
    }

    @Override
    public void exec(CommandLine commandLine) {
        
        List returnResponse;
        HostEntityInfo hostEntityInfo;
        try {
            ArrayList params = new ArrayList();
            params.add(commandLine.getOptionValue("class"));
            //String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
            String target=commandLine.getOptionValue("h");
            if (!target.equals("")) {
                returnResponse = ( List<HostEntityInfo> ) ClusterManagerAdministrationTools.instance().execSyncAdminCommand(this, target, "InfoAgent", "createAgent", params, commandLine.hasOption("xml"));
            }
        } catch (CleverException ex) {
            logger.error(ex);
            if(commandLine.hasOption("debug"))
                 ex.printStackTrace();
            else
                System.out.println(ex);
        }
    }

    @Override
    public void handleMessage(Object response) {
        System.out.println(response);
    }
    
     public void handleMessageError(CleverException e) {
        System.out.println(e);
    }

}
