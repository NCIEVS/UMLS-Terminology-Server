/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.maint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.KeyValuePair;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.TypeKeyValue;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;
import com.wci.umls.server.jpa.helpers.TypeKeyValueJpa;
import com.wci.umls.server.services.handlers.IdentifierAssignmentHandler;

/**
 * Implementation of an algorithm to add or remove integrity checks to the
 * project.
 */
public class AddRemoveIntegrityCheckAlgorithm extends AbstractAlgorithm {

  /** The previous progress. */
  private int previousProgress;

  /** The steps. */
  private int steps;

  /** The steps completed. */
  private int stepsCompleted;

  /** The add remove. */
  private String addRemove;

  /** The check name. */
  private String checkName;

  /** The value 1. */
  private String value1;

  /** The value 2. */
  private String value2;

  /**
   * Instantiates an empty {@link AddRemoveIntegrityCheckAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public AddRemoveIntegrityCheckAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("ADDREMOVEINTEGRITYCHECK");
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
          "Add/Remove Integrity Check requires a project to be set");
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
    logInfo("Starting ADDREMOVEINTEGRITYCHECK");

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    // Set up the handler for identifier assignment
    final IdentifierAssignmentHandler handler =
        newIdentifierAssignmentHandler(getProject().getTerminology());
    handler.setTransactionPerOperation(false);
    handler.beginTransaction();

    try {

      logInfo(
          "[Add/Remove Integrity Check] Adding/Removing Integrity Checks to the project");

      // Each AddRemove check has only a single step
      steps = 1;

      previousProgress = 0;
      stepsCompleted = 0;

      TypeKeyValue validationCheckData =
          this.addTypeKeyValue(new TypeKeyValueJpa(checkName, value1, value2));

      List<TypeKeyValue> validationData = getProject().getValidationData();
      if (addRemove.equals("Remove")) {
        if (validationData.contains(validationCheckData)) {
          getProject().getValidationData().remove(validationCheckData);
          updateProject(getProject());
        } else {
          // Do nothing - removal is successful by default.
        }
      }

      else if (addRemove.equals("Add")) {
        if (validationData.contains(validationCheckData)) {
          // Do nothing - addition has already been done.
        } else {
          getProject().getValidationData().add(validationCheckData);
          updateProject(getProject());
        }
      }

      else {
        throw new LocalException("Invalid value for AddRemove: " + addRemove);
      }

      // Update the progress
      updateProgress();

      logInfo("  project = " + getProject().getId());
      logInfo("  workId = " + getWorkId());
      logInfo("  activityId = " + getActivityId());
      logInfo("  user  = " + getLastModifiedBy());
      logInfo("Finished ADDREMOVEINTEGRITYCHECK");

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

  /**
   * Update progress.
   *
   * @throws Exception the exception
   */
  public void updateProgress() throws Exception {
    stepsCompleted++;
    int currentProgress = (int) ((100.0 * stepsCompleted / steps));
    if (currentProgress > previousProgress) {
      fireProgressEvent(currentProgress,
          "ADDREMOVEINTEGRITYCHECK progress: " + currentProgress + "%");
      previousProgress = currentProgress;
    }
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        "addRemove", "checkName", "value1"
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {

    if (p.getProperty("addRemove") != null) {
      addRemove = String.valueOf(p.getProperty("addRemove"));
    }
    if (p.getProperty("checkName") != null) {
      checkName = String.valueOf(p.getProperty("checkName"));
    }
    if (p.getProperty("value1") != null) {
      value1 = String.valueOf(p.getProperty("value1"));
    }
    if (p.getProperty("value2") != null) {
      value2 = String.valueOf(p.getProperty("value2"));
    }

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

    AlgorithmParameter param = new AlgorithmParameterJpa("Add/Remove",
        "addRemove", "Adding or Removing integrity check", "e.g. Add", 10,
        AlgorithmParameter.Type.ENUM, "");
    param.setPossibleValues(Arrays.asList("Add", "Remove"));
    params.add(param);

    param = new AlgorithmParameterJpa("Integrity Check", "checkName",
        "The name of the integrity check to add or remove", "e.g. MGV_B", 10,
        AlgorithmParameter.Type.ENUM, "");

    List<String> validationChecks = new ArrayList<>();
    for (final KeyValuePair validationCheck : getValidationCheckNames()
        .getKeyValuePairs()) {
      // Add handler Name to ENUM list
      validationChecks.add(validationCheck.getKey());
    }

    param.setPossibleValues(validationChecks);
    params.add(param);

    param = new AlgorithmParameterJpa("Value 1", "value1",
        "Value 1 of the validation check  (often the Terminology)", "e.g. NCI",
        20, AlgorithmParameter.Type.STRING, "");
    params.add(param);

    param = new AlgorithmParameterJpa("Value 2", "value2",
        "Value 2 of the validation check  (often blank)", "e.g. \"\"", 20,
        AlgorithmParameter.Type.STRING, "");
    params.add(param);

    return params;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Add/Remove integrity check data for <check name>";
  }

}