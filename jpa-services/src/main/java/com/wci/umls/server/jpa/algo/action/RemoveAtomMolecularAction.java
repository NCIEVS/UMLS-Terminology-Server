/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.action;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * A molecular action for removing an atom.
 */
public class RemoveAtomMolecularAction extends AbstractMolecularAction {

  /** The atom. */
  private Atom atom;

  /** The atom id. */
  private Long atomId;

  /**
   * Instantiates an empty {@link RemoveAtomMolecularAction}.
   *
   * @throws Exception the exception
   */
  public RemoveAtomMolecularAction() throws Exception {
    super();
    // n/a
  }

  /**
   * Returns the atom.
   *
   * @return the atom
   */
  public Atom getAtom() {
    return atom;
  }

  /**
   * Returns the atom id.
   *
   * @return the atom id
   */
  public Long getAtomId() {
    return atomId;
  }

  /**
   * Sets the atom id.
   *
   * @param atomId the atom id
   */
  public void setAtomId(Long atomId) {
    this.atomId = atomId;
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    final ValidationResult validationResult = new ValidationResultJpa();

    // Perform action specific validation - n/a

    // Exists check
    for (final Atom atm : getConcept().getAtoms()) {
      if (atm.getId().equals(atomId)) {
        atom = atm;
      }
    }
    if (atom == null) {
      rollback();
      throw new LocalException("Atom to remove does not exist");
    }

    return validationResult;
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    //
    // Perform the action (contentService will create atomic actions for CRUD
    // operations)
    //

    // remove the atom from the concept and update
    getConcept().getAtoms().remove(atom);
    getConcept().setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
    updateConcept(getConcept());

    // remove the atom
    removeAtom(atom.getId());

    // log the REST call
    addLogEntry(getUserName(), getProject().getId(), getConcept().getId(),
        getName() + " " + atom.getName() + " from concept "
            + getConcept().getTerminologyId());

  }

}