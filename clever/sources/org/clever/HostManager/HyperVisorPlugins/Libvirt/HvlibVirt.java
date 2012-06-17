/*
 *  Copyright (c) 2010 Filippo Bua
 *  Copyright (c) 2010 Maurizio Paone
 *  Copyright (c) 2010 Francesco Tusa
 *  Copyright (c) 2010 Massimo Villari
 *  Copyright (c) 2010 Antonio Celesti
 *  Copyright (c) 2010 Antonio Nastasi
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
package org.clever.HostManager.HyperVisorPlugins.Libvirt;

import com.sun.jna.Pointer;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Exceptions.*;
import org.clever.Common.VEInfo.*;
import org.clever.HostManager.HyperVisor.HyperVisorPlugin;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.libvirt.*;
import org.libvirt.jna.ConnectionPointer;
import org.libvirt.jna.DomainPointer;
import org.libvirt.jna.Libvirt.VirConnectDomainEventGenericCallback;
import org.clever.Common.VEInfo.CpuSettings.Architecture;
import org.clever.Common.XMLTools.ParserXML;
import org.libvirt.jna.Libvirt;






class pollEventThread implements Runnable
{
    private Logger logger;
    public pollEventThread(Logger l)
    {
        logger=l;
    }
    @Override
    public void run() {
        while(true)
        {
            logger.info("loop iteration started");
        if(Libvirt.INSTANCE.virEventRunDefaultImpl()!=0)
            logger.error("error on event loop");
        logger.info("loop scheduled");
        }
    }

}



public class HvlibVirt implements HyperVisorPlugin , VirConnectDomainEventGenericCallback
{

  private Map<String, VMWrapper> idVMWrapper = new HashMap<String, VMWrapper>();
  private Agent owner;
  private Connect conn = null;
  private int counter;
  private Logger logger;
  private Architecture DEFAULT_ARCHITECTURE = Architecture.X86_64; //TODO GET THIS INFORMATION FROM HOST ARCHITECTURE



  public HvlibVirt() throws IOException
  {

    logger = Logger.getLogger( "Libvirt plugin" );
    try
    {
      Properties prop = new Properties();
      InputStream in = getClass().getResourceAsStream( "/org/clever/Common/Shared/logger.properties" );
      prop.load( in );
      PropertyConfigurator.configure( prop );
    }
    catch( java.lang.NullPointerException e )
    {
      throw new java.lang.NullPointerException( "Missing logger.properties" );
    }

    logger.info( "LibVirt plugin created: " );

  }


 @Override
    public void init(Element params, Agent owner) throws CleverException
  
  {
    try
    {
        String con , system;
        if((system = params.getChildText( "system" ))!=null)
            {
                if (system.equals("true"))
                    con="qemu:///system";
                else
                    con="qemu:///session";
            }    
        else
            con="qemu:///system";
                 
                    
      this.conn = new Connect( con, false );
     /* int res=99;
     if((res = Libvirt.INSTANCE.virEventRegisterDefaultImpl()) == -1)
          logger.error("Error on register event loop");
      logger.debug("virEventRegisterDefaultImpl restituisce: "+res);
      new Thread(new pollEventThread(logger),"Pollthread").start();
      */
      logger.info( "LibVirt plugin initialized: " + "connected to qemu:///system" );
      updateMap();
    }
    catch( LibvirtException e )
    {
      logger.error( "Error: " + e );
    }
  }

  

 
 
 public boolean shutDownVm( String id , Boolean poweroff) throws CleverException
 {
     if(!poweroff)
         return this.shutDownVm(id);
     try
    {
      Domain dom;
      dom = ( Domain ) resolveUUID( id ).getReference();
      if(dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_SHUTDOWN) == 0){
          logger.error("Error on shutDown: domain is already shut down");
          return (true);
        }
      if(!((dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_PAUSED) == 0) || (dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_RUNNING)) == 0)){
          logger.error("Error on shutDown: domain is not running");
          throw new StopException("Error on shutDown: domain is not running");
      }
      dom.destroy();
      logger.info( "VM " + id + " shutted" );
      return ( true );
    }
    catch( Exception ex )
    {
      logger.error( "Error on shutDown: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
 }
 
  public boolean shutDownVm( String id ) throws CleverException
  {
    try
    {
      Domain dom;

      dom = ( Domain ) resolveUUID( id ).getReference();
      if(dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_SHUTDOWN) == 0){
          logger.error("Error on shutDown: domain is already shut down");
          return (true);
        }
      if(!((dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_PAUSED) == 0) || (dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_RUNNING)) == 0)){
          logger.error("Error on shutDown: domain is not running");
          throw new StopException("Error on shutDown: domain is not running");
      }
      dom.shutdown();
      
      return ( true );
    }
    catch( Exception ex )
    {
      logger.error( "Error on shutDown: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
  }



  public boolean restoreVm( String path ) throws CleverException
  {
    try
    {
      conn.restore( path );
      logger.info( "VM " + path + " restored" );

      return ( true );
    }
    catch( LibvirtException ex )
    {
       logger.error( "Error on restore: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
  }



  public boolean resume( String id ) throws CleverException
  {
    Domain dom;
    try
    {
      dom = ( Domain ) resolveUUID( id ).getReference();
      if(!(dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_PAUSED) == 0)){
        logger.error("Error on resume: cannot resume vm as it is not paused");
        throw new ResumeException("cannot resume vm as it is not paused");
      }
      dom.resume();
      logger.info( "VM " + id + " resumed" );
      return ( true );
    }
    catch( Exception ex )
    {
      logger.error( "Error on resume: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
  }


  public boolean suspend( String id ) throws CleverException{
      try{
          Domain domain = ( Domain ) resolveUUID(id).getReference();
          if(!(domain.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_RUNNING) == 0)){
              logger.error("Error on suspend: domain is not running");
              throw new SuspendException("Error on suspend: domain is not running");
          }
          domain.suspend();
          return (true);
      }
      catch( LibvirtException ex){
          logger.error("Error on suspend: "+ex);
          throw new HyperVisorException(ex.getMessage());
      }
  }


  public boolean createAndStart( String id, VEDescription vmD ) throws CleverException
  {
    createVm( id, vmD );
    return ( startVm( id ) );
  }


  
  public boolean startMonitor() throws CleverException
  {
      
      
      int res=99;
    if((res = Libvirt.INSTANCE.virEventRegisterDefaultImpl()) == -1)
          logger.error("Error on register event loop");
      logger.debug("virEventRegisterDefaultImpl restituisce: "+res);
        try {
            
            this.conn.domainEventRegisterAny(null, 0, this);
        } catch (LibvirtException ex) {
            throw new HyperVisorException("error on callback registration: "+ex.getMessage());
        }
      return true;
  }
  

  public String getName()
  {
    return ( "HvlibVirt" );
  }



  public String getVersion()
  {
    try
    {
      return ( Long.toString( conn.getLibVirVersion() ) );
    }
    catch( LibvirtException ex )
    {
      logger.error( "Error: " + ex );
      return ( null );
    }
  }



  public String getDescription()
  {
    return ( "This plugin provides a virtualizing mechanism through libvirt" );
  }



  private VMWrapper resolveUUID( String id ) throws CleverException
  {
    try
    {
        Domain dom = conn.domainLookupByName(id);
        return new VMWrapper(dom, null);
    }
    catch( Exception ex )
    {
      logger.error( "Error: " + ex );
      throw new HyperVisorException( "id not found" );
    }
  }



  public String xmlToString( String path ) throws IOException
  {
    StringBuffer fileData = new StringBuffer( 1000 );
    BufferedReader reader = new BufferedReader( new FileReader( path ) );
    char[] buf = new char[ 1024 ];
    int numRead = 0;
    while( ( numRead = reader.read( buf ) ) != -1 )
    {
      String readData = String.valueOf( buf, 0, numRead );
      fileData.append( readData );
      buf = new char[ 1024 ];
    }
    reader.close();
    return fileData.toString();
  }



  public boolean saveState( String id, String path ) throws CleverException
  {
    Domain dom;
    try
    {
      dom = ( Domain ) resolveUUID( id ).getReference();
      if(!(dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_PAUSED) == 0) || (dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_RUNNING) == 0)){
           logger.error("Error on savestate: Virtual machine state neither Running nor Paused. ");
           throw new SaveStateException("Virtual machine state neither Running nor Paused");
      }
      dom.save(path);
      return ( true );
    }
    catch( Exception ex )
    {
      logger.error( "Error on savestate: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
  }



  public boolean createVm( String id, VEDescription veD ) throws CleverException
  {
    Domain d = null;
    
    try
    {
      d = conn.domainDefineXML( veDescriptionToXML( veD ) );
      VMWrapper vmW = new VMWrapper( d, veD );
      idVMWrapper.put( id, vmW );
      logger.info( "VM " + id + " created" );
      return true;
    }
    catch( Exception ex )
    {
      logger.error( "Error: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
    
  }



  public boolean resumeState( String id, String path ) throws CleverException
  {
    try
    {
      Domain dom = (Domain) resolveUUID(id).getReference();
      if(!(dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF) == 0)){
          logger.error("Error on resumeState: domain is already active");
          throw new ResumeStateException("Error on resumeState: domain is already active");
      }
      conn.restore( path );
      logger.info( "VM " + id + " resumed" );
      return ( true );
    }
    catch( LibvirtException ex )
    {
       logger.error( "Error on resumeState: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
  }



  public boolean startVm( String id ) throws CleverException
  {  
    try
    {
      int j;
      Domain dom;
      dom = ( Domain ) resolveUUID( id ).getReference();
      dom.create();
      logger.info( "VM " + id + " started" );
      return true;
    }
    catch( Exception ex )
    {
      logger.error( "Error on startVm: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
  }



  public boolean isRunning( String id ) throws CleverException
  {
    try
    {
      Domain dom = null;
      DomainInfo dominf = dom.getInfo();
      if( dominf.state.name().compareTo( DomainInfo.DomainState.VIR_DOMAIN_RUNNING.name() ) == 0 )
      {
        return ( true );
      }
      else
      {
        return ( false );
      }
    }
    catch( LibvirtException ex )
    {
      logger.error( "Error: " + ex );
      throw new HyperVisorException("Unknown error :" + ex.getMessage());
    }
  }



  public boolean addAdapter( String id, NetworkSettings settings )
  {
    logger.error( "addAdapter Not supported yet " );
    throw new UnsupportedOperationException( "Not supported yet." );
  }



//lista le vm secondo libvirt
  public List listHVms() throws CleverException
  {
    try
    {
      int[] activedomains = conn.listDomains();
      String[] inactivedom = conn.listDefinedDomains();
      ArrayList l = new ArrayList();
      Domain dominio;
      for( int id : activedomains )
      {
        dominio = conn.domainLookupByID( id );
        logger.info( "dominio -- " + dominio.getName() );
        l.add( dominio.getName() );
      }
       l.addAll(Arrays.asList(inactivedom));
      logger.info("List VMS returned number of machines: " + l.size());
      return ( l );
    }
    catch( LibvirtException ex )
    {
       logger.error( "Error on listHVms: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
  }

  public List listRunningHVms() throws CleverException{
      try{
          ArrayList l = new ArrayList();
          Domain dom;
          int[] domini = conn.listDomains();
          for(int d : domini){
              dom = conn.domainLookupByID(d);
              if(dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_RUNNING) == 0)
                l.add(dom.getName());
          }
          logger.info( "List running Vms returned number of machine: "+ l.size() );
          return (l);
      }
      catch(LibvirtException ex){
          logger.error("Error on listRunningHVms: "+ex.getMessage());
          throw  new HyperVisorException(ex.getMessage());
      }
  }



  public List listVms() throws CleverException
  {
    try
    {
      logger.info( "Returning the idVMWrapper map" );

      List wrapper = new ArrayList();


      Iterator it = idVMWrapper.entrySet().iterator();
      while( it.hasNext() )
      {
        Map.Entry pairs = ( Map.Entry ) it.next();
        //UuidVEDescriptionWrapper temp = new UuidVEDescriptionWrapper();

        //temp.setUuid( ( String ) pairs.getKey() );

        //VMWrapper tempWrapper = ( VMWrapper ) pairs.getValue();


        //temp.setVeDescriptor( tempWrapper.getDescription() );
        //wrapper.add( temp );
        wrapper.add(( String ) pairs.getKey());


      }


      return wrapper;
         

    }
    catch( Exception ex )
    {
      logger.error( "Error on getVMMap : " + ex );
      throw new CleverException(ex.getMessage());
    }
  }


  public List listRunningVms() throws CleverException{
      String name = "";
      try{
          ArrayList l = new ArrayList(idVMWrapper.keySet());
          ArrayList l2 = new ArrayList();
          Domain dom;
          for(Object obj : l){
              name = (String) obj;
              dom = conn.domainLookupByName(name);
              if(dom.getInfo().state.compareTo(DomainInfo.DomainState.VIR_DOMAIN_RUNNING) == 0)
                  l2.add(name);
          }
           logger.info( "List running Vms returned number of machine: "+ l2.size() );
             return ( l2 );
      }
      catch(Exception ex){
          logger.error("Eror on listRunningVms: "+ex.getMessage());
          throw new CleverException(ex.getMessage());
      }
  }


  private String veDescriptionToXML( VEDescription vmD )
  {
    StorageSettings hd = ( StorageSettings ) vmD.getStorage().get( 0 );
    Element root = new Element( "domain" );
    root.setAttribute( "type", "kvm" );
    Document doc = new Document( root );
    Element name_vm = new Element( "name" );
    name_vm.addContent( vmD.getName() );
    root.addContent( name_vm );
    Element memory = new Element( "memory" );
    memory.addContent( String.valueOf( vmD.getMemorySettings().getSize() ) );
    root.addContent( memory );
    Element cpu_sett = new Element( "vcpu" );
    cpu_sett.setAttribute("placement", "static");
    cpu_sett.addContent( String.valueOf( vmD.getCpu().getNumCpu() ) );
    root.addContent( cpu_sett );
    Element os = new Element( "os" );
    Element type = new Element( "type" );

    switch( vmD.getCpu().getArchitecture() )
    {
        case X86: //defined in cpusettings.architecture
        type.setAttribute( "arch", "i686" );
        break;
      case X86_64:
        type.setAttribute( "arch", "x86_64" );
        break;
    }

    type.setAttribute( "machine", "pc" );
    type.setText( "hvm" );
    os.addContent( type );
    Element boot = new Element( "boot" );
    boot.setAttribute( "dev", "hd" );
    os.addContent( boot );
    root.addContent( os );
    Element devices = new Element( "devices" );
    //Element emulator = new Element("emulator");
    //emulator.setText("/usr/bin/qemu-system-x86_64");
    //devices.addContent(emulator);
    Element disk = new Element( "disk" );
    disk.setAttribute( "type", "file" );
    disk.setAttribute( "device", "disk" );
    StringTokenizer st= new StringTokenizer(hd.getDiskPath(),".");
    String extension=null;
    
    while (st.hasMoreTokens())
        extension=st.nextToken();
    
    if (!(extension.equals("img")))
        {
            Element driver= new Element ("driver");
            driver.setAttribute("name","qemu");
            driver.setAttribute("type",extension);
            disk.addContent(driver);
        }
    Element source = new Element( "source" );
    // Temporary settings
    //source.setAttribute("file", "/home/sabayonuser/software/testbed/clever/img/custom_debian.img");
    source.setAttribute( "file", hd.getDiskPath() );
    disk.addContent( source );
    Element target = new Element( "target" );
    target.setAttribute( "dev", "hda" );
    disk.addContent( target );
    devices.addContent( disk );
    //Element interf = new Element("interface");
    //interf.setAttribute("type", "network");
    //Element source_net = new Element("source");
    //source_net.setAttribute("network", "default");
    //source_net.setAttribute("network", "none");
    //interf.addContent(source_net);
    //devices.addContent(interf);
    Element graphics = new Element( "graphics" );
    graphics.setAttribute( "type", "vnc" );
    graphics.setAttribute( "port", "-1" );
    devices.addContent( graphics );
    root.addContent( devices );
    XMLOutputter xout = new XMLOutputter();
    Format f = Format.getPrettyFormat();
    xout.setFormat( f );
    return ( xout.outputString( doc ) );
  }



 private  void updateMap(){
          try{
            ArrayList listCl = new ArrayList(idVMWrapper.keySet());
            ArrayList listLib = new ArrayList (listHVms());
            logger.debug("Output listHVMs: "+listLib.toString());           
            if (listCl.isEmpty()){
               for(Object id1 : listLib){
                    String id = (String)id1;
                    logger.info("VM adding: "+id);
                    VMWrapper wrap = createVMwrapper(id);
                    idVMWrapper.put(id, wrap);
                    logger.info("VM added: "+id);
               }
               return;
            }
	    else{
		  for(Object id1 : listLib){

		      boolean a = false;
		      for(Object id2 : listCl){
			  if(id1.equals(id2)){
			      a = true;
			      break;
			  }
		      }
		      if(a == false){
			  String id = id1.toString();
			  VMWrapper wrap = createVMwrapper(id);
			  idVMWrapper.put(id, wrap);
		      }
		  }
              }
            
            
        }
          catch(CleverException ex){
              logger.error("Error on updateMap: "+ex.getMessage());
          }
    }

  private VMWrapper createVMwrapper(String id) throws CleverException{

          try{
              Domain domain = conn.domainLookupByName(id);
              if(domain==null)
                  throw (new HyperVisorException("Error on create the VM Wrapper: "+id+" not found"));
              MemorySettings memory = new MemorySettings(domain.getMaxMemory());
              CpuSettings cpu = new CpuSettings(domain.getInfo().nrVirtCpu, 0, domain.getInfo().cpuTime, DEFAULT_ARCHITECTURE);
              ParserXML pars = new ParserXML(domain.getXMLDesc(0));
              logger.debug("VM Description: "+domain.getXMLDesc(0));
              StorageSettings store = new StorageSettings(0, "hd", "",pars.getElementAttributeContent("source", "file") );//TODO: MORE THAN ONE HD ?
              List storage = new ArrayList();
              storage.add(store);
              VEDescription ved = new VEDescription(storage, null, id, cpu, memory, null);//TODO: Network and remote desktop
              
              VMWrapper wrap = new VMWrapper(domain, ved);
              return wrap;
            }

            catch(Exception e){
                 logger.error("Error on createVMWrapper: " + e);
                 e.printStackTrace();
                throw new HyperVisorException(e.getMessage());
            }
        }


  // *****************             gestione snapshot                ***************


  
public boolean takeSnapshot(String id, String nameS, String description) throws CleverException{
    try{
        Domain domain = (Domain) resolveUUID(id).getReference();
        domain.snapshotCreateXML(XmlSnapshot(id, nameS, description));
        return true;
    }
    catch(LibvirtException ex){
        logger.error(ex.getMessage());
        throw new HyperVisorException("Error on takeSnapshot");
    }
}


private String XmlSnapshot(String id, String nameS, String descriptionS) throws CleverException{
    Domain domain = (Domain) resolveUUID(id).getReference();
    Element root = new Element("domainsnapshot");
    Document doc = new Document(root);
    Element name = new Element("name");
    name.addContent(nameS);
    root.addContent(name);
    Element descr = new Element("description");
    descr.addContent(descriptionS);
    root.addContent(descr);
    XMLOutputter xout = new XMLOutputter();
    Format f = Format.getPrettyFormat();
    xout.setFormat( f );
    return ( xout.outputString( doc ) );

}

public boolean restoreSnapshot(String id, String nameS) throws CleverException{
    try{
        Domain domain = (Domain) resolveUUID(id).getReference();
        DomainSnapshot snap = domain.snapshotLookupByName(nameS);
        int i = domain.revertToSnapshot(snap);
        return (true);
    }
    catch(LibvirtException ex){
        logger.error("Error on restoreSnapshot: " + ex);
        throw new HyperVisorException(ex.getMessage());
    }
}

public boolean deleteSnapshot(String id, String nameS) throws CleverException{
    try{
        Domain domain = (Domain) resolveUUID(id).getReference();
        DomainSnapshot snap = domain.snapshotLookupByName(nameS);
        int i = snap.delete(0);
        snap.free();
        return (true);
    }
    catch(LibvirtException ex){
        logger.error("Error on deleteSnapshot: " + ex);
        throw new HyperVisorException(ex.getMessage());
    }
}

public String currentSnapshot(String id) throws CleverException{
    try{
         Domain domain = (Domain) resolveUUID(id).getReference();
         if(domain.hasCurrentSnapshot() == 1){
            DomainSnapshot snap = domain.snapshotCurrent();
            ParserXML pars = new ParserXML(snap.getXMLDesc());
            String nameS = pars.getElementContent("name");
            return (nameS);
        }
        else
            throw new HyperVisorException("the machine "+ id + " currently has no snapshot");

    }
    catch(LibvirtException ex){
        logger.error("Error on the currentSnapshot: " + ex);
        throw new HyperVisorException(ex.getMessage());
    }
}

public long snapshotCount(String id) throws CleverException{
    try{
        Domain domain = (Domain) resolveUUID(id).getReference();
        return ( domain.snapshotNum());
    }
    catch(LibvirtException ex){
        logger.error("error on snapshotCount: " + ex);
        throw  new HyperVisorException(ex.getMessage());
    }
}

    

    @Override
    public void eventCallback(ConnectionPointer cp, DomainPointer dp, Pointer pntr) {
        return;
    }

    @Override
    public boolean destroyVm(String id) throws CleverException {
        try
    {
      Domain dom;
      dom = ( Domain ) resolveUUID( id ).getReference();
      dom.destroy();
      logger.info( "VM " + id + " destroied" );
      return true;
    }
    catch( Exception ex )
    {
      logger.error( "Error on destroyVm: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
       
    }

   

   

    @Override
    public List getOSTypes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean cloneVM(String id, String clone, String description) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean deleteAllSnapshot(String id) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean renameVM(String id, String new_id) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean resetVM(String id) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List listSnapshot(String id) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean renameSnapshot(String id, String snapName, String newSnapName, String description) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean attachPortRemoteAccessVm(String id) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void releasePortRemoteAccessVm(String id) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean registerVm(String id, String path) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean unregisterVm(String id) throws CleverException {
        try
    {
      Domain dom;
      dom = ( Domain ) resolveUUID( id ).getReference();
      dom.undefine();
      logger.info( "VM " + id + "undefined" );
      return true;
    }
    catch( Exception ex )
    {
      logger.error( "Error on unregisterVm: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
        
    }

    @Override
    public boolean ExportOvfToLocal(String id, String TargetPhysicalPath) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean ImportLocalOvf(String id, String OVF_physicalPath) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOwner(Agent owner) {
        this.owner=owner;
    }

    public String getLocalPath(String id) throws HyperVisorException{
     
      
      try{
     
      Domain dom;
      dom = ( Domain ) resolveUUID( id ).getReference();
      InputStream is = new ByteArrayInputStream(dom.getXMLDesc(0).getBytes("UTF-8"));
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      org.w3c.dom.Document doc = builder.parse(is);
      String path=doc.getElementsByTagName("source").item(0).getAttributes().item(0).getTextContent();
      return path;
    }
    catch( Exception ex )
    {
      logger.error( "Error on startVm: " + ex );
      throw new HyperVisorException(ex.getMessage());
    }
       
    }
 
 
 
 
 
 
}
