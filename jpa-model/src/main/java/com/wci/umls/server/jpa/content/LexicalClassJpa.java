/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import com.wci.umls.server.model.content.LexicalClass;

/**
 * JPA-enabled implementation of {@link LexicalClass}.
 */
@Entity
@Table(name = "lexical_classes", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "version", "id"
}))
@Audited
@XmlRootElement(name = "lexicalClass")
@Indexed
public class LexicalClassJpa extends AbstractAtomClass implements LexicalClass {

  /** The normalized string. */
  @Column(nullable = true, length = 4000)
  private String normalizedName;

  /** The marker sets. */
  @ElementCollection(fetch = FetchType.EAGER)
  // consider this: @Fetch(sFetchMode.JOIN)
  @CollectionTable(name = "lexical_class_marker_sets")
  @Column(nullable = true)
  List<String> markerSets;

  /**
   * Instantiates an empty {@link LexicalClassJpa}.
   */
  public LexicalClassJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link LexicalClassJpa} from the specified parameters.
   *
   * @param lexicalClass the lexical class
   * @param deepCopy the deep copy
   */
  public LexicalClassJpa(LexicalClass lexicalClass, boolean deepCopy) {
    super(lexicalClass, deepCopy);
    normalizedName = lexicalClass.getNormalizedName();
    markerSets = lexicalClass.getMarkerSets();
  }

  /**
   * Returns the normalized string.
   *
   * @return the normalized string
   */
  @Override
  @Fields({
      @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO),
      @Field(name = "normalizedNameSort", index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  })
  @Analyzer(definition = "noStopWord")
  public String getNormalizedName() {
    return normalizedName;
  }

  /**
   * Sets the normalized string.
   *
   * @param normalizedName the normalized string
   */
  @Override
  public void setNormalizedName(String normalizedName) {
    this.normalizedName = normalizedName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.helpers.HasMarkerSets#getMarkerSets()
   */
  @Override
  public List<String> getMarkerSets() {
    return markerSets;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.helpers.HasMarkerSets#setMarkerSets(java.util.List)
   */
  @Override
  public void setMarkerSets(List<String> markerSets) {
    this.markerSets = markerSets;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.helpers.HasMarkerSets#addMarkerSet(java.lang.String)
   */
  @Override
  public void addMarkerSet(String markerSet) {
    if (markerSets == null) {
      markerSets = new ArrayList<String>();
    }
    markerSets.add(markerSet);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.helpers.HasMarkerSets#removeMarkerSet(java.lang.String)
   */
  @Override
  public void removeMarkerSet(String markerSet) {
    if (markerSets == null) {
      markerSets = new ArrayList<String>();
    }
    markerSets.remove(markerSet);

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.content.AbstractAtomClass#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result
            + ((normalizedName == null) ? 0 : normalizedName.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.content.AbstractAtomClass#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    LexicalClassJpa other = (LexicalClassJpa) obj;
    if (normalizedName == null) {
      if (other.normalizedName != null)
        return false;
    } else if (!normalizedName.equals(other.normalizedName))
      return false;
    return true;
  }

}
