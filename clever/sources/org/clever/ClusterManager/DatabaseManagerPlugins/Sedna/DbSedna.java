/*
 * The MIT License
 *
 * Copyright 2011 Alessio Di Pietro.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.clever.ClusterManager.DatabaseManagerPlugins.Sedna;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.cfoster.sedna.SednaUpdateService;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.clever.ClusterManager.DatabaseManager.DatabaseManagerPlugin;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Exceptions.CleverException;
import org.jdom.Element;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;

/**
 *
 * @author alessiodipietro
 */
public class DbSedna implements DatabaseManagerPlugin {
    private Agent owner;
    private Logger logger;
    private String serverURL;
    private String dbName;
    private String user;
    private String password;
    private String document;
    private String xpath = "/clever/cluster[@id='clustermain']";

    public DbSedna() throws CleverException {
        try {
            logger = Logger.getLogger("DbSednaPlugin");
            Properties prop = new Properties();
            InputStream in = getClass().getResourceAsStream("/org/clever/Common/Shared/logger.properties");
            prop.load(in);
            PropertyConfigurator.configure(prop);
            this.registerXMLDBDriver();
            logger.debug("DbSedna plugin created!");




        } catch (XMLDBException ex) {
            logger.error("XMLDB registration failed");
        } catch (IOException ex) {
            throw new CleverException("Error on reading logger.properties");
        }
    }

    private void registerXMLDBDriver() throws XMLDBException {
        try {
            Database sednaDatabase;

            Class clazz = Class.forName("net.cfoster.sedna.DatabaseImpl");
            sednaDatabase = (Database) (clazz.newInstance());
            DatabaseManager.registerDatabase(sednaDatabase);

        } catch (InstantiationException ex) {
            logger.error("Instantiation error registering XMLDB: " + ex);
        } catch (IllegalAccessException ex) {
            logger.error("Illegal access error registering XMLDB: " + ex);
        } catch (ClassNotFoundException ex) {
            logger.error("Class not found error registering XMLDB: " + ex);
        }
    }

    private Collection connect() throws XMLDBException {
        return DatabaseManager.getCollection("xmldb:sedna://" + serverURL + "/" + dbName, user, password);
    }

