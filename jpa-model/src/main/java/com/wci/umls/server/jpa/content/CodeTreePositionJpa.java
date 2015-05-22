/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import com.wci.umls.server.model.content.Code;
import com.wci.umls.server.model.content.CodeTreePosition;

/**
 * JPA-enabled implementation of {@link CodeTreePosition}.
 */
@Entity
@Table(name = "code_tree_positions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {
        "terminologyId", "terminology", "terminologyVersion", "id"
    }), @UniqueConstraint(columnNames = {
        "ancestorPath", "terminologyId"
    })
})
@Audited
@Indexed
@XmlRootElement(name = "codeTreePosition")
public class CodeTreePositionJpa extends AbstractTreePosition<Code> implements
    CodeTreePosition {

  /** The code. */
  @ManyToOne(targetEntity = CodeJpa.class, fetch = FetchType.EAGER, optional = false)
  @JoinColumn(nullable = false)
  private Code node;

  /**
   * Instantiates an empty {@link CodeTreePositionJpa}.
   */
  public CodeTreePositionJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link CodeTreePositionJpa} from the specified parameters.
   *
   * @param treepos the treepos
   * @param deepCopy the deep copy
   */
  public CodeTreePositionJpa(CodeTreePosition treepos, boolean deepCopy) {
    super(treepos, deepCopy);
    node = treepos.getNode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.content.TreePosition#getNode()
   */
  @XmlTransient
  @Override
  public Code getNode() {
    return node;
  }

  /**
   * Returns the node id. For JAXB.
   *
   * @return the node id
   */
  @XmlElement
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getNodeId() {
    return node == null ? null : node.getId();
  }

  /**
   * Sets the node id. For JAXB.
   *
   * @param id the node id
   */
  public void setNodeId(Long id) {
    if (node == null) {
      node = new CodeJpa();
    }
    node.setId(id);
  }

  /**
   * Returns the node name. For JAXB.
   *
   * @return the node name
   */
  @Field(index = Index.YES, store = Store.NO, analyze = Analyze.YES, analyzer = @Analyzer(definition = "noStopWord"))
  public String getNodeName() {
    return node == null ? null : node.getName();
  }

  /**
   * Sets the node name. For JAXB.
   *
   * @param name the node name
   */
  public void setNodeName(String name) {
    if (node == null) {
      node = new CodeJpa();
    }
    node.setName(name);
  }

  /**
   * Returns the node terminology id. For JAXB.
   *
   * @return the node terminology id
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getNodeTerminologyId() {
    return node == null ? null : node.getTerminologyId();
  }

  /**
   * Sets the node terminology id. For JAXB.
   *
   * @param terminologyId the node terminology id
   */
  public void setNodeTerminologyId(String terminologyId) {
    if (node == null) {
      node = new CodeJpa();
    }
    node.setTerminologyId(terminologyId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.content.TreePosition#setNode(com.wci.umls.server
   * .model.content.ComponentHasAttributesAndName)
   */
  @Override
  public void setNode(Code code) {
    this.node = code;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.content.AbstractTreePosition#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime
            * result
            + ((node == null || node.getTerminologyId() == null) ? 0 : node
                .getTerminologyId().hashCode());
    return result;
  }

  /**
   * CUSTOM for concept id.
   *
   * @param obj the obj
   * @return true, if successful
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    CodeTreePositionJpa other = (CodeTreePositionJpa) obj;
    if (node == null) {
      if (other.node != null)
        return false;
    } else if (node.getTerminologyId() == null) {
      if (other.node != null && other.node.getTerminologyId() != null)
        return false;
    } else if (!node.getTerminologyId().equals(other.node.getTerminologyId()))
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.content.AbstractTreePosition#toString()
   */
  @Override
  public String toString() {
    return "CodeTreePositionJpa [code=" + node + "]";
  }

}
