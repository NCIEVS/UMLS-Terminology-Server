/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.spell.LuceneLevenshteinDistance;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.jpa.FullTextQuery;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.helpers.HasId;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.PfscParameter;
import com.wci.umls.server.jpa.services.helper.IndexUtility;
import com.wci.umls.server.services.handlers.SearchHandler;

/**
 * Default implementation of {@link SearchHandler}. This provides an algorithm
 * to aide in lucene searches.
 */
public class DefaultSearchHandler implements SearchHandler {

  /** The acronym expansion map. */
  private Map<String, Set<String>> acronymExpansionMap = new HashMap<>();

  /** The spell checker. */
  private SpellChecker spellChecker = null;

  /** The score map. */
  private Map<Long, Float> scoreMap = new HashMap<>();

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {

    // Initialize acronyms map
    if (p.containsKey("acronymsFile")) {
      BufferedReader in = new BufferedReader(
          new FileReader(new File(p.getProperty("acronymsFile"))));
      String line;
      while ((line = in.readLine()) != null) {
        String[] tokens = FieldedStringTokenizer.split(line, "\t");
        if (!acronymExpansionMap.containsKey(tokens[0])) {
          acronymExpansionMap.put(tokens[0], new HashSet<String>(2));
        }
        acronymExpansionMap.get(tokens[0]).add(tokens[1]);
      }
      in.close();
    } else {
      throw new Exception("Required property acronymsFile not present.");
    }

    // Initialize spell checker
    if (p.containsKey("spellingFile") && p.containsKey("spellingIndex")) {
      // expect properties to have "spellingFile" and "spellingIndex"
      File dir = new File(p.getProperty("spellingIndex"));
      Directory directory = FSDirectory.open(dir);
      spellChecker =
          new SpellChecker(directory, new LuceneLevenshteinDistance());
      IndexWriterConfig indexWriterConfig =
          new IndexWriterConfig(Version.LATEST, new WhitespaceAnalyzer());
      spellChecker.indexDictionary(
          new PlainTextDictionary(new File(p.getProperty("spellingFile"))),
          indexWriterConfig, false);

    } else {
      throw new Exception(
          "Required property spellingFile or spellingIndex not present.");
    }
  }

