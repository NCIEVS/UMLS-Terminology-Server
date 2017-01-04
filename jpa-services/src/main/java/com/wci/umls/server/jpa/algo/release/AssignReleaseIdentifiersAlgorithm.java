/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.release;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PrecedenceList;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;
import com.wci.umls.server.jpa.services.handlers.RrfComputePreferredNameHandler;
import com.wci.umls.server.jpa.services.handlers.UmlsIdentifierAssignmentHandler;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.meta.AdditionalRelationshipType;
import com.wci.umls.server.model.meta.RelationshipType;
import com.wci.umls.server.services.RootService;
import com.wci.umls.server.services.handlers.IdentifierAssignmentHandler;

/**
 * Algorithm for assigning release identifiers.
 */
public class AssignReleaseIdentifiersAlgorithm extends AbstractAlgorithm {

  /** The previous progress. */
  private int previousProgress;

  /** The steps. */
  private int steps;

  /** The steps completed. */
  private int stepsCompleted;

  /**
   * Instantiates an empty {@link AssignReleaseIdentifiersAlgorithm}.
   *
   * @throws Exception the exception
   */
  public AssignReleaseIdentifiersAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("ASSIGNRELEASEIDS");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting assign release identifiers");
    fireProgressEvent(0, "Starting");

    // TODO Save cui/rui/atui max values for "reset"
    // need an accessible properties object for this - the release info??

    steps = 4;
    previousProgress = 0;
    stepsCompleted = 0;

    assignCuis();

    assignRuis();

    assignAtuis();

