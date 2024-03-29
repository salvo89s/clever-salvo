package org.clever.administration.commands;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.Shared.VirtualMachine;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.administration.ClusterManagerAdministrationTools;

/**
 *
 * @author Luca Ciarniello
 */
public class ListVMsDbCommand extends CleverCommand {
  @Override
  public Options getOptions() {
    Options options = new Options();
    options.addOption("h", true, "Lists VMs on this host only");
    options.addOption("running", false, "Lists running VMs only");
    options.addOption("debug", false, "Print stacktrace of Exceptions");
    options.addOption("xml", false, "Displays the XML request/response Messages." );
    return options;
  }

  @Override
  public void exec(CommandLine commandLine) {
    try
    {
      ArrayList params = new ArrayList();
      params.add(commandLine.hasOption("running"));
      params.add(commandLine.hasOption("h") ? commandLine.getOptionValue("h") : "n/a");
      String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);

      List<VirtualMachine> vmlist =
              (List<VirtualMachine>) ClusterManagerAdministrationTools.instance().execSyncAdminCommand(
              this, target, "DatabaseManagerAgent", "listVMs", params, commandLine.hasOption("xml"));
      if (vmlist == null)
      {
        System.out.println("Error during the listing of the VMs");
        return;
      }

      if (vmlist.isEmpty())
      {
        System.out.println("No VM to list");
        return;
      }

      System.out.println("List of Vms");
      for (int i = 0; i < vmlist.size(); i++)
        System.out.println("VM " + vmlist.get(i).Name + " on Host " +
                vmlist.get(i).Host + (vmlist.get(i).isRunning ? " *" : ""));

    } catch (CleverException ex) {
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
  public void handleMessage(Object response) {
    System.out.println(response);
  }
  
 public void handleMessageError(CleverException e) {
        System.out.println(e);
    }
 
}