  /* see superclass */
  @Override
  public <T extends HasId> List<T> getQueryResults(String terminology,
    String version, String branch, String query, String literalField,
    Class<?> fieldNamesKey, Class<T> clazz, PfsParameter pfs, int[] totalCt,
    EntityManager manager) throws Exception {

    // if the literal field specified is a sort field, also search normalized
    // field
    // TODO This currently does not work for tree positions -- do we want to
    // index those as well?
    // probably yes, but space considerations... DECISION: NOt doing this
    // TODO Scale DefaultSearchHandler down, don't care about spelling or acronyms
    // those functions are handled in the AtomClassSearchHandler for use cases
    // where we care
   
    // Build an escaped form of the query with wrapped quotes removed
    // This will be used for literal/exact searching
    String escapedQuery = query;
    if (query.startsWith("\"") && query.endsWith("\"")) {
      escapedQuery = escapedQuery.substring(1);
      escapedQuery = escapedQuery.substring(0, query.length() - 2);
    }
    escapedQuery = "\"" + QueryParserBase.escape(escapedQuery) + "\"";

    // A slash character indicats a regex in lucene, fix that
    final String fixedQuery = query == null ? "" : query.replaceAll("\\/", " ");

    // Build a combined query with an OR between query typed and exact match
    String combinedQuery = null;
    // For a fielded query search, simply perform the search as written
    // no need for modifications. Also if no literal or normalized search field
    // is supplied
    if (fixedQuery.isEmpty() || query.contains(":")
        || literalField == null) {
      combinedQuery = fixedQuery;
    } else {
      combinedQuery = fixedQuery.isEmpty() ? "" : fixedQuery;
     
      if (literalField != null && !literalField.isEmpty()) {
        combinedQuery += " OR " + literalField + ":" + escapedQuery + "^10.0";
      }

      // create an exact expansion entry. i.e. if the search term exactly
      // matches something in the acronyms file, then use additional "OR"
      // clauses
      if (acronymExpansionMap.containsKey(fixedQuery)) {
        for (String expansion : acronymExpansionMap.get(fixedQuery)) {    
          if (literalField != null && !literalField.isEmpty()) {
            combinedQuery +=
                " OR " + literalField + ":\"" + expansion + "\"" + "^10.0";
          }
        }
      }
    }

    // Add terminology conditions
    StringBuilder terminologyClause = new StringBuilder();
    if (terminology != null && !terminology.equals("") && version != null
        && !version.equals("")) {
      terminologyClause.append(
          " AND terminology:" + terminology + " AND version:" + version);
    }

    // Assemble query
    StringBuilder finalQuery = new StringBuilder();
    if (fixedQuery.isEmpty()) {
      // Just use PFS and skip the leading "AND"
      finalQuery.append(terminologyClause.substring(5));
    } else if (combinedQuery.contains(" OR ")) {
      // Use parens
      finalQuery.append("(").append(combinedQuery).append(")")
          .append(terminologyClause);
    } else {
      // Don't use parens
      finalQuery.append(combinedQuery).append(terminologyClause);

    }
    FullTextQuery fullTextQuery = null;
    try {
      Logger.getLogger(getClass()).debug("query = " + finalQuery);
      fullTextQuery = IndexUtility.applyPfsToLuceneQuery(clazz, fieldNamesKey,
          finalQuery.toString(), pfs, manager);
    } catch (ParseException | IllegalArgumentException e) {
      e.printStackTrace();
      // If there's a parse exception, try the literal query
      Logger.getLogger(getClass()).debug("query = " + finalQuery);
      fullTextQuery = IndexUtility.applyPfsToLuceneQuery(clazz, fieldNamesKey,
          escapedQuery + terminologyClause, pfs, manager);
    }

    // Apply paging and sorting parameters for the PFSC case
    // This is needed for the combined search with "search criteria"
    if (!(pfs instanceof PfscParameter)) {
      totalCt[0] = fullTextQuery.getResultSize();
    } else if (pfs instanceof PfscParameter
        && ((PfscParameter) pfs).getSearchCriteria().isEmpty()) {
      // Get result size if we know it.
      totalCt[0] = fullTextQuery.getResultSize();
    } else {
      // If with search criteria, save paging
      fullTextQuery.setFirstResult(0);
      fullTextQuery.setMaxResults(Integer.MAX_VALUE);
      totalCt[0] = fullTextQuery.getResultSize();
    }

    // Only look to other algorithms if this is NOT a fielded query
    // and the query exists
    if (fixedQuery != null && !fixedQuery.isEmpty()
        && !fixedQuery.contains(":")) {

     

      // if still zero, do wildcard search at the end of each term of the
      // original query
      // e.g. a* b* c*
      if (totalCt[0] == 0) {
        // use wordInd tokenization
        String[] tokens = FieldedStringTokenizer.split(fixedQuery,
            " \t-({[)}]_!@#%&*\\:;\"',.?/~+=|<>$`^");
        StringBuilder newQuery = new StringBuilder();
        for (String token : tokens) {
          if (newQuery.length() != 0) {
            newQuery.append(" ");
          }
          if (token.length() > 0) {
            newQuery.append(token).append("*");
          }
        }
        // Try the query again
        fullTextQuery = IndexUtility.applyPfsToLuceneQuery(clazz, fieldNamesKey,
            newQuery.toString() + terminologyClause, pfs, manager);
        totalCt[0] = fullTextQuery.getResultSize();

      }

    }

    // Use this code to see the actual score values
    fullTextQuery.setProjection(ProjectionConstants.SCORE,
        ProjectionConstants.THIS);
    final List<T> classes = new ArrayList<>();
    @SuppressWarnings("unchecked")
    final List<Object[]> results = fullTextQuery.getResultList();
    for (final Object[] result : results) {
      Object score = result[0];
      @SuppressWarnings("unchecked")
      T t = (T) result[1];
      classes.add(t);

      // normalize results to a "good match" (lucene score of 5.0+)
      // Double normScore = Math.log(Math.max(5, scoreMap.get(sr.getId())) /
      // Math.log(5));

      // cap the score to a maximum of 5.0 and normalize to the range [0,1]

      Float normScore = Math.min(5, Float.valueOf(score.toString())) / 5;

      // store the score
      scoreMap.put(t.getId(), normScore.floatValue());
    }
    Logger.getLogger(getClass()).debug("  scoreMap = " + scoreMap);

    return classes;

  }

  /* see superclass */
  @Override
  public String getName() {
    return "Default Search Handler";
  }

  /* see superclass */
  @Override
  public Map<Long, Float> getScoreMap() {
    return scoreMap;
  }
}
