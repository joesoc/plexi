// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.adaptor;

import org.w3c.dom.*;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;


/** Makes XML metadata-and-url feed file from DocIds.
  This code is based on information provided by Google at
  http://code.google.com/apis/searchappliance/documentation/64/feedsguide.html
 */
class GsaFeedFileMaker {
  // DateFormats are relatively expensive to create, and cannot be used from
  // multiple threads
  private static ThreadLocal<DateFormat> rfc822Format
      = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
          DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
          df.setTimeZone(TimeZone.getTimeZone("GMT"));
          return df;
        }
      };

  private DocIdEncoder idEncoder;

  public GsaFeedFileMaker(DocIdEncoder encoder) {
    this.idEncoder = encoder;
  }

  /** Adds header to document's root.
      @param srcName Used as datasource name. */
  private void constructMetadataAndUrlFeedFileHead(Document doc,
      Element root, String srcName) {
    Comment comment = doc.createComment("GSA EasyConnector");
    root.appendChild(comment);
    Element header = doc.createElement("header");
    root.appendChild(header);
    Element datasource = doc.createElement("datasource");
    header.appendChild(datasource);
    Element feedtype = doc.createElement("feedtype");
    header.appendChild(feedtype);
    Text srcText = doc.createTextNode(srcName);
    datasource.appendChild(srcText);
    Text feedText = doc.createTextNode("metadata-and-url");
    feedtype.appendChild(feedText);
  }

  /** Adds a single record to feed-file-document's group,
      communicating the information represented by DocId. */
  private void constructSingleMetadataAndUrlFeedFileRecord(
      Document doc, Element group, DocIdPusher.Record docRecord) {
    DocId docForGsa = docRecord.getDocId();
    Element record = doc.createElement("record");
    group.appendChild(record);
    record.setAttribute("url", "" + idEncoder.encodeDocId(docForGsa));
    if (null != docRecord.getResultLink()) {
      record.setAttribute("displayurl", "" + docRecord.getResultLink());
    } else {
      // Explicitly clear the displayurl. Sending no displayurl attribute causes
      // a previous displayurl to be maintained.
      record.setAttribute("displayurl", "");
    }
    if (docRecord.isToBeDeleted()) {
      record.setAttribute("action", "delete");
    }
    record.setAttribute("mimetype", "text/plain"); // Required but ignored :)
    if (null != docRecord.getLastModified()) {
      String dateStr = rfc822Format.get().format(docRecord.getLastModified());
      record.setAttribute("last-modified", dateStr);
    }
    if (docRecord.isToBeLocked()) {
      record.setAttribute("lock", "true");
    }
    if (docRecord.isToBeCrawledImmediately()) {
      record.setAttribute("crawl-immediately", "true");
    }
    if (docRecord.isToBeCrawledOnce()) {
      record.setAttribute("crawl-once", "true");
    }
    // TODO(pjo): record.setAttribute(no-follow,);
  }

  /**
   * Adds a single ACL tag to the provided group, communicating the named
   * resource's information provided in {@code docAcl}.
   */
  private void constructSingleMetadataAndUrlFeedFileAcl(
      Document doc, Element group, DocIdSender.AclItem docAcl) {
    Element aclElement = doc.createElement("acl");
    group.appendChild(aclElement);
    aclElement.setAttribute("url",
        idEncoder.encodeDocId(docAcl.getDocId()).toString());
    Acl acl = docAcl.getAcl();
    if (acl.getInheritFrom() != null) {
      aclElement.setAttribute("inherit-from",
          idEncoder.encodeDocId(acl.getInheritFrom()).toString());
    }
    if (acl.getInheritanceType() != Acl.InheritanceType.LEAF_NODE) {
      aclElement.setAttribute("inheritance-type",
          acl.getInheritanceType().getCommonForm());
    }
    for (String permitUser : acl.getPermitUsers()) {
      constructPrincipal(doc, aclElement, "user", "permit", permitUser);
    }
    for (String permitGroup : acl.getPermitGroups()) {
      constructPrincipal(doc, aclElement, "group", "permit", permitGroup);
    }
    for (String denyUser : acl.getDenyUsers()) {
      constructPrincipal(doc, aclElement, "user", "deny", denyUser);
    }
    for (String denyGroup : acl.getDenyGroups()) {
      constructPrincipal(doc, aclElement, "group", "deny", denyGroup);
    }
  }

  private void constructPrincipal(Document doc, Element acl, String scope,
      String access, String principal) {
    Element principalElement = doc.createElement("principal");
    principalElement.setAttribute("scope", scope);
    principalElement.setAttribute("access", access);
    principalElement.appendChild(doc.createTextNode(principal));
    acl.appendChild(principalElement);
  }

  /** Adds all the DocIds into feed-file-document one record
    at a time. */
  private void constructMetadataAndUrlFeedFileBody(Document doc,
      Element root, List<? extends DocIdSender.Item> items) {
    Element group = doc.createElement("group");
    root.appendChild(group);
    for (DocIdSender.Item item : items) {
      if (item instanceof DocIdPusher.Record) {
        constructSingleMetadataAndUrlFeedFileRecord(doc, group,
                                                    (DocIdPusher.Record) item);
      } else if (item instanceof DocIdSender.AclItem) {
        constructSingleMetadataAndUrlFeedFileAcl(doc, group,
                                                 (DocIdSender.AclItem) item);
      } else {
        throw new IllegalArgumentException("Unable to process class: "
                                           + item.getClass().getName());
      }
    }
  }

  /** Puts all DocId into metadata-and-url GSA feed file. */
  private void constructMetadataAndUrlFeedFile(Document doc,
      String srcName, List<? extends DocIdSender.Item> items) {
    Element root = doc.createElement("gsafeed");
    doc.appendChild(root);
    constructMetadataAndUrlFeedFileHead(doc, root, srcName);
    constructMetadataAndUrlFeedFileBody(doc, root, items);
  }

  /** Makes a Java String from the XML feed-file-document passed in. */
  private String documentToString(Document doc)
      throws TransformerConfigurationException, TransformerException {
    TransformerFactory transfac = TransformerFactory.newInstance();
    Transformer trans = transfac.newTransformer();
    String doctype = "-//Google//DTD GSA Feeds//EN";
    trans.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype);
    trans.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "");
    trans.setOutputProperty(OutputKeys.INDENT, "yes");
    trans.setOutputProperty(OutputKeys.STANDALONE, "no");
    StringWriter sw = new StringWriter();
    StreamResult result = new StreamResult(sw);
    DOMSource source = new DOMSource(doc);
    trans.transform(source, result);
    String xmlString = "" + sw;
    return xmlString;
  }

  /** Makes a metadata-and-url feed file from upto 
     provided DocIds and source name.  Is used by
     GsaCommunicationHandler.pushDocIds(). */
  public String makeMetadataAndUrlXml(String srcName,
                                      List<? extends DocIdSender.Item> items) {
    try {
      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      Document doc = docBuilder.newDocument();
      constructMetadataAndUrlFeedFile(doc, srcName, items);
      String xmlString = documentToString(doc); 
      return xmlString;
    } catch (TransformerConfigurationException tce) {
      throw new IllegalStateException(tce);
    } catch (TransformerException te) {
      throw new IllegalStateException(te);
    } catch (ParserConfigurationException pce) {
      throw new IllegalStateException(pce);
    }
  }
}