    /*
     * HM
     */
    @Override
    synchronized public void  insertNode(String hostId, String agentId, String node, String where, String location) throws CleverException {
        String updateStr = null;
        Collection collect = null;
        String xpathHm = xpath + "/hm[@name='" + hostId + "']";
        String xpathAgent = xpathHm + "/agent[@name='" + agentId + "']";

        /*
         * Element elem=new Element(node); elem.addContent(valore); XMLOutputter
         * outputter = new XMLOutputter(Format.getPrettyFormat()); String xmlNode=outputter.outputString(elem);
         */


        logger.debug("Connecting to XMLDB");

        //check HM node
        if (!checkHm(hostId)) {
            //if no hm node -> insert
            this.addHm(hostId);
        }
        //check Agent node
        if (!checkAgent(agentId, hostId, "hm")) {
            //if no agent node -> insert
            this.addAgent(agentId, hostId, "hm");
        }
        try {
            //insert data node
            collect = this.connect();
            SednaUpdateService serviceUpdate = (SednaUpdateService) collect.getService("SednaUpdateService", "1.0");

            updateStr = "update insert " + node + " "
                    + where + " document(\"" + this.document + "\")/" + xpathAgent + location;
            serviceUpdate.update(updateStr);
            //collect.close();
        } catch (XMLDBException ex) {
            logger.error("Insert node failed: " + ex.getMessage());
            throw new CleverException("HM database update failed! " + ex);
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
                throw new CleverException("Error closing connection " + ex);
            }
        }

    }

    /*
     * CM
     */
    @Override
    synchronized public void insertNode(String agentId, String node, String where, String location) throws CleverException {
        String updateStr = null;
        Collection collect = null;
        String xpathCm = xpath + "/cm";
        String xpathAgent = xpathCm + "/agent[@name='" + agentId + "']";
        if (!checkCm()) {
            this.addCm();
        }
        if (!checkAgent(agentId, null, "cm")) {
            this.addAgent(agentId, null, "cm");
        }
        try {
            //insert data node
            collect = this.connect();
            SednaUpdateService serviceUpdate = (SednaUpdateService) collect.getService("SednaUpdateService", "1.0");

            updateStr = "update insert " + node + " "
                    + where + " document(\"" + this.document + "\")/" + xpathAgent + location;
            serviceUpdate.update(updateStr);
            //System.out.println(updateStr);
            collect.close();
        } catch (XMLDBException ex) {
            logger.error("Insert node failed: " + ex.getMessage());
            throw new CleverException("CM database update failed! " + ex);
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
                throw new CleverException("Error closing connection " + ex);
            }
        }
    }

    @Override
    synchronized public void addHm(String hostId) {
        Collection collect = null;

        try {
            collect = this.connect();
            SednaUpdateService serviceUpdate = (SednaUpdateService) collect.getService("SednaUpdateService", "1.0");
            String updateStr = "update insert <hm name='" + hostId + "'></hm>"
                    + " into document(\"" + this.document + "\")/" + xpath;
            serviceUpdate.update(updateStr);

        } catch (XMLDBException ex) {
            logger.error("Insert HM node failed: " + ex.getMessage());
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
            }
        }

    }

    private void addCm() {
        Collection collect = null;
        try {
            collect = this.connect();
            SednaUpdateService serviceUpdate = (SednaUpdateService) collect.getService("SednaUpdateService", "1.0");
            String updateStr = "update insert <cm></cm>"
                    + " into document(\"" + this.document + "\")/" + xpath;
            serviceUpdate.update(updateStr);
        } catch (XMLDBException ex) {
            logger.error("Insert CM node failed: " + ex.getMessage());
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
            }

        }

    }

    @Override
    synchronized public boolean checkHm(String hostId) throws CleverException {
        boolean existsHm = false;
        Collection collect = null;
        try {
            collect = this.connect();
            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");

            //check HM node
            ResourceSet resultSet = serviceXQuery.queryResource(document, xpath + "/hm[@name='" + hostId + "']");
            ResourceIterator results = resultSet.getIterator();
            existsHm = results.hasMoreResources();
        } catch (XMLDBException ex) {
            logger.error("Check HM node failed: " + ex.getMessage());
            throw new CleverException("Check HM node failed!");
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
            }
        }

        return existsHm;
    }

    private boolean checkCm() {
        boolean existsCm = false;
        Collection collect = null;
        try {
            collect = this.connect();
            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");

            //check HM node
            ResourceSet resultSet = serviceXQuery.queryResource(document, xpath + "/cm");
            ResourceIterator results = resultSet.getIterator();
            existsCm = results.hasMoreResources();

        } catch (XMLDBException ex) {
            logger.error("Check CM node failed: " + ex.getMessage());
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
            }
        }

        return existsCm;

    }

    synchronized private boolean checkAgent(String agentId, String hostId, String owner) {
        boolean existsAgent = false;
        String filter = "";
        Collection collect = null;
        if (hostId != null) {
            filter = "[@name='" + hostId + "']";
        }
        try {

            collect = this.connect();
            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
            ResourceSet resultSet = serviceXQuery.queryResource(document, xpath + "/" + owner + filter + "/agent[@name='" + agentId + "']");
            ResourceIterator results = resultSet.getIterator();
            existsAgent = results.hasMoreResources();


        } catch (XMLDBException ex) {
            logger.error("Check Agent node failed: " + ex.getMessage());
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
            }
        }
        return existsAgent;
    }

    synchronized public void addAgent(String agentId, String hostId, String owner) {
        String filter = "";
        Collection collect = null;
        if (hostId != null) {
            filter = "[@name='" + hostId + "']";
        }
        try {
            collect = this.connect();
            SednaUpdateService serviceUpdate = (SednaUpdateService) collect.getService("SednaUpdateService", "1.0");
            String updateStr = "update insert <agent name='" + agentId + "'></agent>"
                    + " into document(\"" + this.document + "\")/" + xpath + "/" + owner + filter;
            serviceUpdate.update(updateStr);

        } catch (XMLDBException ex) {
            logger.error("Insert Agent node failed: " + ex.getMessage());
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());

            }

        }



    }

    
    
    
    
    private void init() {
        ResourceSet resultSet = null;
        ResourceIterator results = null;
        Collection collect = null;
        try {
            //check document
            collect = this.connect();
            //XMLResource resource = (XMLResource) collect.getResource(document);
            
            String [] resources=collect.listResources();
            Boolean exists=false;
            for(int i=0; i<resources.length;i++){
                if(resources[i].equals(document)){
                    exists=true;
                }
            }
            
            if (!exists) {
                SednaUpdateService serviceUpdate = (SednaUpdateService) collect.getService("SednaUpdateService", "1.0");
                String updateStr = "create document('" + document + "')";
                serviceUpdate.update(updateStr);

            }
            //check clever node
            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
            SednaUpdateService serviceUpdate = (SednaUpdateService) collect.getService("SednaUpdateService", "1.0");

            resultSet = serviceXQuery.queryResource(document, "/clever");
            results = resultSet.getIterator();
            if (!results.hasMoreResources()) {
                String updateStr = "update insert <clever></clever> "
                        + "into document('" + document + "')";
                serviceUpdate.update(updateStr);
            }
            //check cluster node
            resultSet = serviceXQuery.queryResource(document, "/clever/cluster[@id='clustermain']");
            results = resultSet.getIterator();
            if (!results.hasMoreResources()) {
                String updateStr = "update insert <cluster id='clustermain'></cluster> "
                        + "into document('" + document + "')//clever";
                serviceUpdate.update(updateStr);
            }

        } catch (XMLDBException ex) {
            logger.error("Check document failed: " + ex.getMessage());
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
            }
        }


    }
    

    @Override
    public void init(Element params, Agent owner) throws CleverException {
        this.serverURL = params.getChildText("serverURL");
        this.dbName = params.getChildText("dbName");
        this.user = params.getChildText("user");
        this.password = params.getChildText("password");
        this.document = params.getChildText("document");
        init();


    }

    @Override
    public String getName() {

        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getVersion() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /*
     * generic query
     *
     */
    @Override
    synchronized public String query(String xpath) throws CleverException {
        StringBuffer result = new StringBuffer();
        Collection collect = null;
        try {
            collect = this.connect();
            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
            ResourceSet resultSet = serviceXQuery.queryResource(document, this.xpath + xpath);
            ResourceIterator results = resultSet.getIterator();
            logger.debug("Executing query xpath=" + this.xpath + xpath);
            while (results.hasMoreResources()) {
                XMLResource resource = (XMLResource) results.nextResource();
                result.append(resource.toString());
                result.append('\n');
            }


        } catch (XMLDBException ex) {
            logger.error("Error executing query: " + ex);
            throw new CleverException("Error executing query: " + ex);
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
            }

        }
        return result.toString();
    }

    /*
     * HM Agent query
     *
     */
    @Override
    synchronized public String query(String hostId, String agentId, String location) throws CleverException {
        StringBuffer result = new StringBuffer();
        String filter = "[@name='" + hostId + "']";
        Collection collect = null;
        try {
            collect = this.connect();

            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
            ResourceSet resultSet = serviceXQuery.queryResource(document, xpath + "/hm" + filter + "/agent[@name='" + agentId + "']" + location);
            ResourceIterator results = resultSet.getIterator();
            logger.debug("Executing query xpath=" + this.xpath + "/hm" + filter + "/agent[@name='" + agentId + "']" + location);
            while (results.hasMoreResources()) {
                XMLResource resource = (XMLResource) results.nextResource();
                result.append(resource.toString());
                result.append('\n');
            }

        } catch (XMLDBException ex) {
            logger.error("Error executing query: " + ex);
            throw new CleverException("Error executing query: " + ex);
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
            }
        }
        return result.substring(0);
    }

    /*
     * CM Agent query
     *
     */
    @Override
    synchronized public String query(String agentId, String location) throws CleverException {
        StringBuffer result = new StringBuffer();
        Collection collect = null;
        try {
            collect = this.connect();
            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
            ResourceSet resultSet = serviceXQuery.queryResource(document, xpath + "/cm/agent[@name='" + agentId + "']" + location);
            ResourceIterator results = resultSet.getIterator();
            logger.debug("Executing query xpath=" + this.xpath + "/cm/agent[@name='" + agentId + "']" + location);
            while (results.hasMoreResources()) {
                XMLResource resource = (XMLResource) results.nextResource();
                result.append(resource.toString());
                //result.append('\n');
            }

        } catch (XMLDBException ex) {
            logger.error("Execute query failed: " + ex);
            throw new CleverException("Error executing query: " + ex);
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
            }
        }
        return result.substring(0);
    }

    // QUERY PER LO STORAGE
    /**
     * This method returns the attribute of a node
     *
     * @param agentId
     * @param location
     * @param tipo
     * @return
     * @throws XMLDBException
     */
    @Override
        synchronized public String getAttributeNode(String agentId,String location,String tipo) throws XMLDBException{
        String a="";   
        String xpathAgent ="/agent[@name='" + agentId + "']";
        Collection collect = this.connect();
        String updateStr = "let $p := document(\"" + this.document + "\")/" + xpathAgent + "" + location + "/@" + tipo + " return data($p)";
        XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
        ResourceSet resultSet = serviceXQuery.queryResource(document, updateStr);
        ResourceIterator results = resultSet.getIterator();

        if ("localpath".equals(tipo)) {
            while (results.hasMoreResources()) {
                Resource res = results.nextResource();
                a = a + (String) res.getContent() + "\n";
            }
        } else {
            while (results.hasMoreResources()) {
                Resource res = results.nextResource();
                a = (String) res.getContent();
            }
        }

        collect.close();
        //System.out.println(updateStr);
        return a;

    }

    /**
     * This method checks if the node exists
     *
     * @param agentId
     * @param location
     * @return
     */
    @Override
        synchronized public boolean existNode(String agentId,String location) {
        boolean state = false;
        String xpathAgent = "/agent[@name='" + agentId + "']";
        try {
            String updateStr = "document(\"" + this.document + "\")/" + xpathAgent + "" + location;
            //System.out.println(updateStr);
            Collection collect = this.connect();
            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
            ResourceSet resultSet = serviceXQuery.queryResource(document, updateStr);
            ResourceIterator results = resultSet.getIterator();
            state = results.hasMoreResources();
            //System.out.println(updateStr);
            collect.close();

        } catch (XMLDBException ex) {
            logger.error("find node failed: " + ex.getMessage());
        }
        return state;
    }

    /**
     * This method returns the children of a node
     *
     * @param agentId
     * @param location
     * @param tipo
     * @return
     */
    @Override
  synchronized public String getChild(String agentId,String location,String tipo) {
        String a="";     
        String xpathAgent ="/agent[@name='" + agentId + "']";
        try{
            Collection collect = this.connect();
            String updateStr = "let $p := document(\"" + this.document + "\")/" + xpathAgent + "" + location + "/node/@" + tipo + " return data($p)";
            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
            ResourceSet resultSet = serviceXQuery.queryResource(document, updateStr);
            ResourceIterator results = resultSet.getIterator();
            while (results.hasMoreResources()) {
                Resource res = results.nextResource();
                a = a + (String) res.getContent() + "\n";
            }

            collect.close();

        } catch (XMLDBException ex) {
            logger.error("Insert node failed: " + ex.getMessage());
        }
        return a;
    }

    /**
     * This method returns the content of a node
     *
     * @param agentId
     * @param location
     * @return
     * @throws XMLDBException
     */
    @Override
  synchronized public List getContentNode(String agentId,String location) throws XMLDBException{
        String xpathAgent ="/agent[@name='" + agentId + "']";
        List params = new ArrayList();
        Collection collect = this.connect();
        String updateStr = "document(\"" + this.document + "\")/" + xpathAgent + "" + location + "//*/text()";
        XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
        ResourceSet resultSet = serviceXQuery.queryResource(document, updateStr);
        ResourceIterator results = resultSet.getIterator();
        while (results.hasMoreResources()) {
            Resource res = results.nextResource();
            params.add(res.getContent());
        }
        collect.close();
        return params;
    }

    /**
     * This method returns the content XML of a node
     *
     * @param agentId
     * @param location
     * @param property
     * @return
     * @throws XMLDBException
     */
    @Override
      synchronized public String getContentNodeXML(String agentId,String location,String property) throws XMLDBException{
        String xpathAgent ="/agent[@name='" + agentId + "']";
        String a="";
        Collection collect = this.connect();
        String updateStr = "document(\"" + this.document + "\")/" + xpathAgent + "" + location + property;
        XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
        ResourceSet resultSet = serviceXQuery.queryResource(document, updateStr);
        ResourceIterator results = resultSet.getIterator();
        while (results.hasMoreResources()) {
            Resource res = results.nextResource();
            a = a + (String) res.getContent();
        }
        collect.close();
        //System.out.println(updateStr);
        return a;
    }

    /**
     * This method modifies a node from the database
     *
     * @param agentId
     * @param node
     * @param where
     * @param location
     * @throws CleverException
     */
    @Override
        synchronized public void updateNode(String agentId, String node, String where, String location) throws CleverException {
        String updateStr = null;
        Collection collect = null;
        String xpathCm = xpath + "/cm";
        String xpathAgent = xpathCm + "/agent[@name='" + agentId + "']";
        if (!checkCm()) {
            this.addCm();
        }
        if (!checkAgent(agentId, null, "cm")) {
            this.addAgent(agentId, null, "cm");
        }
        try {
            //insert data node
            collect = this.connect();
            SednaUpdateService serviceUpdate = (SednaUpdateService) collect.getService("SednaUpdateService", "1.0");

            updateStr = "update replace $a in document(\"" + this.document + "\")/" + xpathAgent + location + " " + where + " " + node;
            //System.out.println(updateStr);
            serviceUpdate.update(updateStr);

            collect.close();
        } catch (XMLDBException ex) {
            logger.error("Update node failed: " + ex.getMessage());
            throw new CleverException("CM database update failed! " + ex);
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
                throw new CleverException("Error closing connection " + ex);
            }
        }
    }

    /**
     * This method deletes a node from the database
     *
     * @param agentId
     * @param location
     * @throws CleverException
     */
    @Override
        synchronized public void deleteNode(String agentId, String location) throws CleverException {
        String updateStr = null;
        Collection collect = null;
        String xpathCm = xpath + "/cm";
        String xpathAgent = xpathCm + "/agent[@name='" + agentId + "']";

        try {
            //delete data node
            collect = this.connect();
            SednaUpdateService serviceUpdate = (SednaUpdateService) collect.getService("SednaUpdateService", "1.0");

            updateStr = "update delete "
                    + " document(\"" + this.document + "\")/" + xpathAgent + location;
            serviceUpdate.update(updateStr);
            collect.close();
        } catch (XMLDBException ex) {
            logger.error("Delete node failed: " + ex.getMessage());
            throw new CleverException("CM database update failed! " + ex);
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());
                throw new CleverException("Error closing connection " + ex);
            }
        }
    }

    /**
     *
     * @param agentId
     * @param location
     * @return
     * @throws XMLDBException
     */
    @Override
    synchronized public String getContentNodeObject(String agentId,String location) throws XMLDBException{
        String a="";
        String xpathAgent ="/agent[@name='" + agentId + "']";
        Collection collect = this.connect();
        String updateStr = "document(\"" + this.document + "\")/" + xpathAgent + "" + location + "/*";
        XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
        ResourceSet resultSet = serviceXQuery.queryResource(document, updateStr);
        ResourceIterator results = resultSet.getIterator();
        while (results.hasMoreResources()) {
            Resource res = results.nextResource();
            a = a + (String) res.getContent();
        }
        collect.close();
        //System.out.println(updateStr);
        return a;

    }

    @Override
    synchronized public boolean checkAgentNode(String agentId, String location) throws CleverException {
        boolean existsAgentNode = false;
        String filter = "";
        Collection collect = null;
        try {

            collect = this.connect();
            XQueryService serviceXQuery = (XQueryService) collect.getService("XQueryService", "1.0");
            ResourceSet resultSet = serviceXQuery.queryResource(document, xpath + "/cm/agent[@name='" + agentId + "']" + location);
            ResourceIterator results = resultSet.getIterator();
            existsAgentNode = results.hasMoreResources();


        } catch (XMLDBException ex) {
            logger.error("Check Agent node failed: " + ex.getMessage());
            throw new CleverException("Error checking node: " + ex);
        } finally {
            try {
                if (collect != null) {
                    collect.close();
                }
            } catch (XMLDBException ex) {
                logger.error("Error closing connection: " + ex.getMessage());

            }
        }
        return existsAgentNode;
    }

    @Override
    public void setOwner(Agent owner) {
        this.owner=owner;
    }
}
