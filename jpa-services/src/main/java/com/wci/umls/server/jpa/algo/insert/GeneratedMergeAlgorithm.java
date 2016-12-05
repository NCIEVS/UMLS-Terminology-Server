/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.KeyValuePair;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractMergeAlgorithm;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Concept;

/**
 * Implementation of an algorithm to import attributes.
 */
public class GeneratedMergeAlgorithm extends AbstractMergeAlgorithm {

  /** The query type. */
  private QueryType queryType;

  /** The query. */
  private String query;

  /** The check names. */
  private List<String> checkNames;

  /** The new atoms only filter. */
  private Boolean newAtomsOnly = null;

  /** The filter query type. */
  private QueryType filterQueryType;

  /** The filter query. */
  private String filterQuery;

  /** The make demotions. */
  private Boolean makeDemotions = null;

  /** The change status. */
  private Boolean changeStatus = null;

  /** The merge set. */
  private String mergeSet;

  /**
   * Instantiates an empty {@link GeneratedMergeAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public GeneratedMergeAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("GENERATEDMERGE");
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
      throw new Exception("Generated Merge requires a project to be set");
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
    logInfo("Starting GENERATEDMERGE");

    // Molecular actions WILL be generated by this algorithm
    setMolecularActionFlag(true);

    // Count number of merges successfully and unsuccessfully performed
    int successfulMerges = 0;
    int unsuccessfulMerges = 0;

    try {
      logInfo("[GeneratedMerge] Performing generated merges");

      // Generate parameters to pass into query executions
      Map<String, String> params = new HashMap<>();
      params.put("terminology", this.getTerminology());
      params.put("version", this.getVersion());
      params.put("projectTerminology", getProject().getTerminology());
      params.put("projectVersion", getProject().getVersion());

      // Execute query to get atom1,atom2 Id pairs
      List<Long[]> atomIdPairs =
          executeComponentIdPairQuery(query, queryType, params, Atom.class);

      // Remove all atom pairs caught by the filters, and calculate the
      // remaining
      // pairs' Merge Levels
      // pairs are <AtomId1, AtomId2>
      final List<Pair<Long, Long>> filteredAtomIdPairs =
          applyFilters(atomIdPairs, params);

      // Order atomIdPairs
      // sort by MergeLevel, atomId1, atomId2
      sortPairsByMergeLevelAndId(filteredAtomIdPairs);

      // Set the steps count to the number of atomPairs merges will be
      // attempted for
      setSteps(filteredAtomIdPairs.size());

      // Attempt to perform the merge given the integrity checks
      for (Pair<Long, Long> atomIdPair : filteredAtomIdPairs) {
        //TODO - pass in stats map container
        boolean mergeSuccess =
            merge(atomIdPair.getLeft(), atomIdPair.getRight(), checkNames,
                makeDemotions, changeStatus, getProject());
        // Increment the counts based on success of merge attempt
        if (mergeSuccess) {
          successfulMerges++;
        } else {
          unsuccessfulMerges++;
        }

        // Update the progress
        updateProgress();
      }

      commitClearBegin();

      logInfo("[GeneratedMerge] " + successfulMerges
          + " merges successfully performed.");
      logInfo("[GeneratedMerge] " + unsuccessfulMerges
          + " attempted merges were unsuccessful.");

      logInfo(" project = " + getProject().getId());
      logInfo(" workId = " + getWorkId());
      logInfo(" activityId = " + getActivityId());
      logInfo(" mergeSet = " + mergeSet);
      logInfo(" user = " + getLastModifiedBy());
      logInfo("Finished GENERATEDMERGE");
    } catch (Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }
  }

  private List<Pair<Long, Long>> applyFilters(List<Long[]> atomIdPairs,
    Map<String, String> params) throws Exception {
    final List<Pair<Long, Long>> filteredAtomIdsPairs = new ArrayList<>();

    // Run the filters, and save the unique atomIds/atomIdPairs to sets
    // SQL/JQL queries will populate filterAtomIdPairs set
    // LUCENE queries will populate the filterAtomIds set
    Set<Pair<Long, Long>> filterAtomIdPairs = null;
    Set<Long> filterAtomIds = null;

    // If LUCENE filter query, returns concept id
    if (filterQueryType == QueryType.LUCENE) {
      final List<Long[]> filterConceptIds =
          executeSingleComponentIdQuery(filterQuery, filterQueryType, params, Concept.class);

      // For each returned concept, filter for all of its atoms' ids
      filterAtomIds = new HashSet<>();
      for (Long[] conceptId : filterConceptIds) {
        Concept c = getConcept(conceptId[0]);
        for (Atom a : c.getAtoms()) {
          filterAtomIds.add(a.getId());
        }
      }
    }

    // PROGRAM filter queries not supported yet
    else if (queryType == QueryType.PROGRAM) {
      throw new Exception("PROGRAM queries not yet supported");
    }

    // If JQL/SQL filter query, returns atom1,atom2 Id pairs
    else if (filterQueryType == QueryType.SQL
        || filterQueryType == QueryType.JQL) {
      final List<Long[]> filterAtomIdPairArray =
          executeComponentIdPairQuery(filterQuery, filterQueryType, params, Atom.class);

      // For each returned atom pair, filter for atomIdPairs in 1,2 or 2,1 order
      filterAtomIdPairs = new HashSet<>();
      for (Long[] filterAtomIdPair : filterAtomIdPairArray) {
        final Pair<Long, Long> atomOneTwoPair =
            new ImmutablePair<>(filterAtomIdPair[0], filterAtomIdPair[1]);
        final Pair<Long, Long> atomTwoOnePair =
            new ImmutablePair<>(filterAtomIdPair[1], filterAtomIdPair[0]);
        filterAtomIdPairs.add(atomOneTwoPair);
        filterAtomIdPairs.add(atomTwoOnePair);
      }
    }

    // Go through each atom pair. If it makes it past all of the filters, add
    // it to the filtered list
    for (Long[] atomIdArrayPair : atomIdPairs) {
      // Recast the array as a Pair, for easier comparison
      final Pair<Long, Long> atomIdPair =
          new ImmutablePair<Long, Long>(atomIdArrayPair[0], atomIdArrayPair[1]);

      // New Atoms Only Filter
      if (newAtomsOnly) {
        Long maxAtomIdPreInsertion = null;
        if (getProcess().getExecutionInfo()
            .containsKey("maxAtomIdPreInsertion")) {
          maxAtomIdPreInsertion = Long.parseLong(
              getProcess().getExecutionInfo().get("maxAtomIdPreInsertion"));
        }
        if (maxAtomIdPreInsertion != null) {
          if (atomIdPair.getLeft() <= maxAtomIdPreInsertion
              || atomIdPair.getRight() <= maxAtomIdPreInsertion) {
            continue;
          }
        }
      }

      // Check SQL/JQL filter atom id pairs, if any
      if (filterAtomIdPairs != null) {
        if (filterAtomIdPairs.contains(atomIdPair)) {
          continue;
        }
      }

      // Check LUCENE filter atom ids, if any
      // If atomId1 is one of the Atoms contained in the
      // filter atoms, don't keep pair
      if (filterAtomIds != null) {
        if (filterAtomIds.contains(atomIdPair.getLeft())) {
          continue;
        }
      }

      // This pair has made it past all of the filters!
      filteredAtomIdsPairs.add(atomIdPair);
    }

    return filteredAtomIdsPairs;
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
        "mergeSet", "queryType", "query", "checkNames"
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {

    if (p.getProperty("queryType") != null) {
      queryType = QueryType.valueOf(QueryType.class,
          String.valueOf(p.getProperty("queryType")));
    }
    if (p.getProperty("query") != null) {
      query = String.valueOf(p.getProperty("query"));
    }
    if (p.getProperty("checkNames") != null) {
      checkNames =
          Arrays.asList(String.valueOf(p.getProperty("checkNames")).split(";"));
    }
    if (p.getProperty("newAtomsOnly") != null) {
      newAtomsOnly = Boolean.parseBoolean(p.getProperty("newAtomsOnly"));
    }
    if (p.getProperty("filterQueryType") != null) {
      filterQueryType = QueryType.valueOf(QueryType.class,
          String.valueOf(p.getProperty("filterQueryType")));
    }
    if (p.getProperty("filterQuery") != null) {
      filterQuery = String.valueOf(p.getProperty("filterQuery"));
    }
    if (p.getProperty("makeDemotions") != null) {
      makeDemotions = Boolean.parseBoolean(p.getProperty("makeDemotions"));
    }
    if (p.getProperty("changeStatus") != null) {
      changeStatus = Boolean.parseBoolean(p.getProperty("changeStatus"));
    }
    if (p.getProperty("mergeSet") != null) {
      mergeSet = String.valueOf(p.getProperty("mergeSet"));
    }
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() {
    final List<AlgorithmParameter> params = super.getParameters();

    // Query Type (only support JQL and SQL)
    AlgorithmParameter param = new AlgorithmParameterJpa("Query Type",
        "queryType", "The language the query is written in", "e.g. JQL", 200,
        AlgorithmParameter.Type.ENUM, QueryType.JQL.toString());
    param.setPossibleValues(
        Arrays.asList(QueryType.JQL.toString(), QueryType.SQL.toString()));
    params.add(param);

    // Query
    param = new AlgorithmParameterJpa("Query", "query",
        "A query to perform action only on objects that meet the criteria",
        "e.g. SELECT a.id, b.id FROM AtomJpa a, AtomJpa b ...", 4000,
        AlgorithmParameter.Type.TEXT, "");
    params.add(param);

    // Integrity check names
    param = new AlgorithmParameterJpa("Integrity Checks", "checkNames",
        "The names of the integrity checks to run", "e.g. MGV_B", 200,
        AlgorithmParameter.Type.MULTI, "");

    List<String> validationChecks = new ArrayList<>();
    for (final KeyValuePair validationCheck : getValidationCheckNames()
        .getKeyValuePairs()) {
      // Add handler Name to ENUM list
      validationChecks.add(validationCheck.getKey());
    }
    param.setPossibleValues(validationChecks);
    params.add(param);

    // New atoms only filter
    param = new AlgorithmParameterJpa("New Atoms Only", "newAtomsOnlyFilter",
        "Restrict to new atoms only?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    // filter query type
    param = new AlgorithmParameterJpa("Filter Query Type", "filterQueryType",
        "The language the filter query is written in", "e.g. JQL", 200,
        AlgorithmParameter.Type.ENUM, "");
    param.setPossibleValues(EnumSet.allOf(QueryType.class).stream()
        .map(e -> e.toString()).collect(Collectors.toList()));
    param.getPossibleValues().remove("");
    params.add(param);

    // filter query
    param = new AlgorithmParameterJpa("Filter Query", "filterQuery",
        "A filter query to further restrict the objects to run the merge on",
        "e.g. query in format of the query type", 4000,
        AlgorithmParameter.Type.TEXT, "");
    params.add(param);

    // make demotions
    param = new AlgorithmParameterJpa("Make Demotions", "makeDemotions",
        "Make demotions for failed merges?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    // change status
    param = new AlgorithmParameterJpa("Change Status", "changeStatus",
        "Change status when performing merges?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    // mergeSet
    param = new AlgorithmParameterJpa("Merge Set", "mergeSet",
        "The merge set to perform the merges on", "e.g. NCI-SY", 10,
        AlgorithmParameter.Type.STRING, "");
    params.add(param);

    return params;
  }
}