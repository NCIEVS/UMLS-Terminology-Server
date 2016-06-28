/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
/*
 * 
 */
package com.wci.umls.server.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.wci.umls.server.Project;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.jpa.content.AttributeJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.jpa.content.SemanticTypeComponentJpa;
import com.wci.umls.server.jpa.helpers.PfsParameterJpa;
import com.wci.umls.server.model.actions.AtomicAction;
import com.wci.umls.server.model.actions.MolecularAction;
import com.wci.umls.server.model.actions.MolecularActionList;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.rest.client.IntegrationTestClientRest;

/**
 * Implementation of the "MetaEditing Service REST Normal Use" Test Cases.
 */
public class MetaEditingServiceRestNormalUseTest
    extends MetaEditingServiceRestTest {

  /** The auth token. */
  private static String authToken;

  /** The project. */
  private static Project project;

  /** The umls terminology. */
  private String umlsTerminology = "UMLS";

  /** The umls version. */
  private String umlsVersion = "latest";

  /**
   * The concept (will be copied from existing concept, to avoid affecting
   * database values.
   */
  private ConceptJpa concept;

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Override
  @Before
  public void setup() throws Exception {

    // authentication (admin for editing permissions)
    authToken =
        securityService.authenticate(adminUser, adminPassword).getAuthToken();

    // ensure there is a concept associated with the project
    ProjectList projects = projectService.getProjects(authToken);
    assertTrue(projects.getCount() > 0);
    project = projects.getObjects().get(0);

    // verify terminology and branch are expected values
    assertTrue(project.getTerminology().equals(umlsTerminology));
    // assertTrue(project.getBranch().equals(Branch.ROOT));

    // Copy existing concept to avoid messing with actual database data.
    concept = new ConceptJpa(contentService.getConcept("C0000294",
        umlsTerminology, umlsVersion, null, authToken), false);
    concept.setId(null);
    concept.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    concept = (ConceptJpa) testService.addConcept(concept, authToken);
  }

  /**
   * Test add and remove semanticType to concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAddAndRemoveSemanticTypeToConcept() throws Exception {
    Logger.getLogger(getClass()).debug("Start test");

    Logger.getLogger(getClass())
        .info("TEST - Add and remove semantic type to/from " + "C0000294,"
            + umlsTerminology + ", " + umlsVersion + ", " + authToken);

    //
    // Prepare the test and check prerequisites
    //
    Date startDate = new Date();

    // get the concept
    Concept c =
        contentService.getConcept(concept.getId(), project.getId(), authToken);
    assertNotNull(c);

    // check against project
    // assertTrue(c.getBranch().equals(project.getBranch()));

    // construct a semantic type not present on concept (here, Lipid)
    SemanticTypeComponentJpa semanticType = new SemanticTypeComponentJpa();
    semanticType.setBranch(Branch.ROOT);
    semanticType.setSemanticType("Lipid");
    semanticType.setTerminologyId("TestId");
    semanticType.setTerminology(umlsTerminology);
    semanticType.setVersion(umlsVersion);
    semanticType.setTimestamp(new Date());

    //
    // Test addition
    //

    // add the semantic type to the concept
    ValidationResult v =
        metaEditingService.addSemanticType(project.getId(), c.getId(),
            c.getLastModified().getTime(), semanticType, false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check semantic types
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    semanticType = null;
    for (SemanticTypeComponent s : c.getSemanticTypes()) {
      if (s.getSemanticType().equals("Lipid")) {
        semanticType = (SemanticTypeComponentJpa) s;
      }
    }
    assertNotNull(semanticType);

    // verify the molecular action exists
    PfsParameterJpa pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    MolecularActionList list = contentService
        .findMolecularActionsForConcept(c.getId(), null, pfs, authToken);
    assertTrue(list.getCount() > 0);
    MolecularAction ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that two atomic actions exists for add Semantic Type, and update
    // Concept WorkflowStatus
    pfs.setSortField("idType");
    pfs.setAscending(true);

    List<AtomicAction> atomicActions = contentService
        .findAtomicActions(ma.getId(), null, pfs, authToken).getObjects();
    assertEquals(atomicActions.size(), 2);
    assertEquals(atomicActions.get(0).getIdType().toString(), "CONCEPT");
    assertNotNull(atomicActions.get(0).getOldValue());
    assertNotNull(atomicActions.get(0).getNewValue());
    assertEquals(atomicActions.get(1).getIdType().toString(), "SEMANTIC_TYPE");
    assertNull(atomicActions.get(1).getOldValue());
    assertNotNull(atomicActions.get(1).getNewValue());

    // Verify the log entry exists
    String logEntry = projectService.getLog(project.getId(), c.getId(),
        1, authToken);   
    assertTrue(logEntry
        .contains("ADD_SEMANTIC_TYPE " + semanticType.getSemanticType()));

    //
    // Test removal
    //

    // remove the semantic type from the concept
    v = metaEditingService.removeSemanticType(project.getId(), c.getId(),
        c.getLastModified().getTime(), semanticType.getId(), false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check semantic types
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    boolean semanticTypePresent = false;
    for (SemanticTypeComponent s : c.getSemanticTypes()) {
      if (s.getSemanticType().equals("Lipid")) {
        semanticTypePresent = true;
      }
    }
    assertTrue(!semanticTypePresent);

    // verify the molecular action exists
    pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    list = contentService.findMolecularActionsForConcept(c.getId(), null, pfs,
        authToken);
    assertTrue(list.getCount() > 0);
    ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that one atomic action exists for remove Semantic Type
    pfs.setAscending(true);
    
    atomicActions = contentService
        .findAtomicActions(ma.getId(), null, null, authToken).getObjects();
    assertEquals(atomicActions.size(), 1);
    assertEquals(atomicActions.get(0).getIdType().toString(), "SEMANTIC_TYPE");
    assertNotNull(atomicActions.get(0).getOldValue());
    assertNull(atomicActions.get(0).getNewValue());


//    for (AtomicAction a : contentService
//        .findAtomicActions(ma.getId(), null, null, authToken).getObjects()) {
//      Logger.getLogger(getClass())
//          .info("TEST - Included atomic action: " + a.toString());
//    }

    // Verify the log entry exists
    logEntry = projectService.getLog(project.getId(), c.getId(),
        1, authToken);   
    assertTrue(logEntry
        .contains("REMOVE_SEMANTIC_TYPE " + semanticType.getSemanticType())); 
    
  }

  /**
   * Test add and remove attribute to concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAddAndRemoveAttributeToConcept() throws Exception {
    Logger.getLogger(getClass()).debug("Start test");

    Logger.getLogger(getClass())
        .info("TEST - Add and remove attribute to/from " + "C0000294,"
            + umlsTerminology + ", " + umlsVersion + ", " + authToken);

    //
    // Prepare the test and check prerequisites
    //
    Date startDate = new Date();

    // get the concept
    Concept c =
        contentService.getConcept(concept.getId(), project.getId(), authToken);
    assertNotNull(c);

    // construct a attribute not present on concept (here, UMLSRELA)
    AttributeJpa attribute = new AttributeJpa();
    attribute.setBranch(Branch.ROOT);
    attribute.setName("UMLSRELA");
    attribute.setValue("VALUE");
    attribute.setTerminologyId("TestId");
    attribute.setTerminology(umlsTerminology);
    attribute.setVersion(umlsVersion);
    attribute.setTimestamp(new Date());
    attribute.setPublishable(true);

    //
    // Test addition
    //

    // add the attribute to the concept
    ValidationResult v = metaEditingService.addAttribute(project.getId(),
        c.getId(), c.getLastModified().getTime(), attribute, false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check attributes
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);
    
    attribute = null;
    for (Attribute s : c.getAttributes()) {
      if (s.getName().equals("UMLSRELA")) {
        attribute = (AttributeJpa) s;
      }
    }
    assertNotNull(attribute);

    // verify the molecular action exists
    PfsParameterJpa pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    MolecularActionList list = contentService
        .findMolecularActionsForConcept(c.getId(), null, pfs, authToken);
    assertTrue(list.getCount() > 0);
    MolecularAction ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);

    // Verify that two atomic actions exists for add attribute, and update
    // Concept WorkflowStatus

    pfs.setSortField("idType");
    pfs.setAscending(true);    

    List<AtomicAction> atomicActions = contentService
        .findAtomicActions(ma.getId(), null, pfs, authToken).getObjects();
    assertEquals(atomicActions.size(), 2);
    assertEquals(atomicActions.get(0).getIdType().toString(), "ATTRIBUTE");
    assertNull(atomicActions.get(0).getOldValue());
    assertNotNull(atomicActions.get(0).getNewValue());
    assertEquals(atomicActions.get(1).getIdType().toString(), "CONCEPT");
    assertNotNull(atomicActions.get(1).getOldValue());
    assertNotNull(atomicActions.get(1).getNewValue());
    
    // Verify the log entry exists
    String logEntry = projectService.getLog(project.getId(), c.getId(),
        1, authToken);       
    assertTrue(logEntry
        .contains("ADD_ATTRIBUTE " + attribute.getName()));    

    //
    // Test removal
    //

    // remove the attribute from the concept
    v = metaEditingService.removeAttribute(project.getId(), c.getId(),
        c.getLastModified().getTime(), attribute.getId(), false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check attributes
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);
    
    boolean attributePresent = false;
    for (Attribute a : c.getAttributes()) {
      if (a.getName().equals("UMLSRELA")) {
        attributePresent = true;
      }
    }
    assertTrue(!attributePresent);

    // verify the molecular action exists
    pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    list = contentService.findMolecularActionsForConcept(c.getId(), null, pfs,
        authToken);
    assertTrue(list.getCount() > 0);
    ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that one atomic action exists for remove Attribute
    pfs.setAscending(true);
    
    atomicActions = contentService
        .findAtomicActions(ma.getId(), null, null, authToken).getObjects();
    assertEquals(atomicActions.size(), 1);
    assertEquals(atomicActions.get(0).getIdType().toString(), "ATTRIBUTE");
    assertNotNull(atomicActions.get(0).getOldValue());
    assertNull(atomicActions.get(0).getNewValue());

    // Verify the log entry exists
    logEntry = projectService.getLog(project.getId(), c.getId(),
        1, authToken);   
    assertTrue(logEntry
        .contains("REMOVE_ATTRIBUTE " + attribute.getName())); 
    
  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @Override
  @After
  public void teardown() throws Exception {

    // Copy existing concept to avoid messing with actual database data.
    IntegrationTestClientRest testService =
        new IntegrationTestClientRest(ConfigUtility.getConfigProperties());
    testService.removeConcept(concept.getId(), authToken);

    // logout
    securityService.logout(authToken);

  }

}