    fireProgressEvent(100, "Finished");
    logInfo("Finished assign release identifiers");
  }

  /**
   * Assign cuis.
   *
   * @throws Exception the exception
   */
  private void assignCuis() throws Exception {

    // Get a UMLS identity handler
    final UmlsIdentifierAssignmentHandler handler =
        (UmlsIdentifierAssignmentHandler) getIdentifierAssignmentHandler(
            getProject().getTerminology());

    // Assign CUIs:
    // Rank all atoms

    // 1. Rank all atoms in (project) precedence order and iterate through
    final Map<Long, String> atomRankMap = new HashMap<>(20000);
    final Map<Long, Long> atomConceptMap = new HashMap<>(20000);
    final Session session = manager.unwrap(Session.class);
    final org.hibernate.Query hQuery = session.createQuery(
        "select distinct c from ConceptJpa c join c.atoms a where c.terminology = :terminology "
            + "  and c.version = :version");
    hQuery.setParameter("terminology", getTerminology());
    hQuery.setParameter("version", getVersion());
    hQuery.setReadOnly(true).setFetchSize(2000).setCacheable(false);
    final ScrollableResults results = hQuery.scroll(ScrollMode.FORWARD_ONLY);
    // NOTE: this assumes RRF preferred name handler
    final RrfComputePreferredNameHandler prefHandler =
        new RrfComputePreferredNameHandler();
    final PrecedenceList list = getProject().getPrecedenceList();
    prefHandler.cacheList(list);
    int ct = 0;
    while (results.next()) {
      final Concept concept = (Concept) results.get()[0];
      for (final Atom atom : concept.getAtoms()) {
        final String rank = prefHandler.getRank(atom, list);
        atomRankMap.put(atom.getId(), rank);
        atomConceptMap.put(atom.getId(), concept.getId());
      }
      if (++ct % 1000 == 0) {
        checkCancel();
      }
    }
    results.close();
    updateProgress();

    // Sort all atoms
    final List<Long> atomIds = new ArrayList<>(atomRankMap.keySet());
    Collections.sort(atomIds,
        (a1, a2) -> atomRankMap.get(a1).compareTo(atomRankMap.get(a2)));

    // Iterate through sorted atom ids
    int objectCt = 0;
    int startProgress = 20;
    int prevProgress = 0;
    int totalCt = atomIds.size();
    final Set<Long> assignedConcepts = new HashSet<>(20000);
    for (final Long id : atomIds) {

      // If the concept for this atom already has an assignment, move on
      if (assignedConcepts.contains(atomConceptMap.get(id))) {
        continue;
      }

      final Atom atom = getAtom(id);
      final String cui =
          atom.getAlternateTerminologyIds().get(getProject().getTerminology());
      final Concept concept = getConcept(atomConceptMap.get(id));

      // If the CUI is set, assign it to the concept and move on
      if (cui != null) {
        // If this is already the concept id, move on
        if (concept.getTerminologyId().equals(cui)) {
          continue;
        }
        // Otherwise assign it
        concept.setTerminologyId(cui);
        updateConcept(concept);
      }

      // otherwise, create a new CUI, assign it ,etc.
      else {
        // prompt for new assignment
        concept.setTerminologyId("");
        concept.setTerminologyId(handler.getTerminologyId(concept));
        updateConcept(concept);
      }

      // log, commit, check cancel, advance progress
      int progress = (int) (objectCt * 100.0 / totalCt);
      if (progress != prevProgress) {
        checkCancel();
        this.fireAdjustedProgressEvent(progress, stepsCompleted, steps,
            "Assigning CUIs");
        prevProgress = progress;
      }
      logAndCommit(++objectCt, RootService.logCt, RootService.commitCt);
    }

  }

  /**
   * Assign ruis.
   *
   * @throws Exception the exception
   */
  private void assignRuis() throws Exception {
    final IdentifierAssignmentHandler handler =
        getIdentifierAssignmentHandler(getProject().getTerminology());

    // Assign RUIs to concept relationships
    // First create map of rel and rela inverses
    final Map<String, String> relToInverseMap = new HashMap<>();
    for (final RelationshipType relType : getRelationshipTypes(
        getProject().getTerminology(), getProject().getVersion())
            .getObjects()) {
      relToInverseMap.put(relType.getAbbreviation(),
          relType.getInverse().getAbbreviation());
    }
    for (final AdditionalRelationshipType relType : getAdditionalRelationshipTypes(
        getProject().getTerminology(), getProject().getVersion())
            .getObjects()) {
      relToInverseMap.put(relType.getAbbreviation(),
          relType.getInverse().getAbbreviation());
    }
    // Iterate through all concept relationships
    int objectCt = 0;
    // NOTE: Hibernate-specific to support iterating
    final Session session = manager.unwrap(Session.class);
    final org.hibernate.Query hQuery = session.createQuery(
        "select a from ConceptRelationshipJpa a WHERE a.publishable = true "
            + "and terminology = :terminology");
    hQuery.setParameter("terminology", getProject().getTerminology());
    hQuery.setReadOnly(true).setFetchSize(2000).setCacheable(false);
    ScrollableResults results = hQuery.scroll(ScrollMode.FORWARD_ONLY);
    while (results.next()) {
      final ConceptRelationship rel = (ConceptRelationship) results.get()[0];

      final String origRui = rel.getTerminologyId();
      rel.setTerminologyId("");

      final String rui = handler.getTerminologyId(rel,
          relToInverseMap.get(rel.getRelationshipType()),
          relToInverseMap.get(rel.getAdditionalRelationshipType()));
      if (!origRui.equals(rui)) {
        rel.setTerminologyId(rui);
        updateRelationship(rel);
      }
      logAndCommit(++objectCt, RootService.logCt, RootService.commitCt);
    }
    commitClearBegin();
  }

  /**
   * Assign atuis.
   *
   * @throws Exception the exception
   */
  private void assignAtuis() throws Exception {
    final IdentifierAssignmentHandler handler =
        getIdentifierAssignmentHandler(getProject().getTerminology());

    // Assign ATUIs for semantic types
    int objectCt = 0;

    final Session session = manager.unwrap(Session.class);
    final org.hibernate.Query hQuery = session
        .createQuery("select a, b from ConceptJpa a join a.semanticTypes b "
            + "where a.publishable = true and b.publishable = true "
            + "and a.terminology = :terminology");
    hQuery.setParameter("terminology", getProject().getTerminology());
    hQuery.setReadOnly(true).setFetchSize(2000).setCacheable(false);
    final ScrollableResults results = hQuery.scroll(ScrollMode.FORWARD_ONLY);
    while (results.next()) {
      final Concept c = (Concept) results.get()[0];
      final SemanticTypeComponent sty =
          (SemanticTypeComponent) results.get()[1];

      // For each semantic type component (e.g. concept.getSemanticTypes())
      final String origAtui = sty.getTerminologyId();
      sty.setTerminologyId("");

      final String atui = handler.getTerminologyId(sty, c);
      if (!origAtui.equals(atui)) {
        sty.setTerminologyId(atui);
        updateSemanticTypeComponent(sty, c);
      }
      logAndCommit(++objectCt, RootService.logCt, RootService.commitCt);
    }

    logInfo("Finished Assign release identifiers");
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        ""
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /**
   * Update progress.
   *
   * @throws Exception the exception
   */
  public void updateProgress() throws Exception {
    stepsCompleted++;
    int currentProgress = (int) ((100.0 * stepsCompleted / steps));
    if (currentProgress > previousProgress) {
      checkCancel();
      fireProgressEvent(currentProgress,
          "ASSIGN RELEASE IDS progress: " + currentProgress + "%");
      previousProgress = currentProgress;
    }
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return ConfigUtility.getNameFromClass(getClass());
  }
}