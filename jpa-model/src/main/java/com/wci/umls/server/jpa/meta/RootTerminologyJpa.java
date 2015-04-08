/*
 * 
 */
package com.wci.umls.server.jpa.meta;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;

import com.wci.umls.server.model.meta.ContactInfo;
import com.wci.umls.server.model.meta.Language;
import com.wci.umls.server.model.meta.RootTerminology;
import com.wci.umls.server.model.meta.Terminology;

/**
 * JPA-enabled implementation of {@link RootTerminology}.
 */
@Entity
@Table(name = "root_terminologies", uniqueConstraints = @UniqueConstraint(columnNames = {
  "abbreviation"
}))
@Audited
@XmlRootElement(name = "rootTerminology")
public class RootTerminologyJpa extends AbstractAbbreviation implements
    RootTerminology {

  /** The acquisition contact. */
  @ManyToOne(targetEntity = ContactInfoJpa.class, fetch = FetchType.EAGER, optional = true)
  private ContactInfo acquisitionContact;

  /** The content contact. */
  @ManyToOne(targetEntity = ContactInfoJpa.class, fetch = FetchType.EAGER, optional = true)
  private ContactInfo contentContact;

  /** The polyhierarchy flag. */
  @Column(nullable = false)
  private boolean polyhierarchy;

  /** The current version. */
  @OneToOne(targetEntity = TerminologyJpa.class, fetch = FetchType.EAGER, optional = true)
  private Terminology currentVersion;

  /** The family. */
  @Column(nullable = false)
  private String family;

  /** The hierarchical name. */
  @Column(nullable = true, length = 3000)
  private String hierarchicalName;

  /** The language. */
  @ManyToOne(targetEntity = LanguageJpa.class, fetch = FetchType.EAGER, optional = false)
  @JoinColumn(nullable = false)
  private Language language;

  /** The license contact. */
  @ManyToOne(targetEntity = ContactInfoJpa.class, fetch = FetchType.EAGER, optional = true)
  private ContactInfo licenseContact;

  /** The preferred name. */
  @Column(nullable = false, length = 3000)
  private String preferredName;

  /** The previous version. */
  @OneToOne(targetEntity = TerminologyJpa.class, fetch = FetchType.EAGER, optional = true)
  private Terminology previousVersion;

  /** The restriction level. */
  @Column(nullable = false)
  private int restrictionLevel;

  /** The short name. */
  @Column(nullable = true, length = 3000)
  private String shortName;

  /** The short name. */
  @ElementCollection
  @Column(nullable = true)
  private List<String> synonymousNames;

  /** The descriptions. */
  @OneToMany(mappedBy = "rootTerminology", fetch = FetchType.EAGER, orphanRemoval = true, targetEntity = TerminologyJpa.class)
  private List<Terminology> terminologies = null;

  /**
   * Instantiates an empty {@link RootTerminologyJpa}.
   */
  public RootTerminologyJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link RootTerminologyJpa} from the specified parameters.
   *
   * @param t the terminology
   */
  public RootTerminologyJpa(RootTerminology t) {
    super(t);
    acquisitionContact = t.getAcquisitionContact();
    contentContact = t.getContentContact();
    currentVersion = t.getCurrentVersion();
    family = t.getFamily();
    hierarchicalName = t.getHierarchicalName();
    language = t.getLanguage();
    licenseContact = t.getLicenseContact();
    preferredName = t.getPreferredName();
    previousVersion = t.getPreviousVersion();
    restrictionLevel = t.getRestrictionLevel();
    shortName = t.getShortName();
    synonymousNames = t.getSynonymousNames();
    polyhierarchy = t.isPolyhierachy();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getAcquisitionContact()
   */
  @Override
  @XmlElement(type = ContactInfoJpa.class, name = "acquisitionContact")
  public ContactInfo getAcquisitionContact() {
    return acquisitionContact;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setAcquisitionContact(com
   * .wci.umls.server.model.meta.ContactInfo)
   */
  @Override
  public void setAcquisitionContact(ContactInfo acquisitionContact) {
    this.acquisitionContact = acquisitionContact;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getContentContact()
   */
  @Override
  @XmlElement(type = ContactInfoJpa.class, name = "contentContact")
  public ContactInfo getContentContact() {
    return contentContact;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setContentContact(com.wci
   * .umls.server.model.meta.ContactInfo)
   */
  @Override
  public void setContentContact(ContactInfo contentContact) {
    this.contentContact = contentContact;
  }

  /**
   * Indicates whether or not polyhierarchy is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isPolyhierarchy() {
    return polyhierarchy;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setPolyhierarchy(boolean)
   */
  @Override
  public void setPolyhierarchy(boolean polyhierarchy) {
    this.polyhierarchy = polyhierarchy;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getCurrentVersion()
   */
  @Override
  @XmlElement(type = TerminologyJpa.class, name = "currentVersion")
  public Terminology getCurrentVersion() {
    return currentVersion;
  }

  /**
   * Sets the current version.
   *
   * @param currentVersion the current version
   */
  public void setCurrentVersion(Terminology currentVersion) {
    this.currentVersion = currentVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getFamily()
   */
  @Override
  public String getFamily() {
    return family;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setFamily(java.lang.String)
   */
  @Override
  public void setFamily(String family) {
    this.family = family;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getHierarchicalName()
   */
  @Override
  public String getHierarchicalName() {
    return hierarchicalName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setHierarchicalName(java
   * .lang.String)
   */
  @Override
  public void setHierarchicalName(String hierarchicalName) {
    this.hierarchicalName = hierarchicalName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.helpers.HasLanguage#getLanguage()
   */
  @Override
  @XmlElement(type = LanguageJpa.class, name = "language")
  public Language getLanguage() {
    return language;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.helpers.HasLanguage#setLanguage(com.wci.umls.server
   * .model.meta.Language)
   */
  @Override
  public void setLanguage(Language language) {
    this.language = language;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getLicenseContact()
   */
  @Override
  @XmlElement(type = ContactInfoJpa.class, name = "licenseContact")
  public ContactInfo getLicenseContact() {
    return licenseContact;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setLicenseContact(com.wci
   * .umls.server.model.meta.ContactInfo)
   */
  @Override
  public void setLicenseContact(ContactInfo licenseContact) {
    this.licenseContact = licenseContact;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getPreferredName()
   */
  @Override
  public String getPreferredName() {
    return preferredName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setPreferredName(java.lang
   * .String)
   */
  @Override
  public void setPreferredName(String preferredName) {
    this.preferredName = preferredName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getPreviousVersion()
   */
  @Override
  @XmlElement(type = TerminologyJpa.class, name = "previousVersion")
  public Terminology getPreviousVersion() {
    return previousVersion;
  }

  /**
   * Sets the previous version.
   *
   * @param previousVersion the previous version
   */
  public void setPreviousVersion(Terminology previousVersion) {
    this.previousVersion = previousVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getRestrictionLevel()
   */
  @Override
  public int getRestrictionLevel() {
    return restrictionLevel;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setRestrictionLevel(int)
   */
  @Override
  public void setRestrictionLevel(int restrictionLevel) {
    this.restrictionLevel = restrictionLevel;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getShortName()
   */
  @Override
  public String getShortName() {
    return shortName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setShortName(java.lang.String
   * )
   */
  @Override
  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#getSynonymousNames()
   */
  @Override
  public List<String> getSynonymousNames() {
    return synonymousNames;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.model.meta.RootTerminology#setSynonymousNames(java.
   * util.List)
   */
  @Override
  public void setSynonymousNames(List<String> synonymousNames) {
    this.synonymousNames = synonymousNames;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.model.meta.RootTerminology#isPolyhierachy()
   */
  @Override
  public boolean isPolyhierachy() {
    return polyhierarchy;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.jpa.meta.AbstractAbbreviation#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime
            * result
            + ((acquisitionContact == null) ? 0 : acquisitionContact.hashCode());
    result =
        prime * result
            + ((contentContact == null) ? 0 : contentContact.hashCode());
    result = prime * result + ((family == null) ? 0 : family.hashCode());
    result =
        prime * result
            + ((hierarchicalName == null) ? 0 : hierarchicalName.hashCode());
    result = prime * result + ((language == null) ? 0 : language.hashCode());
    result =
        prime * result
            + ((licenseContact == null) ? 0 : licenseContact.hashCode());
    result = prime * result + (polyhierarchy ? 1231 : 1237);
    result =
        prime * result
            + ((preferredName == null) ? 0 : preferredName.hashCode());
    result = prime * result + restrictionLevel;
    result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
    result =
        prime * result
            + ((synonymousNames == null) ? 0 : synonymousNames.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.jpa.meta.AbstractAbbreviation#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    RootTerminologyJpa other = (RootTerminologyJpa) obj;
    if (acquisitionContact == null) {
      if (other.acquisitionContact != null)
        return false;
    } else if (!acquisitionContact.equals(other.acquisitionContact))
      return false;
    if (contentContact == null) {
      if (other.contentContact != null)
        return false;
    } else if (!contentContact.equals(other.contentContact))
      return false;
    if (family == null) {
      if (other.family != null)
        return false;
    } else if (!family.equals(other.family))
      return false;
    if (hierarchicalName == null) {
      if (other.hierarchicalName != null)
        return false;
    } else if (!hierarchicalName.equals(other.hierarchicalName))
      return false;
    if (language == null) {
      if (other.language != null)
        return false;
    } else if (!language.equals(other.language))
      return false;
    if (licenseContact == null) {
      if (other.licenseContact != null)
        return false;
    } else if (!licenseContact.equals(other.licenseContact))
      return false;
    if (polyhierarchy != other.polyhierarchy)
      return false;
    if (preferredName == null) {
      if (other.preferredName != null)
        return false;
    } else if (!preferredName.equals(other.preferredName))
      return false;
    if (restrictionLevel != other.restrictionLevel)
      return false;
    if (shortName == null) {
      if (other.shortName != null)
        return false;
    } else if (!shortName.equals(other.shortName))
      return false;
    if (synonymousNames == null) {
      if (other.synonymousNames != null)
        return false;
    } else if (!synonymousNames.equals(other.synonymousNames))
      return false;
    return true;
  }

}
