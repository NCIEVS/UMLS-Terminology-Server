/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractSourceInsertionAlgorithm;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.services.handlers.IdentifierAssignmentHandler;

/**
 * Implementation of an algorithm to find all concepts with "new" and "old"
 * semantic type components, and remove either old or new depending on params..
 */
public class SemanticTypeResolverAlgorithm extends AbstractSourceInsertionAlgorithm {

  /** Whether new semanticTypes 'win' and replace older ones, or vice versa. */
  private String winLose = null;

  /**
   * Instantiates an empty {@link SemanticTypeResolverAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public SemanticTypeResolverAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("SEMANTICTYPERESOLVER");
    setLastModifiedBy("admin");
  }

  /**
   * Check preconditions.
   *
   * @return the validation result
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception(
          "Semantic Type Resolving requires a project to be set");
    }
    if (winLose == null) {
      throw new Exception(
          "Semantic Type Resolving requires winLose to be set.");
    }
    if (!(winLose.equals("win") || winLose.equals("lose"))) {
      throw new Exception("winLose= " + winLose + " is invalid: must be either 'win' or 'lose'");
    }

    return validationResult;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting SEMANTICTYPERESOLVING");

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    // Set up the handler for identifier assignment
    final IdentifierAssignmentHandler handler =
        newIdentifierAssignmentHandler(getProject().getTerminology());
    handler.setTransactionPerOperation(false);
    handler.beginTransaction();

    // Count number of removed Semantic Types, for logging
    int removedStyCount = 0;

    try {
      logInfo("[SemanticTypeResolver] Checking for new/updated Semantic Types");

      // Find all atoms pairs that satisfy the specified matching criteria and are
      // in the same project Terminology concept, where one atom is old and the
      // other is new

      //TODO question - is this the correct query?
      // Generate query string
      String query =
          "SELECT DISTINCT c.id "
              + "FROM ConceptJpa c JOIN c.semanticTypes s1 JOIN c.semanticTypes s2 "
              + "WHERE c.terminology=:projectTerminology AND c.version=:projectVersion "
              + "AND s1.terminology=:terminology AND s2.terminology=:terminology "
              + "AND NOT s1.version=:version AND s2.version=:version";

      // Generate parameters to pass into query executions
      Map<String, String> params = new HashMap<>();
      params.put("terminology", this.getTerminology());
      params.put("version", this.getVersion());
      params.put("projectTerminology", getProject().getTerminology());
      params.put("projectVersion", getProject().getVersion());

      final List<Long[]> atomIdPairArray =
          executeComponentIdPairQuery(query, QueryType.LUCENE, params, AtomJpa.class);

      setSteps(atomIdPairArray.size());
      
      // Update the progress
      updateProgress();

      logInfo(
          "[SemanticTypeResolver] Removed " + removedStyCount + " Semantic Types.");


      logInfo("  project = " + getProject().getId());
      logInfo("  workId = " + getWorkId());
      logInfo("  activityId = " + getActivityId());
      logInfo("  user  = " + getLastModifiedBy());
      logInfo("Finished SEMANTICTYPERESOLVING");

    } catch (

    Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }

  }

  /**
   * Reset.
   *
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public void reset() throws Exception {
    // n/a - No reset
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        "winLose"
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
   winLose = String.valueOf(p.getProperty("winLose"));

  }

  /**
   * Returns the parameters.
   *
   * @return the parameters
   */
  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() {
    final List<AlgorithmParameter> params = super.getParameters();
    AlgorithmParameter param = new AlgorithmParameterJpa("WinLose", "winLose",
        "Whether new SemanticTypes 'win' over semantic types from previous versions.",
        "e.g. win", 200, AlgorithmParameter.Type.ENUM,"");
    param.setPossibleValues(Arrays.asList("yes","no"));
    params.add(param);

    return params;
  }

}