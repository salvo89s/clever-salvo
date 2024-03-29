package org.clever.administration.commands;

import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.administration.ClusterManagerAdministrationTools;

/**
 *
 * @author Luca Ciarniello
 */
public class RemoveCleverVMCommand extends CleverCommand {
  @Override
  public Options getOptions() {
    Options options = new Options();
    options.addOption("n", true, "The name of the Virtual Environment.");
    options.addOption("xml", false, "Displays the XML request/response Messages.");
    options.addOption("debug", false, "Displays debug information.");
    return options;
  }

  @Override
  public void exec(final CommandLine commandLine) {
    try
    {
      ArrayList params = new ArrayList();
      params.add(commandLine.getOptionValue("n"));
      String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
      boolean res = (Boolean) ClusterManagerAdministrationTools.instance().execSyncAdminCommand(
              this, target, "StorageManagerAgent", "removeVe", params, commandLine.hasOption("xml"));

      if (res)
        System.out.println("Virtual Environment successfully removed");
      else
        System.out.println("Failed to remove the Virtual Environment");

    } catch (CleverException ex) {
      if (commandLine.hasOption("debug"))
        ex.printStackTrace();
      else
        logger.error(ex);
    }
  }

  @Override
  public void handleMessage(Object response) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  
   public void handleMessageError(CleverException e) {
        System.out.println(e);
    }
}